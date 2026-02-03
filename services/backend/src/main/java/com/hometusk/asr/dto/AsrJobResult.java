package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AsrJobResult(
        UUID id,
        String status,
        String text,
        List<AsrSegment> segments,
        String model,
        Integer durationMs,
        String lang,
        Instant createdAt,
        Instant finishedAt,
        AsrTranscriptionError error) {}
