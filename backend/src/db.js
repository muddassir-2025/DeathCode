'use strict';

const { Pool } = require('pg');
const { config } = require('./config');

/**
 * Neon PostgreSQL connection pool.
 *
 * Only this process ever holds database credentials: the Android app talks to the REST API
 * over HTTPS and never connects to PostgreSQL directly.
 */
const pool = new Pool({
  connectionString: config.databaseUrl,
  ssl: config.databaseSsl ? { rejectUnauthorized: false } : false,
  max: 5,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 15000,
});

pool.on('error', (error) => {
  // Keep the process alive; the pool discards broken clients automatically.
  console.error('[db] unexpected pool error:', error.message);
});

async function query(text, params) {
  return pool.query(text, params);
}

/** Runs `fn` inside a transaction and rolls back on any error. */
async function withTransaction(fn) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    try {
      await client.query('ROLLBACK');
    } catch (rollbackError) {
      console.error('[db] rollback failed:', rollbackError.message);
    }
    throw error;
  } finally {
    client.release();
  }
}

module.exports = { pool, query, withTransaction };
