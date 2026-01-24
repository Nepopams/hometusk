-- V018: Create gamification tables (points ledger + badges)

CREATE TABLE badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    criteria VARCHAR(500),
    icon_name VARCHAR(50)
);

CREATE TABLE points_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    points INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    note VARCHAR(500),
    CONSTRAINT points_ledger_reason_check CHECK (
        reason IN ('TASK_COMPLETED', 'ON_TIME_BONUS', 'TASK_UNCOMPLETED', 'ON_TIME_BONUS_REVERSED')
    )
);

CREATE TABLE user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id, badge_id)
);

CREATE UNIQUE INDEX idx_points_ledger_task_user_reason
ON points_ledger(task_id, user_id, reason);

CREATE INDEX idx_points_ledger_user_household_created
ON points_ledger(user_id, household_id, created_at DESC);

CREATE INDEX idx_points_ledger_household_created
ON points_ledger(household_id, created_at DESC);

CREATE INDEX idx_user_badges_user_household
ON user_badges(user_id, household_id);

-- Seed badges (S08 streak-free)
INSERT INTO badges (id, code, name, description, criteria, icon_name) VALUES
    (gen_random_uuid(), 'FIRST_TASK', 'Task Starter', 'You completed your first task!', 'Complete 1 task', 'star'),
    (gen_random_uuid(), 'TEN_TASKS', 'Task Champion', 'You are on a roll!', 'Complete 10 tasks', 'trophy'),
    (gen_random_uuid(), 'WEEK_WARRIOR', 'Week Warrior', 'Productive week!', 'Complete 7+ tasks in one week', 'fire'),
    (gen_random_uuid(), 'ZONE_SPECIALIST', 'Zone Specialist', 'Master of your domain!', 'Complete 5+ tasks in one zone', 'target'),
    (gen_random_uuid(), 'ON_TIME_HERO', 'On-Time Hero', 'Beating the clock!', 'Complete 5 tasks before deadline', 'clock');

-- Allow badge earned notification type
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN (
        'INVITE_ACCEPTED',
        'TASK_ASSIGNED',
        'TASK_COMPLETED',
        'SHOPPING_ITEM_ADDED',
        'SHOPPING_ITEM_PURCHASED',
        'BADGE_EARNED'
    )
);
