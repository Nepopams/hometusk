-- V029: Add explicit email verification state to user profiles

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_source VARCHAR(32) NOT NULL DEFAULT 'unknown',
    ADD COLUMN email_updated_at TIMESTAMP WITH TIME ZONE;

UPDATE users
SET email = LOWER(TRIM(email)),
    email_updated_at = COALESCE(updated_at, NOW())
WHERE email IS NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_email_source
    CHECK (email_source IN ('idp_claim', 'manual', 'unknown'));

CREATE INDEX idx_users_email_verified
    ON users(email_verified)
    WHERE email IS NOT NULL;

COMMENT ON COLUMN users.email IS 'Normalized email address when known';
COMMENT ON COLUMN users.email_verified IS 'Whether the current email is verified by a trusted identity source';
COMMENT ON COLUMN users.email_source IS 'Source of the current email value: idp_claim, manual, or unknown';
COMMENT ON COLUMN users.email_updated_at IS 'Timestamp when the email value last changed';
