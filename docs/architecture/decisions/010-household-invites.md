# ADR-010: Household Invites (Token-Based)

**Status:** Accepted  
**Date:** 2026-01-16  
**Context:** MVP Closure / Iteration 1 / Step 2

## Context

Web MVP requires a household invite flow without AI dependencies:
1. Member creates invite token for their household.
2. Authenticated user accepts invite by token (no householdId in request).
3. Membership is created transactionally and the invite becomes single-use.

We must preserve anti-IDOR boundaries and consistent error semantics.

## Decision

### Token Format
- Prefix: `hti_`
- Body: 32 bytes from `SecureRandom`, encoded with Base64URL (RFC 4648 §5), no padding.

### Invite Lifecycle
- Statuses: `ACTIVE`, `REDEEMED`, `EXPIRED`, `REVOKED`
- Default expiry: 7 days from creation
- Single-use: once redeemed, subsequent accepts return **410 Gone**

### Access Control
- Create invite: any household member (no role expansion)
- Accept invite: JWT required; request contains **only** `inviteToken` (anti-IDOR)

### Status Codes
- Invalid token: **404 Not Found**
- Expired / redeemed / revoked: **410 Gone**
- Already member on accept (token ACTIVE and not expired): **200 OK** (no-op, do not redeem token)
- Non-member creating invite: **403 Forbidden**

### Concurrency Safety
Accept is transactional and uses row-level locking (PESSIMISTIC_WRITE) to ensure:
- One accept succeeds, concurrent accept returns **410 Gone**.

## Consequences

### Positive
- Clear anti-IDOR boundary (token-only acceptance)
- Deterministic concurrency behavior
- Simple, auditable invite lifecycle

### Negative
- Active token remains usable if an existing member “accepts” it (explicit no-op)
- Token distribution is out of scope (no delivery in Step 2)

## References
- `docs/contracts/http/commands.openapi.yaml` (public API)
- `docs/architecture/service-catalog.md`
