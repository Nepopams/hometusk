package com.hometusk.asr.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.asr.exception.AsrAudioTooLongException;
import com.hometusk.asr.exception.AsrFileTooLargeException;
import com.hometusk.asr.exception.AsrIdempotencyConflictException;
import com.hometusk.asr.exception.AsrInvalidFormatException;
import com.hometusk.asr.exception.AsrMissingFileException;
import com.hometusk.asr.exception.AsrNotFoundException;
import com.hometusk.asr.exception.AsrRateLimitedException;
import com.hometusk.asr.exception.AsrTimeoutException;
import com.hometusk.asr.exception.AsrUnauthorizedException;
import com.hometusk.asr.exception.AsrUnavailableException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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

        assertThat(registry.counter("asr_requests_total", "phase", "create", "status", "success")
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.counter("asr_requests_total", "phase", "create", "status", "error")
                        .count())
                .isEqualTo(1.0);
        assertThat(registry.counter("asr_requests_total", "phase", "poll", "status", "success")
                        .count())
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

        assertThat(registry.counter("asr_failures_total", "reason", "timeout").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("asr_failures_total", "reason", "unavailable")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void reasonFromException_mapsCorrectly() {
        assertThat(AsrMetrics.reasonFromException(new AsrUnavailableException("X", "x", 10)))
                .isEqualTo("unavailable");
        assertThat(AsrMetrics.reasonFromException(new AsrTimeoutException("X", "x", HttpStatus.GATEWAY_TIMEOUT)))
                .isEqualTo("timeout");
        assertThat(AsrMetrics.reasonFromException(new AsrRateLimitedException("X", "x", 10)))
                .isEqualTo("rate_limited");
        assertThat(AsrMetrics.reasonFromException(new AsrFileTooLargeException("X", "x")))
                .isEqualTo("file_too_large");
        assertThat(AsrMetrics.reasonFromException(new AsrInvalidFormatException("X", "x")))
                .isEqualTo("invalid_format");
        assertThat(AsrMetrics.reasonFromException(new AsrMissingFileException()))
                .isEqualTo("missing_file");
        assertThat(AsrMetrics.reasonFromException(new AsrAudioTooLongException("X", "x")))
                .isEqualTo("audio_too_long");
        assertThat(AsrMetrics.reasonFromException(new AsrNotFoundException("X", "x")))
                .isEqualTo("not_found");
        assertThat(AsrMetrics.reasonFromException(new AsrIdempotencyConflictException()))
                .isEqualTo("idempotency_conflict");
        assertThat(AsrMetrics.reasonFromException(new AsrUnauthorizedException("X", "x", HttpStatus.UNAUTHORIZED)))
                .isEqualTo("unauthorized");
        assertThat(AsrMetrics.reasonFromException(new RuntimeException("other")))
                .isEqualTo("internal");
    }
}
