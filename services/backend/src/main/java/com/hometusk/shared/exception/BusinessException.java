package com.hometusk.shared.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<Violation> violations;
    private final Map<String, Object> context;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.violations = Collections.emptyList();
        this.context = Collections.emptyMap();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.violations = Collections.emptyList();
        this.context = Collections.emptyMap();
    }

    public BusinessException(ErrorCode errorCode, List<Violation> violations) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.violations = violations;
        this.context = Collections.emptyMap();
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.violations = Collections.emptyList();
        this.context = context;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public record Violation(String rule, String message, Map<String, Object> context) {
        public Violation(String rule, String message) {
            this(rule, message, Collections.emptyMap());
        }
    }
}
