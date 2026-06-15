package com.hometusk.voice.service;

import com.hometusk.voice.config.VoiceProperties;
import com.hometusk.voice.exception.VoiceAsrException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class VoiceRateLimitService {

    private final ConcurrentMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public VoiceRateLimitService(VoiceProperties properties) {
        this.requestsPerMinute = properties.asr().requestsPerMinute();
    }

    public void checkLimit(UUID userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            int retryAfterSeconds = retryAfterSeconds(probe.getNanosToWaitForRefill());
            throw VoiceAsrException.localRateLimit(
                    "Too many voice transcription requests. Please retry later", retryAfterSeconds);
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        requestsPerMinute, Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private int retryAfterSeconds(long nanosToWait) {
        int seconds = (int) Math.ceil(nanosToWait / 1_000_000_000.0);
        return Math.max(seconds, 1);
    }
}
