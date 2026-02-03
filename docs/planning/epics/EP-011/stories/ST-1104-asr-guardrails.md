# Story: ST-1104 — Guardrails (Validation + Rate Limiting)

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Blocked by ST-1103 (endpoints must exist)

## User Value
> "Система говорит мне если файл слишком большой или формат неправильный — до отправки в asr-service."

---

## Description
Implement input validation and rate limiting:
- File size check (max 10MB)
- Audio duration check (max 60s, if detectable)
- Format validation (allowed MIME types)
- Rate limiting per user per household
- Return 429 with Retry-After

---

## In Scope
- File size validation (before forwarding)
- Content-Type validation (allowed formats incl. audio/webm)
- **Separate rate limit buckets** (see epic Decision H):
  - POST: 5 req/min (strict, expensive operation)
  - GET: 30 req/min (softer, polling control)
- **Idempotency handling** via `AsrIdempotencyRecord` (see epic Decision F)
- Configuration in application.yml
- Return proper error codes (ASR_FILE_TOO_LARGE, ASR_INVALID_FORMAT, ASR_RATE_LIMITED, IDEMPOTENCY_CONFLICT)
- Retry-After header for 429

## Out of Scope
- **Audio duration detection locally** (complex codec parsing, skipped in v0)
- Per-household rate limit configuration (use global default)
- Admin override for rate limits

**Clarification on duration validation:** HomeTusk does NOT parse audio files to detect duration. Instead:
- If asr-service returns `AUDIO_TOO_LONG`, HomeTusk maps it to `ASR_AUDIO_TOO_LONG` (400)
- This is "validated by upstream, mapped back" pattern

---

## Acceptance Criteria

### AC-1: File size validated
```
Given file larger than 10MB
When POST /households/{id}/asr/transcriptions
Then response 413 with code "ASR_FILE_TOO_LARGE"
And asr-service NOT called
```

### AC-2: Format validated
```
Given file with Content-Type "video/mp4"
When POST /households/{id}/asr/transcriptions
Then response 400 with code "ASR_INVALID_FORMAT"
And message lists allowed formats
```

### AC-3: Allowed formats work (incl. webm)
```
Given file with Content-Type "audio/ogg" OR "audio/webm"
When POST /households/{id}/asr/transcriptions
Then validation passes (continues to asr-service)

Allowed formats:
- audio/ogg (OGG/Opus)
- audio/mpeg (MP3)
- audio/wav (WAV)
- audio/mp4 (M4A)
- audio/webm (WebM/Opus - native web MediaRecorder)
```

### AC-4: POST rate limiting enforced (strict)
```
Given user made 5 POST requests in last minute
When 6th POST request arrives
Then response 429 with:
  - code: "ASR_RATE_LIMITED"
  - Retry-After header (seconds)
```

### AC-5: Rate limit per user per household (isolated buckets)
```
Given user A in household H hit POST rate limit (5/min)
When user B in household H makes POST request
Then user B request allowed (separate bucket per user)
```

### AC-6: Rate limit configuration (separate limits)
```
Given application.yml with:
  asr.rate-limit.post-requests-per-minute: 5
  asr.rate-limit.get-requests-per-minute: 30
When configuration loaded
Then POST uses 5/min bucket
And GET uses 30/min bucket (separate)
```

### AC-7: Missing file returns error
```
Given POST without file part
When endpoint called
Then response 400 with code "ASR_MISSING_FILE"
```

### AC-8: GET rate limiting enforced (softer, separate bucket)
```
Given user made 30 GET requests in last minute
When 31st GET request arrives
Then response 429 with code "ASR_RATE_LIMITED"
And Retry-After header present
Note: GET bucket (30/min) is separate from POST bucket (5/min)
```

### AC-9: Idempotency - same key + same file returns cached result
```
Given POST with Idempotency-Key="key-123" and file with SHA-256 digest "abc..."
And previous request with same key+digest returned transcriptionId="xyz"
When same POST repeated
Then response 202 with same transcriptionId="xyz"
And asr-service NOT called (short-circuit)
```

### AC-10: Idempotency - same key + different file returns conflict
```
Given POST with Idempotency-Key="key-123" already used with different file
When new POST with Idempotency-Key="key-123" but different file digest
Then response 409 with code "IDEMPOTENCY_CONFLICT"
And asr-service NOT called
```

### AC-11: Idempotency key TTL (24 hours)
```
Given AsrIdempotencyRecord older than 24 hours
When same Idempotency-Key used
Then treated as new request (key can be reused)
```

---

## Test Strategy

### Unit Tests
- `AsrValidationServiceTest`:
  - `validateSize_tooLarge_throwsException`
  - `validateFormat_invalidMime_throwsException`
  - `validateFormat_validOgg_passes`

### Integration Tests
- `AsrRateLimitIntegrationTest`:
  - `rateLimitExceeded_returns429`
  - `rateLimitPerUser_isolatedBuckets`
  - `retryAfterHeader_present`

---

## Points
**3 points**

## Dependencies
- ST-1103 (controller must exist to add validation)

## Flags
- contract_impact: no (errors already in contract)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no
