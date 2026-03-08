-- =============================================================================
-- Migrate to progress-only: remove units/tasks/activities, keep only progress by slug
-- Run: psql -h localhost -U aady -d ukti_db -f scripts/migrate-to-progress-only.sql
-- Run Step 1 & 2 export queries FIRST to backup data!
-- =============================================================================

-- 1. Create new progress table (slug-based)
CREATE TABLE IF NOT EXISTS user_activity_progress (
    id UUID PRIMARY KEY DEFAULT (substring(md5(random()::text) from 1 for 8) || '-' || substring(md5(random()::text) from 1 for 4) || '-4' || substring(md5(random()::text) from 1 for 3) || '-' || substring('89ab' from (1 + (random() * 4)::int) for 1) || substring(md5(random()::text) from 1 for 3) || '-' || substring(md5(random()::text) from 1 for 12))::uuid,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    unit_slug VARCHAR(100) NOT NULL,
    activity_slug VARCHAR(150) NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB,
    UNIQUE (user_id, unit_slug, activity_slug)
);

CREATE INDEX IF NOT EXISTS idx_user_activity_progress_user ON user_activity_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_progress_unit ON user_activity_progress(unit_slug);

-- 2. Migrate existing unit_progress to new table (skip if tables don't exist)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'unit_progress') THEN
    INSERT INTO user_activity_progress (user_id, unit_slug, activity_slug, completed_at, metadata)
    SELECT up.user_id, u.slug, t.slug || '-' || a."order", up.completed_at, up.metadata
    FROM unit_progress up
    JOIN units u ON up.unit_id = u.id
    JOIN tasks t ON up.task_id = t.id
    JOIN activities a ON up.activity_id = a.id
    ON CONFLICT (user_id, unit_slug, activity_slug) DO NOTHING;
  END IF;
END $$;

-- 3. Drop old tables (order matters - FKs)
DROP TABLE IF EXISTS unit_progress;
DROP TABLE IF EXISTS activities;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS units;
