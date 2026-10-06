# Death Code API

Content distribution and Community service for the Death Code Android app.

```
Android app  ──HTTPS/JSON──▶  Death Code API (Render)  ──▶  Neon PostgreSQL
```

The backend is **not** part of the normal runtime path. Browsing, searching, private notes,
snippets and the keyboard all run against the on-device Room database. The API exists only for:

1. distributing official content versions, and
2. Community accounts, submissions and moderation.

The Android app never connects to PostgreSQL and never holds database credentials.

---

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/health` | – | Liveness probe + database reachability |
| `GET` | `/api/content/:kind/manifest` | – | Latest published version metadata |
| `GET` | `/api/content/:kind/package` | – | Latest published content package |
| `GET` | `/api/content/:kind/package/:version` | – | A specific historical version |
| `POST` | `/api/auth/signup` | – | Create a Community account |
| `POST` | `/api/auth/login` | – | Sign in, returns a JWT |
| `POST` | `/api/community/submissions` | JWT | Submit content for review |
| `GET` | `/api/community/submissions/mine` | JWT | Own submissions + moderation state |
| `DELETE` | `/api/community/submissions/:id` | JWT | Withdraw a non-approved submission |
| `POST` | `/api/admin/official/publish` | Admin | Publish a new official content version |
| `GET` | `/api/admin/community/submissions` | Admin | Moderation queue |
| `POST` | `/api/admin/community/submissions/:id/approve` | Admin | Approve |
| `POST` | `/api/admin/community/submissions/:id/reject` | Admin | Reject (optional note) |
| `DELETE` | `/api/admin/community/submissions/:id` | Admin | Remove content |
| `POST` | `/api/admin/community/publish` | Admin | Rebuild the Community package from approved content |

`:kind` is `official` or `community`.

---

## Local setup

```bash
cd backend
npm install
cp .env.example .env        # then fill in DB_URL, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD
npm run migrate             # creates the tables in Neon (idempotent)
npm start                   # http://localhost:8080
node scripts/smoke-test.js  # end-to-end check against the running server
```

### Environment variables

See `.env.example`. The important ones:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | Neon PostgreSQL connection string (`postgresql://…?sslmode=require`) |
| `JWT_SECRET` | Signs Community login tokens. Generate with `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Bootstrap administrator, created/promoted on first start |
| `PORT` | Set automatically by Render |

---

## Deploying to Render

1. Push the repository to GitHub.
2. In Render: **New +** → **Blueprint**, select the repository. `backend/render.yaml` is picked up automatically.
3. Fill in the `DB_URL`, `ADMIN_EMAIL` and `ADMIN_PASSWORD` values when prompted (`JWT_SECRET` is generated for you).
4. After the first deploy, run the migration once:
   ```bash
   # Locally, pointed at the same Neon database
   npm run migrate
   ```
   or add `npm run migrate && npm start` as the start command if you prefer the server to migrate on boot.
5. Put the resulting public URL (for example `https://deathcode-api.onrender.com`) into the Android app under **Settings → Content synchronization → Backend API URL**.

---

## Publishing official content

Official content is published as a versioned **content package**:

```json
{
  "packageVersion": 2,
  "kind": "OFFICIAL",
  "fullSnapshot": false,
  "nodes": [
    {
      "id": "cpp-builtins-algorithms-equal-range",
      "parentId": "cpp-builtins-algorithms",
      "title": "equal_range()",
      "markdown": "…",
      "syntax": "…",
      "notes": "…",
      "language": "cpp",
      "keywords": ["equalrange"],
      "sortOrder": 4
    }
  ]
}
```

* `fullSnapshot: true` — the package is the **complete** official library. Anything not
  listed is unpublished and removed from devices. Every node's `parentId` must be inside the
  package.
* `fullSnapshot: false` — an additive/partial publication. Nodes are created or updated and
  **nothing is deleted**; `parentId` may point at content the device already has.

The server recomputes the SHA-256 checksum over the nodes; the Android client verifies it
before applying anything, and applies updates in a single transaction so a failed update can
never destroy working local content.

```bash
# Publish the bundled sample (additive, safe on top of the content that ships in the app)
npm run publish:official

# Publish your own package, forcing a version number
node scripts/publish-official.js ./content/my-release.json --version 5
```

Or POST the same JSON to `/api/admin/official/publish` with an admin token.

## Community workflow

```
User signs up          →  POST /api/auth/signup
User submits content   →  POST /api/community/submissions   (server-side rate limits)
Admin reviews          →  GET  /api/admin/community/submissions?status=PENDING
Admin approves/rejects →  POST /api/admin/community/submissions/:id/approve|reject
Admin publishes        →  POST /api/admin/community/publish
Devices synchronize    →  GET  /api/content/community/package
```

## Security

* Database credentials exist only in the server environment.
* Community tokens are signed with `JWT_SECRET` and re-checked against the database on every
  request; admin rights come from the `users.is_admin` column, never from the client.
* Community submissions are rate limited **server side** (hourly/daily quotas and a cap on
  pending submissions), because client-side limits can be bypassed.
* Payload sizes (markdown length, package node count, JSON body size) are bounded.
* All traffic is HTTPS (Render terminates TLS).
