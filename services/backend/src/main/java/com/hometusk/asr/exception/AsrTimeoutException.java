package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class AsrTimeoutException extends AsrException {

    public AsrTimeoutException(String code, String message, HttpStatusCode statusCode) {
        super(code, message, statusCode);
    }

    public AsrTimeoutException(String code, String message, Throwable cause) {
        super(code, message, HttpStatus.GATEWAY_TIMEOUT, cause);
    }
}
