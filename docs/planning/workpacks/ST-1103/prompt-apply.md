# Codex APPLY Prompt: ST-1103 — ASR Proxy Endpoints (Controller)

## Instructions

You are in APPLY mode. Implement ST-1103 based on the workpack and PLAN findings.

**ALLOWED:**
- Create new files in specified paths
- Modify GlobalExceptionHandler to add ASR exception handlers
- Create DB migration V023
- Create integration tests

**FORBIDDEN:**
- Modifying files outside asr package (except GlobalExceptionHandler, ErrorCode)
- Creating rate limiting (ST-1104)
- Creating input validation (size/format checks) (ST-1104)
- Creating metrics/observability (ST-1105)
- Creating AsrIdempotencyRecord (ST-1104)

**STOP-THE-LINE:** If you need to deviate from this plan, STOP and ask.

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1103/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1103-asr-proxy-endpoints.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- HomeTusk Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## PLAN Findings Summary

### Reference Patterns (COPY THESE)
- **Controller example**: `services/backend/src/main/java/com/hometusk/tasks/api/TaskController.java`
- **MembershipService**: `services/backend/src/main/java/com/hometusk/users/service/MembershipService.java`
- **GlobalExceptionHandler**: `services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java`
- **CorrelationIdFilter**: `services/backend/src/main/java/com/hometusk/shared/logging/CorrelationIdFilter.java`
- **IntegrationTestBase**: `services/backend/src/test/java/com/hometusk/integration/IntegrationTestBase.java`
- **WireMock base**: `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`

### Key Patterns
- **Membership check**: `membershipService.requireMembership(userId, householdId)` throws AccessDeniedException
- **Get current user**: `UserResolver.resolveCurrentUser()` → CurrentUser
- **Correlation ID**: `MDC.get(MdcKeys.CORRELATION_ID)` or parse X-Correlation-ID header
- **Entity UUID**: `@GeneratedValue(strategy = GenerationType.UUID)`
- **Repository**: extends `JpaRepository<Entity, UUID>`
- **Auth in tests**: `SecurityMockMvcRequestPostProcessors.jwt()`

### AsrClient Interface (from ST-1102)
```java
AsrJobCreated createTranscription(byte[] fileBytes, String fileName, String contentType,
    String languageHint, String correlationId, String idempotencyKey);
AsrJobResult getTranscription(String transcriptionId, String correlationId);
```

### Important Notes
- **No multipart pattern exists** — create new pattern with @RequestParam for file
- **IDOR prevention**: Return 404 (not 403) for wrong household transcription
- **pollAfterMs**: Add to GET response when status is "queued" or "processing" (default 2000ms)

---

## Implementation Steps

### Step 1: Create DB migration V023

**File:** `services/backend/src/main/resources/db/migration/V023__create_asr_transcription_refs.sql`

```sql
-- ASR Transcription Reference for IDOR prevention (EP-011, Decision G)
CREATE TABLE asr_transcription_refs (
    transcription_id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_asr_transcription_refs_household ON asr_transcription_refs(household_id);
CREATE INDEX idx_asr_transcription_refs_expires ON asr_transcription_refs(expires_at);

COMMENT ON TABLE asr_transcription_refs IS 'Maps transcription IDs to households for IDOR prevention';
```

---

### Step 2: Create AsrTranscriptionRef entity

**File:** `services/backend/src/main/java/com/hometusk/asr/domain/AsrTranscriptionRef.java`

```java
package com.hometusk.asr.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asr_transcription_refs")
public class AsrTranscriptionRef {

    @Id
    @Column(name = "transcription_id")
    private UUID transcriptionId;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected AsrTranscriptionRef() {}

    public AsrTranscriptionRef(UUID transcriptionId, UUID householdId, UUID createdByUserId) {
        this.transcriptionId = transcriptionId;
        this.householdId = householdId;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plus(java.time.Duration.ofDays(7));
    }

    // Getters
    public UUID getTranscriptionId() { return transcriptionId; }
    public UUID getHouseholdId() { return householdId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
```

---

### Step 3: Create AsrTranscriptionRefRepository

**File:** `services/backend/src/main/java/com/hometusk/asr/repository/AsrTranscriptionRefRepository.java`

