package com.hometusk.voice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class VoiceMetrics {

    private final MeterRegistry meterRegistry;

    public VoiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAsrRequest(String status) {
        Counter.builder("voice.asr.requests")
                .tag("status", status)
                .description("Voice ASR request count")
                .register(meterRegistry)
                .increment();
    }

    public void recordAsrError(String code) {
        Counter.builder("voice.asr.errors")
                .tag("code", code)
                .description("Voice ASR error count")
                .register(meterRegistry)
                .increment();
    }

    public void recordAsrLatency(long durationMs) {
        Timer.builder("voice.asr.latency")
                .description("Voice ASR end-to-end latency")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordFileSize(long bytes) {
        Counter.builder("voice.asr.file_size_bucket")
                .tag("bucket", bucket(bytes))
                .description("Voice ASR uploaded file size bucket")
                .register(meterRegistry)
                .increment();
    }

    public void recordVoiceCommandReceived() {
        Counter.builder("voice.command.source.voice.count")
                .description("Voice-originated command count")
                .register(meterRegistry)
                .increment();
    }

    public void recordVoiceCommandOutcome(String outcome) {
        Counter.builder("voice.command.source.voice.outcome")
                .tag("outcome", outcome)
                .description("Voice-originated command outcome")
                .register(meterRegistry)
                .increment();
    }

    private String bucket(long bytes) {
        if (bytes < 256 * 1024L) {
            return "lt_256kb";
        }
        if (bytes < 1024 * 1024L) {
            return "lt_1mb";
        }
        if (bytes < 5 * 1024 * 1024L) {
            return "lt_5mb";
        }
        if (bytes < 10 * 1024 * 1024L) {
            return "lt_10mb";
        }
        return "gte_10mb";
    }
}
