package com.hometusk.asr.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.hometusk.asr.dto.AsrJobCreated;
import com.hometusk.asr.dto.AsrJobResult;
import com.hometusk.asr.exception.AsrAudioTooLongException;
import com.hometusk.asr.exception.AsrFileTooLargeException;
import com.hometusk.asr.exception.AsrInvalidFormatException;
import com.hometusk.asr.exception.AsrNotFoundException;
import com.hometusk.asr.exception.AsrRateLimitedException;
import com.hometusk.asr.exception.AsrUnavailableException;
import com.hometusk.asr.metrics.AsrMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsrClientTest {

    private static WireMockServer wireMockServer;
    private AsrClientImpl client;

    @BeforeAll
    static void startServer() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        client = buildClient();
    }

    @Test
    void createTranscription_success_returnsJobCreated() {
        String responseBody =
                """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "status": "queued",
                  "createdAt": "2026-01-29T10:30:00Z"
                }
                """;

        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        AsrJobCreated result =
                client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", "idem-1");

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), result.id());
        assertEquals("queued", result.status());
        assertEquals(Instant.parse("2026-01-29T10:30:00Z"), result.createdAt());
    }

    @Test
    void createTranscription_invalidFormat_throwsAsrInvalidFormatException() {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"INVALID_FORMAT\",\"message\":\"Unsupported audio format\"}")));

        assertThrows(
                AsrInvalidFormatException.class,
                () -> client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", null));
    }

    @Test
    void createTranscription_audioTooLong_throwsAsrAudioTooLongException() {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"AUDIO_TOO_LONG\",\"message\":\"Audio too long\"}")));

        assertThrows(
                AsrAudioTooLongException.class,
                () -> client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", null));
    }

    @Test
    void createTranscription_fileTooLarge_throwsAsrFileTooLargeException() {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(413)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"FILE_TOO_LARGE\",\"message\":\"File too large\"}")));

        assertThrows(
                AsrFileTooLargeException.class,
                () -> client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", null));
    }

    @Test
    void createTranscription_rateLimited_throwsAsrRateLimitedException() {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "60")
                        .withBody("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}")));

        AsrRateLimitedException exception = assertThrows(
                AsrRateLimitedException.class,
                () -> client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", null));

        assertEquals(60, exception.getRetryAfterSeconds());
    }

    @Test
    void createTranscription_unavailable_retriesThenThrows() {
        stubFor(post(urlEqualTo("/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"Unavailable\"}")));

        assertThrows(
                AsrUnavailableException.class,
                () -> client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", null));

        verify(2, postRequestedFor(urlEqualTo("/transcriptions")));
    }

    @Test
    void getTranscription_success_returnsJobResult() {
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
                  "createdAt": "2026-01-29T10:30:00Z",
                  "finishedAt": "2026-01-29T10:30:02Z",
                  "error": null
                }
                """;

        stubFor(get(urlEqualTo("/transcriptions/550e8400-e29b-41d4-a716-446655440111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        AsrJobResult result = client.getTranscription("550e8400-e29b-41d4-a716-446655440111", "corr-1");

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440111"), result.id());
        assertEquals("done", result.status());
        assertEquals("Hello", result.text());
        assertNotNull(result.segments());
    }

    @Test
    void getTranscription_notFound_throwsAsrNotFoundException() {
        stubFor(get(urlEqualTo("/transcriptions/550e8400-e29b-41d4-a716-446655440999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"NOT_FOUND\",\"message\":\"Not found\"}")));

        assertThrows(
                AsrNotFoundException.class,
                () -> client.getTranscription("550e8400-e29b-41d4-a716-446655440999", "corr-1"));
    }

    @Test
    void correlationId_propagatedAsXRequestIdHeader() {
        stubFor(
                post(urlEqualTo("/transcriptions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(202)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"id\":\"550e8400-e29b-41d4-a716-446655440222\",\"status\":\"queued\",\"createdAt\":\"2026-01-29T10:30:00Z\"}")));

        client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-123", null);

        verify(postRequestedFor(urlEqualTo("/transcriptions")).withHeader("X-Request-Id", equalTo("corr-123")));
    }

    @Test
    void idempotencyKey_propagatedAsHeader() {
        stubFor(
                post(urlEqualTo("/transcriptions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(202)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"id\":\"550e8400-e29b-41d4-a716-446655440333\",\"status\":\"queued\",\"createdAt\":\"2026-01-29T10:30:00Z\"}")));

        client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", "idem-123");

        verify(postRequestedFor(urlEqualTo("/transcriptions")).withHeader("Idempotency-Key", equalTo("idem-123")));
    }

    @Test
    void apiKey_sentAsXApiKeyHeader() {
        stubFor(
                post(urlEqualTo("/transcriptions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(202)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"id\":\"550e8400-e29b-41d4-a716-446655440444\",\"status\":\"queued\",\"createdAt\":\"2026-01-29T10:30:00Z\"}")));

        client.createTranscription(sampleBytes(), "audio.ogg", "audio/ogg", "ru", "corr-1", null);

        verify(postRequestedFor(urlEqualTo("/transcriptions")).withHeader("X-API-Key", equalTo("test-api-key")));
    }

    private AsrClientImpl buildClient() {
        AsrProperties properties =
                new AsrProperties("http://localhost:" + wireMockServer.port(), "test-api-key", 1000, 1000, null, null);

        AsrResilienceProperties resilienceProperties = new AsrResilienceProperties();
        resilienceProperties.getRetry().setWaitDurationMs(1);

        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        AsrResilienceConfig.registerAsrRetry(retryRegistry, resilienceProperties);

        AsrMetrics metrics = new AsrMetrics(new SimpleMeterRegistry());
        return new AsrClientImpl(properties, retryRegistry, new ObjectMapper(), metrics);
    }

    private byte[] sampleBytes() {
        return "audio".getBytes(StandardCharsets.UTF_8);
    }
}
