# Sprint S13 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`

---

## Committed Scope

### ST-1101: ASR Proxy Contract (OpenAPI)
**Points:** 2
**Priority:** P1
**Status:** Ready (contract file exists, needs validation)
**Story:** `docs/planning/epics/EP-011/stories/ST-1101-asr-proxy-contract.md`
**Workpack:** `docs/planning/workpacks/ST-1101/workpack.md` (to be created)

**What's included:**
- Validate existing `docs/contracts/http/asr-proxy.openapi.yaml`
- Run spectral lint: `npx @stoplight/spectral-cli lint docs/contracts/http/asr-proxy.openapi.yaml`
- Verify all error codes defined (400, 401, 403, 404, 409, 413, 429, 500, 503)
- Verify POST and GET endpoints match epic decisions
- Verify idempotency support (Idempotency-Key header)
- Verify correlation ID support (X-Correlation-ID)
- Verify polling guidance (pollAfterMs field)
- Update `docs/_indexes/contracts-index.md` with ASR Proxy entry
- Document validation results

**What's NOT included:**
- Changes to contract (already approved in epic)
- Implementation (ST-1102, ST-1103)

**Flags:**
- contract_impact: yes (this IS the contract)
- security_sensitive: no

**DoR:** PASS
**Dependencies:** None

---

### ST-1102: AsrClient HTTP Adapter
**Points:** 5
**Priority:** P1
**Status:** Draft (blocked by ST-1101 validation)
**Story:** `docs/planning/epics/EP-011/stories/ST-1102-asr-client-adapter.md`
**Workpack:** `docs/planning/workpacks/ST-1102/workpack.md` (to be created)

**What's included:**
- `AsrClient` interface:
  - `AsrJobCreated createTranscription(MultipartFile file, String languageHint, String correlationId, String idempotencyKey)`
  - `AsrJobResult getTranscription(String transcriptionId, String correlationId)`
- `AsrClientImpl` implementation (Spring RestClient or WebClient)
- `AsrProperties` configuration class:
  ```yaml
  asr:
    base-url: ${ASR_SERVICE_URL}
    api-key: ${ASR_API_KEY}
    connect-timeout-ms: 5000
    read-timeout-ms: 30000
  ```
- DTOs for asr-service communication:
  - `AsrJobCreated` (id, status, createdAt)
  - `AsrJobResult` (id, status, text, error, durationMs, lang, etc.)
- `AsrException` hierarchy:
  - `AsrException` (base)
  - `AsrInvalidFormatException` (400 INVALID_FORMAT)
  - `AsrAudioTooLongException` (400 AUDIO_TOO_LONG)
  - `AsrFileTooLargeException` (413 FILE_TOO_LARGE)
  - `AsrNotFoundException` (404)
  - `AsrTimeoutException` (timeout)
  - `AsrUnavailableException` (503)
  - `AsrRateLimitedException` (429)
- Header propagation:
  - `X-Request-Id` = correlationId
  - `X-API-Key` from config (never logged)
  - `Idempotency-Key` if provided
- Timeout configuration (connect: 5s, read: 30s)
- Retry on 503/timeout (1 retry with backoff)
- Unit tests with WireMock/MockServer

**What's NOT included:**
- Controller/endpoints (ST-1103)
- Rate limiting (ST-1104)
- Metrics beyond basic logging (ST-1105)
- Circuit breaker (later enhancement)

**Flags:**
- contract_impact: no (internal adapter)
- security_sensitive: yes (API key handling)

**DoR:** PASS (blocked by ST-1101)
**Dependencies:** ST-1101 (contract defines expected shapes)

---

### ST-1103: ASR Proxy Endpoints (Controller)
**Points:** 5
**Priority:** P1
**Status:** Draft (blocked by ST-1102)
**Story:** `docs/planning/epics/EP-011/stories/ST-1103-asr-proxy-endpoints.md`
**Workpack:** `docs/planning/workpacks/ST-1103/workpack.md` (to be created)

**What's included:**

**Controller:**
- `AsrController`:
  - `POST /api/v1/households/{householdId}/asr/transcriptions`
    - Accept multipart/form-data (file, languageHint)
    - Validate membership
    - Call AsrClient.createTranscription()
    - Persist AsrTranscriptionRef on success
    - Return 202 with id, status, createdAt
  - `GET /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}`
    - Validate membership
    - Validate household boundary via AsrTranscriptionRef (IDOR prevention)
    - Call AsrClient.getTranscription()
    - Return 200 with full result (incl. pollAfterMs if in-progress)

**Entity/Repository:**
- `AsrTranscriptionRef` entity:
  - `transcriptionId` (UUID, PK)
  - `householdId` (UUID, FK)
  - `createdByUserId` (UUID)
  - `createdAt` (timestamp)
  - `expiresAt` (timestamp, createdAt + 7 days)
- `AsrTranscriptionRefRepository` (Spring Data JPA)
- DB migration for `asr_transcription_ref` table

**Exception Handling:**
- `@ControllerAdvice` for AsrException mapping:
  - `AsrInvalidFormatException` -> 400 ASR_INVALID_FORMAT
  - `AsrAudioTooLongException` -> 400 ASR_AUDIO_TOO_LONG
  - `AsrFileTooLargeException` -> 413 ASR_FILE_TOO_LARGE
  - `AsrNotFoundException` -> 404 ASR_NOT_FOUND
  - `AsrTimeoutException` -> 503 ASR_UNAVAILABLE
  - `AsrUnavailableException` -> 503 ASR_UNAVAILABLE
  - `AsrRateLimitedException` -> 429 ASR_RATE_LIMITED (with Retry-After)

**Correlation ID:**
- Generate UUID if X-Correlation-ID not present
- Pass to AsrClient
- Return in response header

