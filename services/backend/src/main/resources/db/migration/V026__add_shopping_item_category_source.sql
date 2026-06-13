-- V026: Add optional shopping item category/source metadata
-- Nullable and additive for backward compatibility with existing shopping items and run snapshots.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shopping_items' AND column_name = 'category'
    ) THEN
        ALTER TABLE shopping_items
        ADD COLUMN category VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shopping_items' AND column_name = 'source'
    ) THEN
        ALTER TABLE shopping_items
        ADD COLUMN source VARCHAR(120);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shopping_run_items' AND column_name = 'category'
    ) THEN
        ALTER TABLE shopping_run_items
        ADD COLUMN category VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shopping_run_items' AND column_name = 'source'
    ) THEN
        ALTER TABLE shopping_run_items
        ADD COLUMN source VARCHAR(120);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'shopping_items' AND constraint_name = 'chk_shopping_items_category'
    ) THEN
        ALTER TABLE shopping_items
        ADD CONSTRAINT chk_shopping_items_category CHECK (
            category IS NULL
            OR category IN ('groceries', 'cleaning', 'personal_care', 'diy', 'electronics', 'other')
        );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'shopping_run_items' AND constraint_name = 'chk_shopping_run_items_category'
    ) THEN
        ALTER TABLE shopping_run_items
        ADD CONSTRAINT chk_shopping_run_items_category CHECK (
            category IS NULL
            OR category IN ('groceries', 'cleaning', 'personal_care', 'diy', 'electronics', 'other')
        );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_shopping_items_list_category
    ON shopping_items(shopping_list_id, category)
    WHERE category IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_shopping_items_list_source
    ON shopping_items(shopping_list_id, source)
    WHERE source IS NOT NULL;

COMMENT ON COLUMN shopping_items.category IS 'Optional coarse category for shopping item grouping/filtering';
COMMENT ON COLUMN shopping_items.source IS 'Optional source/store display text for shopping item grouping/filtering';
COMMENT ON COLUMN shopping_run_items.category IS 'Snapshot of shopping item category at run creation time';
COMMENT ON COLUMN shopping_run_items.source IS 'Snapshot of shopping item source/store at run creation time';
