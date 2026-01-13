-- V012: Add Task <-> Shopping linkage for Stage 5
-- Adds linked_task_id FK and idempotency_key for duplicate prevention

-- Add nullable FK column to shopping_items for task linkage
-- ON DELETE SET NULL ensures items remain if task is deleted (safe behavior)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shopping_items' AND column_name = 'linked_task_id'
    ) THEN
        ALTER TABLE shopping_items
        ADD COLUMN linked_task_id UUID REFERENCES tasks(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Add idempotency_key column for duplicate prevention on retries
-- Key format: hash(command_id + list_id + normalized_name + linked_task_id|null)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shopping_items' AND column_name = 'idempotency_key'
    ) THEN
        ALTER TABLE shopping_items
        ADD COLUMN idempotency_key VARCHAR(64);
    END IF;
END $$;

-- Index for efficient lookups by linked task
CREATE INDEX IF NOT EXISTS idx_shopping_items_linked_task_id
ON shopping_items(linked_task_id)
WHERE linked_task_id IS NOT NULL;

-- Unique constraint for idempotency (prevents duplicates on retry)
-- Only applies when idempotency_key is set (non-null)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_shopping_items_idempotency_key'
    ) THEN
        CREATE UNIQUE INDEX idx_shopping_items_idempotency_key
        ON shopping_items(idempotency_key)
        WHERE idempotency_key IS NOT NULL;
    END IF;
END $$;

COMMENT ON COLUMN shopping_items.linked_task_id IS 'Optional FK to task that requires these items (Stage 5)';
COMMENT ON COLUMN shopping_items.idempotency_key IS 'Hash for duplicate prevention: hash(command_id + list_id + name + linked_task_id)';
