-- V009: Create task_activities table
-- Activity events for audit and history

CREATE TABLE task_activities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    command_id      UUID REFERENCES commands(id) ON DELETE SET NULL,
    correlation_id  UUID NOT NULL,
    activity_type   VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    actor_id        UUID NOT NULL REFERENCES users(id),
    changes         JSONB,
    metadata        JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT task_activities_type_check CHECK (activity_type IN (
        'task_created', 'task_assigned', 'task_started',
        'task_completed', 'task_cancelled', 'task_updated',
        'shopping_item_added', 'shopping_item_purchased'
    )),
    CONSTRAINT task_activities_entity_type_check CHECK (entity_type IN ('task', 'shopping_item'))
);

-- Index for listing activities in a household
CREATE INDEX idx_task_activities_household_id ON task_activities(household_id);

-- Index for finding activities for an entity
CREATE INDEX idx_task_activities_entity ON task_activities(entity_type, entity_id);

-- Index for correlation ID lookups
CREATE INDEX idx_task_activities_correlation_id ON task_activities(correlation_id);

-- Index for finding activities by command
CREATE INDEX idx_task_activities_command_id ON task_activities(command_id) WHERE command_id IS NOT NULL;

-- Index for chronological listing
CREATE INDEX idx_task_activities_created_at ON task_activities(created_at DESC);

-- Composite index for common query: household + entity + created_at
CREATE INDEX idx_task_activities_household_entity ON task_activities(household_id, entity_type, entity_id, created_at DESC);

COMMENT ON TABLE task_activities IS 'Activity events for audit and history';
COMMENT ON COLUMN task_activities.changes IS 'What changed: { field: { old: ..., new: ... } }';
COMMENT ON COLUMN task_activities.metadata IS 'Additional context for the activity';
