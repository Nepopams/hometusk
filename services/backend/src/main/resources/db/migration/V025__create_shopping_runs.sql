-- Shopping runs (shopping trip snapshots)
CREATE TABLE IF NOT EXISTS shopping_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL,
    source_list_id  UUID NOT NULL,
    list_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by_id   UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_shopping_runs_household
        FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_runs_source_list
        FOREIGN KEY (source_list_id) REFERENCES shopping_lists(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shopping_runs_created_by
        FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT chk_shopping_runs_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

-- Shopping run items (snapshots of list items)
CREATE TABLE IF NOT EXISTS shopping_run_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id            UUID NOT NULL,
    original_item_id  UUID,
    name              VARCHAR(255) NOT NULL,
    quantity          INTEGER NOT NULL DEFAULT 1,
    unit              VARCHAR(50),
    purchased         BOOLEAN NOT NULL DEFAULT FALSE,
    purchased_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_shopping_run_items_run
        FOREIGN KEY (run_id) REFERENCES shopping_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_run_items_original
        FOREIGN KEY (original_item_id) REFERENCES shopping_items(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_shopping_runs_household_id
    ON shopping_runs(household_id);
CREATE INDEX IF NOT EXISTS idx_shopping_runs_source_list_id
    ON shopping_runs(source_list_id);
CREATE INDEX IF NOT EXISTS idx_shopping_run_items_run_id
    ON shopping_run_items(run_id);
CREATE INDEX IF NOT EXISTS idx_shopping_run_items_original_item_id
    ON shopping_run_items(original_item_id)
    WHERE original_item_id IS NOT NULL;
