# Story: ST-1103 — ASR Proxy Endpoints (Controller)

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- HomeTusk Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Blocked by ST-1102 (needs AsrClient)

## User Value
> "Могу отправить аудио через HomeTusk API и получить текст, не зная про asr-service."

---

## Description
Implement REST controller for ASR proxy:
- `AsrController` with POST/GET endpoints
- JWT authentication
- Household membership enforcement
- Map AsrClient calls to HTTP responses
- Map exceptions to error responses per contract

---

## In Scope
- `AsrController`:
  - `POST /api/v1/households/{householdId}/asr/transcriptions`
  - `GET /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}`
- JWT authentication (existing Spring Security)
- Household membership check (existing `MembershipService`)
- **Persist `AsrTranscriptionRef`** on successful POST (for IDOR prevention)
- **Validate `AsrTranscriptionRef`** on GET before calling asr-service
- Map `AsrException` subtypes to HTTP error responses
- Generate correlationId if not present
- Integration tests (happy path)

## Out of Scope
- Input validation beyond basic (ST-1104)
- Rate limiting (ST-1104)
- Metrics (ST-1105)
- Security edge cases (ST-1106)

---

## Acceptance Criteria

### AC-1: POST endpoint works
```
Given authenticated user who is member of householdId
When POST /households/{householdId}/asr/transcriptions with audio file
Then response 202 with:
  - id (uuid)
  - status = "queued"
  - createdAt
```

### AC-2: GET endpoint works with polling hint
```
Given existing transcription created by household member
When GET /households/{householdId}/asr/transcriptions/{id}
Then response 200 with transcription details
And if status is "queued" or "processing":
  - response includes pollAfterMs (e.g., 2000)
  - client should wait pollAfterMs before next poll
```

### AC-3: Authentication required
```
Given no JWT token
When calling any ASR endpoint
Then response 401 Unauthorized
```

### AC-4: Membership required
```
Given authenticated user NOT member of householdId
When calling POST /households/{householdId}/asr/transcriptions
Then response 403 Forbidden
```

### AC-5: Error responses mapped
```
Given AsrClient throws AsrInvalidFormatException
When controller handles exception
Then response 400 with:
  - code: "ASR_INVALID_FORMAT"
  - message: human-readable
  - correlationId: <uuid>
```

### AC-6: CorrelationId generated if missing
```
Given request without X-Correlation-ID header
When endpoint called
Then correlationId generated (UUID)
And passed to AsrClient
And returned in response header
```

### AC-7: CorrelationId preserved if present
```
Given request with X-Correlation-ID: "f47ac10b-58cc-4372-a567-0e02b2c3d479"
When endpoint called
Then same correlationId used throughout
And returned in X-Correlation-ID response header
```

### AC-8: Transcription scoped to household
```
Given transcription created in household A
When user from household B tries GET /households/B/asr/transcriptions/{id}
Then response 404 (not 403, to avoid leaking existence)
```

### AC-9: AsrTranscriptionRef persisted on POST success
```
Given successful POST to asr-service returning transcriptionId
Then AsrTranscriptionRef record created with:
  - transcriptionId
  - householdId
  - createdByUserId
  - createdAt
  - expiresAt (createdAt + 7 days)
```

### AC-10: GET validates boundary via AsrTranscriptionRef
```
Given GET /households/{householdId}/asr/transcriptions/{transcriptionId}
When AsrTranscriptionRef lookup by transcriptionId
  AND householdId does NOT match stored value
Then response 404 ASR_NOT_FOUND
And asr-service NOT called (short-circuit)
```

---

## Test Strategy

### Integration Tests
- `AsrControllerIntegrationTest`:
  - `createTranscription_asMember_returns202`
  - `createTranscription_notMember_returns403`
  - `createTranscription_noAuth_returns401`
  - `getTranscription_asMember_returns200`
  - `getTranscription_wrongHousehold_returns404`
  - `createTranscription_asrError_returnsMappedError`

### WireMock
- Mock asr-service for integration tests

---

## Points
**5 points**

## Dependencies
- ST-1102 (AsrClient must exist)
- ST-1101 (contract defines response shapes)

## Flags
- contract_impact: yes (implements contract)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: yes (authz enforcement)
