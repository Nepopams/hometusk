package com.hometusk.asr.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hometusk.asr.domain.AsrTranscriptionRef;
import com.hometusk.asr.repository.AsrTranscriptionRefRepository;
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

class AsrGuardrailsIntegrationTest extends IntegrationTestBase {

    private static final WireMockServer wireMockServer;

    @Autowired
    private AsrTranscriptionRefRepository refRepository;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureAsr(DynamicPropertyRegistry registry) {
        registry.add("asr.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("asr.api-key", () -> "test-api-key");
        registry.add("asr.connect-timeout-ms", () -> "1000");
        registry.add("asr.read-timeout-ms", () -> "1000");
        registry.add("asr.guardrails.max-size-bytes", () -> "5");
        registry.add("asr.guardrails.allowed-formats", () -> "audio/ogg,audio/webm");
        registry.add("asr.rate-limit.post-requests-per-minute", () -> "2");
        registry.add("asr.rate-limit.get-requests-per-minute", () -> "2");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void fileTooLarge_returns413() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "audio.ogg", "audio/ogg", "123456".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("ASR_FILE_TOO_LARGE"));
    }

    @Test
    void invalidFormat_returns400() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "audio.txt", "text/plain", "1234".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASR_INVALID_FORMAT"));
    }

    @Test
    void missingFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "audio/ogg", new byte[0]);

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASR_MISSING_FILE"));
    }

    @Test
    void postRateLimit_returns429WithRetryAfter() throws Exception {
        stubFor(
                post(urlEqualTo("/transcriptions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(202)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"id\":\"550e8400-e29b-41d4-a716-446655440001\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

        MockMultipartFile file = sampleFile("audio.ogg", "audio/ogg", "1234".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isAccepted());

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isAccepted());

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("ASR_RATE_LIMITED"));
    }

    @Test
    void getRateLimit_returns429WithRetryAfter() throws Exception {
        UUID transcriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        stubFor(get(urlEqualTo("/transcriptions/" + transcriptionId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + transcriptionId
                                + "\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                testHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                testHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                testHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("ASR_RATE_LIMITED"));
    }

    @Test
    void rateLimitPerUser_isolatedBuckets() throws Exception {
        membershipRepository.save(new Membership(testUser2, testHousehold, MembershipRole.member));

        stubFor(
                post(urlEqualTo("/transcriptions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(202)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"id\":\"550e8400-e29b-41d4-a716-446655440003\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

        MockMultipartFile file = sampleFile("audio.ogg", "audio/ogg", "1234".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isAccepted());

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isAccepted());

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isAccepted());
    }

    @Test
    void idempotency_sameKeyAndFile_returnsCached() throws Exception {
        String responseBody =
                "{\"id\":\"550e8400-e29b-41d4-a716-446655440004\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}";

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        MockMultipartFile file = sampleFile("audio.ogg", "audio/ogg", "1234".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .header("Idempotency-Key", "idem-1")
                        .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("550e8400-e29b-41d4-a716-446655440004"));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file)
                        .header("Idempotency-Key", "idem-1")
                        .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("550e8400-e29b-41d4-a716-446655440004"));

        verify(1, postRequestedFor(urlEqualTo("/transcriptions")));
    }

    @Test
    void idempotency_sameKeyDifferentFile_returns409() throws Exception {
        String responseBody =
                "{\"id\":\"550e8400-e29b-41d4-a716-446655440005\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}";

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        MockMultipartFile file1 = sampleFile("audio.ogg", "audio/ogg", "1234".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = sampleFile("audio.ogg", "audio/ogg", "5678".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file1)
                        .header("Idempotency-Key", "idem-2")
                        .with(jwt()))
                .andExpect(status().isAccepted());

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(file2)
                        .header("Idempotency-Key", "idem-2")
                        .with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    private MockMultipartFile sampleFile(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }
}
