-- V030: Create email notification outbox for async delivery

CREATE TABLE email_notification_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_text TEXT NOT NULL,
    body_html TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(255) NOT NULL,
    correlation_id UUID,
    context_type VARCHAR(64),
    context_id UUID,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    last_error TEXT,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT email_notification_status_check CHECK (
        status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_SCHEDULED', 'CANCELLED')
    ),
    CONSTRAINT email_notification_attempts_check CHECK (
        attempt_count >= 0 AND max_attempts >= 1 AND attempt_count <= max_attempts
    )
);

CREATE UNIQUE INDEX idx_email_notification_outbox_idempotency_key
ON email_notification_outbox(idempotency_key);

CREATE INDEX idx_email_notification_outbox_due
ON email_notification_outbox(next_attempt_at, created_at)
WHERE status IN ('PENDING', 'RETRY_SCHEDULED');

CREATE INDEX idx_email_notification_outbox_status
ON email_notification_outbox(status);

CREATE INDEX idx_email_notification_outbox_correlation_id
ON email_notification_outbox(correlation_id);

CREATE INDEX idx_email_notification_outbox_context
ON email_notification_outbox(context_type, context_id);

COMMENT ON TABLE email_notification_outbox IS 'Async email notifications awaiting delivery through EmailNotificationDeliveryJob';
COMMENT ON COLUMN email_notification_outbox.idempotency_key IS 'Business-event idempotency key; duplicate enqueue returns the existing outbox row';
COMMENT ON COLUMN email_notification_outbox.correlation_id IS 'Correlation ID linking email delivery to the originating command or domain operation';
COMMENT ON COLUMN email_notification_outbox.context_type IS 'Optional business context type, for example task or system';
COMMENT ON COLUMN email_notification_outbox.context_id IS 'Optional business context identifier';
COMMENT ON COLUMN email_notification_outbox.next_attempt_at IS 'Earliest timestamp when the delivery worker may attempt to send this email';
