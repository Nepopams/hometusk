-- V014: Create notifications table
-- Per-recipient in-app notifications for household events

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    read_at TIMESTAMP WITH TIME ZONE,
    correlation_id UUID,
    CONSTRAINT notifications_type_check CHECK (
        type IN (
            'INVITE_ACCEPTED',
            'TASK_ASSIGNED',
            'TASK_COMPLETED',
            'SHOPPING_ITEM_ADDED',
            'SHOPPING_ITEM_PURCHASED'
        )
    )
);

CREATE INDEX idx_notifications_household_user_created_at
ON notifications(household_id, user_id, created_at DESC);

CREATE INDEX idx_notifications_household_user_read_at
ON notifications(household_id, user_id, read_at);

COMMENT ON TABLE notifications IS 'Per-recipient in-app notifications';
COMMENT ON COLUMN notifications.payload_json IS 'Serialized notification payload';
