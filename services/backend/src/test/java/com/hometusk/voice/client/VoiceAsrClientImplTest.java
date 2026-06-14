package com.hometusk.voice.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hometusk.voice.config.VoiceProperties;
import com.hometusk.voice.exception.VoiceAsrException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VoiceAsrClientImplTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void transcribe_readTimeout_mapsToTimeout() {
        stubFor(post(urlEqualTo("/v1/asr/transcribe"))
                .willReturn(aResponse()
                        .withFixedDelay(600)
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transcript\":\"slow\",\"traceId\":\"trace-slow\"}")));

        VoiceAsrClientImpl client = new VoiceAsrClientImpl(properties(200), new ObjectMapper());

        assertThatThrownBy(() -> client.transcribe(
                        "fake-audio".getBytes(StandardCharsets.UTF_8),
                        "recording.webm",
                        "audio/webm",
                        "correlation-id"))
                .isInstanceOfSatisfying(
                        VoiceAsrException.class, ex -> assertThat(ex.getCode()).isEqualTo("timeout"));
    }

    private VoiceProperties properties(int readTimeoutMs) {
        return new VoiceProperties(
                true,
                new VoiceProperties.AsrProperties(
                        true,
                        "http://localhost:" + wireMockServer.port(),
                        "/v1/asr/transcribe",
                        null,
                        200,
                        readTimeoutMs,
                        10_485_760L,
                        List.of("audio/webm"),
                        5));
    }
}
