package com.hometusk.commands.pipeline.decision;

/**
 * Thrown when the decision provider is unavailable and fallback is disabled.
 */
public class DecisionProviderUnavailableException extends RuntimeException {

    public DecisionProviderUnavailableException(String message) {
        super(message);
    }

    public DecisionProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
