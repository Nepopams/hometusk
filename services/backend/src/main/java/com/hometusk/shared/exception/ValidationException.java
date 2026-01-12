package com.hometusk.shared.exception;

import java.util.Collections;
import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super("Request validation failed");
        this.errors = errors;
    }

    public ValidationException(String path, String code, String message) {
        super("Request validation failed");
        this.errors = Collections.singletonList(new ValidationError(path, code, message, null));
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public record ValidationError(String path, String code, String message, Object received) {}
}
