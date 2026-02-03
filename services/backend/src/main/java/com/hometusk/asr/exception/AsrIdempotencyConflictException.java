package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrIdempotencyConflictException extends AsrException {

    public AsrIdempotencyConflictException() {
        super("IDEMPOTENCY_CONFLICT", "Idempotency-Key already used with different payload", HttpStatus.CONFLICT);
    }
}
