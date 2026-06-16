package com.hometusk.commands.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.CommandType;
import com.hometusk.shared.exception.ValidationException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates command payloads against JSON Schema.
 * Part of the command pipeline.
 */
@Component
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    private static final Map<CommandType, String> SCHEMA_PATHS = Map.of(
            CommandType.CREATE_TASK, "/schemas/create-task.schema.json",
            CommandType.COMPLETE_TASK, "/schemas/complete-task.schema.json",
            CommandType.NATURAL_COMMAND, "/schemas/natural-command.schema.json");

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final Map<CommandType, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Validates the payload against the JSON Schema for the given command type.
     *
     * @param commandType The type of command
     * @param payload The payload to validate
     * @throws ValidationException if validation fails
     */
    public void validate(CommandType commandType, Object payload) throws ValidationException {
        JsonSchema schema = getSchema(commandType);
        JsonNode payloadNode = objectMapper.valueToTree(payload);

        Set<ValidationMessage> errors = schema.validate(payloadNode);

        if (!errors.isEmpty()) {
            log.debug("Schema validation failed for {}: {} errors", commandType, errors.size());

            List<ValidationException.ValidationError> validationErrors = errors.stream()
                    .map(error -> new ValidationException.ValidationError(
                            error.getPath(), error.getType(), error.getMessage(), null))
                    .toList();

            throw new ValidationException(validationErrors);
        }

        log.debug("Schema validation passed for {}", commandType);
    }

    /**
     * Checks if the payload is valid without throwing an exception.
     */
    public boolean isValid(CommandType commandType, Object payload) {
        try {
            validate(commandType, payload);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    private JsonSchema getSchema(CommandType commandType) {
        return schemaCache.computeIfAbsent(commandType, this::loadSchema);
    }

    private JsonSchema loadSchema(CommandType commandType) {
        String schemaPath = SCHEMA_PATHS.get(commandType);
        if (schemaPath == null) {
            throw new IllegalArgumentException("No schema defined for command type: " + commandType);
        }

        try {
            InputStream schemaStream = getClass().getResourceAsStream(schemaPath);
            if (schemaStream == null) {
                throw new RuntimeException("Schema not found: " + schemaPath);
            }
            return schemaFactory.getSchema(schemaStream);
        } catch (Exception e) {
            log.error("Failed to load schema: {}", schemaPath, e);
            throw new RuntimeException("Failed to load schema: " + schemaPath, e);
        }
    }
}
