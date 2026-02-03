# Epic: EP-011 — ASR Integration Foundation

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- Error Codes: `docs/contracts/external/asr-service/asr/errors.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval (2026-02-02)

## Initiative Alignment
This epic implements INIT-2026Q2-asr-integration-foundation:
- Contract-first: HomeTusk proxy OpenAPI for ASR
- Backend adapter: AsrClient (HTTP client with resilience)
- Proxy endpoints with JWT + membership enforcement
- Guardrails: max duration, max size, format validation, rate limiting
- Observability: structured logs, metrics
- Security: household boundary enforcement, no secret leaks

**Product Goal Pillar:** Reliability as a Feature (contract-first, error mapping, observability)

---

## Epic Goal
Enable HomeTusk clients (web/bot/mobile) to:
1. **Submit audio for transcription** via HomeTusk proxy (secrets hidden)
2. **Poll for results** without knowing asr-service internals
3. **Receive predictable errors** (mapped error codes, no raw 500s)
4. **Stay within limits** (duration, size, rate) with clear feedback
5. **Be traced** (correlationId end-to-end)

**Core principle:** Thin secure proxy with guardrails, NOT a transcription domain.

---

## Outcome (User Value)
> "Могу отправить голосовое сообщение через HomeTusk и получить текст. Не вижу никаких API-ключей, не думаю про лимиты сервиса — система сама проверяет и говорит если что-то не так."

---

## Non-Goals (Explicit)

| Item | Reason |
|------|--------|
| Streaming partial results | Out of scope for v0 |
| Diarization (speaker separation) | asr-service supports, but not needed |
| Word-level timestamps | Segments only if needed |
| Translation | Out of scope |
| **Audio storage** in HomeTusk | Proxy "pass-through", audio NOT persisted |
| Voice notes as domain entity | Separate initiative if needed |
| Web/mobile UI for voice | Separate epic after foundation |
| Server-side format conversion | Thin proxy; if upstream rejects format, return error |

**Clarification:** "No audio storage" means audio bytes are NOT persisted in HomeTusk. However, **metadata mapping** (transcriptionId ↔ householdId) IS stored for:
- Household boundary enforcement (IDOR prevention)
- Idempotency support (same key + same file → same result)

---

## Key Decisions (ADR-LITE)

### A) Proxy Architecture
**Decision:** Thin BFF proxy (no domain logic)

HomeTusk endpoints:
- `POST /api/v1/households/{householdId}/asr/transcriptions` — create job
- `GET /api/v1/households/{householdId}/asr/transcriptions/{id}` — get status/result

Rationale: Secrets stay in backend, clients use HomeTusk auth (JWT), household scope enforced.

### B) Error Mapping
**Decision:** Map asr-service errors to HomeTusk error format

| asr-service code | HomeTusk code | HTTP |
|------------------|---------------|------|
| INVALID_FORMAT | ASR_INVALID_FORMAT | 400 |
| AUDIO_TOO_LONG | ASR_AUDIO_TOO_LONG | 400 |
| FILE_TOO_LARGE | ASR_FILE_TOO_LARGE | 413 |
| RATE_LIMIT_EXCEEDED | ASR_RATE_LIMITED | 429 |
| SERVICE_UNAVAILABLE | ASR_UNAVAILABLE | 503 |
| UNAUTHORIZED | (internal error, log alert) | 500 |

Rationale: Consistent error format across HomeTusk, hide asr-service internals.

### C) Rate Limiting Strategy
**Decision:** Per-user per-household limit (e.g., 10 requests/min)

- Bucket4j or similar token bucket
- Key: `asr:{householdId}:{userId}`
- Configurable via application.yml
- Return 429 with Retry-After header

Rationale: Prevent abuse without complex infrastructure.

### D) Correlation ID Propagation
**Decision:** Pass X-Correlation-ID (or generate) to asr-service as X-Request-Id

- HomeTusk generates correlationId if not present
- Logs include: correlationId, userId, householdId, durationMs, sizeBytes, status

Rationale: End-to-end tracing for debugging.

### E) Timeout & Retry Policy
**Decision:**
- Connect timeout: 5s
- Read timeout: 30s (transcription can take time)
- Retry: 1 retry on 503/timeout with exponential backoff
- Circuit breaker: open after 5 failures in 60s

Rationale: Balance reliability vs latency.

### F) Idempotency Implementation
**Decision:** Store idempotency mapping in HomeTusk DB

Flow:
1. Client sends `Idempotency-Key` header (optional, max 64 chars)
2. If key present, compute `payloadDigest = SHA-256(file bytes)`
3. Lookup `AsrIdempotencyRecord` by (householdId, userId, idempotencyKey)
4. If found AND payloadDigest matches → return stored transcriptionId (202, same response)
5. If found AND payloadDigest differs → return 409 IDEMPOTENCY_CONFLICT
6. If not found → proceed with asr-service call, store record on success

TTL: 24 hours (after which key can be reused).

Rationale: Safe retries for unreliable networks; consistent with existing `/commands` idempotency pattern.

### G) Household Boundary Enforcement (IDOR Prevention)
**Decision:** Store `AsrTranscriptionRef` mapping on every successful POST

Flow:
1. POST creates transcription in asr-service → receives `transcriptionId`
2. Store `AsrTranscriptionRef(transcriptionId, householdId, userId, now, now+7d)`
3. On GET, **before** calling asr-service:
   - Lookup `AsrTranscriptionRef` by `transcriptionId`
   - If not found OR `householdId` doesn't match → return 404 (not 403, to avoid leaking existence)
   - If found and matches → call asr-service and return result

Rationale: Prevents IDOR attack where user guesses transcriptionId from another household. Without this mapping, proxy would blindly forward any UUID to asr-service.

### H) Rate Limiting Scope
**Decision:** Separate rate limit buckets for POST and GET

| Endpoint | Bucket Key | Limit | Rationale |
|----------|------------|-------|-----------|
| POST /asr/transcriptions | `asr:post:{householdId}:{userId}` | 5 req/min | Expensive (file upload + job creation) |
| GET /asr/transcriptions/{id} | `asr:get:{householdId}:{userId}` | 30 req/min | Softer (polling control) |

Behavior:
- Both return 429 ASR_RATE_LIMITED with `Retry-After` header
- GET 200/202 responses include `pollAfterMs` field to guide client polling interval (e.g., 2000ms)
- Client should use `pollAfterMs` to avoid hitting rate limit during normal polling

Rationale: Strict POST limit prevents abuse; softer GET limit allows reasonable polling while preventing flood. `pollAfterMs` helps well-behaved clients.

### I) WebM Format Support
**Decision:** Allow `audio/webm` in HomeTusk proxy (Option 1)

Supported formats (v0):
- audio/ogg (OGG/Opus)
- audio/mpeg (MP3)
- audio/wav (WAV)
- audio/mp4 (M4A)
- **audio/webm** (WebM/Opus) — native web MediaRecorder format

Flow:
- HomeTusk proxy accepts webm and forwards to asr-service as-is
- If asr-service does NOT support webm → returns INVALID_FORMAT → HomeTusk maps to ASR_INVALID_FORMAT
- No server-side conversion in HomeTusk (thin proxy principle)

Rationale: Web MediaRecorder outputs webm by default. Requiring client-side conversion adds complexity. Let upstream handle format; if unsupported, error is clear.

**Dependency:** asr-service must support webm. If not, this becomes a blocking dependency for voice-input-web initiative.

---

## Data Model

### Technical Persistence (NOT domain entities)

This is a proxy layer, but requires **minimal persistence** for security and idempotency:

```
AsrTranscriptionRef (mapping for IDOR prevention)
- transcriptionId: UUID (PK, from asr-service)
- householdId: UUID (FK)
- createdByUserId: UUID
- createdAt: timestamp
- expiresAt: timestamp (e.g., createdAt + 7 days)

