package com.hometusk.asr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asr_transcription_refs")
public class AsrTranscriptionRef {

    @Id
    @Column(name = "transcription_id")
    private UUID transcriptionId;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected AsrTranscriptionRef() {}

    public AsrTranscriptionRef(UUID transcriptionId, UUID householdId, UUID createdByUserId) {
        this.transcriptionId = transcriptionId;
        this.householdId = householdId;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plus(Duration.ofDays(7));
    }

    public UUID getTranscriptionId() {
        return transcriptionId;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
