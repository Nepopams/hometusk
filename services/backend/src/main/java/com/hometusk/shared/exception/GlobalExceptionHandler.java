package com.hometusk.shared.exception;

import com.hometusk.shared.logging.MdcKeys;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse response = new ErrorResponse(
                getCorrelationId(), ErrorCode.INTERNAL_ERROR.name(), "An unexpected error occurred", null, null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case HOUSEHOLD_NOT_FOUND,
                    TASK_NOT_FOUND,
                    USER_NOT_FOUND,
                    ZONE_NOT_FOUND,
                    NOTIFICATION_NOT_FOUND ->
                HttpStatus.NOT_FOUND;
            case INVITE_EXPIRED, INVITE_REDEEMED, INVITE_REVOKED -> HttpStatus.GONE;
            case IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
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
