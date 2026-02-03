package com.hometusk.asr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asr_idempotency_records")
public class AsrIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "payload_digest", nullable = false, length = 64)
    private String payloadDigest;

    @Column(name = "transcription_id", nullable = false)
    private UUID transcriptionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected AsrIdempotencyRecord() {}

    public AsrIdempotencyRecord(
            UUID householdId,
            UUID userId,
            String idempotencyKey,
            String payloadDigest,
            UUID transcriptionId,
            Instant createdAt,
            Instant expiresAt) {
        this.householdId = householdId;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.payloadDigest = payloadDigest;
        this.transcriptionId = transcriptionId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPayloadDigest() {
        return payloadDigest;
    }

    public UUID getTranscriptionId() {
        return transcriptionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
