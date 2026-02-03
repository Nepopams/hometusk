package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrInvalidFormatException extends AsrException {

    public AsrInvalidFormatException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
