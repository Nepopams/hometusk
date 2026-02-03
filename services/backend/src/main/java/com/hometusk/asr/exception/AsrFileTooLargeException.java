package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrFileTooLargeException extends AsrException {

    public AsrFileTooLargeException(String code, String message) {
        super(code, message, HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
