-- V005: Create tasks table
-- Tasks are work items within a household

CREATE TABLE tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'open',
    assignee_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    zone_id         UUID REFERENCES zones(id) ON DELETE SET NULL,
    deadline        TIMESTAMP WITH TIME ZONE,
    created_by_id   UUID NOT NULL REFERENCES users(id),
    command_id      UUID,
    created_via     VARCHAR(50) NOT NULL DEFAULT 'command',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT tasks_status_check CHECK (status IN ('open', 'in_progress', 'done', 'cancelled')),
    CONSTRAINT tasks_created_via_check CHECK (created_via IN ('command', 'fallback', 'direct'))
);

-- Index for listing tasks in a household
CREATE INDEX idx_tasks_household_id ON tasks(household_id);

-- Index for finding tasks by assignee
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id) WHERE assignee_id IS NOT NULL;

-- Index for filtering by status
CREATE INDEX idx_tasks_status ON tasks(status);

-- Index for linking back to command
CREATE INDEX idx_tasks_command_id ON tasks(command_id) WHERE command_id IS NOT NULL;

-- Composite index for common query: household + status
CREATE INDEX idx_tasks_household_status ON tasks(household_id, status);

COMMENT ON TABLE tasks IS 'Work items within a household';
COMMENT ON COLUMN tasks.command_id IS 'Command that created this task';
COMMENT ON COLUMN tasks.created_via IS 'How task was created: command, fallback, or direct';
