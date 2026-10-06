'use strict';

const express = require('express');
const db = require('../db');
const { config } = require('../config');
const { requireAdmin } = require('../middleware/auth');
const {
  validatePackage,
  publishPackage,
  latestPackage,
} = require('../packages');

const router = express.Router();

// Every route below is admin-only. The check is performed server side from the database
// flag, so a modified client can never publish official content or moderate submissions.
router.use(requireAdmin);

/**
 * Publishes a new OFFICIAL content version.
 *
 * Body:
 * ```
 * { packageVersion, fullSnapshot, nodes: [ { id, parentId, title, markdown, ... } ] }
 * ```
 * The server recomputes the checksum; a client-supplied checksum is only accepted when it
 * matches. Drafts live on the admin side — only this endpoint makes content public.
 */
router.post('/official/publish', async (req, res) => {
  const result = validatePackage(req.body, 'OFFICIAL');
  if (!result.ok) return res.status(400).json({ error: result.error });

  const pkg = result.package;

  try {
    const current = await latestPackage(db, 'OFFICIAL');
    const currentVersion = current ? Number(current.version) : 0;
    if (pkg.packageVersion <= currentVersion) {
      return res.status(409).json({
        error: `Version ${pkg.packageVersion} is not newer than the published version ${currentVersion}`,
      });
    }

    await publishPackage(db, pkg);
    return res.status(201).json({
      version: pkg.packageVersion,
      checksum: pkg.checksum,
      nodeCount: pkg.nodes.length,
      fullSnapshot: pkg.fullSnapshot,
    });
  } catch (error) {
    console.error('[admin] publish official failed:', error.message);
    return res.status(503).json({ error: 'Publishing temporarily unavailable' });
  }
});

/** Moderation queue. */
router.get('/community/submissions', async (req, res) => {
  const status = String(req.query.status || '').toUpperCase();
  const allowed = ['PENDING', 'APPROVED', 'REJECTED'];

  try {
    const params = [];
    let filter = '';
    if (allowed.includes(status)) {
      params.push(status);
      filter = `WHERE s.status = $1`;
    }

    const { rows } = await db.query(
      `SELECT s.id, s.title, s.markdown, s.syntax, s.language, s.keywords,
              s.status, s.review_message, s.created_at, s.updated_at,
              u.email AS author_email
         FROM community_submissions s
         JOIN users u ON u.id = s.user_id
         ${filter}
        ORDER BY s.created_at DESC
        LIMIT 200`,
      params,
    );

    return res.json({
      submissions: rows.map((row) => ({
        id: row.id,
        title: row.title,
        markdown: row.markdown,
        syntax: row.syntax,
        language: row.language,
        keywords: row.keywords ?? [],
        status: row.status,
        message: row.review_message,
        authorEmail: row.author_email,
        createdAt: new Date(row.created_at).getTime(),
        updatedAt: new Date(row.updated_at).getTime(),
      })),
    });
  } catch (error) {
    console.error('[admin] list submissions failed:', error.message);
    return res.status(503).json({ error: 'Service unavailable' });
  }
});

/** Approve or reject a submission, optionally leaving a note for the author. */
async function moderate(req, res, status) {
  const message = req.body?.message ? String(req.body.message).slice(0, 500) : null;
  try {
    const { rows } = await db.query(
      `UPDATE community_submissions
          SET status = $1, review_message = $2, updated_at = now()
        WHERE id = $3
        RETURNING id, status`,
      [status, message, req.params.id],
    );
    if (!rows[0]) return res.status(404).json({ error: 'Submission not found' });
    return res.json({ id: rows[0].id, status: rows[0].status, message });
  } catch (error) {
    if (error.code === '22P02') return res.status(400).json({ error: 'Invalid submission id' });
    console.error('[admin] moderation failed:', error.message);
    return res.status(503).json({ error: 'Service unavailable' });
  }
}

router.post('/community/submissions/:id/approve', (req, res) => moderate(req, res, 'APPROVED'));
router.post('/community/submissions/:id/reject', (req, res) => moderate(req, res, 'REJECTED'));

/** Remove content that is already public. */
router.delete('/community/submissions/:id', async (req, res) => {
  try {
    const { rowCount } = await db.query(
      'DELETE FROM community_submissions WHERE id = $1',
      [req.params.id],
    );
    if (rowCount === 0) return res.status(404).json({ error: 'Submission not found' });
    return res.status(204).send();
  } catch (error) {
    if (error.code === '22P02') return res.status(400).json({ error: 'Invalid submission id' });
    console.error('[admin] delete failed:', error.message);
    return res.status(503).json({ error: 'Service unavailable' });
  }
});

/**
 * Rebuilds and publishes the COMMUNITY package from every approved submission.
 *
 * Clients then pick it up through the normal synchronization flow, so approved content is
 * searchable offline.
 */
router.post('/community/publish', async (req, res) => {
  try {
    const { rows } = await db.query(
      `SELECT id, title, markdown, syntax, language, keywords, updated_at
         FROM community_submissions
        WHERE status = 'APPROVED'
        ORDER BY created_at ASC
        LIMIT $1`,
      [config.limits.maxPackageNodes],
    );

    if (rows.length === 0) {
      return res.status(400).json({ error: 'There is no approved content to publish' });
    }

    const nodes = rows.map((row, index) => ({
      id: `community-${row.id}`,
      parentId: null,
      title: row.title,
      slug: `community-${row.id}`,
      markdown: row.markdown,
      syntax: row.syntax,
      notes: `Contributed by the community · reviewed ${new Date(row.updated_at).toISOString().slice(0, 10)}`,
      language: row.language,
      keywords: row.keywords ?? [],
      sortOrder: index,
    }));

    const current = await latestPackage(db, 'COMMUNITY');
    const version = (current ? Number(current.version) : 0) + 1;

    const result = validatePackage(
      { packageVersion: version, kind: 'COMMUNITY', fullSnapshot: true, nodes },
      'COMMUNITY',
    );
    if (!result.ok) return res.status(400).json({ error: result.error });

    await publishPackage(db, result.package);
    return res.status(201).json({
      version,
      checksum: result.package.checksum,
      nodeCount: nodes.length,
    });
  } catch (error) {
    console.error('[admin] publish community failed:', error.message);
    return res.status(503).json({ error: 'Publishing temporarily unavailable' });
  }
});

module.exports = router;
