package com.hometusk.shared.exception;

public enum ErrorCode {
    // Schema validation
    SCHEMA_INVALID("Request validation failed"),

    // Business rules
    BUSINESS_RULE_VIOLATION("Domain constraint violated"),
    ASSIGNEE_MUST_BE_MEMBER("Assignee must be a member of the household"),
    ZONE_MUST_EXIST("Zone must exist in the household"),
    DEADLINE_MUST_BE_FUTURE("Deadline must be in the future"),
    INITIATOR_MUST_HAVE_PERMISSION("Initiator must have permission for this action"),
    TASK_ALREADY_COMPLETED("Task is already completed"),

    // Not found
    HOUSEHOLD_NOT_FOUND("Household not found"),
    TASK_NOT_FOUND("Task not found"),
    USER_NOT_FOUND("User not found"),
    ZONE_NOT_FOUND("Zone not found"),

    // Access control
    ACCESS_DENIED("Access denied to this resource"),

    // AI Platform (Stage 2)
    AI_REJECTED("AI rejected the command"),
    AI_UNAVAILABLE("AI Platform is unavailable"),

    // Guardrails (Stage 3)
    GUARDRAILS_REJECTED("Command rejected by guardrails policy"),

    // Other
    IDEMPOTENCY_CONFLICT("Request with this idempotency key already processed"),
    INTERNAL_ERROR("Internal server error");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
