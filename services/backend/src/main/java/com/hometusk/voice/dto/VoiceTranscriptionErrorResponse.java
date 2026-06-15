package com.hometusk.voice.dto;

import java.util.Map;

public record VoiceTranscriptionErrorResponse(
        String code, String message, String correlationId, Map<String, Object> details) {}
