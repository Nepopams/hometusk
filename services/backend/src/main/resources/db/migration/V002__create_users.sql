-- V002: Create users table
-- Users are linked to external identity (Keycloak) via external_id (JWT sub claim)

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255),
    display_name    VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(1024),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for looking up by external_id (JWT sub)
CREATE INDEX idx_users_external_id ON users(external_id);

-- Index for email lookup
CREATE INDEX idx_users_email ON users(email) WHERE email IS NOT NULL;

COMMENT ON TABLE users IS 'User profiles linked to Keycloak identity';
COMMENT ON COLUMN users.external_id IS 'Keycloak subject claim (JWT sub)';
COMMENT ON COLUMN users.display_name IS 'User-facing display name';
