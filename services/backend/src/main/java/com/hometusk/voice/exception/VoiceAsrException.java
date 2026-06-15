package com.hometusk.voice.exception;

import org.springframework.http.HttpStatus;

public class VoiceAsrException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Integer retryAfterSeconds;

    public VoiceAsrException(String code, String message, HttpStatus status) {
        this(code, message, status, null, null);
    }

    public VoiceAsrException(
            String code, String message, HttpStatus status, Integer retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static VoiceAsrException invalidMultipart(String message) {
        return new VoiceAsrException("invalid_multipart", message, HttpStatus.BAD_REQUEST);
    }

    public static VoiceAsrException missingAudioFile() {
        return new VoiceAsrException("missing_audio_file", "Audio file is required", HttpStatus.BAD_REQUEST);
    }

    public static VoiceAsrException fileTooLarge(String message) {
        return new VoiceAsrException("file_too_large", message, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    public static VoiceAsrException unsupportedMedia(String message) {
        return new VoiceAsrException("unsupported_media", message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    public static VoiceAsrException configError(String message) {
        return new VoiceAsrException("asr_config_error", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static VoiceAsrException authError(String message) {
        return new VoiceAsrException("auth_error", message, HttpStatus.BAD_GATEWAY);
    }

    public static VoiceAsrException badUpstreamResponse(String message) {
        return new VoiceAsrException("bad_upstream_response", message, HttpStatus.BAD_GATEWAY);
    }

    public static VoiceAsrException upstreamUnavailable(String message, Throwable cause) {
        return new VoiceAsrException("upstream_unavailable", message, HttpStatus.BAD_GATEWAY, null, cause);
    }

    public static VoiceAsrException timeout(String message, Throwable cause) {
        return new VoiceAsrException("timeout", message, HttpStatus.GATEWAY_TIMEOUT, null, cause);
    }

    public static VoiceAsrException localRateLimit(String message, int retryAfterSeconds) {
        return new VoiceAsrException(
                "local_rate_limit", message, HttpStatus.TOO_MANY_REQUESTS, retryAfterSeconds, null);
    }
}
