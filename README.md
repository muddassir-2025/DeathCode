# Death Code

An offline-first programming knowledge system for Android: a browsable, searchable
library of code knowledge plus a dedicated programming keyboard that pulls snippets
and keywords from that same library.

Everything that matters at runtime — browsing, searching, private notes, snippets and
the keyboard — runs **entirely on the device** against a local Room database. A small
Node.js backend exists only to distribute official content versions and to host the
moderated Community feature. The app never talks to a database directly.

```
┌──────────────────────────── Android app ────────────────────────────┐
│  Compose UI  ·  Programming keyboard (IME)                          │
│                              │                                      │
│                     Room database  (source of truth)                │
│            content tree · FTS index · notes · snippets · sync meta  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  HTTPS / JSON (optional, opt-in)
                    ┌───────────▼───────────┐
                    │  Death Code API        │   backend/
                    │  Node.js + Express     │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │  PostgreSQL (Neon)     │
                    └───────────────────────┘
```

## Repository layout

| Path | Contents |
| --- | --- |
| `app/` | Android application (Kotlin + Jetpack Compose) |
| `app/src/main/java/com/muddassir/deathcode/data/local/` | Room entities, DAOs, FTS search engine |
| `app/src/main/java/com/muddassir/deathcode/keyboard/` | Custom IME, layouts, snippets |
| `app/src/main/java/com/muddassir/deathcode/markdown/` | Markdown → content-tree importer |
| `app/src/main/java/com/muddassir/deathcode/ui/` | Compose screens, theme, navigation |
| `app/src/test/` | JVM unit tests (59 tests) |
| `backend/` | Node.js content-distribution + Community API (see `backend/README.md`) |
| `deathcode plan.md` | The specification this implementation follows |

## Prerequisites

* **JDK 21+** (built and tested with JDK 26)
* **Android SDK** with the platform matching `compileSdk` (Android 37.1)
* **Node.js 20+** — only needed for the backend
* A **PostgreSQL** database (Neon free tier works) — only needed for the backend

## Android app

### Build and test

```bash
./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 59 JVM unit tests
```

Install on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

The app works fully offline on first launch — it seeds a bundled official library
(languages, data structures & algorithms, databases, operating systems and networks)
plus a set of example keyboard snippets.

### Enabling the keyboard

The keyboard is a real Android input method, so it must be enabled once in the OS:

1. **Settings → System → Languages & input → On-screen keyboard → Manage keyboards**
2. Enable **Death Code Keyboard**.
3. In any text field, tap the keyboard-switch button and pick **Death Code Keyboard**.

Inside the app, the **Keyboard** tab documents this flow and lets you pick the active
language, toggle suggestions/symbols, and preview the layout.

### Connecting to the backend (optional)

The app is offline-first and there is no baked-in server URL. To enable content sync
and the Community feature:

1. Open **Settings → Content sync**.
2. Enter your Render URL (for example `https://deathcode-api.onrender.com`) as the
   **Backend API URL** and save.
3. Use **Check for updates** to pull the latest published content. With no URL set,
   sync is simply disabled and nothing leaves the device.

## Backend

The API distributes versioned content packages and handles Community accounts,
submissions and moderation. Full details live in [`backend/README.md`](backend/README.md);
the short version:

```bash
cd backend
npm install
cp .env.example .env      # fill in DB_URL, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD
npm run migrate           # create tables (idempotent)
npm start                 # http://localhost:8080
node scripts/smoke-test.js
```

### Deploying to Render

`backend/render.yaml` is a Render Blueprint. Push the repo, choose
**New + → Blueprint**, select the repository, and fill in `DB_URL`, `ADMIN_EMAIL`
and `ADMIN_PASSWORD` when prompted (`JWT_SECRET` is generated automatically).
Then run `npm run migrate` once against the same database, and put the resulting
public URL into the app as described above.

### Publishing content

```bash
cd backend
npm run publish:official                                    # bundled sample, additive
node scripts/publish-official.js ./content/my-release.json  # or your own package
```

Both `fullSnapshot: true` (complete library, unpublished nodes removed) and
`fullSnapshot: false` (additive, nothing deleted) are supported. The server computes
a SHA-256 checksum over the canonicalized nodes; the Android client recomputes and
verifies it before applying, then applies updates in a **single transaction**, so a
failed sync can never destroy working local content.

## Community & moderation

Anonymous use is fully supported — a login is required only to submit Community
content.

```
sign up → submit (server rate-limited) → admin approve/reject → publish → devices sync
```

Admin rights come from the `users.is_admin` column and are re-checked on every
request, never trusted from the client.

## Security notes

* Database credentials live only in `backend/.env` (gitignored) and the Render
  environment. They are never shipped inside the APK.
* `.env` files are ignored repository-wide; only `.env.example` templates are tracked.
* Community submissions are rate limited server-side, and all payload sizes are bounded.
* All backend traffic is HTTPS (Render terminates TLS).

## Implementation notes

* **Unlimited nesting** — the content tree is recursive with no depth cap. Manual
  creation and Markdown import are available at every node; heading levels are
  interpreted relative to the import location, not globally.
* **Deterministic search** — local FTS4 + prefix + fuzzy matching with a transparent
  weighted ranker. No AI, no embeddings, no network.
* AGP 9 + KSP require two flags in `gradle.properties`
  (`android.disallowKotlinSourceSets=false`,
  `android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE`); removing them
  breaks Room's generated sources.
* Room inserts use `OnConflictStrategy.ABORT` (not `REPLACE`) so replacing a node can
  never cascade-delete its subtree.
