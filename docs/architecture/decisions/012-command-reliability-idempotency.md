# ADR-012: Command Reliability & Idempotency (v2)

**Status:** Accepted  
**Date:** 2026-01-17  
**Context:** MVP Closure / Iteration 2 / Step 1

## Context

`POST /api/v1/commands` must be safe under client retries and external AI Platform instability:
- clients may retry the same command (network errors, timeouts)
- AI Platform may timeout, return 5xx, or be temporarily unavailable
- AI output must be validated against JSON Schema and guardrails before execution

## Decision

### Idempotency-Key (HTTP)
- Header: `Idempotency-Key`
- Format: `^[A-Za-z0-9._-]{1,128}$` (single header only)
- Scope: unique per **initiator user**
- TTL: **24 hours**
- Same key + same request hash → return stored response (200)
- Same key + different payload → **409 IDEMPOTENCY_CONFLICT**
- In-progress duplicate (same key, no stored response yet) → **409 IDEMPOTENCY_CONFLICT**

**Request hash** is SHA-256 over canonical JSON of the request body.

### AI Platform resilience
- Client timeouts enforced via `aiplatform.timeout-ms`
- Resilience4j used for retry + circuit breaker
  - Retry max attempts: 2 (initial + 1 retry)
  - Exponential backoff with jitter
  - Circuit breaker COUNT_BASED with failure rate threshold
- AI failures (timeout / open circuit / 5xx after retries) **degrade** to fallback decision and return
  `executed_degraded` (HTTP 200) with explicit `degradedReason=ai_unavailable`

### Ordering guarantees
1) AI response schema validation
2) Map decision → proposed actions
3) Guardrails evaluation (before any execution)
4) Action execution

### Observability
- `X-Correlation-ID` propagates through request → AI call → DecisionLog → response
- DecisionLog always written for command lifecycle

## Consequences

### Positive
- Safe retries without duplicate actions
- Deterministic behavior under AI instability
- Clear ordering of schema validation and guardrails

### Negative
- Idempotency storage requires cleanup (TTL-based)
- In-progress duplicates return 409 instead of waiting

## References
- `docs/contracts/http/commands.openapi.yaml`
- `docs/architecture/service-catalog.md`
- `docs/mvp/api-coverage.md`
