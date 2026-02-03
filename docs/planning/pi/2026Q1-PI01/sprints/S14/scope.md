# Sprint S14 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- Previous Sprint Scope: `docs/planning/pi/2026Q1-PI01/sprints/S13/scope.md`

---

## Committed Scope

### ST-1104: Guardrails (Validation + Rate Limiting)
**Points:** 3
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-011/stories/ST-1104-asr-guardrails.md`
**Workpack:** `docs/planning/workpacks/ST-1104/workpack.md` (to be created)

**What's included:**

**Input Validation:**
- File size check (max 10MB) - return 413 ASR_FILE_TOO_LARGE if exceeded
- Format validation (Content-Type whitelist):
  - audio/ogg
  - audio/mpeg
  - audio/wav
  - audio/mp4
  - audio/webm
- Return 400 ASR_INVALID_FORMAT for unsupported types
- Return 400 ASR_MISSING_FILE if no file part

**Rate Limiting (Bucket4j):**
- POST bucket: 5 requests/minute per user per household
  - Key: `asr:post:{householdId}:{userId}`
- GET bucket: 30 requests/minute per user per household
  - Key: `asr:get:{householdId}:{userId}`
- Return 429 ASR_RATE_LIMITED with Retry-After header

**Idempotency Support:**
- `AsrIdempotencyRecord` entity:
  - householdId, userId, idempotencyKey (composite PK)
  - payloadDigest (SHA-256 of file bytes)
  - transcriptionId (result)
  - createdAt, expiresAt (24h TTL)
- Same key + same digest = return cached transcriptionId (202)
- Same key + different digest = return 409 IDEMPOTENCY_CONFLICT
- Key reusable after TTL expires

**Configuration:**
```yaml
asr:
  guardrails:
    max-size-bytes: 10485760  # 10MB
    allowed-formats:
      - audio/ogg
      - audio/mpeg
      - audio/wav
      - audio/mp4
      - audio/webm
  rate-limit:
    post-requests-per-minute: 5
    get-requests-per-minute: 30
  idempotency:
    ttl-hours: 24
```

**DB Migration:**
```sql
-- V20260203__create_asr_idempotency_record.sql
CREATE TABLE asr_idempotency_record (
    household_id UUID NOT NULL,
    user_id UUID NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    payload_digest VARCHAR(64) NOT NULL,
    transcription_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    PRIMARY KEY (household_id, user_id, idempotency_key)
);

CREATE INDEX idx_idempotency_expires ON asr_idempotency_record(expires_at);
```

**What's NOT included:**
- Audio duration detection (validated by upstream)
- Per-household rate limit configuration
- Admin override for rate limits

**Flags:**
- contract_impact: no (errors already in contract)
- security_sensitive: no

**DoR:** PASS
**Dependencies:** ST-1103 (completed in S13)

---

### ST-1105: Observability (Metrics + Structured Logs)
**Points:** 3
**Priority:** P2
**Status:** Ready
**Story:** `docs/planning/epics/EP-011/stories/ST-1105-asr-observability.md`
**Workpack:** `docs/planning/workpacks/ST-1105/workpack.md` (to be created)

**What's included:**

**Micrometer Metrics:**
- `asr_requests_total{status}` — counter
  - Labels: status=success|error
- `asr_latency_ms{phase}` — histogram/timer
  - Labels: phase=create|poll
- `asr_failures_total{reason}` — counter
  - Labels: reason=invalid_format|too_large|rate_limited|unavailable|timeout|internal

**Structured Logging:**
- JSON format for ASR operations
- Required fields:
  - correlationId
  - userId
  - householdId
  - sizeBytes (for POST)
  - status (success/error)
  - durationMs
  - errorCode (if error)

**Log Sanitization:**
- No file content in logs
- No PII (email, name) in logs
- No API key in logs (mask or exclude)

**Actuator Endpoint:**
- Metrics exposed at /actuator/prometheus
- ASR metrics (asr_*) visible in Prometheus format

**What's NOT included:**
- Alerting rules (ops concern)
- Grafana dashboards (ops concern)
- Distributed tracing spans (v0 uses correlationId)

**Flags:**
- contract_impact: no
- security_sensitive: no

**DoR:** PASS
**Dependencies:** ST-1103 (completed in S13)

---

### ST-1106: Security Boundaries + Integration Tests
**Points:** 3
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-011/stories/ST-1106-asr-security-tests.md`
**Workpack:** `docs/planning/workpacks/ST-1106/workpack.md` (to be created)

**What's included:**

**Membership Enforcement Tests:**
- POST without membership returns 403
- GET without membership returns 403

**IDOR Prevention Tests:**
- GET with wrong household returns 404 (not 403)
- Guessed transcriptionId (no mapping) returns 404
- Expired AsrTranscriptionRef returns 404
- Verify asr-service NOT called when mapping fails

**API Key Leak Prevention:**
- asr-service 401 returns 500 to client (not auth error)
- API key never in error responses
- API key never in logs (grep verification)

**Error Sanitization Tests:**
- Upstream error details not leaked
- Only HomeTusk error format returned
- No stack traces in responses

**WireMock Edge Case Tests:**
- Timeout (35s delay) handled gracefully
- 500 from asr-service returns 500 INTERNAL_ERROR
- 503 from asr-service returns 503 ASR_UNAVAILABLE with Retry-After