```java
package com.hometusk.asr.repository;

import com.hometusk.asr.domain.AsrTranscriptionRef;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AsrTranscriptionRefRepository extends JpaRepository<AsrTranscriptionRef, UUID> {
    Optional<AsrTranscriptionRef> findByTranscriptionId(UUID transcriptionId);
}
```

---

### Step 4: Create response DTOs

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/CreateTranscriptionResponse.java`

```java
package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateTranscriptionResponse(
    UUID id,
    String status,
    Instant createdAt
) {
    public static CreateTranscriptionResponse from(AsrJobCreated job) {
        return new CreateTranscriptionResponse(job.id(), job.status(), job.createdAt());
    }
}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/TranscriptionResultResponse.java`

```java
package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TranscriptionResultResponse(
    UUID id,
    String status,
    String text,
    List<AsrSegment> segments,
    String model,
    Integer durationMs,
    String lang,
    Instant createdAt,
    Instant finishedAt,
    Integer pollAfterMs,
    AsrTranscriptionError error
) {
    private static final int DEFAULT_POLL_AFTER_MS = 2000;

    public static TranscriptionResultResponse from(AsrJobResult job) {
        Integer pollAfterMs = null;
        if ("queued".equals(job.status()) || "processing".equals(job.status())) {
            pollAfterMs = DEFAULT_POLL_AFTER_MS;
        }
        return new TranscriptionResultResponse(
            job.id(), job.status(), job.text(), job.segments(), job.model(),
            job.durationMs(), job.lang(), job.createdAt(), job.finishedAt(),
            pollAfterMs, job.error()
        );
    }
}
```

---

### Step 5: Create AsrService

**File:** `services/backend/src/main/java/com/hometusk/asr/service/AsrService.java`

```java
package com.hometusk.asr.service;

import com.hometusk.asr.client.AsrClient;
import com.hometusk.asr.domain.AsrTranscriptionRef;
import com.hometusk.asr.dto.*;
import com.hometusk.asr.exception.AsrNotFoundException;
import com.hometusk.asr.repository.AsrTranscriptionRefRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AsrService {

    private final AsrClient asrClient;
    private final AsrTranscriptionRefRepository refRepository;

    public AsrService(AsrClient asrClient, AsrTranscriptionRefRepository refRepository) {
        this.asrClient = asrClient;
        this.refRepository = refRepository;
    }

    @Transactional
    public CreateTranscriptionResponse createTranscription(
            UUID householdId,
            UUID userId,
            MultipartFile file,
            String languageHint,
            String correlationId,
            String idempotencyKey) throws IOException {

        // Call ASR service
        AsrJobCreated job = asrClient.createTranscription(
            file.getBytes(),
            file.getOriginalFilename(),
            file.getContentType(),
            languageHint,
            correlationId,
            idempotencyKey
        );

        // Persist reference for IDOR prevention
        AsrTranscriptionRef ref = new AsrTranscriptionRef(job.id(), householdId, userId);
        refRepository.save(ref);

        return CreateTranscriptionResponse.from(job);
    }

    public TranscriptionResultResponse getTranscription(
            UUID householdId,
            UUID transcriptionId,
            String correlationId) {

        // Validate household boundary (IDOR prevention)
        AsrTranscriptionRef ref = refRepository.findByTranscriptionId(transcriptionId)
            .orElseThrow(() -> new AsrNotFoundException(
                "ASR_NOT_FOUND",
                "Transcription not found",
                HttpStatus.NOT_FOUND
            ));

        // Return 404 if household doesn't match (not 403, to avoid leaking existence)
        if (!ref.getHouseholdId().equals(householdId)) {
            throw new AsrNotFoundException(
                "ASR_NOT_FOUND",
                "Transcription not found",
                HttpStatus.NOT_FOUND
            );
        }

        // Call ASR service
        AsrJobResult job = asrClient.getTranscription(transcriptionId.toString(), correlationId);
        return TranscriptionResultResponse.from(job);
    }
}
```

---

### Step 6: Create AsrController

**File:** `services/backend/src/main/java/com/hometusk/asr/controller/AsrController.java`

```java
package com.hometusk.asr.controller;

