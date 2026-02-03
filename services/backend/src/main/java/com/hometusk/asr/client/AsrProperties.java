package com.hometusk.asr.client;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asr")
public record AsrProperties(
        String baseUrl,
        String apiKey,
        int connectTimeoutMs,
        int readTimeoutMs,
        GuardrailsProperties guardrails,
        RateLimitProperties rateLimit) {
    public AsrProperties {
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 5000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 30000;
        }
        if (guardrails == null) {
            guardrails = new GuardrailsProperties(0, null);
        }
        if (rateLimit == null) {
            rateLimit = new RateLimitProperties(0, 0);
        }
    }

    public record GuardrailsProperties(long maxSizeBytes, List<String> allowedFormats) {
        public GuardrailsProperties {
            if (maxSizeBytes <= 0) {
                maxSizeBytes = 10_485_760L;
            }
            if (allowedFormats == null || allowedFormats.isEmpty()) {
                allowedFormats = defaultAllowedFormats();
            }
        }
    }

    public record RateLimitProperties(int postRequestsPerMinute, int getRequestsPerMinute) {
        public RateLimitProperties {
            if (postRequestsPerMinute <= 0) {
                postRequestsPerMinute = 5;
            }
            if (getRequestsPerMinute <= 0) {
                getRequestsPerMinute = 30;
            }
        }
    }

    private static List<String> defaultAllowedFormats() {
        return List.of("audio/ogg", "audio/mpeg", "audio/wav", "audio/mp4", "audio/webm");
    }
}
