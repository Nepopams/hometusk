# Codex APPLY Prompt: ST-1106 — Security Boundaries + Integration Tests

## Instructions

You are in APPLY mode. Implement ST-1106 based on the workpack and PLAN findings.

**ALLOWED:**
- Create AsrSecurityIntegrationTest.java in asr/controller package

**FORBIDDEN:**
- Modifying any existing files
- Creating non-test files
- Modifying security/controller logic (tests only)

**STOP-THE-LINE:** If you need to deviate, STOP and ask.

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1106/workpack.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## PLAN Findings Summary

### Existing Test Patterns
- **JWT helper**: `jwt()` for testUser, `jwtForUser(User)` for other users
- **WireMock**: static server with dynamicPort(), @DynamicPropertySource for asr.base-url
- **Base class**: `IntegrationTestBase` provides testHousehold, testUser, testUser2, membershipRepository

### Security Enforcement Points
- **Auth**: Spring Security `anyRequest().authenticated()`
- **Membership**: `membershipService.requireMembership(userId, householdId)`
- **IDOR prevention**: `AsrTranscriptionRef` lookup returns 404 if missing or household mismatch

### Error Mapping (from GlobalExceptionHandler)
| Upstream | Exception | Mapped Code | HTTP Status |
|----------|-----------|-------------|-------------|
| 503 | AsrUnavailableException | ASR_UNAVAILABLE | 503 |
| 429 | AsrRateLimitedException | ASR_RATE_LIMITED | 429 |
| timeout | AsrTimeoutException | ASR_UNAVAILABLE | 503 |
| 401 | AsrUnauthorizedException | INTERNAL_ERROR | 500 |

### API Key Handling
- Injected via `X-API-Key` header in AsrClientImpl
- Never exposed in responses (AsrProxyErrorResponse has code/message/correlationId only)

### Coverage Gaps (tests to create)
- `getTranscription_noAuth_returns401`
- `getTranscription_notMember_returns403`
- `getTranscription_nonExistentId_returns404`
- API key non-exposure tests
- Upstream error mapping tests (503, 429, timeout, 401)

---

## Implementation Steps

### Step 1: Create AsrSecurityIntegrationTest

**File:** `services/backend/src/test/java/com/hometusk/asr/controller/AsrSecurityIntegrationTest.java`

```java
package com.hometusk.asr.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hometusk.asr.domain.AsrTranscriptionRef;
import com.hometusk.asr.repository.AsrTranscriptionRefRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class AsrSecurityIntegrationTest extends IntegrationTestBase {

    private static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @Autowired
    private AsrTranscriptionRefRepository refRepository;

    @DynamicPropertySource
    static void configureAsr(DynamicPropertyRegistry registry) {
        registry.add("asr.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("asr.api-key", () -> "secret-api-key-12345");
        registry.add("asr.connect-timeout-ms", () -> "500");
        registry.add("asr.read-timeout-ms", () -> "1000");
        // Disable rate limits for security tests
        registry.add("asr.rate-limit.post-requests-per-minute", () -> "100");
        registry.add("asr.rate-limit.get-requests-per-minute", () -> "100");
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    // ==================== Authentication Tests (401) ====================

    @Test
    void createTranscription_noAuth_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTranscription_noAuth_returns401() throws Exception {
        UUID transcriptionId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                        testHousehold.getId(),
                        transcriptionId))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Membership Tests (403) ====================

    @Test
    void createTranscription_notMember_returns403() throws Exception {
        Household otherHousehold = householdRepository.save(new Household("Other Household"));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", otherHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTranscription_notMember_returns403() throws Exception {
        Household otherHousehold = householdRepository.save(new Household("Other Household"));
        UUID transcriptionId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                        otherHousehold.getId(),
                        transcriptionId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    // ==================== IDOR Prevention Tests (404) ====================

    @Test
    void getTranscription_wrongHousehold_returns404() throws Exception {
        // Create transcription in testHousehold
        UUID transcriptionId = UUID.randomUUID();
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        // Create another household with testUser2 as member
        Household otherHousehold = householdRepository.save(new Household("Other Household"));
        membershipRepository.save(new Membership(testUser2, otherHousehold, MembershipRole.member));

        // Try to access from wrong household - should return 404 (not 403, to prevent enumeration)
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                        otherHousehold.getId(),
                        transcriptionId)
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTranscription_nonExistentId_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                        testHousehold.getId(),
                        nonExistentId)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    // ==================== API Key Non-Exposure Tests ====================

    @Test
    void createTranscription_responseDoesNotContainApiKey() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(content().string(not(containsString("secret-api-key"))))
                .andExpect(content().string(not(containsString("api-key"))))
                .andExpect(content().string(not(containsString("X-API-Key"))));
    }

    @Test
    void errorResponse_doesNotContainApiKey() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Service unavailable\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("secret-api-key"))))
                .andExpect(content().string(not(containsString("api-key"))))
                .andExpect(content().string(not(containsString("X-API-Key"))));
    }

    // ==================== Upstream Error Mapping Tests ====================

    @Test
    void upstream503_returnsMappedAsrUnavailable() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Service unavailable\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ASR_UNAVAILABLE"));
    }

    @Test
    void upstream429_returnsMappedAsrRateLimited() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "60")
                        .withBody("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ASR_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void upstreamTimeout_returnsAsrUnavailable() throws Exception {
        // Stub with delay longer than read-timeout-ms (1000ms)
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"queued\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ASR_UNAVAILABLE"));
    }

    @Test
    void upstream401_returnsInternalError() throws Exception {
        // Upstream 401 means our API key is invalid - should not expose this to client
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Unauthorized\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                // Should not expose that it was an auth error with upstream
                .andExpect(content().string(not(containsString("Unauthorized"))))
                .andExpect(content().string(not(containsString("401"))));
    }

    // ==================== Helper Methods ====================

    private MockMultipartFile sampleFile() {
        return new MockMultipartFile("file", "audio.ogg", "audio/ogg", "audio-content".getBytes(StandardCharsets.UTF_8));
    }
}
```

---

## Verification Commands

```bash
cd services/backend

./gradlew compileJava compileTestJava
./gradlew test --tests "AsrSecurityIntegrationTest"
./gradlew test --tests "*Asr*"
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew build
```

---

## DoD Checklist

- [ ] AsrSecurityIntegrationTest.java created
- [ ] POST without JWT → 401
- [ ] GET without JWT → 401
- [ ] POST as non-member → 403
- [ ] GET as non-member → 403
- [ ] Wrong household access → 404
- [ ] Non-existent ID → 404
- [ ] API key not in POST response
- [ ] API key not in error response
- [ ] 503 → ASR_UNAVAILABLE
- [ ] 429 → ASR_RATE_LIMITED
- [ ] Timeout → ASR_UNAVAILABLE
- [ ] 401 → INTERNAL_ERROR
- [ ] All security tests pass
- [ ] All ASR tests pass
- [ ] spotlessCheck passes

---

## Anti-Scope-Creep

**DO NOT:**
- Modify existing files
- Add rate limiting logic (ST-1104)
- Add metrics/observability (ST-1105)
- Create non-test files