AsrIdempotencyRecord (idempotency support)
- householdId: UUID (PK part)
- userId: UUID (PK part)
- idempotencyKey: string (PK part, max 64 chars)
- payloadDigest: string (SHA-256 of file bytes)
- transcriptionId: UUID (result from asr-service)
- createdAt: timestamp
- expiresAt: timestamp (createdAt + 24h)

Unique constraint: (householdId, userId, idempotencyKey)
```

**Lifecycle:**
- Records auto-expire and can be purged by scheduled cleanup job.
- No audio bytes stored — only metadata for boundary enforcement.

### Configuration
```yaml
asr:
  base-url: ${ASR_SERVICE_URL}
  api-key: ${ASR_API_KEY}
  connect-timeout-ms: 5000
  read-timeout-ms: 30000
  retry-max-attempts: 2
  rate-limit:
    post-requests-per-minute: 5      # strict for expensive POST
    get-requests-per-minute: 30      # softer for polling
  polling:
    default-poll-after-ms: 2000      # hint for clients
  guardrails:
    max-duration-seconds: 60
    max-size-bytes: 10485760  # 10MB
    allowed-formats:
      - audio/ogg
      - audio/mpeg
      - audio/wav
      - audio/mp4
      - audio/webm   # native web MediaRecorder format
```

---

## API Contract (Proposed)

### HomeTusk Proxy Endpoints
```yaml
# Create transcription
POST /api/v1/households/{householdId}/asr/transcriptions
  - Auth: JWT (Bearer)
  - Membership: required
  - Body: multipart/form-data (file, languageHint?)
  - Response: 202 { id, status, createdAt }

# Get transcription
GET /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}
  - Auth: JWT (Bearer)
  - Membership: required
  - Response: 200 { id, status, text?, error?, ... }
