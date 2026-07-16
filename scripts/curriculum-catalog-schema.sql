-- Curriculum catalog: JSONB source of truth for module definitions.
-- Safe to re-run due to IF NOT EXISTS guards.

CREATE TABLE IF NOT EXISTS curriculum_modules (
    module_id VARCHAR(100) PRIMARY KEY,
    class_level VARCHAR(20) NOT NULL,
    payload JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_curriculum_modules_class_level
    ON curriculum_modules (class_level);

CREATE TABLE IF NOT EXISTS curriculum_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_level VARCHAR(20) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (class_level, version)
);

CREATE INDEX IF NOT EXISTS idx_curriculum_releases_latest
    ON curriculum_releases (class_level, version DESC);
