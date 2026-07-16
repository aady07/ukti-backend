-- Authoritative class module progression schema.
-- Safe to re-run due to IF NOT EXISTS guards.

CREATE TABLE IF NOT EXISTS class_module_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    module_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    active_section_index INTEGER NOT NULL DEFAULT 0,
    section_order_json JSONB NOT NULL,
    completion_rule VARCHAR(20) NOT NULL DEFAULT 'all_students',
    completion_target INTEGER NOT NULL DEFAULT 100,
    created_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_class_module_runs_lookup
    ON class_module_runs (school_id, class_id, module_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_class_module_active_run
    ON class_module_runs (school_id, class_id, module_id)
    WHERE status = 'active';

CREATE TABLE IF NOT EXISTS class_section_gates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES class_module_runs(id) ON DELETE CASCADE,
    section_id VARCHAR(150) NOT NULL,
    section_index INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    completion_rule VARCHAR(20) NOT NULL DEFAULT 'all_students',
    completion_target INTEGER NOT NULL DEFAULT 100,
    unlocked_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (run_id, section_index),
    UNIQUE (run_id, section_id)
);

CREATE INDEX IF NOT EXISTS idx_class_section_gates_status
    ON class_section_gates (run_id, status);

CREATE TABLE IF NOT EXISTS student_section_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES class_module_runs(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roll_number VARCHAR(100) NOT NULL,
    section_id VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'not_started',
    started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_activity_id VARCHAR(150) NULL,
    last_challenge_index INTEGER NULL,
    metadata_json JSONB NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (run_id, roll_number, section_id)
);

CREATE INDEX IF NOT EXISTS idx_student_section_progress_roll
    ON student_section_progress (run_id, roll_number);

CREATE TABLE IF NOT EXISTS student_activity_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES class_module_runs(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roll_number VARCHAR(100) NOT NULL,
    section_id VARCHAR(150) NOT NULL,
    activity_id VARCHAR(150) NOT NULL,
    challenge_index INTEGER NULL,
    status VARCHAR(20) NOT NULL,
    score NUMERIC NULL,
    idempotency_key VARCHAR(200) NULL,
    metadata_json JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (run_id, roll_number, section_id, activity_id, challenge_index)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_student_activity_idempotency
    ON student_activity_progress (run_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
