-- V015: Create command idempotency table
-- Stores Idempotency-Key usage for /api/v1/commands

CREATE TABLE command_idempotency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(128) NOT NULL,
    initiator_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_hash VARCHAR(64) NOT NULL,
    stored_response_json TEXT,
    stored_http_status INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_command_idempotency_key_user
ON command_idempotency(idempotency_key, initiator_user_id);

CREATE INDEX idx_command_idempotency_expires_at
ON command_idempotency(expires_at);

COMMENT ON TABLE command_idempotency IS 'Idempotency records for command execution';
COMMENT ON COLUMN command_idempotency.request_hash IS 'SHA-256 hash of request body';
COMMENT ON COLUMN command_idempotency.stored_response_json IS 'Serialized response body';
