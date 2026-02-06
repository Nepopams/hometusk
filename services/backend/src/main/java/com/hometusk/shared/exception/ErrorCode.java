package com.hometusk.shared.exception;

public enum ErrorCode {
    // Schema validation
    SCHEMA_INVALID("Request validation failed"),

    // Business rules
    BUSINESS_RULE_VIOLATION("Domain constraint violated"),
    ASSIGNEE_MUST_BE_MEMBER("Assignee must be a member of the household"),
    ZONE_MUST_EXIST("Zone must exist in the household"),
    INVALID_RECURRENCE_RULE("Invalid recurrence rule"),
    DEADLINE_MUST_BE_FUTURE("Deadline must be in the future"),
    INITIATOR_MUST_HAVE_PERMISSION("Initiator must have permission for this action"),
    TASK_ALREADY_COMPLETED("Task is already completed"),

    // Not found
    HOUSEHOLD_NOT_FOUND("Household not found"),
    COMMAND_NOT_FOUND("Command not found"),
    TASK_NOT_FOUND("Task not found"),
    USER_NOT_FOUND("User not found"),
    ZONE_NOT_FOUND("Zone not found"),
    ROUTINE_NOT_FOUND("Routine not found"),
    SHOPPING_LIST_NOT_FOUND("Shopping list not found"),
    SHOPPING_ITEM_NOT_FOUND("Shopping item not found"),
    SHOPPING_RUN_NOT_FOUND("Shopping run not found"),
    INVALID_TOKEN("Invite token not found"),
    NOTIFICATION_NOT_FOUND("Notification not found"),

    // Shopping validation (Stage 5)
    SHOPPING_ITEM_NAME_EMPTY("Shopping item name cannot be empty"),
    SHOPPING_ITEM_NAME_TOO_LONG("Shopping item name is too long"),

    // Invites
    INVITE_EXPIRED("Invite token expired"),
    INVITE_REDEEMED("Invite token already redeemed"),
    INVITE_REVOKED("Invite token revoked"),

    // Access control
    ACCESS_DENIED("Access denied to this resource"),

    // AI Platform (Stage 2)
    AI_REJECTED("AI rejected the command"),
    AI_UNAVAILABLE("AI Platform is unavailable"),

    // ASR (EP-011)
    ASR_INVALID_FORMAT("Unsupported audio format"),
    ASR_AUDIO_TOO_LONG("Audio duration exceeds maximum"),
    ASR_MISSING_FILE("Audio file is required"),
    ASR_FILE_TOO_LARGE("File size exceeds maximum"),
    ASR_RATE_LIMITED("Too many requests"),
    ASR_UNAVAILABLE("ASR service is unavailable"),
    ASR_NOT_FOUND("Transcription not found"),

    // Guardrails (Stage 3)
    GUARDRAILS_REJECTED("Command rejected by guardrails policy"),
    // Command continuation
    COMMAND_NOT_CONTINUABLE("Command cannot be continued in current state"),

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
