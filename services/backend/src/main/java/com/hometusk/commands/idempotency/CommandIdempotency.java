package com.hometusk.commands.idempotency;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "command_idempotency",
        indexes = {
            @Index(name = "idx_command_idempotency_key_user", columnList = "idempotency_key,initiator_user_id"),
            @Index(name = "idx_command_idempotency_expires_at", columnList = "expires_at")
        })
public class CommandIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "initiator_user_id", nullable = false)
    private UUID initiatorUserId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "stored_response_json", columnDefinition = "TEXT")
    private String storedResponseJson;

    @Column(name = "stored_http_status")
    private Integer storedHttpStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected CommandIdempotency() {}

    public CommandIdempotency(String idempotencyKey, UUID initiatorUserId, String requestHash, Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.initiatorUserId = initiatorUserId;
        this.requestHash = requestHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public void storeResponse(String responseJson, int httpStatus) {
        if (this.storedResponseJson == null) {
            this.storedResponseJson = responseJson;
            this.storedHttpStatus = httpStatus;
        }
    }

    public boolean hasStoredResponse() {
        return storedResponseJson != null && storedHttpStatus != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getInitiatorUserId() {
        return initiatorUserId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getStoredResponseJson() {
        return storedResponseJson;
    }

    public Integer getStoredHttpStatus() {
        return storedHttpStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
