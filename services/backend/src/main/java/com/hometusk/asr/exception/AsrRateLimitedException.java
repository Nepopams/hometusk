package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrRateLimitedException extends AsrException {

    private final Integer retryAfterSeconds;

    public AsrRateLimitedException(String code, String message, Integer retryAfterSeconds) {
        super(code, message, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
