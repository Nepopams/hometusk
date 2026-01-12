package com.hometusk.commands.domain;

/**
 * Command processing status.
 */
public enum CommandStatus {
    RECEIVED,
    VALIDATING,
    PROCESSING,
    EXECUTED,
    FAILED,
    REJECTED
}
