package com.hometusk.asr.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hometusk.asr.client.AsrProperties;
import com.hometusk.asr.exception.AsrFileTooLargeException;
import com.hometusk.asr.exception.AsrInvalidFormatException;
import com.hometusk.asr.exception.AsrMissingFileException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AsrValidationServiceTest {

    @Test
    void validateFile_missing_throwsException() {
        AsrValidationService service = new AsrValidationService(defaultProperties(10, List.of("audio/ogg")));
        assertThrows(AsrMissingFileException.class, () -> service.validateFile(null));
    }

    @Test
    void validateSize_tooLarge_throwsException() {
        AsrValidationService service = new AsrValidationService(defaultProperties(5, List.of("audio/ogg")));
        MockMultipartFile file = new MockMultipartFile("file", "audio.ogg", "audio/ogg", "123456".getBytes());
        assertThrows(AsrFileTooLargeException.class, () -> service.validateFile(file));
    }

    @Test
    void validateSize_atLimit_passes() {
        AsrValidationService service = new AsrValidationService(defaultProperties(4, List.of("audio/ogg")));
        MockMultipartFile file = new MockMultipartFile("file", "audio.ogg", "audio/ogg", "1234".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    @Test
    void validateFormat_invalid_throwsException() {
        AsrValidationService service = new AsrValidationService(defaultProperties(10, List.of("audio/ogg")));
        MockMultipartFile file = new MockMultipartFile("file", "audio.wav", "audio/wav", "1234".getBytes());
        assertThrows(AsrInvalidFormatException.class, () -> service.validateFile(file));
    }

    @Test
    void validateFormat_ogg_passes() {
        AsrValidationService service = new AsrValidationService(defaultProperties(10, List.of("audio/ogg")));
        MockMultipartFile file = new MockMultipartFile("file", "audio.ogg", "audio/ogg", "1234".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    @Test
    void validateFormat_webm_passes() {
        AsrValidationService service = new AsrValidationService(defaultProperties(10, List.of("audio/webm")));
        MockMultipartFile file = new MockMultipartFile("file", "audio.webm", "audio/webm", "1234".getBytes());
        assertDoesNotThrow(() -> service.validateFile(file));
    }

    private AsrProperties defaultProperties(long maxSizeBytes, List<String> allowedFormats) {
        AsrProperties.GuardrailsProperties guardrails =
                new AsrProperties.GuardrailsProperties(maxSizeBytes, allowedFormats);
        AsrProperties.RateLimitProperties rateLimit = new AsrProperties.RateLimitProperties(5, 30);
        return new AsrProperties("http://localhost", "", 1000, 1000, guardrails, rateLimit);
    }
}
