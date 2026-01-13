-- V011: Add Stage 4 context fields for autodelegation
-- Adds zone ownership for intelligent task assignment

-- Add owner to zones table
ALTER TABLE zones ADD COLUMN owner_id UUID;

-- Add foreign key constraint with ON DELETE SET NULL
ALTER TABLE zones ADD CONSTRAINT fk_zones_owner
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;

-- Add index for faster lookups when filtering by owner
CREATE INDEX idx_zones_owner_id ON zones(owner_id) WHERE owner_id IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN zones.owner_id IS 'Preferred assignee for tasks in this zone (optional, nullable)';
