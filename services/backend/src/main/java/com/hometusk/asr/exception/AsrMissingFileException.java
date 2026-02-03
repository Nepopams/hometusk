package com.hometusk.asr.exception;

import org.springframework.http.HttpStatus;

public class AsrMissingFileException extends AsrException {

    public AsrMissingFileException() {
        super("ASR_MISSING_FILE", "Audio file is required", HttpStatus.BAD_REQUEST);
    }
}
