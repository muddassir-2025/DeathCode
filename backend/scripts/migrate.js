'use strict';

const fs = require('fs');
const path = require('path');
const { config } = require('../src/config');
const { pool } = require('../src/db');

async function main() {
  if (!config.databaseUrl) {
    console.error('DB_URL (or DATABASE_URL) is not set. Add your Neon connection string to backend/.env');
    process.exit(1);
  }

  const schemaPath = path.join(__dirname, '..', 'db', 'schema.sql');
  const sql = fs.readFileSync(schemaPath, 'utf8');

  console.log('[migrate] applying schema...');
  await pool.query(sql);
  console.log('[migrate] schema is up to date');
  await pool.end();
}

main().catch((error) => {
  console.error('[migrate] failed:', error.message);
  process.exit(1);
});
