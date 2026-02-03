package com.hometusk.asr.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hometusk.asr.domain.AsrTranscriptionRef;
import com.hometusk.asr.repository.AsrTranscriptionRefRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class AsrControllerIntegrationTest extends IntegrationTestBase {

    private static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @Autowired
    private AsrTranscriptionRefRepository refRepository;

    @Autowired
    private MeterRegistry meterRegistry;

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
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void createTranscription_asMember_returns202() throws Exception {
        UUID transcriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "status": "queued",
                  "createdAt": "2026-02-02T10:30:00Z"
                }
                """;

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .param("languageHint", "ru")
                        .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(transcriptionId.toString()))
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.createdAt").value("2026-02-02T10:30:00Z"));

        assertThat(refRepository.findById(transcriptionId)).isPresent();
        AsrTranscriptionRef ref = refRepository.findById(transcriptionId).orElseThrow();
        assertThat(ref.getHouseholdId()).isEqualTo(testHousehold.getId());
        assertThat(ref.getCreatedByUserId()).isEqualTo(testUser.getId());
    }

    @Test
    void createTranscription_notMember_returns403() throws Exception {
        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTranscription_noAuth_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTranscription_asMember_returns200() throws Exception {
        UUID transcriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440111");
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440111",
                  "status": "done",
                  "text": "Hello",
                  "segments": [
                    { "startMs": 0, "endMs": 1200, "text": "Hello" }
                  ],
                  "model": "whisper-large-v3",
                  "durationMs": 1200,
                  "lang": "en",
                  "createdAt": "2026-02-02T10:30:00Z",
                  "finishedAt": "2026-02-02T10:30:02Z",
                  "error": null
                }
                """;

        stubFor(get(urlEqualTo("/transcriptions/" + transcriptionId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                testHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transcriptionId.toString()))
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.text").value("Hello"));
    }

    @Test
    void getTranscription_wrongHousehold_returns404() throws Exception {
        UUID transcriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440222");
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        Household otherHousehold = householdRepository.save(new Household("Other Household"));
        membershipRepository.save(new Membership(testUser, otherHousehold, MembershipRole.member));

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                otherHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASR_NOT_FOUND"));
    }

    @Test
    void getTranscription_queued_includesPollAfterMs() throws Exception {
        UUID transcriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440333");
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440333",
                  "status": "queued",
                  "text": null,
                  "segments": null,
                  "model": null,
                  "durationMs": null,
                  "lang": null,
                  "createdAt": "2026-02-02T10:30:00Z",
                  "finishedAt": null,
                  "error": null
                }
                """;

        stubFor(get(urlEqualTo("/transcriptions/" + transcriptionId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                testHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollAfterMs").value(2000));
    }

    @Test
    void getTranscription_done_noPollAfterMs() throws Exception {
        UUID transcriptionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440444");
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440444",
                  "status": "done",
                  "text": "Done",
                  "segments": null,
                  "model": "whisper-large-v3",
                  "durationMs": 1000,
                  "lang": "en",
                  "createdAt": "2026-02-02T10:30:00Z",
                  "finishedAt": "2026-02-02T10:30:02Z",
                  "error": null
                }
                """;

        stubFor(get(urlEqualTo("/transcriptions/" + transcriptionId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/v1/households/{id}/asr/transcriptions/{transcriptionId}",
                                testHousehold.getId(),
                                transcriptionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollAfterMs").value(nullValue()));
    }

    @Test
    void correlationId_generated_ifMissing() throws Exception {
        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440555",
                  "status": "queued",
                  "createdAt": "2026-02-02T10:30:00Z"
                }
                """;

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        MvcResult result = mockMvc.perform(
                        multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                                .file(sampleFile())
                                .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();

        String correlationId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotBlank();
        UUID.fromString(correlationId);
    }

    @Test
    void correlationId_preserved_ifPresent() throws Exception {
        String correlationId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440666",
                  "status": "queued",
                  "createdAt": "2026-02-02T10:30:00Z"
                }
                """;

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .header("X-Correlation-ID", correlationId)
                        .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", correlationId));
    }

    @Test
    void asrError_returnsMappedError() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"INVALID_FORMAT\",\"message\":\"Unsupported\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASR_INVALID_FORMAT"));
    }

    @Test
    void metricsRecorded_afterSuccessfulRequest() throws Exception {
        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440777",
                  "status": "queued",
                  "createdAt": "2026-02-02T10:30:00Z"
                }
                """;

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isAccepted());

        // Verify metrics are recorded via MeterRegistry
        var requestCounter = meterRegistry.find("asr_requests_total").counter();
        assertThat(requestCounter).isNotNull();
        assertThat(requestCounter.count()).isGreaterThan(0);

        var latencyTimer = meterRegistry.find("asr_latency_ms").timer();
        assertThat(latencyTimer).isNotNull();
        assertThat(latencyTimer.count()).isGreaterThan(0);
    }

    private MockMultipartFile sampleFile() {
        return new MockMultipartFile("file", "audio.ogg", "audio/ogg", "audio".getBytes(StandardCharsets.UTF_8));
    }
}
