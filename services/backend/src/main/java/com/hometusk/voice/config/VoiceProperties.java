package com.hometusk.voice.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voice")
public record VoiceProperties(Boolean enabled, AsrProperties asr) {

    public VoiceProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (asr == null) {
            asr = new AsrProperties(null, null, null, null, 0, 0, 0, null, 0);
        }
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public record AsrProperties(
            Boolean enabled,
            String baseUrl,
            String transcribePath,
            String apiKey,
            int connectTimeoutMs,
            int readTimeoutMs,
            long maxSizeBytes,
            List<String> allowedMediaTypes,
            int requestsPerMinute) {

        public AsrProperties {
            if (enabled == null) {
                enabled = true;
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "http://localhost:8090";
            }
            if (transcribePath == null || transcribePath.isBlank()) {
                transcribePath = "/v1/asr/transcribe";
            }
            if (connectTimeoutMs <= 0) {
                connectTimeoutMs = 5000;
            }
            if (readTimeoutMs <= 0) {
                readTimeoutMs = 30000;
            }
            if (maxSizeBytes <= 0) {
                maxSizeBytes = 10_485_760L;
            }
            if (allowedMediaTypes == null || allowedMediaTypes.isEmpty()) {
                allowedMediaTypes = defaultAllowedMediaTypes();
            }
            if (requestsPerMinute <= 0) {
                requestsPerMinute = 5;
            }
        }

        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    private static List<String> defaultAllowedMediaTypes() {
        return List.of(
                "audio/mpeg",
                "audio/mp3",
                "audio/mp4",
                "audio/m4a",
                "audio/wav",
                "audio/x-wav",
                "audio/webm",
                "audio/ogg",
                "audio/flac");
    }
}
