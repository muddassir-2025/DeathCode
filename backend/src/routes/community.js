'use strict';

const express = require('express');
const db = require('../db');
const { config } = require('../config');
const { authenticate } = require('../middleware/auth');
const { submissionLimiter, enforceSubmissionQuota } = require('../middleware/rateLimit');

const router = express.Router();

function serialize(row) {
  return {
    id: row.id,
    title: row.title,
    markdown: row.markdown,
    syntax: row.syntax,
    language: row.language,
    keywords: row.keywords ?? [],
    status: row.status,
    message: row.review_message,
    createdAt: new Date(row.created_at).getTime(),
    updatedAt: new Date(row.updated_at).getTime(),
  };
}

function validateSubmission(body) {
  const title = String(body?.title || '').trim();
  const markdown = String(body?.markdown || '');

  if (title.length < 3 || title.length > 200) {
    return { ok: false, error: 'Title must be between 3 and 200 characters' };
  }
  if (markdown.trim().length < 20) {
    return { ok: false, error: 'Content is too short to be useful' };
  }
  if (markdown.length > config.limits.maxMarkdownChars) {
    return { ok: false, error: `Content exceeds ${config.limits.maxMarkdownChars} characters` };
  }

  const keywords = Array.isArray(body?.keywords)
    ? body.keywords.map((k) => String(k).trim().toLowerCase()).filter(Boolean).slice(0, 30)
    : [];

  return {
    ok: true,
    submission: {
      title,
      markdown,
      syntax: body?.syntax ? String(body.syntax).slice(0, config.limits.maxMarkdownChars) : null,
      language: body?.language ? String(body.language).slice(0, 40) : null,
      keywords,
    },
  };
}

/**
 * Submitting a contribution.
 *
 * Everything here is enforced server side: authentication, payload validation, per-account
 * hourly/daily quotas and the maximum number of pending submissions. Approved content only
 * becomes public when an administrator publishes it.
 */
router.post('/submissions', authenticate, submissionLimiter, async (req, res) => {
  const validated = validateSubmission(req.body);
  if (!validated.ok) return res.status(400).json({ error: validated.error });

  try {
    const quota = await enforceSubmissionQuota(req.user.id);
    if (!quota.ok) return res.status(429).json({ error: quota.error });

    const { submission } = validated;
    const { rows } = await db.query(
      `INSERT INTO community_submissions
         (user_id, title, markdown, syntax, language, keywords, status)
       VALUES ($1, $2, $3, $4, $5, $6, 'PENDING')
       RETURNING id, status, review_message`,
      [
        req.user.id,
        submission.title,
        submission.markdown,
        submission.syntax,
        submission.language,
        submission.keywords,
      ],
    );

    return res.status(201).json({
      id: rows[0].id,
      status: rows[0].status,
      message: rows[0].review_message || 'Submitted for review',
    });
  } catch (error) {
    console.error('[community] submission failed:', error.message);
    return res.status(503).json({ error: 'Submission service unavailable' });
  }
});

/** The caller's own submissions and their moderation state. */
router.get('/submissions/mine', authenticate, async (req, res) => {
  try {
    const { rows } = await db.query(
      `SELECT id, title, markdown, syntax, language, keywords, status, review_message,
              created_at, updated_at
         FROM community_submissions
        WHERE user_id = $1
        ORDER BY updated_at DESC
        LIMIT 100`,
      [req.user.id],
    );
    return res.json({ submissions: rows.map(serialize) });
  } catch (error) {
    console.error('[community] list failed:', error.message);
    return res.status(503).json({ error: 'Service unavailable' });
  }
});

/** Withdraw a submission that has not been approved yet. */
router.delete('/submissions/:id', authenticate, async (req, res) => {
  try {
    const { rowCount } = await db.query(
      `DELETE FROM community_submissions
        WHERE id = $1 AND user_id = $2 AND status <> 'APPROVED'`,
      [req.params.id, req.user.id],
    );
    if (rowCount === 0) {
      return res.status(404).json({ error: 'Submission not found or already approved' });
    }
    return res.status(204).send();
  } catch (error) {
    if (error.code === '22P02') return res.status(400).json({ error: 'Invalid submission id' });
    console.error('[community] delete failed:', error.message);
    return res.status(503).json({ error: 'Service unavailable' });
  }
});

module.exports = router;
