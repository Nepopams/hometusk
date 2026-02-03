# Codex APPLY Prompt: ST-1105 — Observability (Metrics + Structured Logs)

## Instructions

You are in APPLY mode. Implement ST-1105 based on the workpack and PLAN findings.

**ALLOWED:**
- Modify build.gradle.kts (add micrometer-registry-prometheus)
- Modify application.yml (add prometheus to actuator endpoints)
- Create AsrMetrics.java in asr package
- Modify AsrService, AsrController, AsrClientImpl
- Create AsrMetricsTest.java
- Modify AsrControllerIntegrationTest (add metrics verification)

**FORBIDDEN:**
- Modifying files outside asr package (except build.gradle.kts, application.yml)
- Creating alerting rules or dashboards
- Adding distributed tracing
- Modifying security tests (ST-1106)

**STOP-THE-LINE:** If you need to deviate, STOP and ask.

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1105/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1105-asr-observability.md`
- Epic: `docs/planning/epics/EP-011/epic.md`

---

## PLAN Findings Summary

### Dependencies
- **Micrometer**: Present via spring-boot-starter-actuator (Spring Boot BOM)
- **Prometheus registry**: NOT present, need to add `micrometer-registry-prometheus`
- **Actuator**: Present, but prometheus not in exposure.include

### Existing Patterns
- **DecisionMetrics**: `com.hometusk.commands.metrics.DecisionMetrics` — use as template
- **Pattern**: @Component + constructor MeterRegistry injection
- **Counter**: `Counter.builder(...).tag(...).description(...).register(registry).increment()`
- **Timer**: `Timer.builder(...).tag(...).description(...).register(registry).record(Duration.ofMillis(...))`

### MDC Setup
- **CorrelationIdFilter**: sets `correlationId`
- **UserResolver**: sets `userId`
- **householdId**: NOT set anywhere — need to add in AsrController

### ASR Exceptions (for failure reason labels)
| Exception | Label |
|-----------|-------|
| AsrUnavailableException | unavailable |
| AsrTimeoutException | timeout |
| AsrRateLimitedException | rate_limited |
| AsrFileTooLargeException | file_too_large |
| AsrInvalidFormatException | invalid_format |
| AsrMissingFileException | missing_file |
| AsrAudioTooLongException | audio_too_long |
| AsrNotFoundException | not_found |
| AsrIdempotencyConflictException | idempotency_conflict |
| AsrUnauthorizedException | unauthorized |
| Other AsrException | internal |

---

## Implementation Steps

### Step 1: Add Prometheus registry dependency

**File:** `services/backend/build.gradle.kts`

Add to dependencies:
```kotlin
implementation("io.micrometer:micrometer-registry-prometheus")
```

---

### Step 2: Enable Prometheus endpoint

**File:** `services/backend/src/main/resources/application.yml`

Update management section:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
    prometheus:
      enabled: true
```

---

### Step 3: Create AsrMetrics component

**File:** `services/backend/src/main/java/com/hometusk/asr/metrics/AsrMetrics.java`

Follow DecisionMetrics pattern:

```java
package com.hometusk.asr.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AsrMetrics {

    private static final String PREFIX = "asr";

    private final MeterRegistry registry;

    public AsrMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // asr_requests_total{status=success|error, phase=create|poll}
    public void recordRequest(String phase, boolean success) {
        Counter.builder(PREFIX + "_requests_total")
                .tag("phase", phase)
                .tag("status", success ? "success" : "error")
                .description("Total ASR requests")
                .register(registry)
                .increment();
    }

    // asr_latency_ms{phase=create|poll}
    public void recordLatency(String phase, long durationMs) {
        Timer.builder(PREFIX + "_latency_ms")
                .tag("phase", phase)
                .description("ASR request latency in milliseconds")
                .register(registry)
                .record(Duration.ofMillis(durationMs));
    }

    // asr_failures_total{reason=unavailable|timeout|rate_limited|...}
    public void recordFailure(String reason) {
        Counter.builder(PREFIX + "_failures_total")
                .tag("reason", reason)
                .description("Total ASR failures by reason")
                .register(registry)
                .increment();
    }

    // Helper to map exception to reason label
    public static String reasonFromException(Exception e) {
        String className = e.getClass().getSimpleName();
        return switch (className) {
            case "AsrUnavailableException" -> "unavailable";
            case "AsrTimeoutException" -> "timeout";
            case "AsrRateLimitedException" -> "rate_limited";
            case "AsrFileTooLargeException" -> "file_too_large";
            case "AsrInvalidFormatException" -> "invalid_format";
            case "AsrMissingFileException" -> "missing_file";
            case "AsrAudioTooLongException" -> "audio_too_long";
            case "AsrNotFoundException" -> "not_found";
            case "AsrIdempotencyConflictException" -> "idempotency_conflict";
            case "AsrUnauthorizedException" -> "unauthorized";
            default -> "internal";
        };
    }
}
```

---

### Step 4: Update AsrController with MDC householdId

**File:** `services/backend/src/main/java/com/hometusk/asr/controller/AsrController.java`

Add MDC setup at start of each endpoint method:
```java
import org.slf4j.MDC;

// In createTranscription method, after membership check:
MDC.put("householdId", householdId.toString());

// In getTranscription method, after membership check:
MDC.put("householdId", householdId.toString());
```

---

### Step 5: Instrument AsrService

**File:** `services/backend/src/main/java/com/hometusk/asr/service/AsrService.java`

Add metrics and structured logging:

```java
// Inject AsrMetrics
private final AsrMetrics metrics;

// In createTranscription:
long startTime = System.currentTimeMillis();
try {
    // ... existing logic ...
    metrics.recordRequest("create", true);
    log.info("ASR transcription created: transcriptionId={}, sizeBytes={}, durationMs={}",
            transcriptionId, file.getSize(), System.currentTimeMillis() - startTime);
    return result;
} catch (Exception e) {
    metrics.recordRequest("create", false);
    metrics.recordFailure(AsrMetrics.reasonFromException(e));
    log.error("ASR transcription failed: reason={}, sizeBytes={}, durationMs={}",
            AsrMetrics.reasonFromException(e), file.getSize(), System.currentTimeMillis() - startTime);
    throw e;
}

// In getTranscription:
long startTime = System.currentTimeMillis();
try {
    // ... existing logic ...
    metrics.recordRequest("poll", true);
    log.info("ASR poll completed: transcriptionId={}, status={}, durationMs={}",
            transcriptionId, result.status(), System.currentTimeMillis() - startTime);
    return result;
} catch (Exception e) {
    metrics.recordRequest("poll", false);
    metrics.recordFailure(AsrMetrics.reasonFromException(e));
    throw e;
}
```

---

### Step 6: Add latency timer to AsrClientImpl

**File:** `services/backend/src/main/java/com/hometusk/asr/client/AsrClientImpl.java`

Inject AsrMetrics and record latency around external calls:

```java
// Inject AsrMetrics
private final AsrMetrics metrics;

// Wrap executeCreateTranscription with timing:
long start = System.currentTimeMillis();
try {
    var result = executeCreateTranscription(...);
    metrics.recordLatency("create", System.currentTimeMillis() - start);
    return result;
} catch (Exception e) {
    metrics.recordLatency("create", System.currentTimeMillis() - start);
    throw e;
}

// Same for executeGetTranscription with phase="poll"
```

---

### Step 7: Create AsrMetricsTest

**File:** `services/backend/src/test/java/com/hometusk/asr/metrics/AsrMetricsTest.java`

