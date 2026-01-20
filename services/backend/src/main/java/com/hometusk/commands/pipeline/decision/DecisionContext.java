package com.hometusk.commands.pipeline.decision;

import com.hometusk.commands.domain.CommandType;
import java.util.Map;
import java.util.UUID;

/**
 * All context needed for decision-making.
 * Sent to external AI Platform as part of DecisionRequest.
 */
public record DecisionContext(
        UUID commandId,
        UUID correlationId,
        CommandType commandType,
        Map<String, Object> payload,
        UUID requesterId,
        UUID householdId,
        /** Minimal context for AI (member names, zone names, etc.) */
        Map<String, Object> householdContext) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID commandId;
        private UUID correlationId;
        private CommandType commandType;
        private Map<String, Object> payload;
        private UUID requesterId;
        private UUID householdId;
        private Map<String, Object> householdContext = Map.of();

        public Builder commandId(UUID commandId) {
            this.commandId = commandId;
            return this;
        }

        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder commandType(CommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public Builder requesterId(UUID requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        public Builder householdId(UUID householdId) {
            this.householdId = householdId;
            return this;
        }

        public Builder householdContext(Map<String, Object> householdContext) {
            this.householdContext = householdContext;
            return this;
        }

        public DecisionContext build() {
            return new DecisionContext(
                    commandId, correlationId, commandType, payload, requesterId, householdId, householdContext);
        }
    }
}
