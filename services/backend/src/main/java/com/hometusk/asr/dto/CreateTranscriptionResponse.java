package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateTranscriptionResponse(UUID id, String status, Instant createdAt) {
    public static CreateTranscriptionResponse from(AsrJobCreated job) {
        return new CreateTranscriptionResponse(job.id(), job.status(), job.createdAt());
    }
}
