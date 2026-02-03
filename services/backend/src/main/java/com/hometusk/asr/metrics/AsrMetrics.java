package com.hometusk.asr.metrics;

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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AsrMetrics {

    private static final String REQUESTS_TOTAL = "asr_requests_total";
    private static final String LATENCY_MS = "asr_latency_ms";
    private static final String FAILURES_TOTAL = "asr_failures_total";

    private final MeterRegistry registry;

    public AsrMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String phase, boolean success) {
        Counter.builder(REQUESTS_TOTAL)
                .tag("phase", phase)
                .tag("status", success ? "success" : "error")
                .description("Total ASR requests")
                .register(registry)
                .increment();
    }

    public void recordLatency(String phase, long durationMs) {
        Timer.builder(LATENCY_MS)
                .tag("phase", phase)
                .description("ASR request latency in milliseconds")
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.ofMillis(durationMs));
    }

    public void recordFailure(String reason) {
        Counter.builder(FAILURES_TOTAL)
                .tag("reason", reason)
                .description("Total ASR failures by reason")
                .register(registry)
                .increment();
    }

    public static String reasonFromException(Exception exception) {
        if (exception instanceof AsrUnavailableException) {
            return "unavailable";
        }
        if (exception instanceof AsrTimeoutException) {
            return "timeout";
        }
        if (exception instanceof AsrRateLimitedException) {
            return "rate_limited";
        }
        if (exception instanceof AsrFileTooLargeException) {
            return "file_too_large";
        }
        if (exception instanceof AsrInvalidFormatException) {
            return "invalid_format";
        }
        if (exception instanceof AsrMissingFileException) {
            return "missing_file";
        }
        if (exception instanceof AsrAudioTooLongException) {
            return "audio_too_long";
        }
        if (exception instanceof AsrNotFoundException) {
            return "not_found";
        }
        if (exception instanceof AsrIdempotencyConflictException) {
            return "idempotency_conflict";
        }
        if (exception instanceof AsrUnauthorizedException) {
            return "unauthorized";
        }
        return "internal";
    }
}
