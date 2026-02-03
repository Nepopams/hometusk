package com.hometusk.asr.exception;

import org.springframework.http.HttpStatusCode;

public class AsrException extends RuntimeException {

    private final String code;
    private final HttpStatusCode statusCode;

    public AsrException(String code, String message, HttpStatusCode statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public AsrException(String code, String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
