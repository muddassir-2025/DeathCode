'use strict';

const path = require('path');

// Load backend/.env (the Neon connection string lives here, never in the Android app).
require('dotenv').config({ path: path.resolve(__dirname, '..', '.env') });

function int(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return fallback;
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

const databaseUrl = process.env.DATABASE_URL || process.env.DB_URL || process.env.NEON_DATABASE_URL || '';

const config = {
  port: int('PORT', 8080),
  databaseUrl,
  // Neon and most managed Postgres providers require TLS.
  databaseSsl: process.env.DATABASE_SSL === 'false'
    ? false
    : !/localhost|127\.0\.0\.1/.test(databaseUrl),

  jwtSecret: process.env.JWT_SECRET || '',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '30d',

  // Bootstrap admin, created on first start when no admin exists.
  bootstrapAdminEmail: (process.env.ADMIN_EMAIL || '').trim().toLowerCase(),
  bootstrapAdminPassword: process.env.ADMIN_PASSWORD || '',

  corsOrigins: (process.env.CORS_ORIGINS || '*')
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean),

  limits: {
    // Requests per minute per IP.
    globalPerMinute: int('RATE_LIMIT_GLOBAL_PER_MINUTE', 180),
    // Auth attempts per 15 minutes per IP.
    authPerWindow: int('RATE_LIMIT_AUTH_PER_WINDOW', 12),
    // Community submissions per hour per account (server side, DB backed).
    submissionsPerHour: int('RATE_LIMIT_SUBMISSIONS_PER_HOUR', 5),
    // Community submissions per day per account (server side, DB backed).
    submissionsPerDay: int('RATE_LIMIT_SUBMISSIONS_PER_DAY', 20),
    // Maximum simultaneously pending submissions per account.
    maxPending: int('RATE_LIMIT_MAX_PENDING', 10),
    // Maximum markdown characters accepted in one submission or package node.
    maxMarkdownChars: int('MAX_MARKDOWN_CHARS', 200000),
    // Maximum nodes in one published content package.
    maxPackageNodes: int('MAX_PACKAGE_NODES', 5000),
    // Maximum JSON body size.
    maxBodySize: process.env.MAX_BODY_SIZE || '2mb',
  },
};

config.isConfigured = Boolean(config.databaseUrl);

/** Fail fast on missing critical secrets, but only when the server is actually starting. */
function assertStartupConfig() {
  const problems = [];
  if (!config.databaseUrl) {
    problems.push('DB_URL (or DATABASE_URL) is missing — add your Neon connection string.');
  }
  if (!config.jwtSecret || config.jwtSecret.length < 32) {
    problems.push('JWT_SECRET is missing or too short (use at least 32 random characters).');
  }
  return problems;
}

module.exports = { config, assertStartupConfig };
