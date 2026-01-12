package com.hometusk.commands.pipeline.decision.client;

import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when AI Platform request fails.
 */
public class AiPlatformException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public AiPlatformException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiPlatformException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
