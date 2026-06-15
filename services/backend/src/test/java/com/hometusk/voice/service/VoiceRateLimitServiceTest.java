package com.hometusk.voice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hometusk.voice.config.VoiceProperties;
import com.hometusk.voice.exception.VoiceAsrException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoiceRateLimitServiceTest {

    @Test
    void checkLimit_whenExceeded_throwsLocalRateLimit() {
        VoiceProperties properties = new VoiceProperties(
                true,
                new VoiceProperties.AsrProperties(
                        true,
                        "http://localhost:8090",
                        "/v1/asr/transcribe",
                        "",
                        1000,
                        1000,
                        10_485_760L,
                        List.of("audio/webm"),
                        1));
        VoiceRateLimitService service = new VoiceRateLimitService(properties);
        UUID userId = UUID.randomUUID();

        service.checkLimit(userId);

        assertThatThrownBy(() -> service.checkLimit(userId))
                .isInstanceOf(VoiceAsrException.class)
                .hasMessageContaining("Too many voice transcription requests")
                .extracting("code")
                .isEqualTo("local_rate_limit");
    }
}