**Integration Tests:**
- `AsrControllerIntegrationTest`:
  - `createTranscription_asMember_returns202`
  - `createTranscription_notMember_returns403`
  - `createTranscription_noAuth_returns401`
  - `getTranscription_asMember_returns200`
  - `getTranscription_wrongHousehold_returns404`
  - `createTranscription_asrError_returnsMappedError`
  - `correlationId_generatedIfMissing`
  - `correlationId_preservedIfPresent`

**What's NOT included:**
- Input validation (size, format, duration) - ST-1104
- Rate limiting - ST-1104
- Metrics - ST-1105
- Security edge case tests (extensive IDOR) - ST-1106
- Idempotency record persistence - deferred to ST-1104

**Flags:**
- contract_impact: yes (implements contract)
- security_sensitive: yes (authz enforcement)

**DoR:** PASS (blocked by ST-1102)
**Dependencies:** ST-1102 (AsrClient must exist), ST-1101 (contract)

---

## Out of Scope (Explicit)

### Deferred to S14 (Hardening)
- Rate limiting (Bucket4j, per-user/household) - ST-1104
- Input validation (file size, audio duration, format check) - ST-1104
- Idempotency record persistence (AsrIdempotencyRecord) - ST-1104
- Metrics (asr_requests_total, asr_latency_ms) - ST-1105
- Structured logging (beyond basic correlation) - ST-1105
- Security edge case tests (IDOR, expired refs, etc.) - ST-1106
- Contract drift tests (WireMock vs real asr-service) - ST-1106

### Never in EP-011 Scope
- Circuit breaker (can add later if needed)
- Streaming partial results
- Diarization, word timestamps
- Translation
- Audio storage in HomeTusk
- Web/mobile UI for voice input
- WebM server-side conversion

---

## Acceptance Criteria Summary

**Sprint succeeds if:**
1. ASR Proxy OpenAPI contract validated (spectral passes)
2. contracts-index.md updated
3. AsrClient can call asr-service (via mock)
4. POST /households/{id}/asr/transcriptions returns 202
5. GET /households/{id}/asr/transcriptions/{id} returns 200/404
6. JWT authentication enforced (401 without token)
7. Household membership enforced (403 if not member)
8. IDOR prevention via AsrTranscriptionRef (wrong household -> 404)
9. Correlation ID flows end-to-end
10. `./gradlew build` passes
11. Integration tests cover happy path + auth

**Sprint fails if:**
- Contract validation fails
- Cannot POST audio via proxy
- Cannot GET transcription result
- Auth/membership not working
- No integration tests
- AsrTranscriptionRef not persisted

---

## Readiness Notes

**All committed stories:**
- Have clear ACs with Given/When/Then format
- Have defined test strategy
- Dependencies are sequential and achievable
- Backend patterns (Spring, JPA, Security) established

**Key patterns to follow:**
- Controller: similar to `NotificationsController`
- HTTP client: similar to `AiPlatformClient`
- Exception mapping: similar to existing `@ControllerAdvice`
- Entity: similar to existing JPA entities
- Integration tests: use `@WebMvcTest` + WireMock

**Human gates:**
- Gate B: approve committed scope (this sprint)
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)

---

## Story Dependency Graph

```
ST-1101 (Contract Validation)
    |
    v
ST-1102 (AsrClient Adapter)
    |
    v
ST-1103 (AsrController)
```

**Critical path:** ST-1101 -> ST-1102 -> ST-1103

---

## Technical Notes

### New Backend Files (ST-1102)

```
backend/src/main/java/dev/hometusk/
├── asr/
│   ├── client/
│   │   ├── AsrClient.java                 # Interface
│   │   ├── AsrClientImpl.java             # Implementation
│   │   ├── AsrProperties.java             # @ConfigurationProperties
│   │   ├── AsrJobCreated.java             # DTO
│   │   ├── AsrJobResult.java              # DTO
│   │   └── AsrClientConfig.java           # @Configuration
│   └── exception/
│       ├── AsrException.java              # Base exception
│       ├── AsrInvalidFormatException.java
│       ├── AsrAudioTooLongException.java
│       ├── AsrFileTooLargeException.java
│       ├── AsrNotFoundException.java
│       ├── AsrTimeoutException.java
│       ├── AsrUnavailableException.java
│       └── AsrRateLimitedException.java
```

### New Backend Files (ST-1103)

```
backend/src/main/java/dev/hometusk/
├── asr/
│   ├── controller/
│   │   ├── AsrController.java             # REST controller
│   │   └── AsrExceptionHandler.java       # @ControllerAdvice
│   ├── entity/
│   │   └── AsrTranscriptionRef.java       # JPA entity
│   ├── repository/
│   │   └── AsrTranscriptionRefRepository.java
│   └── dto/
│       ├── CreateTranscriptionRequest.java
│       ├── TranscriptionCreatedResponse.java
│       └── TranscriptionResultResponse.java
└── ...

backend/src/main/resources/db/migration/
└── V20260202__create_asr_transcription_ref.sql
```

### Configuration Example

```yaml
# application.yml
asr:
  base-url: ${ASR_SERVICE_URL:http://localhost:8081}
  api-key: ${ASR_API_KEY:test-key}
  connect-timeout-ms: 5000
  read-timeout-ms: 30000
  retry:
    max-attempts: 2
    backoff-ms: 1000
```

### Migration SQL

```sql
-- V20260202__create_asr_transcription_ref.sql
CREATE TABLE asr_transcription_ref (
    transcription_id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_asr_ref_household ON asr_transcription_ref(household_id);
CREATE INDEX idx_asr_ref_expires ON asr_transcription_ref(expires_at);
```
