package com.hometusk.commands.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "decision_logs")
public class DecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    private Command command;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "intent", nullable = false, columnDefinition = "jsonb")
    private String intent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_snapshot", nullable = false, columnDefinition = "jsonb")
    private String contextSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision", nullable = false, columnDefinition = "jsonb")
    private String decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private DecisionSource source;

    @Column(name = "confidence", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alternatives_considered", columnDefinition = "jsonb")
    private String alternativesConsidered;

    @Column(name = "schema_valid", nullable = false)
    private boolean schemaValid;

    @Column(name = "business_valid", nullable = false)
    private boolean businessValid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private String validationErrors;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DecisionLog() {}

    public DecisionLog(Command command, UUID correlationId) {
        this.command = command;
        this.correlationId = correlationId;
        this.source = DecisionSource.RULE;
        this.confidence = BigDecimal.ONE; // Stage 1: always 1.0 (rule-based)
        this.schemaValid = true;
        this.businessValid = true;
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Command getCommand() {
        return command;
    }

    public UUID getCommandId() {
        return command.getId();
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getIntent() {
        return intent;
    }

    public String getContextSnapshot() {
        return contextSnapshot;
    }

    public String getDecision() {
        return decision;
    }

    public DecisionSource getSource() {
        return source;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getAlternativesConsidered() {
        return alternativesConsidered;
    }

    public boolean isSchemaValid() {
        return schemaValid;
    }

    public boolean isBusinessValid() {
        return businessValid;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setContextSnapshot(String contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public void setSource(DecisionSource source) {
        this.source = source;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public void setAlternativesConsidered(String alternativesConsidered) {
        this.alternativesConsidered = alternativesConsidered;
    }

    public void setSchemaValid(boolean schemaValid) {
        this.schemaValid = schemaValid;
    }

    public void setBusinessValid(boolean businessValid) {
        this.businessValid = businessValid;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    public void markSchemaInvalid(String validationErrors) {
        this.schemaValid = false;
        this.validationErrors = validationErrors;
    }

    public void markBusinessInvalid(String validationErrors) {
        this.businessValid = false;
        this.validationErrors = validationErrors;
    }
}
