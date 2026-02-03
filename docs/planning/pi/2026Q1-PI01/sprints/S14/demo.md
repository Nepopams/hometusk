# Sprint S14 — Demo Plan

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md`
- Epic: `docs/planning/epics/EP-011/epic.md`

---

## Demo Goal
Demonstrate production-ready ASR proxy with guardrails, observability, and security hardening.

**Audience:** Product Owner, Stakeholders
**Duration:** ~15 minutes

---

## Demo Scenarios

### 1. Guardrails Demo (ST-1104)

#### 1.1 File Size Validation
```bash
# Create 15MB test file
dd if=/dev/urandom of=large.ogg bs=1M count=15

# Attempt upload
curl -X POST "http://localhost:8080/api/v1/households/{id}/asr/transcriptions" \
  -H "Authorization: Bearer {token}" \
  -F "file=@large.ogg"

# Expected: 413 ASR_FILE_TOO_LARGE
```

#### 1.2 Format Validation
```bash
# Attempt with invalid format
curl -X POST "http://localhost:8080/api/v1/households/{id}/asr/transcriptions" \
  -H "Authorization: Bearer {token}" \
  -F "file=@video.mp4;type=video/mp4"

# Expected: 400 ASR_INVALID_FORMAT
```

#### 1.3 Rate Limiting
```bash
# Send 6 rapid POST requests
for i in {1..6}; do
  curl -X POST "..." -F "file=@small.ogg" &
done

# Expected: 6th request returns 429 ASR_RATE_LIMITED with Retry-After header
```

#### 1.4 Idempotency
```bash
# First request with Idempotency-Key
curl -X POST "..." \
  -H "Idempotency-Key: demo-key-123" \
  -F "file=@test.ogg"
# Returns: 202 { "id": "xyz-..." }

# Same request repeated
curl -X POST "..." \
  -H "Idempotency-Key: demo-key-123" \
  -F "file=@test.ogg"
# Expected: 202 with SAME "id" (cached)

# Different file with same key
curl -X POST "..." \
  -H "Idempotency-Key: demo-key-123" \
  -F "file=@different.ogg"
# Expected: 409 IDEMPOTENCY_CONFLICT
```

---

### 2. Observability Demo (ST-1105)

#### 2.1 Metrics Endpoint
```bash
curl http://localhost:8080/actuator/prometheus | grep asr_

# Expected output:
# asr_requests_total{status="success"} 5.0
# asr_requests_total{status="error"} 2.0
# asr_latency_ms_seconds{phase="create",quantile="0.95"} 0.234
# asr_failures_total{reason="rate_limited"} 1.0
```

#### 2.2 Structured Logs
```bash
# Trigger a request, then check logs
cat logs/application.log | jq '. | select(.logger == "AsrController")'

# Expected JSON structure:
# {
#   "timestamp": "2026-02-03T10:15:30.123Z",
#   "level": "INFO",
#   "correlationId": "abc-123",
#   "userId": "user-uuid",
#   "householdId": "household-uuid",
#   "sizeBytes": 1234567,
#   "status": "success",
#   "durationMs": 234
# }
```

#### 2.3 No PII/Secrets in Logs
```bash
# Verify API key not in logs
grep -i "${ASR_API_KEY}" logs/application.log
# Expected: no matches
```

---

### 3. Security Demo (ST-1106)

#### 3.1 Membership Enforcement
```bash
# Request without household membership
curl -X POST ".../households/{other-household-id}/asr/transcriptions" \
  -H "Authorization: Bearer {token-for-different-household}" \
  -F "file=@test.ogg"

# Expected: 403 Forbidden
```

#### 3.2 IDOR Prevention
```bash
# User in household A tries to access transcription from household B
# (transcription T1 belongs to household B)
curl -X GET ".../households/A/asr/transcriptions/T1" \
  -H "Authorization: Bearer {token-for-household-A}"

# Expected: 404 (not 403, to avoid leaking existence)
```

#### 3.3 Guessed ID Prevention
```bash
# Try random UUID that doesn't exist in mapping
curl -X GET ".../households/{id}/asr/transcriptions/$(uuidgen)" \
  -H "Authorization: Bearer {token}"

# Expected: 404 ASR_NOT_FOUND (asr-service NOT called)
```

#### 3.4 API Key Hidden
```bash
# Trigger scenario where asr-service returns 401 (invalid key)
# (use WireMock stub in test environment)

# Expected response: 500 (not 401, no "unauthorized" hint)
# No API key in response body
```

---

## Success Criteria

| Scenario | Expected | Status |
|----------|----------|--------|
| File >10MB rejected | 413 ASR_FILE_TOO_LARGE | [ ] |
| Invalid format rejected | 400 ASR_INVALID_FORMAT | [ ] |
| POST rate limit (6th request) | 429 + Retry-After | [ ] |
| GET rate limit (31st request) | 429 + Retry-After | [ ] |
| Idempotency cache hit | Same transcriptionId | [ ] |
| Idempotency conflict | 409 | [ ] |
| Metrics at /actuator/prometheus | asr_* present | [ ] |
| Structured logs | JSON with required fields | [ ] |
| No API key in logs | grep returns nothing | [ ] |
| Non-member request | 403 | [ ] |
| Wrong household access | 404 (not 403) | [ ] |
| Guessed UUID | 404 (no upstream call) | [ ] |
| asr-service 401 | 500 (not 401) | [ ] |

---

## Demo Environment

**Requirements:**
- Backend running locally
- Test household with member user
- Test audio files (valid OGG, invalid MP4, large file)
- WireMock stubs for asr-service
- Log access for structured log demo

**Pre-demo checklist:**
- [ ] Backend started
- [ ] Test user JWT available
- [ ] Test files prepared
- [ ] WireMock stubs configured
- [ ] Prometheus endpoint accessible

---

## Notes

- Demo order follows feature priority: guardrails -> observability -> security
- All demos can be run via curl (no UI needed)
- WireMock stubs simulate asr-service behavior for edge cases
- After demo, run full test suite to confirm all tests pass
