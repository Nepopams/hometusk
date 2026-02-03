package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrAudioTooLongException extends AsrException {

    public AsrAudioTooLongException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