```

Contract file: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## Stories

| ID | Title | Status | Priority | Points | Dependencies |
|----|-------|--------|----------|--------|--------------|
| ST-1101 | ASR Proxy Contract (OpenAPI) | Ready | P1 | 2 | - |
| ST-1102 | AsrClient HTTP Adapter | Draft | P1 | 5 | ST-1101 |
| ST-1103 | ASR Proxy Endpoints (Controller) | Draft | P1 | 5 | ST-1102 |
| ST-1104 | Guardrails (Validation + Rate Limiting) | Draft | P1 | 3 | ST-1103 |
| ST-1105 | Observability (Metrics + Structured Logs) | Draft | P2 | 3 | ST-1103 |
| ST-1106 | Security Boundaries + Integration Tests | Draft | P1 | 3 | ST-1103 |

**Total:** 21 points

### Sprint Allocation (Proposed)
- **Sprint S13:** ST-1101, ST-1102, ST-1103 (foundation) = 12 pts
- **Sprint S14:** ST-1104, ST-1105, ST-1106 (hardening) = 9 pts

---

## Milestones

| Milestone | Stories | Exit Criteria |
|-----------|---------|---------------|
| M1: Contract ready | ST-1101 | OpenAPI validated, reviewed |
| M2: E2E proxy works | ST-1102, ST-1103 | Can create + poll transcription via HomeTusk |
| M3: Production ready | ST-1104, ST-1105, ST-1106 | Guardrails, metrics, security tests pass |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| asr-service unavailability | Transcription fails | Circuit breaker + degraded response |
| High latency | Bad UX | Timeout config + async polling pattern |
| API key leak | Security incident | Key in env, never in logs/responses |
| Abuse (spam audio) | Cost/performance | Rate limit + size limit |
| Contract drift (asr-service changes) | Breaking errors | Pin version, contract tests |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Proxy availability | >= 99% (when asr-service available) |
| Error rate (non-user-error) | < 2% |
| p95 latency (create) | < 2s |
| p95 latency (poll done) | < 500ms |
| 100% correlation | All requests have correlationId |

---

## Exit Criteria

1. User can POST audio via HomeTusk endpoint and receive transcriptionId
2. User can GET status/result via HomeTusk endpoint
3. Errors are mapped to HomeTusk format (no raw asr-service errors)
4. Guardrails enforce limits (size, duration, format, rate)
5. Metrics exposed: asr_requests_total, asr_latency_ms, asr_failures_total
6. Logs include correlationId, userId, householdId
7. No API key visible to clients
8. Household boundary enforced (403 tests pass)
9. IDOR tests pass (cannot access other household transcriptions)
10. OpenAPI contract created and validated
11. WireMock integration tests: happy + timeout + rate limit + 403

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | New endpoints: asr proxy POST/GET |
| adr_needed | lite | Inline decisions sufficient |
| diagrams_needed | lite | Sequence diagram for proxy flow |
| security_sensitive | yes | API key handling, household boundary |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md` |
| External ASR Contract | `docs/contracts/external/asr-service/asr/openapi.yaml` |
| Error Codes Reference | `docs/contracts/external/asr-service/asr/errors.md` |
| HomeTusk Proxy Contract (new) | `docs/contracts/http/asr-proxy.openapi.yaml` |

---

## Consistency Checklist (2026-02-02 Review, updated)

| Issue | Resolution |
|-------|------------|
| IDOR gap: "no persistence" vs 404-on-wrong-household | Added `AsrTranscriptionRef` table for household boundary enforcement (Decision G) |
| Idempotency undefined | Added `AsrIdempotencyRecord` table + Decision F with SHA-256 digest approach |
| Rate limit scope unclear | **Updated:** Decision H: separate buckets for POST (5/min strict) and GET (30/min softer) |
| Duration validation confusion | Clarified in ST-1104: size/format validated locally, duration validated by upstream and mapped back |
| webm format for web | **Updated:** Decision I: audio/webm IS supported (Option 1); thin proxy forwards to upstream |
| Polling guidance | Added `pollAfterMs` field in GET responses to guide client polling interval |
| Keycloak in OpenAPI | Removed; generic "JWT Bearer token" |
| requestId vs correlationId | Unified to `correlationId` in body, `X-Correlation-ID` in headers |
| GET missing 503/429 | Added to OpenAPI contract |

**Human Gate Status (2026-02-02):**
- [x] Minimal persistence (AsrTranscriptionRef, AsrIdempotencyRecord) — **APPROVED**
- [x] Idempotency approach (SHA-256 digest, 24h TTL) — **APPROVED**
- [x] Rate limit design: separate buckets POST 5/min, GET 30/min + pollAfterMs — **APPROVED**
- [x] WebM support: Option 1 (allow audio/webm, thin proxy forwards to upstream) — **APPROVED**

**Dependency noted:** asr-service must support audio/webm. If not, voice-input-web will be blocked until upstream adds support.
