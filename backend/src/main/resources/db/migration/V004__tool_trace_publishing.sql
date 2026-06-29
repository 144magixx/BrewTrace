CREATE TABLE tool_call_records (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    purpose TEXT,
    input_summary TEXT,
    output_status VARCHAR(32),
    output_summary TEXT,
    risk_level VARCHAR(32),
    requires_confirmation BOOLEAN NOT NULL,
    confirmation_status VARCHAR(32),
    error_category VARCHAR(32),
    recoverable BOOLEAN,
    next_actions TEXT,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP
);

CREATE TABLE agent_traces (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    trace_type VARCHAR(64) NOT NULL,
    orchestration_mode VARCHAR(64) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    final_decision TEXT,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE agent_trace_steps (
    id VARCHAR(64) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    sequence INTEGER NOT NULL,
    step_type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    summary TEXT NOT NULL,
    prompt_snapshot JSONB,
    model_output_snapshot JSONB,
    tool_input_snapshot JSONB,
    tool_output_snapshot JSONB,
    memory_snapshot JSONB,
    decision TEXT,
    tool_selection_reason TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE publishing_packages (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    tags TEXT,
    status VARCHAR(64) NOT NULL,
    risk_checklist TEXT,
    confirmation_id VARCHAR(64)
);
