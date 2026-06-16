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
    /** AI proposed a plan that requires explicit user approval before mutation */
    NEEDS_CONFIRMATION,
    EXECUTED,
    FAILED,
    REJECTED
}
