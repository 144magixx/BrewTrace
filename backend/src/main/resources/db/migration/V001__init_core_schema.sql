CREATE TABLE tasting_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_intent VARCHAR(64),
    orchestration_mode VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    active_draft_id VARCHAR(64)
);

CREATE TABLE coffee_records (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    final_draft_id VARCHAR(64),
    flavor_keywords TEXT,
    published_status VARCHAR(32),
    published_url TEXT,
    created_at TIMESTAMP NOT NULL
);
