-- Campaign read-along funnel tables (also created via Hibernate ddl-auto=update)

CREATE TABLE IF NOT EXISTS campaign_sessions (
    id UUID PRIMARY KEY,
    email VARCHAR(320),
    utm_source VARCHAR(120),
    utm_medium VARCHAR(120),
    utm_campaign VARCHAR(120),
    pack_id VARCHAR(80),
    cognito_sub VARCHAR(128),
    started_at TIMESTAMPTZ NOT NULL,
    free_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS campaign_part_attempts (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    pack_id VARCHAR(80),
    passage_id VARCHAR(120) NOT NULL,
    part_index INT NOT NULL,
    expected_text TEXT,
    spoken_transcript TEXT,
    duration_ms BIGINT,
    pause_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    skip_count INT DEFAULT 0,
    words_total INT,
    words_matched INT,
    accuracy_pct INT,
    gemini_score_json TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'pending_score',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS campaign_events (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    path VARCHAR(400),
    payload_json TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS campaign_leads (
    id UUID PRIMARY KEY,
    email VARCHAR(320),
    cognito_sub VARCHAR(128),
    display_name VARCHAR(200),
    status VARCHAR(40) NOT NULL DEFAULT 'anon',
    session_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
