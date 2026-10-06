'use strict';

const rateLimit = require('express-rate-limit');
const { config } = require('../config');
const { query } = require('../db');

/**
 * Rate limiting is enforced on the server. Android-side limits would be advisory only,
 * because clients can be modified or bypassed.
 *
 * Two layers are used:
 * 1. an in-memory limiter for burst protection per IP, and
 * 2. database-backed per-account quotas for Community submissions, so limits hold even
 *    across multiple server instances.
 */

const globalLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: config.limits.globalPerMinute,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests — slow down.' },
});

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: config.limits.authPerWindow,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many authentication attempts. Try again later.' },
});

const submissionLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: config.limits.submissionsPerHour * 4,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req) => req.user?.id ?? req.ip,
  message: { error: 'Too many submissions — try again later.' },
});

/** Per-account quotas that cannot be bypassed by changing clients. */
async function enforceSubmissionQuota(userId) {
  const { rows } = await query(
    `SELECT
       count(*) FILTER (WHERE created_at > now() - interval '1 hour') AS last_hour,
       count(*) FILTER (WHERE created_at > now() - interval '1 day')  AS last_day,
       count(*) FILTER (WHERE status = 'PENDING')                     AS pending
     FROM community_submissions
     WHERE user_id = $1`,
    [userId],
  );

  const stats = rows[0] ?? { last_hour: '0', last_day: '0', pending: '0' };
  const lastHour = Number(stats.last_hour);
  const lastDay = Number(stats.last_day);
  const pending = Number(stats.pending);

  if (lastHour >= config.limits.submissionsPerHour) {
    return { ok: false, error: `Hourly submission limit reached (${config.limits.submissionsPerHour}).` };
  }
  if (lastDay >= config.limits.submissionsPerDay) {
    return { ok: false, error: `Daily submission limit reached (${config.limits.submissionsPerDay}).` };
  }
  if (pending >= config.limits.maxPending) {
    return { ok: false, error: `Too many pending submissions (${config.limits.maxPending}). Wait for review.` };
  }
  return { ok: true };
}

module.exports = {
  globalLimiter,
  authLimiter,
  submissionLimiter,
  enforceSubmissionQuota,
};