import com.hometusk.asr.dto.CreateTranscriptionResponse;
import com.hometusk.asr.dto.TranscriptionResultResponse;
import com.hometusk.asr.service.AsrService;
import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/asr")
@Tag(name = "ASR", description = "Speech-to-text transcription endpoints")
public class AsrController {

    private final AsrService asrService;
    private final MembershipService membershipService;

    public AsrController(AsrService asrService, MembershipService membershipService) {
        this.asrService = asrService;
        this.membershipService = membershipService;
    }

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a transcription job")
    public ResponseEntity<CreateTranscriptionResponse> createTranscription(
            @PathVariable UUID householdId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "languageHint", defaultValue = "auto") String languageHint,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader) throws IOException {

        // Get current user and enforce membership
        var currentUser = UserResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get or generate correlation ID
        String correlationId = getCorrelationId(correlationIdHeader);

        // Create transcription
        CreateTranscriptionResponse response = asrService.createTranscription(
            householdId,
            currentUser.id(),
            file,
            languageHint,
            correlationId,
            idempotencyKey
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .header("X-Correlation-ID", correlationId)
            .body(response);
    }

    @GetMapping("/transcriptions/{transcriptionId}")
    @Operation(summary = "Get transcription status and result")
    public ResponseEntity<TranscriptionResultResponse> getTranscription(
            @PathVariable UUID householdId,
            @PathVariable UUID transcriptionId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader) {

        // Get current user and enforce membership
        var currentUser = UserResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get or generate correlation ID
        String correlationId = getCorrelationId(correlationIdHeader);

        // Get transcription (IDOR check inside service)
        TranscriptionResultResponse response = asrService.getTranscription(
            householdId,
            transcriptionId,
            correlationId
        );

        return ResponseEntity.ok()
            .header("X-Correlation-ID", correlationId)
            .body(response);
    }

    private String getCorrelationId(String header) {
        if (header != null && !header.isBlank()) {
            try {
                UUID.fromString(header);
                return header;
            } catch (IllegalArgumentException e) {
                // Invalid UUID, fall through to MDC/generate
            }
        }
        String mdcId = MDC.get(MdcKeys.CORRELATION_ID);
        return mdcId != null ? mdcId : UUID.randomUUID().toString();
    }
}
```

---

### Step 7: Add ASR error codes to ErrorCode enum

**File:** `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java`

Add these codes to the existing enum:

```java
// ASR errors
ASR_NOT_FOUND,
ASR_INVALID_FORMAT,
ASR_AUDIO_TOO_LONG,
ASR_FILE_TOO_LARGE,
ASR_RATE_LIMITED,
ASR_UNAVAILABLE,
ASR_MISSING_FILE,
```

---

### Step 8: Extend GlobalExceptionHandler

**File:** `services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java`

Add these handlers:

```java
import com.hometusk.asr.exception.*;

@ExceptionHandler(AsrException.class)
public ResponseEntity<AsrErrorResponse> handleAsrException(AsrException ex) {
    String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
    AsrErrorResponse response = new AsrErrorResponse(
        ex.getCode(),
        ex.getMessage(),
        correlationId,
        null
    );
    return ResponseEntity.status(ex.getStatusCode())
        .header("X-Correlation-ID", correlationId)
        .body(response);
}

@ExceptionHandler(AsrRateLimitedException.class)
public ResponseEntity<AsrErrorResponse> handleAsrRateLimited(AsrRateLimitedException ex) {
    String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
    AsrErrorResponse response = new AsrErrorResponse(
        ex.getCode(),
        ex.getMessage(),
        correlationId,
        Map.of("retryAfterSeconds", ex.getRetryAfterSeconds())
    );
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("X-Correlation-ID", correlationId)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(response);
}

@ExceptionHandler(AsrUnavailableException.class)
public ResponseEntity<AsrErrorResponse> handleAsrUnavailable(AsrUnavailableException ex) {
    String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
    AsrErrorResponse response = new AsrErrorResponse(
        ex.getCode(),
        ex.getMessage(),
        correlationId,
        Map.of("retryAfterSeconds", ex.getRetryAfterSeconds())
    );
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-Correlation-ID", correlationId)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(response);
}
```

Also create ASR error response DTO:

**File:** `services/backend/src/main/java/com/hometusk/asr/dto/AsrProxyErrorResponse.java`

```java
package com.hometusk.asr.dto;

