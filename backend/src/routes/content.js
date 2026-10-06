'use strict';

const express = require('express');
const db = require('../db');
const { latestPackage, toWirePackage } = require('../packages');

const router = express.Router();

function kindFromParam(param) {
  const value = String(param || '').toLowerCase();
  if (value === 'official') return 'OFFICIAL';
  if (value === 'community') return 'COMMUNITY';
  return null;
}

/**
 * Latest published version metadata.
 *
 * A device that is already on this version (or newer) does not download the package at all.
 */
router.get('/:kind/manifest', async (req, res) => {
  const kind = kindFromParam(req.params.kind);
  if (!kind) return res.status(404).json({ error: 'Unknown content kind' });

  try {
    const row = await latestPackage(db, kind);
    if (!row) {
      return res.json({ version: 0, generatedAt: 0, checksum: '' });
    }
    return res.json({
      version: Number(row.version),
      generatedAt: new Date(row.generated_at).getTime(),
      checksum: row.checksum,
    });
  } catch (error) {
    console.error('[content] manifest failed:', error.message);
    return res.status(503).json({ error: 'Content service unavailable' });
  }
});

/** The newest published package. Validated by the client before it is applied. */
router.get('/:kind/package', async (req, res) => {
  const kind = kindFromParam(req.params.kind);
  if (!kind) return res.status(404).json({ error: 'Unknown content kind' });

  try {
    const row = await latestPackage(db, kind);
    if (!row) {
      return res.status(404).json({ error: 'No published content for this kind yet' });
    }
    return res.json(toWirePackage(row));
  } catch (error) {
    console.error('[content] package failed:', error.message);
    return res.status(503).json({ error: 'Content service unavailable' });
  }
});

/** A specific historical version, useful for pinned/delta synchronization later. */
router.get('/:kind/package/:version', async (req, res) => {
  const kind = kindFromParam(req.params.kind);
  const version = Number.parseInt(req.params.version, 10);
  if (!kind || !Number.isInteger(version)) {
    return res.status(404).json({ error: 'Unknown content kind or version' });
  }

  try {
    const { rows } = await db.query(
      `SELECT kind, version, checksum, generated_at, full_snapshot, nodes
         FROM content_packages
        WHERE kind = $1 AND version = $2
        LIMIT 1`,
      [kind, version],
    );
    if (!rows[0]) return res.status(404).json({ error: 'Version not found' });
    return res.json(toWirePackage(rows[0]));
  } catch (error) {
    console.error('[content] version fetch failed:', error.message);
    return res.status(503).json({ error: 'Content service unavailable' });
  }
});

module.exports = router;
