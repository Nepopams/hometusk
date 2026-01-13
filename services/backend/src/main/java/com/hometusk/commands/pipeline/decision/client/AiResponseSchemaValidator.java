package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates AI Platform decision responses against JSON Schema.
 *
 * <p>This validator ensures that AI Platform responses conform to the expected
 * schema BEFORE mapping to DecisionResult. Invalid responses are rejected
 * with detailed error information for debugging.
 *
 * <p>Per CLAUDE.md Rule 1: "AI output MUST be schema-validated before use"
 */
@Component
public class AiResponseSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(AiResponseSchemaValidator.class);
    private static final String SCHEMA_PATH = "/schemas/ai-decision-response.schema.json";

    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    public AiResponseSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schema = loadSchema();
    }

    /**
     * Validates raw JSON response from AI Platform.
     *
     * @param rawJson the raw JSON string from AI Platform
     * @return validation result with errors if any
     */
    public ValidationResult validateRaw(String rawJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawJson);
            return validate(jsonNode);
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON", e);
            return ValidationResult.invalid(
                    List.of(new ValidationError("$", "PARSE_ERROR", "Invalid JSON: " + e.getMessage())));
        }
    }

    /**
     * Validates parsed AI decision response.
     *
     * @param response the parsed response object
     * @return validation result with errors if any
     */
    public ValidationResult validate(AiDecisionResponse response) {
        JsonNode jsonNode = objectMapper.valueToTree(response);
        return validate(jsonNode);
    }

    /**
     * Validates JSON node against schema.
     */
    private ValidationResult validate(JsonNode jsonNode) {
        Set<ValidationMessage> errors = schema.validate(jsonNode);

        if (errors.isEmpty()) {
            log.debug("AI response schema validation passed");
            return ValidationResult.valid();
        }

        log.warn("AI response schema validation failed: {} errors", errors.size());

        List<ValidationError> validationErrors = errors.stream()
                .map(error -> new ValidationError(error.getPath(), error.getType(), error.getMessage()))
                .toList();

        return ValidationResult.invalid(validationErrors);
    }

    private JsonSchema loadSchema() {
        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_PATH);
            if (schemaStream == null) {
                throw new RuntimeException("AI response schema not found: " + SCHEMA_PATH);
            }
            return factory.getSchema(schemaStream);
        } catch (Exception e) {
            log.error("Failed to load AI response schema: {}", SCHEMA_PATH, e);
            throw new RuntimeException("Failed to load AI response schema", e);
        }
    }

    /**
     * Result of schema validation.
     */
    public record ValidationResult(boolean valid, List<ValidationError> errors) {

        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<ValidationError> errors) {
            return new ValidationResult(false, errors);
        }

        public String getErrorSummary() {
            if (valid) {
                return "Valid";
            }
            return errors.stream()
                    .map(e -> e.path() + ": " + e.message())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown error");
        }
    }

    /**
     * Single validation error.
     */
    public record ValidationError(String path, String type, String message) {}
}
