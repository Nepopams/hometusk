package com.hometusk.commands.domain;

import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "command_confirmations")
public class CommandConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    private Command command;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "initiator_id", nullable = false)
    private UUID initiatorId;

    @Column(name = "provider_confirmation_id")
    private String providerConfirmationId;

    @Column(name = "provider_decision_id")
    private UUID providerDecisionId;

    @Column(name = "provider_trace_id")
    private String providerTraceId;

    @Column(name = "schema_version")
    private String schemaVersion;

    @Column(name = "decision_version")
    private String decisionVersion;

    @Column(name = "status", nullable = false, length = 50)
    private CommandConfirmationStatus status;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons", nullable = false, columnDefinition = "jsonb")
    private String reasons;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_labels", nullable = false, columnDefinition = "jsonb")
    private String riskLabels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_actions", nullable = false, columnDefinition = "jsonb")
    private String proposedActions;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "expiry_processed_at")
    private Instant expiryProcessedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_result", columnDefinition = "jsonb")
    private String executionResult;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CommandConfirmation() {}

    public CommandConfirmation(
            Command command,
            String providerConfirmationId,
            UUID providerDecisionId,
            String providerTraceId,
            String schemaVersion,
            String decisionVersion,
            String summary,
            String reasons,
            String riskLabels,
            String proposedActions,
            Instant expiresAt) {
        this.command = command;
        this.householdId = command.getHouseholdId();
        this.initiatorId = command.getRequesterId();
        this.providerConfirmationId = providerConfirmationId;
        this.providerDecisionId = providerDecisionId;
        this.providerTraceId = providerTraceId;
        this.schemaVersion = schemaVersion;
        this.decisionVersion = decisionVersion;
        this.status = CommandConfirmationStatus.PENDING_CONFIRMATION;
        this.summary = summary;
        this.reasons = reasons;
        this.riskLabels = riskLabels;
        this.proposedActions = proposedActions;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Command getCommand() {
        return command;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public UUID getInitiatorId() {
        return initiatorId;
    }

    public String getProviderConfirmationId() {
        return providerConfirmationId;
    }

    public UUID getProviderDecisionId() {
        return providerDecisionId;
    }

    public String getProviderTraceId() {
        return providerTraceId;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getDecisionVersion() {
        return decisionVersion;
    }

    public CommandConfirmationStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getReasons() {
        return reasons;
    }

    public String getRiskLabels() {
        return riskLabels;
    }

    public String getProposedActions() {
        return proposedActions;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getExpiryProcessedAt() {
        return expiryProcessedAt;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPending() {
        return status == CommandConfirmationStatus.PENDING_CONFIRMATION;
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public void markConfirmed(User actor, Instant at) {
        this.status = CommandConfirmationStatus.CONFIRMED;
        this.approvedBy = actor.getId();
        this.approvedAt = at;
        this.updatedAt = at;
    }

    public void markExecuted(String executionResult, Instant at) {
        this.status = CommandConfirmationStatus.EXECUTED;
        this.executionResult = executionResult;
        this.updatedAt = at;
    }

    public void markCancelled(User actor, String reason, Instant at) {
        this.status = CommandConfirmationStatus.CANCELLED;
        this.cancelledBy = actor.getId();
        this.cancelledAt = at;
        this.cancelReason = reason;
        this.updatedAt = at;
    }

    public void markExpired(Instant at) {
        this.status = CommandConfirmationStatus.EXPIRED;
        this.expiryProcessedAt = at;
        this.updatedAt = at;
    }

    public void markRejected(String failureCode, String failureMessage, Instant at) {
        this.status = CommandConfirmationStatus.REJECTED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.updatedAt = at;
    }
}
