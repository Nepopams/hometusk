-- V004: Create zones table
-- Zones are locations within a household (kitchen, bathroom, etc.)

CREATE TABLE zones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT zones_household_name_unique UNIQUE (household_id, name)
);

-- Index for listing zones in a household
CREATE INDEX idx_zones_household_id ON zones(household_id);

COMMENT ON TABLE zones IS 'Locations within a household';
COMMENT ON COLUMN zones.name IS 'Zone name (kitchen, bathroom, etc.)';
