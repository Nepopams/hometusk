package com.hometusk.voice.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hometusk.commands.repository.CommandRepository;
import com.hometusk.integration.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class VoiceTranscriptionControllerIntegrationTest extends IntegrationTestBase {

    private static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @Autowired
    private CommandRepository commandRepository;

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureVoiceAsr(DynamicPropertyRegistry registry) {
        registry.add("voice.enabled", () -> "true");
        registry.add("voice.asr.enabled", () -> "true");
        registry.add("voice.asr.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("voice.asr.transcribe-path", () -> "/v1/asr/transcribe");
        registry.add("voice.asr.connect-timeout-ms", () -> "200");
        registry.add("voice.asr.read-timeout-ms", () -> "200");
        registry.add("voice.asr.requests-per-minute", () -> "100");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void createTranscription_success_returnsTranscriptAndDoesNotCreateCommand() throws Exception {
        long commandCountBefore = commandRepository.count();
        stubFor(
                post(urlEqualTo("/v1/asr/transcribe"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                {
                                  "transcript": "add trash bags",
                                  "status": "ok",
                                  "traceId": "trace-asr-123",
                                  "latencyMs": 321
                                }
                                """)));

        mockMvc.perform(multipart("/api/v1/voice/transcriptions")
                        .file(sampleFile())
                        .with(jwt())
                        .header("X-Correlation-ID", "550e8400-e29b-41d4-a716-446655440999"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "550e8400-e29b-41d4-a716-446655440999"))
                .andExpect(jsonPath("$.transcript").value("add trash bags"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.traceId").value("trace-asr-123"))
                .andExpect(jsonPath("$.latencyMs").isNumber());

        assertThat(commandRepository.count()).isEqualTo(commandCountBefore);
    }

    @Test
    void createTranscription_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/voice/transcriptions").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("missing_audio_file"));
    }

    @Test
    void createTranscription_multipleFiles_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/voice/transcriptions")
                        .file(sampleFile())
                        .file(new MockMultipartFile(
                                "file", "second.webm", "audio/webm", "two".getBytes(StandardCharsets.UTF_8)))
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_multipart"));
    }

    @Test
    void createTranscription_unsupportedMedia_returns415() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/voice/transcriptions").file(file).with(jwt()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("unsupported_media"));
    }

    @Test
    void createTranscription_fileTooLarge_returns413() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "large.webm", "audio/webm", new byte[10_485_761]);

        mockMvc.perform(multipart("/api/v1/voice/transcriptions").file(file).with(jwt()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("file_too_large"));
    }

    @Test
    void createTranscription_badUpstreamResponse_returns502() throws Exception {
        stubFor(post(urlEqualTo("/v1/asr/transcribe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));

        mockMvc.perform(multipart("/api/v1/voice/transcriptions")
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("bad_upstream_response"));
    }

    @Test
    void createTranscription_upstreamAuthError_returns502() throws Exception {
        stubFor(post(urlEqualTo("/v1/asr/transcribe"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"auth_error\",\"message\":\"bad key\"}")));

        mockMvc.perform(multipart("/api/v1/voice/transcriptions")
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("auth_error"));
    }

    @Test
    void createTranscription_upstreamTimeout_returns504() throws Exception {
        stubFor(post(urlEqualTo("/v1/asr/transcribe"))
                .willReturn(aResponse()
                        .withFixedDelay(600)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transcript\":\"slow\",\"traceId\":\"trace-slow\"}")));

        mockMvc.perform(multipart("/api/v1/voice/transcriptions")
                        .file(sampleFile())
                        .with(jwt()))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("timeout"));
    }

    @Test
    void createTranscription_noAuth_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/voice/transcriptions").file(sampleFile()))
                .andExpect(status().isUnauthorized());
    }

    private MockMultipartFile sampleFile() {
        return new MockMultipartFile(
                "file", "recording.webm", "audio/webm", "fake-audio".getBytes(StandardCharsets.UTF_8));
    }
}
