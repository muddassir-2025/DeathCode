'use strict';

const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');

const { config, assertStartupConfig } = require('./config');
const db = require('./db');
const { globalLimiter } = require('./middleware/rateLimit');

const contentRoutes = require('./routes/content');
const authRoutes = require('./routes/auth');
const communityRoutes = require('./routes/community');
const adminRoutes = require('./routes/admin');

const app = express();

app.disable('x-powered-by');
// Render terminates TLS in front of the container.
app.set('trust proxy', 1);

app.use(
  cors({
    origin: config.corsOrigins.includes('*') ? true : config.corsOrigins,
  }),
);
app.use(express.json({ limit: config.limits.maxBodySize }));
app.use(globalLimiter);

/**
 * Liveness/readiness probe used by Render.
 *
 * Reports database reachability without failing the probe outright, so a temporary Neon
 * cold start does not cause the service to be torn down.
 */
app.get('/health', async (_req, res) => {
  let database = false;
  try {
    await db.query('SELECT 1');
    database = true;
  } catch (error) {
    console.error('[health] database unreachable:', error.message);
  }
  res.json({
    status: 'ok',
    database,
    configured: config.isConfigured,
    time: new Date().toISOString(),
  });
});

app.use('/api/content', contentRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/community', communityRoutes);
app.use('/api/admin', adminRoutes);

app.use((_req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// eslint-disable-next-line no-unused-vars
app.use((error, _req, res, _next) => {
  if (error?.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'Request body is not valid JSON' });
  }
  if (error?.type === 'entity.too.large') {
    return res.status(413).json({ error: 'Request body is too large' });
  }
  console.error('[server] unhandled error:', error?.message || error);
  return res.status(500).json({ error: 'Internal server error' });
});

/**
 * Creates or promotes the bootstrap administrator.
 *
 * Admin rights live in the database and are re-checked on every admin request; the client
 * never decides access for itself.
 */
async function bootstrapAdmin() {
  if (!config.bootstrapAdminEmail || !config.bootstrapAdminPassword) return;

  try {
    const { rows } = await db.query(
      'SELECT id, is_admin FROM users WHERE email = $1 LIMIT 1',
      [config.bootstrapAdminEmail],
    );

    if (rows[0]) {
      if (!rows[0].is_admin) {
        await db.query('UPDATE users SET is_admin = true WHERE id = $1', [rows[0].id]);
        console.log('[bootstrap] promoted existing account to administrator');
      }
      return;
    }

    const passwordHash = await bcrypt.hash(config.bootstrapAdminPassword, 12);
    await db.query(
      'INSERT INTO users (email, password_hash, is_admin, display_name) VALUES ($1, $2, true, $3)',
      [config.bootstrapAdminEmail, passwordHash, 'Super Admin'],
    );
    console.log('[bootstrap] created administrator account');
  } catch (error) {
    console.error('[bootstrap] failed:', error.message);
  }
}

async function main() {
  const problems = assertStartupConfig();
  if (problems.length > 0) {
    console.error('Death Code API cannot start:');
    for (const problem of problems) console.error(`  - ${problem}`);
    console.error('Copy backend/.env.example to backend/.env and fill it in.');
    process.exit(1);
  }

  await bootstrapAdmin();

  app.listen(config.port, () => {
    console.log(`[server] Death Code API listening on port ${config.port}`);
  });
}

if (require.main === module) {
  main().catch((error) => {
    console.error('[server] fatal:', error);
    process.exit(1);
  });
}

module.exports = { app };
