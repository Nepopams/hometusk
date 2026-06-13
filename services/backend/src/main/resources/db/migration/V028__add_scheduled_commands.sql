-- V028: Add optional one-off scheduled command execution support.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commands' AND column_name = 'schedule_at'
    ) THEN
        ALTER TABLE commands
        ADD COLUMN schedule_at TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;

ALTER TABLE commands DROP CONSTRAINT IF EXISTS commands_status_check;
ALTER TABLE commands ADD CONSTRAINT commands_status_check CHECK (
    status IN ('received', 'validating', 'processing', 'scheduled', 'needs_input', 'executed', 'failed', 'rejected')
);

CREATE INDEX IF NOT EXISTS idx_commands_scheduled_due
ON commands (schedule_at)
WHERE status = 'scheduled';

COMMENT ON COLUMN commands.schedule_at IS 'Optional one-off command execution time; due scheduled commands are executed by CommandSchedulerJob';
