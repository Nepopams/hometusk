-- V001: Create households table
-- Households are the top-level container for all data

CREATE TABLE households (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for listing households
CREATE INDEX idx_households_created_at ON households(created_at DESC);

COMMENT ON TABLE households IS 'Top-level container for all household data';
COMMENT ON COLUMN households.name IS 'Display name for the household';
