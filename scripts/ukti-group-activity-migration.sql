-- =============================================================================
-- UKTI Group Activity Sessions
-- Run after ukti-schools-migration.sql
-- Use gen_random_uuid() for RDS (no uuid-ossp extension)
-- =============================================================================

CREATE TABLE IF NOT EXISTS group_activity_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    activity_id VARCHAR(50) NOT NULL,
    groups JSONB NOT NULL,
    scores JSONB NOT NULL,
    winner_group INTEGER NOT NULL,
    session_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_by_teacher_id UUID REFERENCES teachers(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_group_activity_school_class ON group_activity_sessions(school_id, class_id);
CREATE INDEX IF NOT EXISTS idx_group_activity_created_at ON group_activity_sessions(created_at DESC);