import java.util.Map;

public record AsrProxyErrorResponse(
    String code,
    String message,
    String correlationId,
    Map<String, Object> details
) {}
```

---

### Step 9: Create integration tests

**File:** `services/backend/src/test/java/com/hometusk/asr/controller/AsrControllerIntegrationTest.java`

Extend from IntegrationTestBase and add WireMock for asr-service:

```java
package com.hometusk.asr.controller;

import com.hometusk.integration.IntegrationTestBase;
// ... imports

@SpringBootTest
@AutoConfigureMockMvc
class AsrControllerIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    // WireMock for asr-service

    @Test
    void createTranscription_asMember_returns202() {
        // Setup: user is member of household
        // WireMock: stub POST /transcriptions -> 202
        // Call: POST /households/{id}/asr/transcriptions with file
        // Assert: 202, response has id, status, createdAt
        // Assert: AsrTranscriptionRef persisted
    }

    @Test
    void createTranscription_notMember_returns403() {
        // Setup: user NOT member of household
        // Call: POST /households/{id}/asr/transcriptions
        // Assert: 403 Forbidden
    }

    @Test
    void createTranscription_noAuth_returns401() {
        // Call without JWT
        // Assert: 401 Unauthorized
    }

    @Test
    void getTranscription_asMember_returns200() {
        // Setup: user is member, AsrTranscriptionRef exists
        // WireMock: stub GET /transcriptions/{id} -> 200
        // Call: GET /households/{id}/asr/transcriptions/{transcriptionId}
        // Assert: 200, response has all fields
    }

    @Test
    void getTranscription_wrongHousehold_returns404() {
        // Setup: AsrTranscriptionRef with household A
        // Call: GET /households/B/asr/transcriptions/{id} (different household)
        // Assert: 404 ASR_NOT_FOUND (not 403!)
    }

    @Test
    void getTranscription_queued_includesPollAfterMs() {
        // WireMock: stub GET -> status="queued"
        // Assert: response includes pollAfterMs=2000
    }

    @Test
    void getTranscription_done_noPollAfterMs() {
        // WireMock: stub GET -> status="done"
        // Assert: pollAfterMs is null
    }

    @Test
    void correlationId_generated_ifMissing() {
        // Call without X-Correlation-ID header
        // Assert: response has X-Correlation-ID header (UUID format)
    }

    @Test
    void correlationId_preserved_ifPresent() {
        // Call with X-Correlation-ID: "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        // Assert: response has same X-Correlation-ID
    }

    @Test
    void asrError_returnsMappedError() {
        // WireMock: stub POST -> 400 INVALID_FORMAT
        // Assert: 400 with code=ASR_INVALID_FORMAT
    }
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

# Run ASR integration tests
./gradlew test --tests "*AsrController*"

# Spotless
./gradlew spotlessApply
./gradlew spotlessCheck

# Full build
./gradlew build
```

---

## DoD Checklist (verify after implementation)

- [ ] Migration V023 created and runs
- [ ] AsrTranscriptionRef entity persists correctly
- [ ] AsrService orchestrates AsrClient + ref persistence
- [ ] AsrController POST returns 202
- [ ] AsrController GET returns 200 with pollAfterMs
- [ ] JWT authentication enforced (401 without token)
- [ ] Household membership enforced (403 if not member)
- [ ] IDOR prevention works (404 for wrong household, not 403)
- [ ] Correlation ID generated if missing
- [ ] Correlation ID preserved if present
- [ ] ASR exceptions mapped to HTTP responses
- [ ] All integration tests pass
- [ ] `./gradlew build` passes
- [ ] `./gradlew spotlessCheck` passes

---

## Anti-Scope-Creep Reminders

**DO NOT:**
- Add rate limiting (ST-1104)
- Add file size/format validation (ST-1104)
- Add AsrIdempotencyRecord (ST-1104)
- Add metrics/logging beyond basic (ST-1105)
- Add security edge case tests beyond basic (ST-1106)

**If blocked:** STOP and report the issue.
