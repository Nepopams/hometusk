# Codex APPLY Prompt: ST-1102 — AsrClient HTTP Adapter

## Instructions

You are in APPLY mode. Implement ST-1102 based on the workpack and PLAN findings.

**ALLOWED:**
- Create new files in specified paths
- Modify application.yml to add asr.* config
- Create unit tests

**FORBIDDEN:**
- Modifying files outside asr package (except application.yml)
- Creating controller/endpoints (ST-1103)
- Creating rate limiting (ST-1104)
- Creating metrics/observability (ST-1105)
- Creating DB migrations (not needed for ST-1102)

**STOP-THE-LINE:** If you need to deviate from this plan, STOP and ask.

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1102/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1102-asr-client-adapter.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- External ASR Errors: `docs/contracts/external/asr-service/asr/errors.md`

---

## PLAN Findings Summary

### Reference Patterns (COPY THESE)
- **AiPlatformClient**: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`
- **Resilience config**: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformResilienceConfig.java`
- **Resilience properties**: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformResilienceProperties.java`
- **Exception base**: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformException.java`
- **WireMock test base**: `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`

### Package Structure
- Create: `services/backend/src/main/java/com/hometusk/asr/`
- Subpackages: `client/`, `dto/`, `exception/`

### Configuration
- Add `asr.*` section to: `services/backend/src/main/resources/application.yml`
- Follow pattern from `aiplatform.*` section

### External ASR Error Codes (map ALL these)
- INVALID_FORMAT → AsrInvalidFormatException
- AUDIO_TOO_LONG → AsrAudioTooLongException
- UNSUPPORTED_LANGUAGE → AsrInvalidFormatException (treat as format issue)
- INVALID_PARAMETER → AsrInvalidFormatException
- MISSING_FILE → AsrInvalidFormatException
- CORRUPTED_FILE → AsrInvalidFormatException
- UNAUTHORIZED → AsrUnauthorizedException
- FORBIDDEN → AsrUnauthorizedException
- NOT_FOUND → AsrNotFoundException
- FILE_TOO_LARGE → AsrFileTooLargeException
- RATE_LIMIT_EXCEEDED → AsrRateLimitedException
- INTERNAL_ERROR → AsrException (generic)
- INFERENCE_ERROR → AsrException (generic)
- INFERENCE_TIMEOUT → AsrTimeoutException
- SERVICE_UNAVAILABLE → AsrUnavailableException

---

## Implementation Steps

### Step 1: Create DTO records

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/AsrJobCreated.java`
```java
package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.UUID;

public record AsrJobCreated(
    UUID id,
    String status,
    Instant createdAt
) {}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/AsrJobResult.java`
```java
package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AsrJobResult(
    UUID id,
    String status,
    String text,
    List<AsrSegment> segments,
    String model,
    Integer durationMs,
    String lang,
    Instant createdAt,
    Instant finishedAt,
    AsrTranscriptionError error
) {}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/AsrSegment.java`
```java
package com.hometusk.asr.dto;

public record AsrSegment(
    int startMs,
    int endMs,
    String text
) {}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/AsrTranscriptionError.java`
```java
package com.hometusk.asr.dto;

public record AsrTranscriptionError(
    String code,
    String message
) {}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/AsrErrorResponse.java`
```java
package com.hometusk.asr.dto;

public record AsrErrorResponse(
    String code,
    String message
) {}
```

---

### Step 2: Create exception hierarchy

**File:** `services/backend/src/main/java/com/hometusk/asr/exception/AsrException.java`
```java
package com.hometusk.asr.exception;

import org.springframework.http.HttpStatusCode;

public class AsrException extends RuntimeException {
    private final String code;
    private final HttpStatusCode statusCode;

