# ADR-018: Email Notification Outbox Platform

**Status:** Accepted
**Date:** 2026-06-13
**Initiative:** INIT-2026Q2-email-notification-platform

## Context

HomeTusk needs email delivery as a platform capability before product-specific
email notifications can be added safely. Direct SMTP calls from task, shopping,
or command flows would couple domain success to provider availability and make
duplicate sends likely during retries.

Email delivery also needs traceability. A command or domain operation should be
able to create one business-event email intent, store it durably, and let a
worker deliver it later with retry status and observable outcomes.

## Decision

HomeTusk will use a database-backed email notification outbox inside the Stage 1
backend.

### 1. Durable Outbox

The `email_notification_outbox` table stores one email intent per business
event:

- recipient email, subject, text body, optional HTML body;
- delivery status: `PENDING`, `SENT`, `FAILED`, `RETRY_SCHEDULED`,
  `CANCELLED`;
- business-event `idempotency_key` with a unique constraint;
- optional `correlation_id`, `context_type`, and `context_id`;
- attempt count, retry limit, last error, next attempt time, and sent time.

Domain operations enqueue email intents and do not send email inline.

### 2. Sender Boundary

Email provider details stay behind the `EmailSender` interface. The initial
implementations are:

- `log` sender for local/dev verification without a provider;
- `smtp` sender using Spring Mail for local SMTP sinks or production SMTP.

Provider credentials are supplied through configuration and must not be stored
in git.

### 3. Async Delivery And Degraded Mode

`EmailNotificationDeliveryJob` polls due `PENDING` and `RETRY_SCHEDULED` rows
when `hometusk.email.enabled=true`. Each row is processed with a pessimistic row
lock and per-email error handling.

Provider failures do not throw back into domain operations. Failed attempts are
stored on the outbox row. Rows move to `RETRY_SCHEDULED` until the configured
retry limit is reached, then move to `FAILED`.

### 4. Templates And Observability

The platform includes a small `{{token}}` text/HTML renderer to avoid adding a
heavy template engine for MVP email copy.

Micrometer metrics expose:

- outbox row count by status;
- enqueue requests by created/duplicate result;
- delivery outcomes by status;
- delivery failures by reason.

Logs intentionally avoid recipient address and body content.

## Consequences

### Positive

- Domain operations are resilient to SMTP/provider outages.
- Duplicate enqueue by business event returns the existing row instead of
  creating a second email.
- Future product email use cases can depend on one sender, retry, and metrics
  boundary.
- Local verification is possible with either the log sender or an SMTP sink.

### Negative

- Delivery is eventually consistent; a task assignment can succeed before the
  email is sent.
- The Stage 1 backend gains another scheduled worker and outbox table.
- Provider-level exactly-once delivery is not guaranteed; the outbox prevents
  duplicate enqueue, but a provider may still accept a message before a later
  database commit failure.

### Risks And Mitigations

| Risk | Mitigation |
|------|------------|
| Duplicate enqueue | Unique `idempotency_key` and duplicate-return service behavior. |
| Provider outage | Async retry and terminal `FAILED` status after max attempts. |
| PII leakage in logs | Log sender emits recipient hash and lengths only. Delivery logs do not include recipient/body. |
| Vendor lock-in | Domain code depends on `EmailNotificationService` and `EmailSender`, not SMTP APIs. |

## Alternatives Considered

### Direct Send From Domain Services

Rejected. It would make task/shopping/command success depend on SMTP latency and
availability, and retries could send duplicate email.

### Reuse In-App Notifications Table

Rejected for now. In-app notifications are per-user UI state. Email delivery has
different fields, retry semantics, provider status, and idempotency requirements.

### External Queue First

Deferred. A broker can replace the polling mechanism later, but PostgreSQL
outbox is enough for the current monolith and keeps local development simple.

## Migration And Rollback

Migration `V030__create_email_notification_outbox.sql` creates a new additive
table and indexes. Runtime rollback can disable `hometusk.email.enabled` to stop
delivery while preserving queued rows. Schema rollback can drop the table before
product use cases depend on it.

## Related

- Initiative: `docs/planning/initiatives/INIT-2026Q2-email-notification-platform.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Diagram: `docs/diagrams/sequence-email-notification-outbox.md`
- Predecessor: `docs/adr/017-user-email-state-source-of-truth.md`
- Next use case: `docs/planning/initiatives/INIT-2026Q2-task-assignment-email-notifications.md`
