package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TranscriptionResultResponse(
        UUID id,
        String status,
        String text,
        List<AsrSegment> segments,
        String model,
        Integer durationMs,
        String lang,
        Instant createdAt,
        Instant finishedAt,
        Integer pollAfterMs,
        AsrTranscriptionError error) {
    private static final int DEFAULT_POLL_AFTER_MS = 2000;

    public static TranscriptionResultResponse from(AsrJobResult job) {
        Integer pollAfterMs = null;
        if (job != null && job.status() != null) {
            String status = job.status();
            if ("queued".equals(status) || "processing".equals(status)) {
                pollAfterMs = DEFAULT_POLL_AFTER_MS;
            }
        }
        return new TranscriptionResultResponse(
                job.id(),
                job.status(),
                job.text(),
                job.segments(),
                job.model(),
                job.durationMs(),
                job.lang(),
                job.createdAt(),
                job.finishedAt(),
                pollAfterMs,
                job.error());
    }
}
