# Codex APPLY Prompt: ST-1104 — Guardrails (Validation + Rate Limiting)

## Instructions

You are in APPLY mode. Implement ST-1104 based on the workpack and PLAN findings.

**ALLOWED:**
- Create new files in asr package
- Modify build.gradle.kts (add Bucket4j)
- Modify AsrProperties, AsrController, AsrService
- Modify GlobalExceptionHandler
- Create migration V024

**FORBIDDEN:**
- Modifying files outside asr package (except build.gradle.kts, GlobalExceptionHandler)
- Creating metrics (ST-1105)
- Creating security tests (ST-1106)

**STOP-THE-LINE:** If you need to deviate, STOP and ask.

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1104/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1104-asr-guardrails.md`
- Epic: `docs/planning/epics/EP-011/epic.md` (Decision F: idempotency, Decision H: rate limits)

---

## PLAN Findings Summary

### Dependencies
- **Bucket4j**: NOT present, need to add to build.gradle.kts
- **Idempotency pattern**: `CommandIdempotencyService` with SHA-256 hashing

### Existing Code
- **Latest migration**: V023
- **AsrProperties**: baseUrl, apiKey, connectTimeoutMs, readTimeoutMs (needs extension)
- **ASR exceptions exist**: AsrFileTooLargeException, AsrInvalidFormatException, AsrRateLimitedException
- **GlobalExceptionHandler**: Already has ASR handlers

### What's Missing
- AsrMissingFileException (need to create)
- AsrIdempotencyConflictException (need to create)
- Guardrails/rate-limit config in AsrProperties
- Validation service
- Rate limit service
- Idempotency service + entity

---

## Implementation Steps

### Step 1: Add Bucket4j dependency

**File:** `services/backend/build.gradle.kts`

Add to dependencies:
```kotlin
implementation("com.bucket4j:bucket4j-core:8.10.1")
```

---

### Step 2: Create migration V024

**File:** `services/backend/src/main/resources/db/migration/V024__create_asr_idempotency.sql`

```sql
-- ASR Idempotency Records (EP-011, Decision F)
CREATE TABLE asr_idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id),
    user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(64) NOT NULL,
    payload_digest VARCHAR(64) NOT NULL,
    transcription_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_asr_idempotency_key UNIQUE (household_id, user_id, idempotency_key)
);

CREATE INDEX idx_asr_idempotency_expires ON asr_idempotency_records(expires_at);
```

---

### Step 3: Create AsrIdempotencyRecord entity

**File:** `services/backend/src/main/java/com/hometusk/asr/domain/AsrIdempotencyRecord.java`

```java
@Entity
@Table(name = "asr_idempotency_records")
public class AsrIdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "payload_digest", nullable = false, length = 64)
    private String payloadDigest;

    @Column(name = "transcription_id", nullable = false)
    private UUID transcriptionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Constructors, getters
}
```

---

### Step 4: Create AsrIdempotencyRecordRepository

**File:** `services/backend/src/main/java/com/hometusk/asr/repository/AsrIdempotencyRecordRepository.java`

```java
public interface AsrIdempotencyRecordRepository extends JpaRepository<AsrIdempotencyRecord, UUID> {
    Optional<AsrIdempotencyRecord> findByHouseholdIdAndUserIdAndIdempotencyKey(
        UUID householdId, UUID userId, String idempotencyKey);
}
```

---

### Step 5: Create new exception classes

**File:** `services/backend/src/main/java/com/hometusk/asr/exception/AsrMissingFileException.java`
```java
public class AsrMissingFileException extends AsrException {
    public AsrMissingFileException() {
        super("ASR_MISSING_FILE", "Audio file is required", HttpStatus.BAD_REQUEST);
    }
}
```

**File:** `services/backend/src/main/java/com/hometusk/asr/exception/AsrIdempotencyConflictException.java`
```java
public class AsrIdempotencyConflictException extends AsrException {
    public AsrIdempotencyConflictException() {
        super("IDEMPOTENCY_CONFLICT", "Idempotency-Key already used with different payload", HttpStatus.CONFLICT);
    }
}
```

---

### Step 6: Extend AsrProperties

**File:** `services/backend/src/main/java/com/hometusk/asr/client/AsrProperties.java`

Add nested config classes:
```java
@ConfigurationProperties(prefix = "asr")
public record AsrProperties(
    String baseUrl,
    String apiKey,
    int connectTimeoutMs,
    int readTimeoutMs,
    GuardrailsProperties guardrails,
    RateLimitProperties rateLimit
) {
    public record GuardrailsProperties(
        long maxSizeBytes,
        List<String> allowedFormats
    ) {
        public GuardrailsProperties {
            if (maxSizeBytes <= 0) maxSizeBytes = 10485760L; // 10MB
            if (allowedFormats == null || allowedFormats.isEmpty()) {
                allowedFormats = List.of("audio/ogg", "audio/mpeg", "audio/wav", "audio/mp4", "audio/webm");
            }
        }
    }

    public record RateLimitProperties(
        int postRequestsPerMinute,
        int getRequestsPerMinute
    ) {
        public RateLimitProperties {
            if (postRequestsPerMinute <= 0) postRequestsPerMinute = 5;
            if (getRequestsPerMinute <= 0) getRequestsPerMinute = 30;
        }
    }
}
```

---

### Step 7: Create AsrValidationService

**File:** `services/backend/src/main/java/com/hometusk/asr/service/AsrValidationService.java`

```java
@Service
public class AsrValidationService {
    private final AsrProperties properties;

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AsrMissingFileException();
        }
        if (file.getSize() > properties.guardrails().maxSizeBytes()) {
            throw new AsrFileTooLargeException("FILE_TOO_LARGE",
                "File exceeds maximum size of " + (properties.guardrails().maxSizeBytes() / 1024 / 1024) + " MB",
                HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.guardrails().allowedFormats().contains(contentType)) {
            throw new AsrInvalidFormatException("INVALID_FORMAT",
                "Unsupported format. Allowed: " + properties.guardrails().allowedFormats(),
                HttpStatus.BAD_REQUEST);
        }
    }
}
```

---

### Step 8: Create AsrRateLimitService

**File:** `services/backend/src/main/java/com/hometusk/asr/service/AsrRateLimitService.java`

```java
@Service
public class AsrRateLimitService {
    private final ConcurrentMap<String, Bucket> postBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> getBuckets = new ConcurrentHashMap<>();
    private final AsrProperties properties;

