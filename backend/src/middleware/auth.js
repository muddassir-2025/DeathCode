'use strict';

const jwt = require('jsonwebtoken');
const { config } = require('../config');
const { query } = require('../db');

function signToken(user) {
  return jwt.sign(
    { sub: user.id, email: user.email },
    config.jwtSecret,
    { expiresIn: config.jwtExpiresIn },
  );
}

/** Verifies the bearer token and loads the account from the database. */
async function authenticate(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7).trim() : '';

  if (!token) {
    return res.status(401).json({ error: 'Authentication required' });
  }

  let payload;
  try {
    payload = jwt.verify(token, config.jwtSecret);
  } catch (error) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }

  try {
    const { rows } = await query(
      'SELECT id, email, is_admin FROM users WHERE id = $1 LIMIT 1',
      [payload.sub],
    );
    if (!rows[0]) {
      return res.status(401).json({ error: 'Account no longer exists' });
    }
    // The authorization decision is always made from the database, never from the token,
    // so a client cannot grant itself admin access.
    req.user = rows[0];
    return next();
  } catch (error) {
    console.error('[auth] lookup failed:', error.message);
    return res.status(503).json({ error: 'Authentication temporarily unavailable' });
  }
}

/** Admin-only guard. Runs [authenticate] first, then re-checks the flag server side. */
function requireAdmin(req, res, next) {
  return authenticate(req, res, () => {
    if (!req.user || req.user.is_admin !== true) {
      return res.status(403).json({ error: 'Administrator access required' });
    }
    return next();
  });
}

module.exports = { signToken, authenticate, requireAdmin };
