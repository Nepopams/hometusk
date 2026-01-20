package com.hometusk.commands.domain;

/**
 * Command processing status.
 */
public enum CommandStatus {
    RECEIVED,
    VALIDATING,
    PROCESSING,
    /** AI needs clarification from user (Stage 2) */
    NEEDS_INPUT,
    EXECUTED,
    FAILED,
    REJECTED
}
