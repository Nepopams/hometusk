-- V006: Create shopping tables
-- Shopping lists and items within a household

-- Shopping lists
CREATE TABLE shopping_lists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL DEFAULT 'Default',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT shopping_lists_household_name_unique UNIQUE (household_id, name)
);

CREATE INDEX idx_shopping_lists_household_id ON shopping_lists(household_id);

-- Shopping items
CREATE TABLE shopping_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_list_id    UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    quantity            INTEGER DEFAULT 1,
    unit                VARCHAR(50),
    is_purchased        BOOLEAN NOT NULL DEFAULT FALSE,
    added_by_id         UUID NOT NULL REFERENCES users(id),
    command_id          UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    purchased_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_shopping_items_list_id ON shopping_items(shopping_list_id);
CREATE INDEX idx_shopping_items_command_id ON shopping_items(command_id) WHERE command_id IS NOT NULL;

COMMENT ON TABLE shopping_lists IS 'Shopping list containers within a household';
COMMENT ON TABLE shopping_items IS 'Items in a shopping list';
