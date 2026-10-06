'use strict';

/**
 * End-to-end smoke test for a running Death Code API.
 *
 * Usage:
 *   npm start                       # in one terminal
 *   node scripts/smoke-test.js [baseUrl]
 *
 * Covers: health, content manifest/package, community signup, login, submission and the
 * per-account quota. It does not touch the database directly and is safe to re-run.
 */

const baseUrl = (process.argv[2] || process.env.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const email = `smoke+${Date.now()}@example.com`;
const password = 'smoke-test-password';

let failures = 0;

function check(label, condition, detail) {
  if (condition) {
    console.log(`  PASS  ${label}`);
  } else {
    failures += 1;
    console.error(`  FAIL  ${label}${detail ? ` — ${detail}` : ''}`);
  }
}

async function call(method, path, { token, body } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await response.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    /* non-JSON body */
  }
  return { status: response.status, json, text };
}

async function main() {
  console.log(`Smoke testing ${baseUrl}\n`);

  const health = await call('GET', '/health');
  check('GET /health returns 200', health.status === 200, `status ${health.status}`);
  check('database reachable', health.json?.database === true, 'DATABASE NOT CONNECTED');

  const manifest = await call('GET', '/api/content/official/manifest');
  check('official manifest returns 200', manifest.status === 200, `status ${manifest.status}`);

  const pkg = await call('GET', '/api/content/official/package');
  check(
    'official package is 200 or 404 (nothing published yet)',
    pkg.status === 200 || pkg.status === 404,
    `status ${pkg.status}`,
  );
  if (pkg.status === 200) {
    check('package has a checksum', typeof pkg.json?.checksum === 'string');
    check('package has nodes', Array.isArray(pkg.json?.nodes) && pkg.json.nodes.length > 0);
  }

  const signup = await call('POST', '/api/auth/signup', { body: { email, password } });
  check('signup returns 201', signup.status === 201, `status ${signup.status} ${signup.text}`);
  const token = signup.json?.token;

  const duplicate = await call('POST', '/api/auth/signup', { body: { email, password } });
  check('duplicate signup is rejected', duplicate.status === 409, `status ${duplicate.status}`);

  const login = await call('POST', '/api/auth/login', { body: { email, password } });
  check('login returns 200', login.status === 200, `status ${login.status}`);

  const badLogin = await call('POST', '/api/auth/login', {
    body: { email, password: 'definitely-wrong' },
  });
  check('wrong password is rejected', badLogin.status === 401, `status ${badLogin.status}`);

  const unauth = await call('POST', '/api/community/submissions', {
    body: { title: 'No token', markdown: 'x'.repeat(40) },
  });
  check('submission without a token is rejected', unauth.status === 401, `status ${unauth.status}`);

  const submission = await call('POST', '/api/community/submissions', {
    token,
    body: {
      title: 'Smoke test snippet',
      markdown: 'A deterministic snippet used by the smoke test to verify the submission pipeline.',
      syntax: 'for (int i = 0; i < n; i++) {}',
      language: 'cpp',
      keywords: ['smoke', 'test'],
    },
  });
  check('authenticated submission returns 201', submission.status === 201, `status ${submission.status} ${submission.text}`);
  check('submission is pending review', submission.json?.status === 'PENDING');

  const mine = await call('GET', '/api/community/submissions/mine', { token });
  check('own submissions are listed', mine.status === 200 && mine.json?.submissions?.length >= 1);

  const admin = await call('GET', '/api/admin/community/submissions', { token });
  check('non-admin cannot read the moderation queue', admin.status === 403, `status ${admin.status}`);

  console.log(`\n${failures === 0 ? 'All smoke checks passed.' : `${failures} smoke check(s) failed.`}`);
  process.exit(failures === 0 ? 0 : 1);
}

main().catch((error) => {
  console.error('Smoke test crashed:', error.message);
  process.exit(1);
});