```java
package com.hometusk.asr.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.asr.exception.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsrMetricsTest {

    private MeterRegistry registry;
    private AsrMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AsrMetrics(registry);
    }

    @Test
    void recordRequest_incrementsCounter() {
        metrics.recordRequest("create", true);
        metrics.recordRequest("create", false);
        metrics.recordRequest("poll", true);

        assertThat(registry.counter("asr_requests_total", "phase", "create", "status", "success").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("asr_requests_total", "phase", "create", "status", "error").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("asr_requests_total", "phase", "poll", "status", "success").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordLatency_recordsTimer() {
        metrics.recordLatency("create", 150);
        metrics.recordLatency("poll", 50);

        assertThat(registry.timer("asr_latency_ms", "phase", "create").count()).isEqualTo(1);
        assertThat(registry.timer("asr_latency_ms", "phase", "poll").count()).isEqualTo(1);
    }

    @Test
    void recordFailure_incrementsCounterWithReason() {
        metrics.recordFailure("timeout");
        metrics.recordFailure("unavailable");

        assertThat(registry.counter("asr_failures_total", "reason", "timeout").count()).isEqualTo(1.0);
        assertThat(registry.counter("asr_failures_total", "reason", "unavailable").count()).isEqualTo(1.0);
    }

    @Test
    void reasonFromException_mapsCorrectly() {
        assertThat(AsrMetrics.reasonFromException(new AsrUnavailableException("", ""))).isEqualTo("unavailable");
        assertThat(AsrMetrics.reasonFromException(new AsrTimeoutException("", ""))).isEqualTo("timeout");
        assertThat(AsrMetrics.reasonFromException(new AsrRateLimitedException("", "", 60))).isEqualTo("rate_limited");
        assertThat(AsrMetrics.reasonFromException(new AsrFileTooLargeException("", ""))).isEqualTo("file_too_large");
        assertThat(AsrMetrics.reasonFromException(new AsrInvalidFormatException("", ""))).isEqualTo("invalid_format");
        assertThat(AsrMetrics.reasonFromException(new AsrMissingFileException())).isEqualTo("missing_file");
        assertThat(AsrMetrics.reasonFromException(new AsrNotFoundException("", ""))).isEqualTo("not_found");
        assertThat(AsrMetrics.reasonFromException(new RuntimeException("other"))).isEqualTo("internal");
    }
}
```

---

### Step 8: Add metrics integration test

**File:** `services/backend/src/test/java/com/hometusk/asr/controller/AsrControllerIntegrationTest.java`

Add test for prometheus endpoint exposure:

```java
@Test
void prometheus_endpoint_exposesAsrMetrics() throws Exception {
    // Make a successful request first to generate metrics
    stubFor(post(urlEqualTo("/transcriptions"))
            .willReturn(aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

    MockMultipartFile file = new MockMultipartFile("file", "audio.ogg", "audio/ogg", "test".getBytes());

    mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                    .file(file)
                    .with(jwt()))
            .andExpect(status().isAccepted());

    // Verify prometheus endpoint has ASR metrics
    mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("asr_requests_total")));
}
```

---

## Verification Commands

```bash
cd services/backend

./gradlew compileJava compileTestJava
./gradlew test --tests "*AsrMetrics*"
./gradlew test --tests "*Asr*"
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew build
```

---

## DoD Checklist

- [ ] micrometer-registry-prometheus dependency added
- [ ] Prometheus actuator endpoint enabled
- [ ] AsrMetrics component created
- [ ] asr_requests_total counter works
- [ ] asr_latency_ms histogram works
- [ ] asr_failures_total counter works
- [ ] Structured logging with correlationId, userId, householdId
- [ ] MDC householdId set in AsrController
- [ ] AsrMetricsTest passes
- [ ] All ASR tests pass
- [ ] spotlessCheck passes
- [ ] No PII in logs (file content not logged)

---

## Anti-Scope-Creep

**DO NOT:**
- Add alerting rules
- Create Grafana dashboards
- Add distributed tracing spans
- Modify security tests (ST-1106)
- Add complex cardinality labels (keep labels bounded)
