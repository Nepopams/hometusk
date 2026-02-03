# Workpack: ST-1104 — Guardrails (Validation + Rate Limiting)

## Sources of Truth
- Story: `docs/planning/epics/EP-011/stories/ST-1104-asr-guardrails.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md`
- Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Goal
Implement input validation, rate limiting, and idempotency for ASR proxy endpoints.

---

## Scope

### In Scope
- File size validation (max 10MB) → 413 ASR_FILE_TOO_LARGE
- Content-Type validation → 400 ASR_INVALID_FORMAT
- Missing file → 400 ASR_MISSING_FILE
- Rate limiting with Bucket4j:
  - POST: 5 req/min per user per household
  - GET: 30 req/min per user per household
- Idempotency via AsrIdempotencyRecord (Decision F)
- Retry-After header for 429

### Out of Scope
- Audio duration detection locally
- Per-household rate limit configuration
- Circuit breaker

---

## Files to Change

| File | Action |
|------|--------|
| `build.gradle.kts` | ADD Bucket4j dependency |
| `V024__create_asr_idempotency.sql` | CREATE migration |
| `AsrIdempotencyRecord.java` | CREATE entity |
| `AsrIdempotencyRecordRepository.java` | CREATE repository |
| `AsrValidationService.java` | CREATE validation |
| `AsrRateLimitService.java` | CREATE rate limiting |
| `AsrIdempotencyService.java` | CREATE idempotency |
| `AsrMissingFileException.java` | CREATE exception |
| `AsrIdempotencyConflictException.java` | CREATE exception |
| `AsrProperties.java` | MODIFY add guardrails config |
| `AsrController.java` | MODIFY add validation/rate limit |
| `AsrService.java` | MODIFY integrate idempotency |
| `GlobalExceptionHandler.java` | MODIFY add handlers |
| `application.yml` | MODIFY add config |

---

## Implementation Plan

1. Add Bucket4j dependency to build.gradle.kts
2. Create V024 migration for asr_idempotency_records
3. Create AsrIdempotencyRecord entity + repository
4. Extend AsrProperties with guardrails/rate-limit config
5. Create AsrValidationService (size, format, missing)
6. Create AsrMissingFileException, AsrIdempotencyConflictException
7. Create AsrRateLimitService (POST 5/min, GET 30/min)
8. Create AsrIdempotencyService (SHA-256, lookup, conflict)
9. Update GlobalExceptionHandler
10. Update AsrController + AsrService
11. Add application.yml config
12. Create unit tests (AsrValidationServiceTest)
13. Create integration tests (rate limit, idempotency)

---

## Verification Commands

```bash
./gradlew build
./gradlew test --tests "*Asr*"
./gradlew spotlessCheck
```

---

## AC Mapping

| AC | Description |
|----|-------------|
| AC-1 | File >10MB → 413 |
| AC-2 | Invalid format → 400 |
| AC-3 | webm passes |
| AC-4 | POST 5/min → 429 |
| AC-5 | Isolated buckets |
| AC-6 | Config in yml |
| AC-7 | Missing file → 400 |
| AC-8 | GET 30/min → 429 |
| AC-9 | Same key+file → cached |
| AC-10 | Same key+diff → 409 |
| AC-11 | 24h TTL |
