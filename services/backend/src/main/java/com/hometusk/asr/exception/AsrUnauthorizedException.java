package com.hometusk.asr.exception;

import org.springframework.http.HttpStatusCode;

public class AsrUnauthorizedException extends AsrException {

    public AsrUnauthorizedException(String code, String message, HttpStatusCode statusCode) {
        super(code, message, statusCode);
    }
}
