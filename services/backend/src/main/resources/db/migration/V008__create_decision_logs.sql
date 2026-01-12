-- V008: Create decision_logs table
-- Audit trail for every command decision

CREATE TABLE decision_logs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    command_id              UUID NOT NULL REFERENCES commands(id) ON DELETE CASCADE,
    correlation_id          UUID NOT NULL,
    intent                  JSONB NOT NULL,
    context_snapshot        JSONB NOT NULL,
    decision                JSONB NOT NULL,
    source                  VARCHAR(50) NOT NULL DEFAULT 'rule',
    confidence              DECIMAL(3,2) NOT NULL DEFAULT 1.0,
    alternatives_considered JSONB,
    schema_valid            BOOLEAN NOT NULL DEFAULT TRUE,
    business_valid          BOOLEAN NOT NULL DEFAULT TRUE,
    validation_errors       JSONB,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT decision_logs_source_check CHECK (source IN ('rule', 'ai', 'fallback', 'user_override')),
    CONSTRAINT decision_logs_confidence_check CHECK (confidence >= 0 AND confidence <= 1)
);

-- Index for finding decision by command
CREATE INDEX idx_decision_logs_command_id ON decision_logs(command_id);

-- Index for correlation ID lookups
CREATE INDEX idx_decision_logs_correlation_id ON decision_logs(correlation_id);

-- Index for filtering by source
CREATE INDEX idx_decision_logs_source ON decision_logs(source);

-- Index for finding invalid decisions
CREATE INDEX idx_decision_logs_invalid ON decision_logs(schema_valid, business_valid)
    WHERE schema_valid = FALSE OR business_valid = FALSE;

COMMENT ON TABLE decision_logs IS 'Audit trail for command processing decisions';
COMMENT ON COLUMN decision_logs.intent IS 'Parsed intent from command';
COMMENT ON COLUMN decision_logs.context_snapshot IS 'Household state at decision time';
COMMENT ON COLUMN decision_logs.decision IS 'Final decision made';
COMMENT ON COLUMN decision_logs.source IS 'Decision source: rule (Stage 1), ai (Stage 2+), fallback, user_override';
COMMENT ON COLUMN decision_logs.confidence IS 'Decision confidence (1.0 for rule-based)';
