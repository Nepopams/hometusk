# Email Notification Outbox

**Type**: Sequence
**Last Updated**: 2026-06-13
**Status**: current

## Purpose

Explain how email intents are enqueued without inline SMTP calls, then delivered
asynchronously with idempotency, retry, and degraded behavior.

## Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Domain as Domain operation
    participant EmailService as EmailNotificationService
    participant DB as PostgreSQL
    participant Worker as EmailNotificationDeliveryJob
    participant Sender as EmailSender
    participant SMTP as SMTP provider or dev sink

    Domain->>EmailService: enqueue(request, idempotencyKey)
    EmailService->>DB: SELECT by idempotency_key
    alt duplicate key exists
        DB-->>EmailService: existing outbox row
        EmailService-->>Domain: existing row reference
    else new email intent
        EmailService->>DB: INSERT status=PENDING
        EmailService-->>Domain: queued row reference
    end

    Worker->>DB: Find due PENDING/RETRY_SCHEDULED rows
    Worker->>DB: SELECT row FOR UPDATE
    Worker->>Sender: send(email message)
    alt provider accepts message
        Sender->>SMTP: deliver
        Sender-->>Worker: success
        Worker->>DB: UPDATE status=SENT, sent_at, attempt_count
    else provider outage or send failure
        Sender-->>Worker: error
        alt attempts remain
            Worker->>DB: UPDATE status=RETRY_SCHEDULED, last_error, next_attempt_at
        else retry limit reached
            Worker->>DB: UPDATE status=FAILED, last_error
        end
    end
```

## Notes

- Domain operations do not call SMTP directly.
- `idempotency_key` represents the business event, not a transport attempt.
- SMTP/provider failures are captured on the outbox row and do not roll back the
  originating domain operation.
- Logs avoid raw recipient addresses and email body content.