    public void checkPostLimit(UUID householdId, UUID userId) {
        String key = "post:" + householdId + ":" + userId;
        Bucket bucket = postBuckets.computeIfAbsent(key, k -> createPostBucket());
        if (!bucket.tryConsume(1)) {
            long waitNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            int retryAfterSeconds = (int) Math.ceil(waitNanos / 1_000_000_000.0);
            throw new AsrRateLimitedException("RATE_LIMIT_EXCEEDED",
                "Too many POST requests", HttpStatus.TOO_MANY_REQUESTS, retryAfterSeconds);
        }
    }

    public void checkGetLimit(UUID householdId, UUID userId) {
        String key = "get:" + householdId + ":" + userId;
        Bucket bucket = getBuckets.computeIfAbsent(key, k -> createGetBucket());
        if (!bucket.tryConsume(1)) {
            long waitNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            int retryAfterSeconds = (int) Math.ceil(waitNanos / 1_000_000_000.0);
            throw new AsrRateLimitedException("RATE_LIMIT_EXCEEDED",
                "Too many GET requests", HttpStatus.TOO_MANY_REQUESTS, retryAfterSeconds);
        }
    }

    private Bucket createPostBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(properties.rateLimit().postRequestsPerMinute(),
                Refill.intervally(properties.rateLimit().postRequestsPerMinute(), Duration.ofMinutes(1))))
            .build();
    }

    private Bucket createGetBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(properties.rateLimit().getRequestsPerMinute(),
                Refill.intervally(properties.rateLimit().getRequestsPerMinute(), Duration.ofMinutes(1))))
            .build();
    }
}
```

---

### Step 9: Create AsrIdempotencyService

**File:** `services/backend/src/main/java/com/hometusk/asr/service/AsrIdempotencyService.java`

Follow `CommandIdempotencyService` pattern:

```java
@Service
public class AsrIdempotencyService {
    private static final Duration TTL = Duration.ofHours(24);
    private final AsrIdempotencyRecordRepository repository;

    // Check for existing idempotency record
    // If found with matching digest -> return cached transcriptionId
    // If found with different digest -> throw AsrIdempotencyConflictException
    // If not found -> return null (proceed with new request)
    public UUID checkIdempotency(UUID householdId, UUID userId, String key, byte[] fileBytes) {
        if (key == null || key.isBlank()) return null;

        String digest = computeDigest(fileBytes);
        Optional<AsrIdempotencyRecord> existing = repository
            .findByHouseholdIdAndUserIdAndIdempotencyKey(householdId, userId, key);

        if (existing.isPresent()) {
            AsrIdempotencyRecord record = existing.get();
            if (record.getExpiresAt().isBefore(Instant.now())) {
                // Expired, delete and treat as new
                repository.delete(record);
                return null;
            }
            if (!record.getPayloadDigest().equals(digest)) {
                throw new AsrIdempotencyConflictException();
            }
            return record.getTranscriptionId(); // Return cached
        }
        return null;
    }

