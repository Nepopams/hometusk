# ADR-011: In-App Notifications Stub (Per-Recipient)

**Status:** Accepted  
**Date:** 2026-01-16  
**Context:** MVP Closure / Iteration 1 / Step 3

## Context

Web MVP needs a minimal, in-app notification feed without external delivery.
Notifications must be household-scoped, anti-IDOR safe, and created in the same transaction as
the triggering business event. No realtime, no push/email/SMS, and no new services.

## Decision

### Storage Model
- Use a `notifications` table with **one row per recipient**.
- Columns: `id`, `household_id`, `user_id`, `type`, `payload_json`, `created_at`, `read_at`, `correlation_id`.
- `payload_json` stores a small JSON object (`actorUserId`, `entityId`, `entityType`, `summary`).

### API
- `GET /api/v1/households/{id}/notifications?since=&limit=`
  - `since` is an RFC3339 timestamp string.
  - `limit` default 50, max 200.
- `POST /api/v1/notifications/{id}/read`
  - Idempotent; returns 200 on repeat.
  - Ownership enforced; unknown/foreign IDs return 404.

### Notification Types (MVP)
- `INVITE_ACCEPTED` → notify inviter
- `TASK_ASSIGNED` → notify assignee (skip actor self-notify)
- `TASK_COMPLETED` → notify creator + assignee, excluding actor
- `SHOPPING_ITEM_ADDED` → notify household members, excluding actor
- `SHOPPING_ITEM_PURCHASED` → notify household members, excluding actor

### Consistency
- Notifications are created **in the same transaction** as the business change.
- No outbox or async relay in Step 3.

### Security
- Listing notifications requires `requireMembership(householdId)`.
- Marking read enforces ownership; no householdId in the request.

## Consequences

### Positive
- Simple and safe read/ack semantics for MVP.
- No cross-household leakage (anti-IDOR).
- Minimal UI payload with clear recipient rules.

### Negative
- Per-recipient rows increase write volume on household-wide events.
- Payload is a JSON blob; schema changes require care.

## References
- `docs/contracts/http/commands.openapi.yaml`
- `docs/architecture/service-catalog.md`
- `docs/mvp/api-coverage.md`
