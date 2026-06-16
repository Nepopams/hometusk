-- V034: Add approval/cancel lifecycle fields for command confirmations.

ALTER TABLE command_confirmations
    ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancelled_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS expiry_processed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS execution_result JSONB,
    ADD COLUMN IF NOT EXISTS failure_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS failure_message TEXT;

CREATE INDEX IF NOT EXISTS idx_command_confirmations_approved_by
ON command_confirmations(approved_by)
WHERE approved_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_command_confirmations_cancelled_by
ON command_confirmations(cancelled_by)
WHERE cancelled_by IS NOT NULL;

COMMENT ON COLUMN command_confirmations.approved_by IS 'HomeTusk user who explicitly approved the pending confirmation';
COMMENT ON COLUMN command_confirmations.cancelled_by IS 'HomeTusk user who cancelled the pending confirmation';
COMMENT ON COLUMN command_confirmations.execution_result IS 'Stable public execution result returned for idempotent repeated approval';
