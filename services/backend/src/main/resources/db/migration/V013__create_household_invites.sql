-- V013: Create household invites table
-- Supports single-use invite tokens for household membership

CREATE TABLE household_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invite_token VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    redeemed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT household_invites_token_unique UNIQUE (invite_token),
    CONSTRAINT household_invites_status_check CHECK (status IN ('ACTIVE', 'REDEEMED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_household_invites_household_id_status
ON household_invites(household_id, status);

COMMENT ON TABLE household_invites IS 'Single-use invite tokens for household membership';
COMMENT ON COLUMN household_invites.invite_token IS 'Invite token with hti_ prefix';
