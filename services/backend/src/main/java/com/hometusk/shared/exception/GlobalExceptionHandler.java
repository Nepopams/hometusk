package com.hometusk.shared.exception;

import com.hometusk.asr.dto.AsrProxyErrorResponse;
import com.hometusk.asr.exception.AsrException;
import com.hometusk.asr.exception.AsrIdempotencyConflictException;
import com.hometusk.asr.exception.AsrMissingFileException;
import com.hometusk.asr.exception.AsrRateLimitedException;
import com.hometusk.asr.exception.AsrTimeoutException;
import com.hometusk.asr.exception.AsrUnauthorizedException;
import com.hometusk.asr.exception.AsrUnavailableException;
import com.hometusk.shared.logging.MdcKeys;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                getCorrelationId(),
                ErrorCode.SCHEMA_INVALID.name(),
                ex.getMessage(),
                ex.getErrors().stream()
                        .map(e -> new ErrorResponse.ValidationError(e.path(), e.code(), e.message()))
                        .toList(),
                null);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.ValidationError(
                        "$." + error.getField(), "VALIDATION_ERROR", error.getDefaultMessage()))
                .toList();

        ErrorResponse response = new ErrorResponse(
                getCorrelationId(), ErrorCode.SCHEMA_INVALID.name(), "Validation failed", errors, null);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid JSON payload: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                getCorrelationId(), ErrorCode.SCHEMA_INVALID.name(), "Invalid JSON payload", null, null);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = getHttpStatus(ex.getErrorCode());

        ErrorResponse response = new ErrorResponse(
                getCorrelationId(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                null,
                ex.getViolations().stream()
                        .map(v -> new ErrorResponse.BusinessViolation(v.rule(), v.message()))
                        .toList());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse response =
                new ErrorResponse(getCorrelationId(), ex.getErrorCode().name(), ex.getMessage(), null, null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse response =
                new ErrorResponse(getCorrelationId(), ex.getErrorCode().name(), ex.getMessage(), null, null);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AsrRateLimitedException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrRateLimited(AsrRateLimitedException ex) {
        log.warn("ASR rate limited: {}", ex.getMessage());
        Integer retryAfterSeconds = ex.getRetryAfterSeconds();
        Map<String, Object> details = retryAfterSeconds != null ? Map.of("retryAfterSeconds", retryAfterSeconds) : null;
        return buildAsrResponse(ex, HttpStatus.TOO_MANY_REQUESTS, details, retryAfterSeconds);
    }

    @ExceptionHandler(AsrMissingFileException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrMissingFile(AsrMissingFileException ex) {
        log.warn("ASR missing file: {}", ex.getMessage());
        return buildAsrResponse(ex, HttpStatus.BAD_REQUEST, null, null);
    }

    @ExceptionHandler(AsrIdempotencyConflictException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrIdempotencyConflict(AsrIdempotencyConflictException ex) {
        log.warn("ASR idempotency conflict: {}", ex.getMessage());
        return buildAsrResponse(ex, HttpStatus.CONFLICT, null, null);
    }

    @ExceptionHandler(AsrUnavailableException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrUnavailable(AsrUnavailableException ex) {
        log.warn("ASR unavailable: {}", ex.getMessage());
        Integer retryAfterSeconds = ex.getRetryAfterSeconds();
        Map<String, Object> details = retryAfterSeconds != null ? Map.of("retryAfterSeconds", retryAfterSeconds) : null;
        return buildAsrResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, details, retryAfterSeconds);
    }

    @ExceptionHandler(AsrTimeoutException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrTimeout(AsrTimeoutException ex) {
        log.warn("ASR timeout: {}", ex.getMessage());
        return buildAsrResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, null, null);
    }

    @ExceptionHandler(AsrUnauthorizedException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrUnauthorized(AsrUnauthorizedException ex) {
        log.error("ASR unauthorized: {}", ex.getMessage());
        return buildAsrResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
    }

    @ExceptionHandler(AsrException.class)
    public ResponseEntity<AsrProxyErrorResponse> handleAsrException(AsrException ex) {
        log.warn("ASR error: {}", ex.getMessage());
        HttpStatus status = resolveStatus(ex);
        return buildAsrResponse(ex, status, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse response = new ErrorResponse(
                getCorrelationId(), ErrorCode.INTERNAL_ERROR.name(), "An unexpected error occurred", null, null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<AsrProxyErrorResponse> buildAsrResponse(
            AsrException ex, HttpStatus status, Map<String, Object> details, Integer retryAfterSeconds) {
        String correlationId = getCorrelationIdValue();
        String code = mapAsrCode(ex);
        AsrProxyErrorResponse response = new AsrProxyErrorResponse(code, ex.getMessage(), correlationId, details);

        HttpHeaders headers = new HttpHeaders();
        if (correlationId != null) {
            headers.set("X-Correlation-ID", correlationId);
        }
        if (retryAfterSeconds != null) {
            headers.set("Retry-After", String.valueOf(retryAfterSeconds));
        }

        return ResponseEntity.status(status).headers(headers).body(response);
    }

    private HttpStatus resolveStatus(AsrException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        if (statusCode != null) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String mapAsrCode(AsrException ex) {
        String code = ex.getCode();
        if (code == null || code.isBlank()) {
            return ErrorCode.INTERNAL_ERROR.name();
        }
        if (code.startsWith("ASR_")
                || ErrorCode.INTERNAL_ERROR.name().equals(code)
                || ErrorCode.IDEMPOTENCY_CONFLICT.name().equals(code)) {
            return code;
        }

        return switch (code) {
            case "INVALID_FORMAT", "UNSUPPORTED_LANGUAGE", "INVALID_PARAMETER", "CORRUPTED_FILE" -> ErrorCode
                    .ASR_INVALID_FORMAT
                    .name();
            case "MISSING_FILE" -> ErrorCode.ASR_MISSING_FILE.name();
            case "AUDIO_TOO_LONG" -> ErrorCode.ASR_AUDIO_TOO_LONG.name();
            case "FILE_TOO_LARGE" -> ErrorCode.ASR_FILE_TOO_LARGE.name();
            case "RATE_LIMIT_EXCEEDED" -> ErrorCode.ASR_RATE_LIMITED.name();
            case "SERVICE_UNAVAILABLE" -> ErrorCode.ASR_UNAVAILABLE.name();
            case "NOT_FOUND" -> ErrorCode.ASR_NOT_FOUND.name();
            case "INFERENCE_TIMEOUT", "TIMEOUT" -> ErrorCode.ASR_UNAVAILABLE.name();
            case "INTERNAL_ERROR", "INFERENCE_ERROR" -> ErrorCode.INTERNAL_ERROR.name();
            default -> ErrorCode.INTERNAL_ERROR.name();
        };
    }

    private UUID getCorrelationId() {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            try {
                return UUID.fromString(correlationId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private String getCorrelationIdValue() {
        UUID correlationId = getCorrelationId();
        return correlationId != null ? correlationId.toString() : null;
    }

    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case AUTH_INVALID_CREDENTIALS, AUTH_REFRESH_REQUIRED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case HOUSEHOLD_NOT_FOUND,
                    TASK_NOT_FOUND,
                    USER_NOT_FOUND,
                    ZONE_NOT_FOUND,
                    SHOPPING_RUN_NOT_FOUND,
                    NOTIFICATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVITE_EXPIRED, INVITE_REDEEMED, INVITE_REVOKED -> HttpStatus.GONE;
            case AUTH_EMAIL_EXISTS, IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case AUTH_PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public record ErrorResponse(
            UUID correlationId,
            String errorCode,
            String message,
            List<ValidationError> validationErrors,
            List<BusinessViolation> violations) {

        public record ValidationError(String path, String code, String message) {}

        public record BusinessViolation(String rule, String message) {}
    }
}
