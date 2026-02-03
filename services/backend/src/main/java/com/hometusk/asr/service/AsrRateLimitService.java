package com.hometusk.asr.service;

import com.hometusk.asr.client.AsrProperties;
import com.hometusk.asr.exception.AsrRateLimitedException;
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
public class AsrRateLimitService {

    private final ConcurrentMap<String, Bucket> postBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> getBuckets = new ConcurrentHashMap<>();
    private final int postRequestsPerMinute;
    private final int getRequestsPerMinute;

    public AsrRateLimitService(AsrProperties properties) {
        this.postRequestsPerMinute = properties.rateLimit().postRequestsPerMinute();
        this.getRequestsPerMinute = properties.rateLimit().getRequestsPerMinute();
    }

    public void checkPostLimit(UUID householdId, UUID userId) {
        checkLimit(postBuckets, "post", householdId, userId, postRequestsPerMinute);
    }

    public void checkGetLimit(UUID householdId, UUID userId) {
        checkLimit(getBuckets, "get", householdId, userId, getRequestsPerMinute);
    }

    private void checkLimit(
            ConcurrentMap<String, Bucket> buckets, String prefix, UUID householdId, UUID userId, int limit) {
        String key = prefix + ":" + householdId + ":" + userId;
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(limit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            int retryAfterSeconds = retryAfterSeconds(probe.getNanosToWaitForRefill());
            String message = "Too many requests. Please retry after " + retryAfterSeconds + " seconds";
            throw new AsrRateLimitedException("ASR_RATE_LIMITED", message, retryAfterSeconds);
        }
    }

    private Bucket newBucket(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofMinutes(1))))
                .build();
    }

    private int retryAfterSeconds(long nanosToWait) {
        int seconds = (int) Math.ceil(nanosToWait / 1_000_000_000.0);
        return Math.max(seconds, 1);
    }
}