    public AsrException(String code, String message, HttpStatusCode statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public AsrException(String code, String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() { return code; }
    public HttpStatusCode getStatusCode() { return statusCode; }
}
```

Create subclasses (each in separate file):
- `AsrInvalidFormatException` extends AsrException (code: varies, status: 400)
- `AsrAudioTooLongException` extends AsrException (code: AUDIO_TOO_LONG, status: 400)
- `AsrFileTooLargeException` extends AsrException (code: FILE_TOO_LARGE, status: 413)
- `AsrRateLimitedException` extends AsrException (code: RATE_LIMIT_EXCEEDED, status: 429) — add retryAfterSeconds field
- `AsrUnavailableException` extends AsrException (code: SERVICE_UNAVAILABLE, status: 503) — add retryAfterSeconds field
- `AsrNotFoundException` extends AsrException (code: NOT_FOUND, status: 404)
- `AsrUnauthorizedException` extends AsrException (code: UNAUTHORIZED, status: 401)
- `AsrTimeoutException` extends AsrException (code: TIMEOUT, status: 504)

---

### Step 3: Create configuration classes

**File:** `services/backend/src/main/java/com/hometusk/asr/client/AsrProperties.java`
```java
package com.hometusk.asr.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asr")
public record AsrProperties(
    String baseUrl,
    String apiKey,
    int connectTimeoutMs,
    int readTimeoutMs
) {
    public AsrProperties {
        if (connectTimeoutMs <= 0) connectTimeoutMs = 5000;
        if (readTimeoutMs <= 0) readTimeoutMs = 30000;
    }
}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/client/AsrResilienceProperties.java`

Follow pattern from `AiPlatformResilienceProperties`:
- `retry.maxAttempts` (default 2)
- `retry.waitDurationMs` (default 500)
- `retry.backoffMultiplier` (default 2.0)

**File:** `services/backend/src/main/java/com/hometusk/asr/client/AsrResilienceConfig.java`

Follow pattern from `AiPlatformResilienceConfig`:
- Create RetryRegistry bean named "asrRetryRegistry"
- Configure retry on `AsrUnavailableException` and `java.net.SocketTimeoutException`

---

### Step 4: Create AsrClient

**File:** `services/backend/src/main/java/com/hometusk/asr/client/AsrClient.java`

Follow `AiPlatformClient` pattern:
- Use RestClient.builder()
- Configure timeouts via SimpleClientHttpRequestFactory
- Implement two methods:

```java
public AsrJobCreated createTranscription(
    byte[] fileBytes,
    String fileName,
    String contentType,
    String languageHint,
    String correlationId,
    String idempotencyKey
);

public AsrJobResult getTranscription(
    String transcriptionId,
    String correlationId
);
```

**Key implementation details:**

1. **Headers for ALL requests:**
   - `X-API-Key: {apiKey}` (from properties)
   - `X-Request-Id: {correlationId}` (if provided)

2. **Headers for POST only:**
   - `Idempotency-Key: {idempotencyKey}` (if provided)
   - `Content-Type: multipart/form-data`

3. **POST /transcriptions:**
   - Multipart body with `file` (binary) and `languageHint` (text)
   - Expect 202 response with AsrJobCreated

4. **GET /transcriptions/{id}:**
   - Expect 200 response with AsrJobResult

5. **Error handling in onStatus():**
   - Parse response body as AsrErrorResponse
   - Map `code` field to appropriate exception
   - Include Retry-After header in rate limit/unavailable exceptions

6. **Retry decorator:**
   - Wrap calls with Retry from asrRetryRegistry
   - Retry on AsrUnavailableException and timeout

---

### Step 5: Add application.yml configuration

**File:** `services/backend/src/main/resources/application.yml`

Add after existing aiplatform section:
```yaml
asr:
  base-url: ${ASR_SERVICE_URL:http://localhost:8000/api/v1/asr}
  api-key: ${ASR_API_KEY:}
  connect-timeout-ms: 5000
  read-timeout-ms: 30000
  resilience:
    retry:
      max-attempts: 2
      wait-duration-ms: 500
      backoff-multiplier: 2.0
```

---

### Step 6: Create WireMock tests

**File:** `services/backend/src/test/java/com/hometusk/asr/client/AsrClientTest.java`

Follow `AiPlatformIntegrationTestBase` pattern:
- WireMockServer on dynamic port
- @DynamicPropertySource to set asr.base-url
- Reset WireMock after each test

**Test cases to implement:**

```java
@Test
void createTranscription_success_returnsJobCreated() {
    // Stub POST /transcriptions -> 202 with JSON body
    // Verify AsrJobCreated returned with correct fields
}

@Test
void createTranscription_invalidFormat_throwsAsrInvalidFormatException() {
    // Stub POST /transcriptions -> 400 with {"code": "INVALID_FORMAT", "message": "..."}
    // Verify AsrInvalidFormatException thrown
}

@Test
void createTranscription_audioTooLong_throwsAsrAudioTooLongException() {
    // Stub POST /transcriptions -> 400 with {"code": "AUDIO_TOO_LONG", "message": "..."}
}

@Test
void createTranscription_fileTooLarge_throwsAsrFileTooLargeException() {
    // Stub POST /transcriptions -> 413 with {"code": "FILE_TOO_LARGE", "message": "..."}
}

@Test
void createTranscription_rateLimited_throwsAsrRateLimitedException() {
    // Stub POST /transcriptions -> 429 with Retry-After header
}

@Test
void createTranscription_unavailable_retriesThenThrows() {
    // Stub POST /transcriptions -> 503 twice
    // Verify 2 requests made (original + 1 retry)
    // Verify AsrUnavailableException thrown
}

@Test
void getTranscription_success_returnsJobResult() {
    // Stub GET /transcriptions/{id} -> 200 with full JSON body
}

@Test
void getTranscription_notFound_throwsAsrNotFoundException() {
    // Stub GET /transcriptions/{id} -> 404
}

@Test
void correlationId_propagatedAsXRequestIdHeader() {
    // Stub any request
    // Call with correlationId
    // Verify X-Request-Id header in WireMock request
}

@Test
void idempotencyKey_propagatedAsHeader() {
    // Stub POST
    // Call with idempotencyKey
    // Verify Idempotency-Key header in WireMock request
}

@Test
void apiKey_sentAsXApiKeyHeader() {
    // Verify X-API-Key header present in requests
}
```

---

## Verification Commands

After implementation, run:

```bash
cd services/backend

# Compile
./gradlew compileJava compileTestJava

# Run ASR tests
./gradlew test --tests "com.hometusk.asr.*"

# Spotless
./gradlew spotlessApply
./gradlew spotlessCheck

# Full build
./gradlew build
```

---

## DoD Checklist (verify after implementation)

- [ ] AsrClient class created with 2 methods
- [ ] All DTO records created (5 files)
- [ ] All exception classes created (8 files)
- [ ] AsrProperties and AsrResilienceProperties created
- [ ] AsrResilienceConfig created with RetryRegistry
- [ ] application.yml has asr.* section
- [ ] WireMock tests cover all error mappings
- [ ] Tests verify header propagation
- [ ] Tests verify retry behavior
- [ ] `./gradlew build` passes
- [ ] `./gradlew spotlessCheck` passes

---

## Anti-Scope-Creep Reminders

**DO NOT:**
- Create AsrController (ST-1103)
- Create AsrTranscriptionRef entity (ST-1103)
- Create rate limiting logic (ST-1104)
- Add metrics/logging beyond basic (ST-1105)
- Create circuit breaker (LATER)
- Modify any existing code outside application.yml

**If blocked:** STOP and report the issue.
