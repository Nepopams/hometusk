-- V027: Add optional command-level task attributes.
-- Nullable and additive for backward compatibility with existing command payload-only clients.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commands' AND column_name = 'due_date'
    ) THEN
        ALTER TABLE commands
        ADD COLUMN due_date TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commands' AND column_name = 'assignee_id'
    ) THEN
        ALTER TABLE commands
        ADD COLUMN assignee_id UUID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commands' AND column_name = 'zone_id'
    ) THEN
        ALTER TABLE commands
        ADD COLUMN zone_id UUID;
    END IF;
END $$;

COMMENT ON COLUMN commands.due_date IS 'Optional explicit command-level due date for create_task; maps to task deadline';
COMMENT ON COLUMN commands.assignee_id IS 'Optional explicit command-level assignee user id for create_task';
COMMENT ON COLUMN commands.zone_id IS 'Optional explicit command-level zone id for create_task';
