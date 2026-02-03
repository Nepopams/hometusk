# Story: ST-1102 — AsrClient HTTP Adapter

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Blocked by ST-1101 (contract must be approved first)

## User Value
> "Backend может общаться с asr-service надёжно: с таймаутами, ретраями и понятными ошибками."

---

## Description
Implement HTTP client adapter for asr-service:
- `AsrClient` interface + `AsrClientImpl` (RestClient/WebClient)
- Configuration (URL, API key, timeouts)
- Error mapping (asr-service errors -> AsrException)
- Correlation ID propagation
- Retry policy (1 retry on 503/timeout)

---

## In Scope
- `AsrClient` interface:
  - `createTranscription(file, languageHint, correlationId, idempotencyKey?) -> AsrJobCreated`
  - `getTranscription(transcriptionId, correlationId) -> AsrJobResult`
- `AsrClientImpl` with Spring RestClient or WebClient
- `AsrProperties` configuration class
- `AsrException` hierarchy (mapped from error codes)
- X-Request-Id header with correlationId
- X-API-Key header from config
- Idempotency-Key header forwarding to asr-service (if provided)
- Timeout configuration (connect: 5s, read: 30s)
- Retry on 503/timeout (max 1 retry, exponential backoff)
- Unit tests with mocked HTTP

## Out of Scope
- Controller/endpoints (ST-1103)
- Rate limiting (ST-1104)
- Metrics/logs beyond basic (ST-1105)
- Circuit breaker (can be added later)

---

## Acceptance Criteria

### AC-1: AsrClient interface defined
```
Given AsrClient interface
Then methods exist:
  - AsrJobCreated createTranscription(MultipartFile file, String languageHint, String correlationId, String idempotencyKey)
  - AsrJobResult getTranscription(String transcriptionId, String correlationId)
```

### AC-2: Configuration externalized
```
Given application.yml
When asr.base-url and asr.api-key configured
Then AsrClientImpl uses these values
And API key never logged
```

### AC-3: Correlation ID propagated
```
Given request with correlationId
When calling asr-service
Then X-Request-Id header set to correlationId
```

### AC-4: Error mapping works
```
Given asr-service returns 400 with code "INVALID_FORMAT"
When AsrClient receives response
Then throws AsrInvalidFormatException (extends AsrException)
And exception contains original error details
```

### AC-5: Timeout configured
```
Given AsrClient
When asr-service does not respond within 30s
Then throws AsrTimeoutException
```

### AC-6: Retry on 503
```
Given asr-service returns 503
When first attempt fails
Then one retry with backoff
And if still 503, throws AsrUnavailableException
```

### AC-7: API key not leaked
```
Given any log output
Then API key value never appears
And API key not in exception messages
```

### AC-8: File forwarded correctly
```
Given multipart file (audio/ogg, 1MB)
When createTranscription called
Then file forwarded to asr-service as multipart/form-data
And Content-Type preserved
```

### AC-9: Idempotency-Key forwarded to asr-service
```
Given createTranscription called with idempotencyKey="client-req-123"
When calling asr-service
Then Idempotency-Key header set to "client-req-123"
```

---

## Test Strategy

### Unit Tests
- `AsrClientImplTest`:
  - `createTranscription_success_returnsJobCreated`
  - `createTranscription_invalidFormat_throwsException`
  - `createTranscription_timeout_throwsTimeoutException`
  - `createTranscription_503_retriesOnce`
  - `getTranscription_success_returnsResult`
  - `getTranscription_notFound_throwsNotFoundException`
  - `correlationId_propagatedAsHeader`

### Mocking
- Use WireMock or MockServer for HTTP mocking
- Mock asr-service responses

---

## Points
**5 points**

## Dependencies
- ST-1101 (contract defines expected request/response shapes)

## Flags
- contract_impact: no (internal adapter)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: yes (API key handling)
