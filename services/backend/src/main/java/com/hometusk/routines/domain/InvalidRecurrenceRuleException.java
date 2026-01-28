package com.hometusk.routines.domain;

/**
 * Exception thrown when a RecurrenceRule is invalid for parsing.
 */
public class InvalidRecurrenceRuleException extends RuntimeException {

    public InvalidRecurrenceRuleException(String message) {
        super(message);
    }
}
