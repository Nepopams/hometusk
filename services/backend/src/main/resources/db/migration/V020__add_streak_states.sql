-- Streak states table (streakVisible is stored in gamification_settings)
CREATE TABLE streak_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    last_activity_date DATE,
    grace_used_today BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id)
);

CREATE INDEX idx_streak_states_user_household
    ON streak_states(user_id, household_id);

ALTER TABLE gamification_settings
    ADD COLUMN streak_visible BOOLEAN NOT NULL DEFAULT TRUE;
