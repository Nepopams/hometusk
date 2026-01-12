-- V010: Add AI Platform integration fields
-- Stage 2: Support for external AI Platform decision provider

-- Add external tracking fields to decision_logs
ALTER TABLE decision_logs
ADD COLUMN external_decision_id UUID,
ADD COLUMN raw_decision_payload JSONB;

-- Index for external decision lookups (sparse index for non-null values only)
CREATE INDEX idx_decision_logs_external_id
ON decision_logs(external_decision_id)
WHERE external_decision_id IS NOT NULL;

-- Update source constraint to include new values (manual, ai_platform)
-- Keep existing values (rule, ai) for backward compatibility
ALTER TABLE decision_logs DROP CONSTRAINT decision_logs_source_check;
ALTER TABLE decision_logs ADD CONSTRAINT decision_logs_source_check
CHECK (source IN ('rule', 'ai', 'fallback', 'user_override', 'manual', 'ai_platform'));

COMMENT ON COLUMN decision_logs.external_decision_id IS 'External AI Platform decision ID for cross-system tracing';
COMMENT ON COLUMN decision_logs.raw_decision_payload IS 'Raw response from AI Platform for audit trail';
