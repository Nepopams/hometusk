-- V016: Update enum check constraints for new statuses and activity types

ALTER TABLE commands DROP CONSTRAINT commands_status_check;
ALTER TABLE commands ADD CONSTRAINT commands_status_check CHECK (
    status IN ('received', 'validating', 'processing', 'needs_input', 'executed', 'failed', 'rejected')
);

ALTER TABLE task_activities DROP CONSTRAINT task_activities_type_check;
ALTER TABLE task_activities ADD CONSTRAINT task_activities_type_check CHECK (
    activity_type IN (
        'task_created', 'task_assigned', 'task_started',
        'task_completed', 'task_cancelled', 'task_updated',
        'shopping_item_added', 'shopping_item_purchased', 'shopping_item_deleted'
    )
);
