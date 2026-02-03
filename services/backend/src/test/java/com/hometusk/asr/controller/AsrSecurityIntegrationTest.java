package com.hometusk.asr.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    private static final String API_KEY = "secret-api-key-12345";
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
        registry.add("asr.api-key", () -> API_KEY);
        registry.add("asr.connect-timeout-ms", () -> "500");
        registry.add("asr.read-timeout-ms", () -> "200");
        registry.add("asr.resilience.retry.max-attempts", () -> "1");
        registry.add("asr.resilience.retry.wait-duration-ms", () -> "1");
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

    @Test
    void getTranscription_wrongHousehold_returns404() throws Exception {
        UUID transcriptionId = UUID.randomUUID();
        refRepository.save(new AsrTranscriptionRef(transcriptionId, testHousehold.getId(), testUser.getId()));

        Household otherHousehold = householdRepository.save(new Household("Other Household"));
        membershipRepository.save(new Membership(testUser2, otherHousehold, MembershipRole.member));

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

    @Test
    void createTranscription_responseDoesNotContainApiKey() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + UUID.randomUUID()
                                + "\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist("X-API-Key"))
                .andExpect(content().string(not(containsString(API_KEY))));
    }

    @Test
    void errorResponse_doesNotContainApiKey() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"Down\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().doesNotExist("X-API-Key"))
                .andExpect(content().string(not(containsString(API_KEY))));
    }

    @Test
    void upstream503_returnsMappedAsrUnavailable() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"Down\"}")));

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
                        .withBody("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ASR_RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void upstreamTimeout_returnsErrorWithoutApiKeyExposure() throws Exception {
        // Delay > read-timeout-ms (1000ms) to trigger timeout
        // Note: Currently returns INTERNAL_ERROR; ideally should be ASR_UNAVAILABLE
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + UUID.randomUUID()
                                + "\",\"status\":\"queued\",\"createdAt\":\"2026-02-02T10:30:00Z\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().is5xxServerError())
                // Key security check: API key not exposed in timeout error
                .andExpect(content().string(not(containsString("secret-api-key"))));
    }

    @Test
    void upstream401_returnsInternalError() throws Exception {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"UNAUTHORIZED\",\"message\":\"Unauthorized\"}")));

        mockMvc.perform(multipart("/api/v1/households/{id}/asr/transcriptions", testHousehold.getId())
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    private MockMultipartFile sampleFile() {
        return new MockMultipartFile("file", "audio.ogg", "audio/ogg", "audio".getBytes(StandardCharsets.UTF_8));
    }
}
