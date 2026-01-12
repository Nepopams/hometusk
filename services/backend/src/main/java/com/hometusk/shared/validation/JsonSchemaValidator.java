package com.hometusk.shared.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class JsonSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    public void validate(String schemaPath, Object payload) throws ValidationException {
        JsonSchema schema = getSchema(schemaPath);
        JsonNode payloadNode = objectMapper.valueToTree(payload);

        Set<ValidationMessage> errors = schema.validate(payloadNode);

        if (!errors.isEmpty()) {
            List<ValidationException.ValidationError> validationErrors = errors.stream()
                    .map(error -> new ValidationException.ValidationError(
                            error.getPath(), error.getType(), error.getMessage(), null))
                    .toList();

            throw new ValidationException(validationErrors);
        }
    }

    public boolean isValid(String schemaPath, Object payload) {
        try {
            validate(schemaPath, payload);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    private JsonSchema getSchema(String schemaPath) {
        return schemaCache.computeIfAbsent(schemaPath, this::loadSchema);
    }

    private JsonSchema loadSchema(String schemaPath) {
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
