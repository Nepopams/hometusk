# Story: ST-1106 — Security Boundaries + Integration Tests

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Blocked by ST-1103 (endpoints must exist)

## User Value
> "Никто извне моего домохозяйства не получит доступ к моим транскрипциям. API-ключ asr-service не утечёт."

---

## Description
Verify and test security boundaries:
- Household membership enforcement
- No cross-household access (IDOR prevention)
- API key never exposed
- Comprehensive integration tests

---

## In Scope
- Integration tests for 403 scenarios
- IDOR tests (access transcription from wrong household)
- **AsrTranscriptionRef boundary validation tests** (IDOR prevention via mapping)
- API key leak prevention tests
- Error response sanitization tests
- WireMock tests for edge cases

## Out of Scope
- Penetration testing (separate process)
- Rate limiting tests (ST-1104)
- Role-based access (all members equal for v0)

---

## Acceptance Criteria

### AC-1: POST - membership required
```
Given user NOT member of householdId
When POST /households/{householdId}/asr/transcriptions
Then response 403 Forbidden
And asr-service NOT called
```

### AC-2: GET - membership required
```
Given user NOT member of householdId
When GET /households/{householdId}/asr/transcriptions/{id}
Then response 403 Forbidden
```

### AC-3: GET - wrong household returns 404 (not 403)
```
Given transcription T1 created in household A
And user is member of household B
When GET /households/B/asr/transcriptions/T1
Then response 404 (not 403, to avoid leaking existence)
```

### AC-4: No cross-household access via mapping validation
```
Given household A has transcription T1 (AsrTranscriptionRef exists with householdId=A)
And user is member of household B only
When GET /households/B/asr/transcriptions/T1
Then 404 Not Found (AsrTranscriptionRef lookup fails: householdId mismatch)
And asr-service NOT called (short-circuit before upstream call)
```

### AC-5: API key not in error response
```
Given asr-service returns 401 (invalid API key)
When HomeTusk processes error
Then response to client does NOT contain API key
And response code is 500 (internal error, not auth error)
```

### AC-6: API key not in logs
```
Given any error scenario
When logs written
Then API key value never appears in logs
```

### AC-7: Upstream errors sanitized
```
Given asr-service returns error with internal details
When HomeTusk returns error to client
Then sensitive upstream details removed
And only HomeTusk error format returned
```

### AC-8: Timeout test (WireMock)
```
Given WireMock configured to delay 35s
When client calls create transcription
Then response within reasonable time (timeout triggers)
And error code indicates timeout
```

### AC-9: 5xx from asr-service handled
```
Given WireMock returns 500
When client calls create transcription
Then response 500 with INTERNAL_ERROR
And no stack trace leaked
```

### AC-10: 503 from asr-service handled
```
Given WireMock returns 503 with Retry-After: 30
When client calls (and retries exhausted)
Then response 503 with ASR_UNAVAILABLE
And Retry-After header forwarded
```

### AC-11: Guessed transcriptionId returns 404
```
Given user guesses random UUID not in AsrTranscriptionRef
When GET /households/{householdId}/asr/transcriptions/{guessedId}
Then response 404 ASR_NOT_FOUND
And asr-service NOT called (no mapping = no upstream call)
```

### AC-12: Expired AsrTranscriptionRef treated as not found
```
Given AsrTranscriptionRef with expiresAt < now (expired)
When GET /households/{householdId}/asr/transcriptions/{id}
Then response 404 ASR_NOT_FOUND
(expired records should be cleaned up; if found, treat as not found)
```

---

## Test Strategy

### Integration Tests (WireMock)
- `AsrSecurityIntegrationTest`:
  - `createTranscription_notMember_returns403`
  - `getTranscription_notMember_returns403`
  - `getTranscription_wrongHousehold_returns404_viaMapping`
  - `getTranscription_guessedId_returns404_noUpstreamCall`
  - `getTranscription_expiredMapping_returns404`
  - `asrUnauthorized_returns500NotAuthError`
  - `asrTimeout_handledGracefully`
  - `asr5xx_returnsMappedError`
  - `asr503_returnsUnavailableWithRetryAfter`

### AsrTranscriptionRef Boundary Tests
- Verify GET checks mapping BEFORE calling asr-service
- Verify wrong household returns 404 (not 403)
- Verify guessed UUID returns 404 (no upstream call)

### API Key Leak Tests
- Search log output for API key pattern
- Verify error responses do not contain API key

---

## Points
**3 points**

## Dependencies
- ST-1103 (endpoints must exist)

## Flags
- contract_impact: no
- adr_needed: no
- diagrams_needed: no
- security_sensitive: yes (this IS the security story)
