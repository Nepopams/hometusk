# Workpack: ST-1101 — ASR Proxy Contract (OpenAPI)

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1101-asr-proxy-contract.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- Contracts Index: `docs/_indexes/contracts-index.md`

---

## Status
**Ready** — Contract file exists, validation and review remaining

---

## Outcome

OpenAPI contract for HomeTusk ASR proxy endpoints is validated and indexed, enabling frontend/bot teams to plan integration in parallel with backend implementation.

---

## Acceptance Criteria Summary

| AC | Description | Status |
|----|-------------|--------|
| AC-1 | OpenAPI file created and valid | VERIFY |
| AC-2 | POST endpoint defined (multipart, languageHint, 202 response) | VERIFY |
| AC-3 | GET endpoint defined (status, text, error, etc.) | VERIFY |
| AC-4 | POST error responses defined (400, 401, 403, 409, 413, 429, 500, 503) | VERIFY |
| AC-4a | GET error responses defined (401, 403, 404, 429, 500, 503) | VERIFY |
| AC-5 | Security scheme (JWT Bearer) defined | VERIFY |
| AC-6 | Contracts index updated | VERIFY |
| AC-7 | Limitations documented (60s, 10MB, formats) | VERIFY |

---

## Scope

### In Scope
- Validate existing `docs/contracts/http/asr-proxy.openapi.yaml` with spectral
- Verify all ACs against contract content
- Confirm contracts-index.md entry is correct

### Out of Scope
- Implementation (ST-1102, ST-1103)
- Rate limiting logic (ST-1104)
- Metrics definition (ST-1105)
- Any code changes

---

## Files to Verify (no changes expected)

| File | Action |
|------|--------|
| `docs/contracts/http/asr-proxy.openapi.yaml` | Validate with spectral |
| `docs/_indexes/contracts-index.md` | Verify entry exists |

---

## Implementation Plan

### Step 1: Validate OpenAPI with spectral
**File:** `docs/contracts/http/asr-proxy.openapi.yaml`
**Expected result:** No errors (warnings acceptable)

```bash
npx @stoplight/spectral-cli lint docs/contracts/http/asr-proxy.openapi.yaml
```

### Step 2: Verify AC-2 (POST endpoint)
**Expected result:** POST /households/{householdId}/asr/transcriptions defined with:
- Request body: multipart/form-data
- file: required, binary
- languageHint: optional, enum [ru, en, auto]
- Response 202: id (uuid), status (queued), createdAt (datetime)

### Step 3: Verify AC-3 (GET endpoint)
**Expected result:** GET /households/{householdId}/asr/transcriptions/{transcriptionId} defined with:
- Response 200: id, status, text (nullable), error (nullable), durationMs, lang, createdAt, finishedAt, pollAfterMs

### Step 4: Verify AC-4 (POST error responses)
**Expected result:** Error codes defined:
- 400: ASR_INVALID_FORMAT, ASR_AUDIO_TOO_LONG, ASR_MISSING_FILE
- 401: UNAUTHORIZED
- 403: FORBIDDEN
- 409: IDEMPOTENCY_CONFLICT
- 413: ASR_FILE_TOO_LARGE
- 429: ASR_RATE_LIMITED (with Retry-After)
- 500: INTERNAL_ERROR
- 503: ASR_UNAVAILABLE (with Retry-After)

### Step 5: Verify AC-4a (GET error responses)
**Expected result:** Error codes defined:
- 401: UNAUTHORIZED
- 403: FORBIDDEN
- 404: ASR_NOT_FOUND
- 429: ASR_RATE_LIMITED (with Retry-After)
- 500: INTERNAL_ERROR
- 503: ASR_UNAVAILABLE (with Retry-After)

### Step 6: Verify AC-5 (Security scheme)
**Expected result:**
- bearerAuth security scheme defined (type: http, scheme: bearer, bearerFormat: JWT)
- Both endpoints require bearerAuth
- householdId path parameter documented as membership-scoped

### Step 7: Verify AC-7 (Limitations)
**Expected result:** info.description includes:
- Max duration: 60 seconds
- Max size: 10 MB
- Supported formats: OGG/Opus, MP3, WAV, M4A, WebM/Opus

### Step 8: Verify AC-6 (Contracts index)
**File:** `docs/_indexes/contracts-index.md`
**Expected result:** Entry exists for ASR Proxy API

---

## Verification Commands

```bash
# Primary validation
npx @stoplight/spectral-cli lint docs/contracts/http/asr-proxy.openapi.yaml

# Check OpenAPI version
grep -E "^openapi:" docs/contracts/http/asr-proxy.openapi.yaml

# Check POST endpoint exists
grep -A5 "post:" docs/contracts/http/asr-proxy.openapi.yaml | head -10

# Check GET endpoint exists
grep -A5 "get:" docs/contracts/http/asr-proxy.openapi.yaml | head -10

# Check security scheme
grep -A5 "securitySchemes:" docs/contracts/http/asr-proxy.openapi.yaml

# Check contracts-index entry
grep -i "asr" docs/_indexes/contracts-index.md
```

---

## DoD Checklist

- [ ] OpenAPI file valid (spectral lint passes)
- [ ] POST endpoint defined correctly (AC-2)
- [ ] GET endpoint defined correctly (AC-3)
- [ ] POST error responses complete (AC-4)
- [ ] GET error responses complete (AC-4a)
- [ ] Security scheme defined (AC-5)
- [ ] Contracts index updated (AC-6)
- [ ] Limitations documented (AC-7)
- [ ] Contract reviewed against external ASR contract

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Spectral version incompatibility | LOW | Use npx for latest version |
| Missing error code in contract | MEDIUM | Systematic AC verification |

---

## Rollback

Not applicable — this is a contract-only story with no code changes.

---

## Notes

**Contract Status:** The contract file at `docs/contracts/http/asr-proxy.openapi.yaml` was created in a previous session. This workpack focuses on **validation and verification** rather than creation.

**Blocking Dependents:** ST-1102 (AsrClient) and ST-1103 (Controller) depend on this contract being validated.
