ALTER TABLE tasks DROP CONSTRAINT tasks_created_via_check;
ALTER TABLE tasks
    ADD CONSTRAINT tasks_created_via_check
        CHECK (created_via IN ('command', 'fallback', 'direct', 'routine', 'scheduler'));

COMMENT ON COLUMN tasks.created_via IS 'How task was created: command, fallback, direct, routine, or scheduler';