**Test Class Structure:**
```java
AsrSecurityIntegrationTest:
  - createTranscription_notMember_returns403
  - getTranscription_notMember_returns403
  - getTranscription_wrongHousehold_returns404_viaMapping
  - getTranscription_guessedId_returns404_noUpstreamCall
  - getTranscription_expiredMapping_returns404
  - asrUnauthorized_returns500NotAuthError
  - asrTimeout_handledGracefully
  - asr5xx_returnsMappedError
  - asr503_returnsUnavailableWithRetryAfter
  - apiKeyNotInLogs_verified
  - apiKeyNotInErrorResponse_verified
```

**What's NOT included:**
- Penetration testing (separate process)
- Rate limiting tests (covered in ST-1104)
- Role-based access tests (all members equal in v0)

**Flags:**
- contract_impact: no
- security_sensitive: yes

**DoR:** PASS
**Dependencies:** ST-1103 (completed in S13)

---

## Out of Scope (Explicit)

### Never in S14 Scope
- Circuit breaker implementation
- Local audio duration detection (complex codec parsing)
- Per-household rate limit configuration
- Alerting rules (Grafana/PagerDuty)
- Distributed tracing (Jaeger/Zipkin spans)
- Web/mobile UI for voice input
- Penetration testing

### Deferred to LATER
- Circuit breaker (add if stability issues arise)
- Admin rate limit overrides
- Dashboard templates (Grafana)
- Alerting playbooks

---

## Acceptance Criteria Summary

**Sprint succeeds if:**

**ST-1104 (Guardrails):**
1. File >10MB returns 413 ASR_FILE_TOO_LARGE
2. Invalid format returns 400 ASR_INVALID_FORMAT
3. POST rate limit (5/min) returns 429 with Retry-After
4. GET rate limit (30/min) returns 429 with Retry-After
5. Idempotency same key+digest returns cached result
6. Idempotency same key+different digest returns 409

**ST-1105 (Observability):**
7. asr_requests_total metric increments
8. asr_latency_ms metric records duration
9. asr_failures_total metric increments with reason
10. Structured logs include correlationId/userId/householdId
11. No PII/API key in logs
12. /actuator/prometheus exposes asr_* metrics

**ST-1106 (Security):**
13. Non-member POST returns 403
14. Non-member GET returns 403
15. Wrong household GET returns 404 (not 403)
16. Guessed ID returns 404 (no upstream call)
17. asr-service 401 returns 500 (not auth error)
18. API key never in logs/responses
19. Timeout handled gracefully
20. 503 returns ASR_UNAVAILABLE with Retry-After

**Sprint fails if:**
- Any guardrail bypassed
- Metrics not exposed
- Cross-household access possible
- API key visible anywhere
- Integration tests fail

---

## Readiness Notes

**All committed stories:**
- Have clear ACs with Given/When/Then format
- Have defined test strategies
- All dependencies completed (S13)
- Backend patterns established

**Key patterns to follow:**
- Validation: similar to existing input validation
- Rate limiting: Bucket4j standard configuration
- Metrics: Micrometer standard patterns
- Structured logging: existing JSON logging config
- Security tests: existing security test patterns

**Human gates:**
- Gate B: approve committed scope (this document)
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)

---

## Story Dependency Graph

```
S13 Complete (ST-1103)
         |
         v
   +-----+-----+-----+
   |     |     |     |
   v     v     v     v
ST-1104 ST-1105 ST-1106
(guard) (obsrv) (secur)
   |     |     |
   +-----+-----+
         |
         v
   EP-011 Complete
   (Milestone M3)
```

**No inter-story dependencies in S14.** All stories depend only on S13 completion.

**Recommended execution order:** ST-1104 -> ST-1105 -> ST-1106
- Rationale: Validation before metrics (metrics can observe validation), security tests last (verify complete behavior)

---

## Technical Notes

### New Backend Files (ST-1104)

```
backend/src/main/java/dev/hometusk/asr/
├── validation/
│   ├── AsrValidationService.java        # Size/format checks
│   └── AsrValidationException.java      # Validation errors
├── ratelimit/
│   ├── AsrRateLimitService.java         # Bucket4j wrapper
│   └── AsrRateLimitConfig.java          # Bucket configuration
├── idempotency/
│   ├── AsrIdempotencyService.java       # Idempotency logic
│   ├── AsrIdempotencyRecord.java        # JPA entity
│   └── AsrIdempotencyRepository.java    # Repository
└── ...

backend/src/main/resources/db/migration/
└── V20260203__create_asr_idempotency_record.sql
```

### New Backend Files (ST-1105)

```
backend/src/main/java/dev/hometusk/asr/
├── observability/
│   ├── AsrMetrics.java                  # Micrometer metrics
│   └── AsrLoggingAspect.java            # Structured logging (or direct in controller)
└── ...
```

### New Test Files (ST-1106)

```
backend/src/test/java/dev/hometusk/asr/
├── AsrSecurityIntegrationTest.java      # Security boundary tests
├── AsrRateLimitIntegrationTest.java     # Rate limit tests (ST-1104)
└── AsrMetricsTest.java                  # Metrics tests (ST-1105)
```

### Configuration Updates

```yaml
# application.yml additions
asr:
  # ... existing config ...
  guardrails:
    max-size-bytes: 10485760
    allowed-formats:
      - audio/ogg
      - audio/mpeg
      - audio/wav
      - audio/mp4
      - audio/webm
  rate-limit:
    post-requests-per-minute: 5
    get-requests-per-minute: 30
  idempotency:
    ttl-hours: 24
```
