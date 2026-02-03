# Story: ST-1101 — ASR Proxy Contract (OpenAPI)

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — No blockers, can proceed

## User Value
> "Контракт HomeTusk ASR proxy определён до реализации — фронтенд/бот могут планировать интеграцию параллельно."

---

## Description
Define OpenAPI contract for HomeTusk ASR proxy endpoints:
- POST /households/{householdId}/asr/transcriptions
- GET /households/{householdId}/asr/transcriptions/{id}
- Error responses mapped from asr-service
- Request/response schemas

This is a **contract-first** story: no implementation, only specification.

---

## In Scope
- Create `docs/contracts/http/asr-proxy.openapi.yaml`
- Define endpoints with path parameters, request/response schemas
- Define error responses (400, 401, 403, 404, 409, 413, 429, 500, 503) for both POST and GET
- Map asr-service error codes to HomeTusk error codes (with prefix ASR_)
- Document limitations (incl. webm NOT supported in v0)
- Add to contracts-index.md
- Validate OpenAPI with spectral or similar

## Out of Scope
- Implementation (ST-1102, ST-1103)
- Rate limiting logic (ST-1104)
- Metrics definition (ST-1105)

---

## Acceptance Criteria

### AC-1: OpenAPI file created
```
Given docs/contracts/http/asr-proxy.openapi.yaml
Then file exists and is valid OpenAPI 3.0+
And passes spectral/openapi-generator validation
```

### AC-2: POST endpoint defined
```
Given POST /api/v1/households/{householdId}/asr/transcriptions
Then request body is multipart/form-data with:
  - file (required, binary)
  - languageHint (optional, enum: ru, en, auto)
And response 202 returns:
  - id (uuid)
  - status (queued)
  - createdAt (datetime)
```

### AC-3: GET endpoint defined
```
Given GET /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}
Then response 200 returns:
  - id, status, text (nullable), error (nullable)
  - durationMs, lang, createdAt, finishedAt
```

### AC-4: Error responses defined for POST
```
Given POST error scenarios
Then following error codes defined:
  - 400: ASR_INVALID_FORMAT, ASR_AUDIO_TOO_LONG, ASR_MISSING_FILE
  - 401: UNAUTHORIZED
  - 403: FORBIDDEN (not household member)
  - 409: IDEMPOTENCY_CONFLICT
  - 413: ASR_FILE_TOO_LARGE
  - 429: ASR_RATE_LIMITED (with Retry-After header)
  - 500: INTERNAL_ERROR
  - 503: ASR_UNAVAILABLE (with Retry-After header)
```

### AC-4a: Error responses defined for GET
```
Given GET error scenarios
Then following error codes defined:
  - 401: UNAUTHORIZED
  - 403: FORBIDDEN
  - 404: ASR_NOT_FOUND (incl. wrong household returns 404)
  - 429: ASR_RATE_LIMITED (with Retry-After header)
  - 500: INTERNAL_ERROR
  - 503: ASR_UNAVAILABLE (with Retry-After header)
```

### AC-5: Security scheme defined
```
Given security requirements
Then JWT Bearer auth required on both endpoints
And householdId path parameter documented as membership-scoped
```

### AC-6: Contracts index updated
```
Given new contract
Then docs/_indexes/contracts-index.md updated with:
  - ASR Proxy API | Web/Bot | HomeTusk Backend | v1 | draft | Link
```

### AC-7: Limitations documented
```
Given OpenAPI info.description
Then limitations section includes:
  - Max duration: 60 seconds
  - Max size: 10 MB
  - Supported formats: OGG/Opus, MP3, WAV, M4A
  - Note: audio/webm NOT supported in v0; web must convert to OGG/Opus
```

---

## Test Strategy

### Validation
- Run `npx @stoplight/spectral-cli lint docs/contracts/http/asr-proxy.openapi.yaml`
- Generate TypeScript/Java types to verify schema correctness

### Review
- Contract-owner agent or manual review against external ASR contract

---

## Points
**2 points**

## Dependencies
- None (contract-first, no code dependency)

## Flags
- contract_impact: yes (this IS the contract)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no (contract only)
