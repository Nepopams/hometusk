package com.hometusk.voice.dto;

public record VoiceAsrUpstreamResponse(
        String transcript, String text, String status, String traceId, String requestId, Long latencyMs) {}
