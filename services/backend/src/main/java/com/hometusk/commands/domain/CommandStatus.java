package com.hometusk.commands.domain;

/**
 * Command processing status.
 */
public enum CommandStatus {
    RECEIVED,
    VALIDATING,
    PROCESSING,
    /** Command accepted for later execution */
    SCHEDULED,
    /** AI needs clarification from user (Stage 2) */
    NEEDS_INPUT,
    EXECUTED,
    FAILED,
    REJECTED
}
