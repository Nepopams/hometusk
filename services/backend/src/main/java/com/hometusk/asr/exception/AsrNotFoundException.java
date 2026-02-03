package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrNotFoundException extends AsrException {

    public AsrNotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
