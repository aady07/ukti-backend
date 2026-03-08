-- =============================================================================
-- Ukti Education - EC2/RDS Schema
-- *** MUST RUN AS RDS MASTER USER *** (myappuser cannot create tables)
-- Example: PGPASSWORD=MASTER_PASS psql -h motor-claim... -U postgres -d ukti_db -f scripts/ec2-schema.sql
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------------
-- users
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cognito_sub VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    username VARCHAR(255),
    display_name VARCHAR(255),
    school_id VARCHAR(255),
    password_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_cognito_sub ON users(cognito_sub);

-- -----------------------------------------------------------------------------
-- units
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    module_type VARCHAR(50) NOT NULL,
    subject VARCHAR(50) NOT NULL,
    "order" INTEGER NOT NULL,
    video_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- tasks
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    "order" INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tasks_unit_id ON tasks(unit_id);

-- -----------------------------------------------------------------------------
-- activities
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    "order" INTEGER NOT NULL,
    prop_name VARCHAR(255),
    video_url VARCHAR(500),
    config JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activities_task_id ON activities(task_id);

-- -----------------------------------------------------------------------------
-- unit_progress
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS unit_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB,
    UNIQUE (user_id, activity_id)
);

CREATE INDEX IF NOT EXISTS idx_unit_progress_user_id ON unit_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_unit_progress_unit_id ON unit_progress(unit_id);

-- -----------------------------------------------------------------------------
-- module_completions
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS module_completions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id VARCHAR(20) NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, module_id)
);

CREATE INDEX IF NOT EXISTS idx_module_completions_user_id ON module_completions(user_id);

-- -----------------------------------------------------------------------------
-- task_completions
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_completions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id VARCHAR(20) NOT NULL,
    task_id VARCHAR(100) NOT NULL,
    task_type VARCHAR(50),
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB,
    UNIQUE (user_id, module_id, task_id)
);

CREATE INDEX IF NOT EXISTS idx_task_completions_user_id ON task_completions(user_id);

-- -----------------------------------------------------------------------------
-- experiential_stats
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS experiential_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id VARCHAR(20) NOT NULL,
    stat_type VARCHAR(50) NOT NULL,
    count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, module_id, stat_type)
);

CREATE INDEX IF NOT EXISTS idx_experiential_stats_user_id ON experiential_stats(user_id);
