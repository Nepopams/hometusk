package com.hometusk.commands.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.Command;
import com.hometusk.commands.domain.DecisionLog;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.repository.DecisionLogRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes decision logs for audit and traceability.
 *
 * Per CLAUDE.md rule 3: Command traceability is mandatory.
 * Every command must have a DecisionLog.
 */
@Component
public class DecisionLogWriter {

    private static final Logger log = LoggerFactory.getLogger(DecisionLogWriter.class);

    private final DecisionLogRepository decisionLogRepository;
    private final ObjectMapper objectMapper;

    public DecisionLogWriter(DecisionLogRepository decisionLogRepository, ObjectMapper objectMapper) {
        this.decisionLogRepository = decisionLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates and persists a decision log.
     */
    public DecisionLog write(DecisionLogEntry entry) {
        DecisionLog decisionLog = new DecisionLog(entry.command(), entry.correlationId());

        decisionLog.setIntent(toJson(entry.intent()));
        decisionLog.setContextSnapshot(toJson(entry.contextSnapshot()));
        decisionLog.setDecision(toJson(entry.decision()));
        decisionLog.setSource(entry.source());
        decisionLog.setConfidence(entry.confidence());

        if (entry.alternativesConsidered() != null) {
            decisionLog.setAlternativesConsidered(toJson(entry.alternativesConsidered()));
        }

        decisionLog.setSchemaValid(entry.schemaValid());
        decisionLog.setBusinessValid(entry.businessValid());

        if (entry.validationErrors() != null) {
            decisionLog.setValidationErrors(toJson(entry.validationErrors()));
        }

        DecisionLog saved = decisionLogRepository.save(decisionLog);
        log.debug("Decision log written: id={}, correlationId={}", saved.getId(), saved.getCorrelationId());

        return saved;
    }

    /**
     * Creates a decision log for a failed validation.
     */
    public DecisionLog writeValidationFailure(
            Command command, UUID correlationId, Object intent, Object errors, boolean isSchemaError) {
        DecisionLog decisionLog = new DecisionLog(command, correlationId);

        decisionLog.setIntent(toJson(intent));
        decisionLog.setContextSnapshot(toJson(Map.of("type", "validation_failure")));
        decisionLog.setDecision(toJson(Map.of("action", "rejected", "reason", "validation_failed")));

        if (isSchemaError) {
            decisionLog.markSchemaInvalid(toJson(errors));
        } else {
            decisionLog.markBusinessInvalid(toJson(errors));
        }

        DecisionLog saved = decisionLogRepository.save(decisionLog);
        log.debug("Validation failure logged: id={}, correlationId={}", saved.getId(), saved.getCorrelationId());

        return saved;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return "{}";
        }
    }

    public record DecisionLogEntry(
            Command command,
            UUID correlationId,
            Object intent,
            Object contextSnapshot,
            Object decision,
            DecisionSource source,
            BigDecimal confidence,
            Object alternativesConsidered,
            boolean schemaValid,
            boolean businessValid,
            Object validationErrors) {

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Command command;
            private UUID correlationId;
            private Object intent;
            private Object contextSnapshot;
            private Object decision;
            private DecisionSource source = DecisionSource.RULE;
            private BigDecimal confidence = BigDecimal.ONE;
            private Object alternativesConsidered;
            private boolean schemaValid = true;
            private boolean businessValid = true;
            private Object validationErrors;

            public Builder command(Command command) {
                this.command = command;
                return this;
            }

            public Builder correlationId(UUID correlationId) {
                this.correlationId = correlationId;
                return this;
            }

            public Builder intent(Object intent) {
                this.intent = intent;
                return this;
            }

            public Builder contextSnapshot(Object contextSnapshot) {
                this.contextSnapshot = contextSnapshot;
                return this;
            }

            public Builder decision(Object decision) {
                this.decision = decision;
                return this;
            }

            public Builder source(DecisionSource source) {
                this.source = source;
                return this;
            }

            public Builder confidence(BigDecimal confidence) {
                this.confidence = confidence;
                return this;
            }

            public Builder alternativesConsidered(Object alternativesConsidered) {
                this.alternativesConsidered = alternativesConsidered;
                return this;
            }

            public Builder schemaValid(boolean schemaValid) {
                this.schemaValid = schemaValid;
                return this;
            }

            public Builder businessValid(boolean businessValid) {
                this.businessValid = businessValid;
                return this;
            }

            public Builder validationErrors(Object validationErrors) {
                this.validationErrors = validationErrors;
                return this;
            }

            public DecisionLogEntry build() {
                return new DecisionLogEntry(
                        command,
                        correlationId,
                        intent,
                        contextSnapshot,
                        decision,
                        source,
                        confidence,
                        alternativesConsidered,
                        schemaValid,
                        businessValid,
                        validationErrors);
            }
        }
    }
}
