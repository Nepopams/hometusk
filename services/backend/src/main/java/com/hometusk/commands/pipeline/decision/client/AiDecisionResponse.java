package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO from the upstream AI Platform decision API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiDecisionResponse(
        @JsonProperty("decision_id") String decisionId,
        @JsonProperty("command_id") String commandId,
        String status,
        String action,
        BigDecimal confidence,
        Map<String, Object> payload,
        String explanation,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("decision_version") String decisionVersion,
        @JsonProperty("created_at") String createdAt) {

    public UUID decisionUuidOrNull() {
        if (decisionId == null || decisionId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(decisionId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
