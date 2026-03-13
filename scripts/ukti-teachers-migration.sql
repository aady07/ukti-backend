-- =============================================================================
-- UKTI Teachers - Database Migration
-- Run AFTER ukti-schools-migration.sql
-- *** MUST RUN AS RDS MASTER USER ***
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Teachers table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teachers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(school_id, email)
);

CREATE INDEX IF NOT EXISTS idx_teachers_school_id ON teachers(school_id);

-- -----------------------------------------------------------------------------
-- 2. Teacher-class assignment (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_classes (
    teacher_id UUID NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    is_main_teacher BOOLEAN DEFAULT true,
    PRIMARY KEY (teacher_id, class_id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_classes_teacher_id ON teacher_classes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_classes_class_id ON teacher_classes(class_id);
