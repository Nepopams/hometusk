package com.hometusk.voice.dto;

public record VoiceTranscriptionResponse(String transcript, String status, String traceId, long latencyMs) {}
