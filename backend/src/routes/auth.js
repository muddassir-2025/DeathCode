'use strict';

const express = require('express');
const bcrypt = require('bcryptjs');
const db = require('../db');
const { signToken } = require('../middleware/auth');
const { authLimiter } = require('../middleware/rateLimit');

const router = express.Router();

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

function normalizeEmail(value) {
  return String(value || '').trim().toLowerCase();
}

/**
 * Community account creation.
 *
 * Accounts exist only so community submissions can be attributed and moderated — the app
 * itself never requires a login.
 */
router.post('/signup', authLimiter, async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const password = String(req.body?.password || '');
  const displayName = req.body?.displayName ? String(req.body.displayName).slice(0, 80) : null;

  if (!EMAIL_PATTERN.test(email)) {
    return res.status(400).json({ error: 'A valid email address is required' });
  }
  if (password.length < MIN_PASSWORD_LENGTH) {
    return res.status(400).json({ error: `Password must be at least ${MIN_PASSWORD_LENGTH} characters` });
  }

  try {
    const passwordHash = await bcrypt.hash(password, 12);
    const { rows } = await db.query(
      `INSERT INTO users (email, password_hash, display_name)
       VALUES ($1, $2, $3)
       RETURNING id, email, is_admin`,
      [email, passwordHash, displayName],
    );
    const user = rows[0];
    return res.status(201).json({
      token: signToken(user),
      userId: user.id,
      email: user.email,
      isAdmin: user.is_admin === true,
    });
  } catch (error) {
    if (error.code === '23505' || error.code === '23000') {
      return res.status(409).json({ error: 'An account with that email already exists' });
    }
    if (error.code === '23514' || error.code === '22P02') {
      return res.status(400).json({ error: 'Invalid account details' });
    }
    console.error('[auth] signup failed:', error.message);
    return res.status(503).json({ error: 'Registration temporarily unavailable' });
  }
});

router.post('/login', authLimiter, async (req, res) => {
  const email = normalizeEmail(req.body?.email);
  const password = String(req.body?.password || '');

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required' });
  }

  try {
    const { rows } = await db.query(
      'SELECT id, email, password_hash, is_admin FROM users WHERE email = $1 LIMIT 1',
      [email],
    );
    const user = rows[0];

    // Always run a comparison so timing does not reveal whether the account exists.
    const hash = user?.password_hash || '$2a$12$invalidinvalidinvalidinvalidinvalidinvalidinvalidinvalidin';
    const valid = await bcrypt.compare(password, hash);

    if (!user || !valid) {
      return res.status(401).json({ error: 'Incorrect email or password' });
    }

    return res.json({
      token: signToken(user),
      userId: user.id,
      email: user.email,
      isAdmin: user.is_admin === true,
    });
  } catch (error) {
    console.error('[auth] login failed:', error.message);
    return res.status(503).json({ error: 'Sign in temporarily unavailable' });
  }
});

module.exports = router;
