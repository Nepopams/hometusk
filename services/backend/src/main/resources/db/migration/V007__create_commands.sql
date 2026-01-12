-- V007: Create commands table
-- Commands are first-class entities with their own lifecycle

CREATE TABLE commands (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id      UUID NOT NULL UNIQUE,
    household_id        UUID NOT NULL REFERENCES households(id),
    requester_id        UUID NOT NULL REFERENCES users(id),
    type                VARCHAR(50) NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'received',
    error_code          VARCHAR(100),
    error_message       TEXT,
    source              VARCHAR(50) NOT NULL DEFAULT 'api',
    client_timestamp    TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMP WITH TIME ZONE,
    execution_ms        INTEGER,
    CONSTRAINT commands_type_check CHECK (type IN ('create_task', 'complete_task', 'add_shopping_item')),
    CONSTRAINT commands_status_check CHECK (status IN ('received', 'validating', 'processing', 'executed', 'failed', 'rejected')),
    CONSTRAINT commands_source_check CHECK (source IN ('api', 'web', 'mobile'))
);

-- Index for correlation ID lookups
CREATE INDEX idx_commands_correlation_id ON commands(correlation_id);

-- Index for listing commands by household
CREATE INDEX idx_commands_household_id ON commands(household_id);

-- Index for finding commands by requester
CREATE INDEX idx_commands_requester_id ON commands(requester_id);

-- Index for filtering by status
CREATE INDEX idx_commands_status ON commands(status);

-- Index for chronological listing
CREATE INDEX idx_commands_created_at ON commands(created_at DESC);

-- Composite index for common query: household + type + created_at
CREATE INDEX idx_commands_household_type_created ON commands(household_id, type, created_at DESC);

COMMENT ON TABLE commands IS 'First-class command entities with full lifecycle';
COMMENT ON COLUMN commands.correlation_id IS 'Unique ID for distributed tracing';
COMMENT ON COLUMN commands.payload IS 'Command-specific payload as JSONB';
COMMENT ON COLUMN commands.execution_ms IS 'Server-side execution time in milliseconds';
