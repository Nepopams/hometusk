-- V003: Create memberships table
-- Memberships link users to households with specific roles

CREATE TABLE memberships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    household_id    UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL DEFAULT 'member',
    joined_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT memberships_user_household_unique UNIQUE (user_id, household_id),
    CONSTRAINT memberships_role_check CHECK (role IN ('admin', 'member'))
);

-- Index for finding user's households
CREATE INDEX idx_memberships_user_id ON memberships(user_id);

-- Index for finding household members
CREATE INDEX idx_memberships_household_id ON memberships(household_id);

COMMENT ON TABLE memberships IS 'User membership in households with role';
COMMENT ON COLUMN memberships.role IS 'admin or member';
