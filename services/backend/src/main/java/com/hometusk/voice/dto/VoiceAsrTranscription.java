package com.hometusk.voice.dto;

public record VoiceAsrTranscription(String transcript, String traceId, Long latencyMs) {}
