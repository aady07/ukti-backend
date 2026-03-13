-- =============================================================================
-- UKTI in Schools - Database Migration
-- Run this AFTER ec2-schema.sql (or on existing DB)
-- *** MUST RUN AS RDS MASTER USER ***
-- Example: PGPASSWORD=MASTER_PASS psql -h <host> -U postgres -d ukti_db -f scripts/ukti-schools-migration.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Schools table (stores school name; school_id = schools.id)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 2. Classes table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(school_id, name)
);

CREATE INDEX IF NOT EXISTS idx_classes_school_id ON classes(school_id);

-- -----------------------------------------------------------------------------
-- 3. Extend users table
-- -----------------------------------------------------------------------------

-- Add school_uuid (FK to schools) - used for school_admin and students
ALTER TABLE users ADD COLUMN IF NOT EXISTS school_uuid UUID REFERENCES schools(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_users_school_uuid ON users(school_uuid);

-- Add user_type: 'individual' | 'school_admin' | 'student'
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'individual';

-- Add class_id (FK to classes) - for students only
ALTER TABLE users ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES classes(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_users_class_id ON users(class_id);

-- Add roll_number - for students only, unique per (school_uuid, class_id)
ALTER TABLE users ADD COLUMN IF NOT EXISTS roll_number VARCHAR(50);

-- Make email nullable (students have no email)
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Make cognito_sub nullable (students have no Cognito)
-- PostgreSQL: UNIQUE allows multiple NULLs
ALTER TABLE users ALTER COLUMN cognito_sub DROP NOT NULL;

-- Unique constraint: (school_uuid, class_id, roll_number) for students
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_school_class_roll
    ON users(school_uuid, class_id, roll_number)
    WHERE user_type = 'student' AND roll_number IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 4. Backfill: set user_type for existing users
-- -----------------------------------------------------------------------------
UPDATE users SET user_type = 'individual' WHERE user_type IS NULL;
