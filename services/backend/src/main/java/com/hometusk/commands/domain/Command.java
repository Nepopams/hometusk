package com.hometusk.commands.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "commands")
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, unique = true)
    private UUID correlationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "type", nullable = false)
    private CommandType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status", nullable = false)
    private CommandStatus status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "client_timestamp")
    private Instant clientTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "execution_ms")
    private Integer executionMs;

    protected Command() {}

    public Command(
            UUID correlationId,
            Household household,
            User requester,
            CommandType type,
            String payload,
            String source,
            Instant clientTimestamp) {
        this.correlationId = correlationId;
        this.household = household;
        this.requester = requester;
        this.type = type;
        this.payload = payload;
        this.source = source;
        this.clientTimestamp = clientTimestamp;
        this.status = CommandStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public Household getHousehold() {
        return household;
    }

    @Transient
    public UUID getHouseholdId() {
        return household.getId();
    }

    public User getRequester() {
        return requester;
    }

    @Transient
    public UUID getRequesterId() {
        return requester.getId();
    }

    public CommandType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSource() {
        return source;
    }

    public Instant getClientTimestamp() {
        return clientTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Integer getExecutionMs() {
        return executionMs;
    }

    // Status transitions
    public void markValidating() {
        this.status = CommandStatus.VALIDATING;
    }

    public void markProcessing() {
        this.status = CommandStatus.PROCESSING;
    }

    public void markExecuted(int executionMs) {
        this.status = CommandStatus.EXECUTED;
        this.processedAt = Instant.now();
        this.executionMs = executionMs;
    }

    public void markFailed(String errorCode, String errorMessage, int executionMs) {
        this.status = CommandStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
        this.executionMs = executionMs;
    }

    public void markRejected(String errorCode, String errorMessage, int executionMs) {
        this.status = CommandStatus.REJECTED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
        this.executionMs = executionMs;
    }

    /** Stage 2: Mark command as needing user input (AI returned clarify) */
    public void markNeedsInput(String clarificationQuestion) {
        this.status = CommandStatus.NEEDS_INPUT;
        this.errorMessage = clarificationQuestion; // Store question in error_message field
    }

    public boolean isExecuted() {
        return this.status == CommandStatus.EXECUTED;
    }

    public boolean isFailed() {
        return this.status == CommandStatus.FAILED;
    }

    public boolean isRejected() {
        return this.status == CommandStatus.REJECTED;
    }
}