    // Store idempotency record after successful creation
    public void storeIdempotency(UUID householdId, UUID userId, String key, byte[] fileBytes, UUID transcriptionId) {
        if (key == null || key.isBlank()) return;

        String digest = computeDigest(fileBytes);
        AsrIdempotencyRecord record = new AsrIdempotencyRecord(
            householdId, userId, key, digest, transcriptionId, Instant.now(), Instant.now().plus(TTL));
        repository.save(record);
    }

    private String computeDigest(byte[] data) {
        // SHA-256 hex string
    }
}
```

---

### Step 10: Update GlobalExceptionHandler

Add handlers for new exceptions:

```java
@ExceptionHandler(AsrMissingFileException.class)
public ResponseEntity<AsrProxyErrorResponse> handleMissingFile(AsrMissingFileException ex) {
    // Return 400 with ASR_MISSING_FILE
}

@ExceptionHandler(AsrIdempotencyConflictException.class)
public ResponseEntity<AsrProxyErrorResponse> handleIdempotencyConflict(AsrIdempotencyConflictException ex) {
    // Return 409 with IDEMPOTENCY_CONFLICT
}
```

---

### Step 11: Update AsrController

Add validation and rate limiting calls:

```java
@PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<CreateTranscriptionResponse> createTranscription(...) {
    var currentUser = UserResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    // Rate limit check
    rateLimitService.checkPostLimit(householdId, currentUser.id());

    // Validation
    validationService.validateFile(file);

    // Idempotency check
    byte[] fileBytes = file.getBytes();
    UUID cachedId = idempotencyService.checkIdempotency(householdId, currentUser.id(), idempotencyKey, fileBytes);
    if (cachedId != null) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new CreateTranscriptionResponse(cachedId, "queued", Instant.now()));
    }

    // Proceed with creation...
    // After success: idempotencyService.storeIdempotency(...)
}

@GetMapping("/transcriptions/{transcriptionId}")
public ResponseEntity<TranscriptionResultResponse> getTranscription(...) {
    // Rate limit check
    rateLimitService.checkGetLimit(householdId, currentUser.id());
    // ...
}
```

---

### Step 12: Update application.yml

```yaml
asr:
  base-url: ${ASR_SERVICE_URL:http://localhost:8000/api/v1/asr}
  api-key: ${ASR_API_KEY:}
  connect-timeout-ms: 5000
  read-timeout-ms: 30000
  guardrails:
    max-size-bytes: 10485760
    allowed-formats:
      - audio/ogg
      - audio/mpeg
      - audio/wav
      - audio/mp4
      - audio/webm
  rate-limit:
    post-requests-per-minute: 5
    get-requests-per-minute: 30
```

---

### Step 13: Create unit tests

**File:** `services/backend/src/test/java/com/hometusk/asr/service/AsrValidationServiceTest.java`

- `validateSize_tooLarge_throwsException`
- `validateSize_atLimit_passes`
- `validateFormat_invalid_throwsException`
- `validateFormat_ogg_passes`
- `validateFormat_webm_passes`
- `validateFile_missing_throwsException`

---

### Step 14: Create integration tests

**File:** `services/backend/src/test/java/com/hometusk/asr/controller/AsrGuardrailsIntegrationTest.java`

- `fileTooLarge_returns413`
- `invalidFormat_returns400`
- `missingFile_returns400`
- `postRateLimit_returns429WithRetryAfter`
- `getRateLimit_returns429WithRetryAfter`
- `rateLimitPerUser_isolatedBuckets`
- `idempotency_sameKeyAndFile_returnsCached`
- `idempotency_sameKeyDifferentFile_returns409`

---

## Verification Commands

```bash
cd services/backend

./gradlew compileJava compileTestJava
./gradlew test --tests "*Asr*"
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew build
```

---

## DoD Checklist

- [ ] Bucket4j dependency added
- [ ] Migration V024 runs successfully
- [ ] AsrIdempotencyRecord entity works
- [ ] AsrValidationService validates size/format/missing
- [ ] AsrRateLimitService enforces POST 5/min, GET 30/min
- [ ] AsrIdempotencyService handles cache/conflict
- [ ] application.yml has guardrails config
- [ ] All tests pass
- [ ] spotlessCheck passes

---

## Anti-Scope-Creep

**DO NOT:**
- Add metrics (ST-1105)
- Add security tests beyond basic (ST-1106)
- Implement circuit breaker
- Add per-household rate limit configuration
