CREATE TABLE gamification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    show_progress_to_others BOOLEAN NOT NULL DEFAULT TRUE,
    gamification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id)
);

CREATE INDEX idx_gamification_settings_user_household
    ON gamification_settings(user_id, household_id);
