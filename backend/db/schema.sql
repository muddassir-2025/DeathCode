-- Death Code server schema (Neon PostgreSQL).
-- Safe to run repeatedly: every statement is idempotent.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------- accounts

CREATE TABLE IF NOT EXISTS users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    display_name  text,
    is_admin      boolean NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- ------------------------------------------------- published content versions

-- One row per published version. `nodes` is the serialized content package, which keeps
-- the wire format and the storage format identical and makes delta packages a drop-in
-- extension later.
CREATE TABLE IF NOT EXISTS content_packages (
    kind          text        NOT NULL,
    version       bigint      NOT NULL,
    checksum      text        NOT NULL,
    generated_at  timestamptz NOT NULL DEFAULT now(),
    full_snapshot boolean     NOT NULL DEFAULT true,
    nodes         jsonb       NOT NULL DEFAULT '[]'::jsonb,
    PRIMARY KEY (kind, version),
    CONSTRAINT content_packages_kind_check CHECK (kind IN ('OFFICIAL', 'COMMUNITY'))
);

CREATE INDEX IF NOT EXISTS content_packages_kind_version_idx
    ON content_packages (kind, version DESC);

-- ------------------------------------------------------------ community queue

CREATE TABLE IF NOT EXISTS community_submissions (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title          text NOT NULL,
    markdown       text NOT NULL,
    syntax         text,
    language       text,
    keywords       text[] NOT NULL DEFAULT '{}',
    status         text NOT NULL DEFAULT 'PENDING',
    review_message text,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT community_submissions_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS community_submissions_status_idx
    ON community_submissions (status, created_at DESC);

CREATE INDEX IF NOT EXISTS community_submissions_user_idx
    ON community_submissions (user_id, created_at DESC);
