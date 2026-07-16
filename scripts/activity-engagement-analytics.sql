-- Activity engagement analytics (per-student activity visit + optional events)
-- Run against ukti_db when not relying on spring.jpa.hibernate.ddl-auto=update.

CREATE TABLE IF NOT EXISTS activity_engagement_session (
    id UUID PRIMARY KEY,
    client_session_id UUID NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    school_id UUID,
    class_id UUID,
    roll_number VARCHAR(64),
    run_id UUID,
    section_id VARCHAR(200),
    unit_slug VARCHAR(200) NOT NULL,
    activity_slug VARCHAR(200) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    video_complete INTEGER NOT NULL DEFAULT 0,
    video_skip INTEGER NOT NULL DEFAULT 0,
    video_replay INTEGER NOT NULL DEFAULT 0,
    video_error INTEGER NOT NULL DEFAULT 0,
    vision_attempts INTEGER NOT NULL DEFAULT 0,
    vision_passes INTEGER NOT NULL DEFAULT 0,
    vision_failures INTEGER NOT NULL DEFAULT 0,
    stt_listen_starts INTEGER NOT NULL DEFAULT 0,
    pron_pass INTEGER NOT NULL DEFAULT 0,
    pron_fail INTEGER NOT NULL DEFAULT 0,
    skip_audio INTEGER NOT NULL DEFAULT 0,
    stt_empty_cycles INTEGER NOT NULL DEFAULT 0,
    client_error INTEGER NOT NULL DEFAULT 0,
    payload_version INTEGER NOT NULL DEFAULT 1,
    raw_events JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_aes_school_class_roll_started
    ON activity_engagement_session (school_id, class_id, roll_number, started_at);
CREATE INDEX IF NOT EXISTS idx_aes_unit_activity_started
    ON activity_engagement_session (unit_slug, activity_slug, started_at);

CREATE TABLE IF NOT EXISTS activity_engagement_event (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES activity_engagement_session(id) ON DELETE CASCADE,
    ts TIMESTAMPTZ NOT NULL,
    type VARCHAR(64) NOT NULL,
    phase VARCHAR(128),
    challenge_index INTEGER,
    payload JSONB
);

CREATE INDEX IF NOT EXISTS idx_aee_session_ts ON activity_engagement_event (session_id, ts);

CREATE TABLE IF NOT EXISTS activity_engagement_processed_batch (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES activity_engagement_session(id) ON DELETE CASCADE,
    batch_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_aes_batch_session_batch UNIQUE (session_id, batch_id)
);

CREATE TABLE IF NOT EXISTS activity_engagement_idempotency (
    idempotency_key VARCHAR(200) PRIMARY KEY,
    session_id UUID NOT NULL,
    received_batch_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
