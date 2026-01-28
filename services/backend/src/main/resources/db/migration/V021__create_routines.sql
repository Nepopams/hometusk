-- V021: Create routines table for EP-010 Recurring Tasks

CREATE TABLE routines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    zone_id UUID REFERENCES zones(id) ON DELETE SET NULL,
    recurrence_rule JSONB NOT NULL,
    assignment_policy VARCHAR(20) NOT NULL,
    fixed_assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    round_robin_state JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    generation_window_days INTEGER NOT NULL DEFAULT 7,
    created_by_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    paused_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT routines_status_check CHECK (status IN ('ACTIVE', 'PAUSED', 'DELETED')),
    CONSTRAINT routines_assignment_policy_check CHECK (assignment_policy IN ('FIXED', 'ROUND_ROBIN', 'MANUAL')),
    CONSTRAINT routines_generation_window_check CHECK (generation_window_days BETWEEN 1 AND 30)
);

CREATE INDEX idx_routines_household_id ON routines(household_id);
CREATE INDEX idx_routines_household_status ON routines(household_id, status);

ALTER TABLE tasks ADD COLUMN routine_id UUID REFERENCES routines(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN scheduled_date DATE;

ALTER TABLE tasks ADD CONSTRAINT chk_routine_date_consistency
    CHECK ((routine_id IS NULL) = (scheduled_date IS NULL));

CREATE UNIQUE INDEX idx_task_routine_scheduled_date
    ON tasks (routine_id, scheduled_date)
    WHERE routine_id IS NOT NULL;

CREATE INDEX idx_tasks_routine_id ON tasks(routine_id) WHERE routine_id IS NOT NULL;
