'use strict';

const crypto = require('crypto');
const { config } = require('./config');

/**
 * Content package handling.
 *
 * The checksum algorithm here is the exact counterpart of the Android client's
 * `ContentChecksum`, so a package published by the server validates on the device and a
 * tampered download is rejected.
 *
 * Canonical form: nodes sorted by id, each node's fields joined with U+001F, nodes joined
 * with U+001E, hashed with SHA-256 (hex, UTF-8).
 */
const FIELD_SEPARATOR = '\u001f';
const NODE_SEPARATOR = '\u001e';

function normalizeNode(raw) {
  const keywords = Array.isArray(raw.keywords)
    ? raw.keywords.map((k) => String(k).trim().toLowerCase()).filter(Boolean)
    : [];

  return {
    id: String(raw.id),
    parentId: raw.parentId === undefined || raw.parentId === null ? null : String(raw.parentId),
    title: String(raw.title ?? '').trim(),
    slug: String(raw.slug ?? raw.id),
    markdown: String(raw.markdown ?? ''),
    syntax: raw.syntax === undefined || raw.syntax === null ? null : String(raw.syntax),
    notes: raw.notes === undefined || raw.notes === null ? null : String(raw.notes),
    language: raw.language === undefined || raw.language === null ? null : String(raw.language),
    keywords,
    sortOrder: Number.isFinite(raw.sortOrder) ? Number(raw.sortOrder) : 0,
  };
}

function computeChecksum(nodes) {
  const canonical = [...nodes]
    .sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
    .map((node) =>
      [
        node.id,
        node.parentId ?? '',
        node.title,
        node.slug ?? '',
        node.markdown ?? '',
        node.syntax ?? '',
        node.notes ?? '',
        node.language ?? '',
        (node.keywords ?? []).join(','),
        String(node.sortOrder ?? 0),
      ].join(FIELD_SEPARATOR),
    )
    .join(NODE_SEPARATOR);

  return crypto.createHash('sha256').update(canonical, 'utf8').digest('hex');
}

/**
 * Validates a package submitted for publication.
 * Returns `{ ok: true, package }` or `{ ok: false, error }`.
 */
function validatePackage(body, expectedKind) {
  if (!body || typeof body !== 'object') return { ok: false, error: 'Body must be a JSON object' };

  const kind = String(body.kind ?? expectedKind ?? 'OFFICIAL').toUpperCase();
  if (expectedKind && kind !== expectedKind) {
    return { ok: false, error: `Package kind must be ${expectedKind}` };
  }

  const version = Number.parseInt(body.packageVersion ?? body.version, 10);
  if (!Number.isInteger(version) || version < 1) {
    return { ok: false, error: 'packageVersion must be a positive integer' };
  }

  if (!Array.isArray(body.nodes) || body.nodes.length === 0) {
    return { ok: false, error: 'Package must contain at least one node' };
  }
  if (body.nodes.length > config.limits.maxPackageNodes) {
    return { ok: false, error: `Package exceeds ${config.limits.maxPackageNodes} nodes` };
  }

  const nodes = body.nodes.map(normalizeNode);
  const ids = new Set();
  for (const node of nodes) {
    if (!node.id) return { ok: false, error: 'Every node needs an id' };
    if (ids.has(node.id)) return { ok: false, error: `Duplicate node id: ${node.id}` };
    ids.add(node.id);
    if (!node.title) return { ok: false, error: `Node ${node.id} needs a title` };
    if (node.markdown.length > config.limits.maxMarkdownChars) {
      return { ok: false, error: `Node ${node.id} markdown exceeds the size limit` };
    }
  }

  // A full snapshot must reference only its own nodes; a partial publication may attach to
  // content that already exists on the devices.
  const fullSnapshot = body.fullSnapshot !== false;
  if (fullSnapshot) {
    for (const node of nodes) {
      if (node.parentId !== null && !ids.has(node.parentId)) {
        return { ok: false, error: `Node ${node.id} references missing parent ${node.parentId}` };
      }
    }
  }

  // Cycle detection over the parent graph.
  const byId = new Map(nodes.map((node) => [node.id, node]));
  for (const node of nodes) {
    const seen = new Set();
    let current = node;
    while (current && current.parentId) {
      if (seen.has(current.id)) return { ok: false, error: `Cycle detected at node ${current.id}` };
      seen.add(current.id);
      current = byId.get(current.parentId);
    }
  }

  const checksum = computeChecksum(nodes);
  if (body.checksum && String(body.checksum).toLowerCase() !== checksum) {
    return { ok: false, error: 'Checksum does not match the package contents' };
  }

  return {
    ok: true,
    package: {
      packageVersion: version,
      generatedAt: Date.now(),
      checksum,
      kind,
      fullSnapshot,
      nodes,
    },
  };
}

/** Reads the newest published package for a kind. */
async function latestPackage(db, kind) {
  const { rows } = await db.query(
    `SELECT kind, version, checksum, generated_at, full_snapshot, nodes
       FROM content_packages
      WHERE kind = $1
      ORDER BY version DESC
      LIMIT 1`,
    [kind],
  );
  return rows[0] ?? null;
}

async function publishPackage(db, pkg) {
  await db.query(
    `INSERT INTO content_packages (kind, version, checksum, generated_at, full_snapshot, nodes)
     VALUES ($1, $2, $3, now(), $4, $5::jsonb)
     ON CONFLICT (kind, version) DO UPDATE
        SET checksum = EXCLUDED.checksum,
            generated_at = now(),
            full_snapshot = EXCLUDED.full_snapshot,
            nodes = EXCLUDED.nodes`,
    [pkg.kind, pkg.packageVersion, pkg.checksum, pkg.fullSnapshot, JSON.stringify(pkg.nodes)],
  );
}

/** Serializes a stored row into the wire format the Android client expects. */
function toWirePackage(row) {
  const nodes = typeof row.nodes === 'string' ? JSON.parse(row.nodes) : row.nodes;
  return {
    packageVersion: Number(row.version),
    generatedAt: new Date(row.generated_at).getTime(),
    checksum: row.checksum,
    kind: row.kind,
    fullSnapshot: row.full_snapshot !== false,
    nodes,
  };
}

module.exports = {
  computeChecksum,
  normalizeNode,
  validatePackage,
  latestPackage,
  publishPackage,
  toWirePackage,
};
