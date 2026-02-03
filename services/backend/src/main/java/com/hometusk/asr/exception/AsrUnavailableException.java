package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrUnavailableException extends AsrException {

    private final Integer retryAfterSeconds;

    public AsrUnavailableException(String code, String message, Integer retryAfterSeconds) {
        super(code, message, HttpStatus.SERVICE_UNAVAILABLE);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
