'use strict';

const fs = require('fs');
const path = require('path');
const db = require('../src/db');
const { validatePackage, publishPackage, latestPackage } = require('../src/packages');

/**
 * Publishes an OFFICIAL content package from a JSON file.
 *
 * Usage:
 *   node scripts/publish-official.js [path/to/package.json] [--version N]
 *
 * The file must contain `{ packageVersion, kind: "OFFICIAL", fullSnapshot, nodes: [...] }`.
 * A package that declares `fullSnapshot: true` replaces the whole official library on every
 * device, so it must contain the complete content set.
 */
async function main() {
  const args = process.argv.slice(2);
  const versionFlagIndex = args.indexOf('--version');
  const overrideVersion = versionFlagIndex >= 0 ? Number.parseInt(args[versionFlagIndex + 1], 10) : null;
  const filePath = args.find((arg, index) => !arg.startsWith('--') && index !== versionFlagIndex + 1);

  const resolved = path.resolve(filePath || path.join(__dirname, '..', 'content', 'official.sample.json'));
  if (!fs.existsSync(resolved)) {
    console.error(`[publish] file not found: ${resolved}`);
    process.exit(1);
  }

  const raw = JSON.parse(fs.readFileSync(resolved, 'utf8'));
  const current = await latestPackage(db, 'OFFICIAL');
  const currentVersion = current ? Number(current.version) : 0;

  const packageVersion = Number.isInteger(overrideVersion)
    ? overrideVersion
    : Math.max(Number(raw.packageVersion || 0), currentVersion + 1);

  const result = validatePackage({ ...raw, packageVersion, kind: 'OFFICIAL' }, 'OFFICIAL');
  if (!result.ok) {
    console.error(`[publish] invalid package: ${result.error}`);
    process.exit(1);
  }

  await publishPackage(db, result.package);
  console.log(
    `[publish] published OFFICIAL version ${result.package.packageVersion} ` +
      `(${result.package.nodes.length} nodes, checksum ${result.package.checksum.slice(0, 12)}…, ` +
      `fullSnapshot=${result.package.fullSnapshot})`,
  );
  await db.pool.end();
}

main().catch((error) => {
  console.error('[publish] failed:', error.message);
  process.exit(1);
});
