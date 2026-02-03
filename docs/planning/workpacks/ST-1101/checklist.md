# Checklist: ST-1101 — ASR Proxy Contract (OpenAPI)

## DoR Verification (Pre-Implementation)
- [x] Story has clear title and description
- [x] Acceptance criteria defined (AC-1 through AC-7)
- [x] Test strategy defined (spectral validation)
- [x] Dependencies identified (none)
- [x] Flags set (contract_impact: yes)

---

## Acceptance Criteria Verification

### AC-1: OpenAPI file created and valid
- [ ] File exists at `docs/contracts/http/asr-proxy.openapi.yaml`
- [ ] OpenAPI version is 3.0+ or 3.1
- [ ] `npx @stoplight/spectral-cli lint` passes with no errors

### AC-2: POST endpoint defined
- [ ] Path: `/households/{householdId}/asr/transcriptions`
- [ ] Method: POST
- [ ] Request body: `multipart/form-data`
- [ ] `file` field: required, binary
- [ ] `languageHint` field: optional, enum [ru, en, auto]
- [ ] Response 202 includes: `id` (uuid), `status` (queued), `createdAt` (datetime)

### AC-3: GET endpoint defined
- [ ] Path: `/households/{householdId}/asr/transcriptions/{transcriptionId}`
- [ ] Method: GET
- [ ] Response 200 includes: `id`, `status`, `text` (nullable), `error` (nullable)
- [ ] Response 200 includes: `durationMs`, `lang`, `createdAt`, `finishedAt`, `pollAfterMs`

### AC-4: POST error responses defined
- [ ] 400 with codes: ASR_INVALID_FORMAT, ASR_AUDIO_TOO_LONG, ASR_MISSING_FILE
- [ ] 401 with code: UNAUTHORIZED
- [ ] 403 with code: FORBIDDEN
- [ ] 409 with code: IDEMPOTENCY_CONFLICT
- [ ] 413 with code: ASR_FILE_TOO_LARGE
- [ ] 429 with code: ASR_RATE_LIMITED + Retry-After header
- [ ] 500 with code: INTERNAL_ERROR
- [ ] 503 with code: ASR_UNAVAILABLE + Retry-After header

### AC-4a: GET error responses defined
- [ ] 401 with code: UNAUTHORIZED
- [ ] 403 with code: FORBIDDEN
- [ ] 404 with code: ASR_NOT_FOUND
- [ ] 429 with code: ASR_RATE_LIMITED + Retry-After header
- [ ] 500 with code: INTERNAL_ERROR
- [ ] 503 with code: ASR_UNAVAILABLE + Retry-After header

### AC-5: Security scheme defined
- [ ] bearerAuth scheme: type=http, scheme=bearer, bearerFormat=JWT
- [ ] Global security requirement set
- [ ] householdId parameter documented as membership-scoped

### AC-6: Contracts index updated
- [ ] Entry exists in `docs/_indexes/contracts-index.md`
- [ ] Link points to correct file

### AC-7: Limitations documented
- [ ] Max duration: 60 seconds mentioned
- [ ] Max size: 10 MB mentioned
- [ ] Supported formats listed: OGG/Opus, MP3, WAV, M4A, WebM/Opus

---

## DoD Verification (Post-Validation)

### Tests
- [ ] Spectral lint passes with no errors
- [ ] Manual review completed

### Documentation
- [ ] Contract file complete and valid
- [ ] Contracts index updated (AC-6)

---

## Final Sign-off

- [ ] All ACs verified
- [ ] DoD criteria met
- [ ] Ready for ST-1102 (AsrClient) to proceed
