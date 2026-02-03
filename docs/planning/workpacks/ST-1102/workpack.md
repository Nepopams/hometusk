# Workpack: ST-1102 — AsrClient HTTP Adapter

## Sources of Truth
- Story: `docs/planning/epics/EP-011/stories/ST-1102-asr-client-adapter.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- External ASR Errors: `docs/contracts/external/asr-service/asr/errors.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Implement HTTP client adapter for asr-service with resilience patterns (timeouts, retries, error mapping) following existing AiPlatformClient conventions.

## User Value
> Backend can communicate with asr-service reliably: with timeouts, retries, and clear error handling.

---

## Scope

### In Scope
- `AsrClient` interface with two methods
- `AsrClientImpl` using Spring RestClient
- `AsrProperties` configuration class
- `AsrException` hierarchy mapped from asr-service errors
- X-Request-Id header propagation (correlationId)
- X-API-Key header from config
- Idempotency-Key header forwarding
- Timeout configuration (connect: 5s, read: 30s)
- Retry on 503/timeout (1 retry with exponential backoff)
- Unit tests with WireMock

### Out of Scope
- Controller/endpoints (ST-1103)
- Rate limiting (ST-1104)
- Metrics/logs beyond basic (ST-1105)
- Circuit breaker (deferred to LATER)
- AsrTranscriptionRef persistence (ST-1103)
- AsrIdempotencyRecord persistence (ST-1104)

---

## Anchors (Non-negotiables)

| Anchor | Constraint |
|--------|------------|
| External ASR Contract | Request/response shapes from `openapi.yaml` |
| Error Mapping (Epic Decision B) | Map asr-service codes to AsrException subclasses |
| Timeout Policy (Epic Decision E) | Connect: 5s, Read: 30s |
| Retry Policy (Epic Decision E) | 1 retry on 503/timeout, exponential backoff |
| API Key Security | Never log API key, never in exception messages |
| Correlation ID (Epic Decision D) | Pass correlationId as X-Request-Id header |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `services/backend/src/main/java/com/hometusk/asr/client/AsrClient.java` | CREATE | Interface |
| `services/backend/src/main/java/com/hometusk/asr/client/AsrClientImpl.java` | CREATE | Implementation |
| `services/backend/src/main/java/com/hometusk/asr/client/AsrProperties.java` | CREATE | Configuration |
| `services/backend/src/main/java/com/hometusk/asr/client/AsrResilienceConfig.java` | CREATE | Retry config |
| `services/backend/src/main/java/com/hometusk/asr/dto/AsrJobCreated.java` | CREATE | DTO |
| `services/backend/src/main/java/com/hometusk/asr/dto/AsrJobResult.java` | CREATE | DTO |
| `services/backend/src/main/java/com/hometusk/asr/dto/AsrSegment.java` | CREATE | DTO |
| `services/backend/src/main/java/com/hometusk/asr/dto/AsrTranscriptionError.java` | CREATE | DTO |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrException.java` | CREATE | Base exception |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrInvalidFormatException.java` | CREATE | 400 error |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrAudioTooLongException.java` | CREATE | 400 error |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrFileTooLargeException.java` | CREATE | 413 error |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrRateLimitedException.java` | CREATE | 429 error |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrUnavailableException.java` | CREATE | 503 error |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrNotFoundException.java` | CREATE | 404 error |
| `services/backend/src/main/java/com/hometusk/asr/exception/AsrTimeoutException.java` | CREATE | Timeout |
| `services/backend/src/main/resources/application.yml` | MODIFY | Add asr.* config |
| `services/backend/src/test/java/com/hometusk/asr/client/AsrClientImplTest.java` | CREATE | Tests |

---

## Implementation Plan

### Commit 1: Create DTO classes
- `AsrJobCreated`: `record(UUID id, String status, Instant createdAt)`
- `AsrJobResult`: `record(UUID id, String status, String text, List<AsrSegment> segments, String model, Integer durationMs, String lang, Instant createdAt, Instant finishedAt, AsrTranscriptionError error)`
- `AsrSegment`: `record(int startMs, int endMs, String text)`
- `AsrTranscriptionError`: `record(String code, String message)`

### Commit 2: Create exception hierarchy

| asr-service code | Exception class |
|------------------|-----------------|
| INVALID_FORMAT | AsrInvalidFormatException |
| AUDIO_TOO_LONG | AsrAudioTooLongException |
| FILE_TOO_LARGE | AsrFileTooLargeException |
| RATE_LIMIT_EXCEEDED | AsrRateLimitedException |
| SERVICE_UNAVAILABLE | AsrUnavailableException |
| NOT_FOUND | AsrNotFoundException |
| (timeout) | AsrTimeoutException |

### Commit 3: Create configuration classes
- `AsrProperties`: baseUrl, apiKey, connectTimeoutMs, readTimeoutMs
- `AsrResilienceConfig`: RetryRegistry bean

### Commit 4: Create AsrClient interface
```java
public interface AsrClient {
    AsrJobCreated createTranscription(
        MultipartFile file, String languageHint, String correlationId, String idempotencyKey
    );
    AsrJobResult getTranscription(String transcriptionId, String correlationId);
}
```

### Commit 5: Create AsrClientImpl
- RestClient with timeouts
- Header propagation (X-API-Key, X-Request-Id, Idempotency-Key)
- Error response parsing and exception mapping
- Retry decorator for 503/timeout

### Commit 6: Add application.yml configuration
```yaml
asr:
  base-url: ${ASR_SERVICE_URL:http://localhost:8000/api/v1/asr}
  api-key: ${ASR_API_KEY:}
  connect-timeout-ms: 5000
  read-timeout-ms: 30000
  resilience:
    retry:
      max-attempts: 2
      initial-interval-ms: 500
```

### Commit 7: Create WireMock unit tests
Test cases:
- `createTranscription_success_returnsJobCreated`
- `createTranscription_invalidFormat_throwsException`
- `createTranscription_unavailable_retriesOnce`
- `getTranscription_success_returnsResult`
- `getTranscription_notFound_throwsException`
- `correlationId_propagatedAsHeader`
- `apiKey_notInLogs`

---

## Verification Commands

```bash
# Full build
cd services/backend && ./gradlew build

# Run ASR client tests only
cd services/backend && ./gradlew test --tests "com.hometusk.asr.client.*"

# Spotless check
cd services/backend && ./gradlew spotlessCheck
```

---

## DoD Checklist

- [ ] AsrClient interface defined with 2 methods
- [ ] AsrClientImpl compiles and works
- [ ] All exception types created
- [ ] Configuration loaded from application.yml
- [ ] Correlation ID propagated as X-Request-Id
- [ ] API key not logged
- [ ] Retry on 503 works
- [ ] All unit tests pass
- [ ] Spotless formatting applied

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| WireMock multipart handling | MEDIUM | Test incrementally |
| API key accidentally logged | HIGH | Explicit test |

---

## Rollback

1. Revert commits
2. Remove asr.* configuration from application.yml
3. No database migration needed
