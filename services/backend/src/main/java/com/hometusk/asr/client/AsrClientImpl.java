package com.hometusk.asr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.asr.dto.AsrErrorResponse;
import com.hometusk.asr.dto.AsrJobCreated;
import com.hometusk.asr.dto.AsrJobResult;
import com.hometusk.asr.exception.AsrAudioTooLongException;
import com.hometusk.asr.exception.AsrException;
import com.hometusk.asr.exception.AsrFileTooLargeException;
import com.hometusk.asr.exception.AsrInvalidFormatException;
import com.hometusk.asr.exception.AsrNotFoundException;
import com.hometusk.asr.exception.AsrRateLimitedException;
import com.hometusk.asr.exception.AsrTimeoutException;
import com.hometusk.asr.exception.AsrUnauthorizedException;
import com.hometusk.asr.exception.AsrUnavailableException;
import com.hometusk.asr.metrics.AsrMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AsrClientImpl implements AsrClient {

    private static final Logger log = LoggerFactory.getLogger(AsrClientImpl.class);

    private final RestClient restClient;
    private final Retry retry;
    private final ObjectMapper objectMapper;
    private final AsrMetrics metrics;

    public AsrClientImpl(
            AsrProperties properties,
            @Qualifier("asrRetryRegistry") RetryRegistry retryRegistry,
            ObjectMapper objectMapper,
            AsrMetrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());

        var builder = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory);

        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder.defaultHeader("X-API-Key", properties.apiKey());
        }

        this.restClient = builder.build();
        this.retry = retryRegistry.retry("asr");

        log.info(
                "ASR client initialized: baseUrl={}, connectTimeoutMs={}, readTimeoutMs={}",
                properties.baseUrl(),
                properties.connectTimeoutMs(),
                properties.readTimeoutMs());
    }

    @Override
    public AsrJobCreated createTranscription(
            byte[] fileBytes,
            String fileName,
            String contentType,
            String languageHint,
            String correlationId,
            String idempotencyKey) {
        return recordLatency(
                "create",
                () -> executeWithRetry(() -> executeCreateTranscription(
                        fileBytes, fileName, contentType, languageHint, correlationId, idempotencyKey)));
    }

    @Override
    public AsrJobResult getTranscription(String transcriptionId, String correlationId) {
        return recordLatency(
                "poll", () -> executeWithRetry(() -> executeGetTranscription(transcriptionId, correlationId)));
    }

    private AsrJobCreated executeCreateTranscription(
            byte[] fileBytes,
            String fileName,
            String contentType,
            String languageHint,
            String correlationId,
            String idempotencyKey) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", buildFilePart(fileBytes, fileName, contentType));
        if (languageHint != null && !languageHint.isBlank()) {
            body.add("languageHint", languageHint);
        }

        var spec = restClient.post().uri("/transcriptions").contentType(MediaType.MULTIPART_FORM_DATA);

        if (correlationId != null && !correlationId.isBlank()) {
            spec = spec.header("X-Request-Id", correlationId);
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }

        return spec.body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    AsrErrorResponse errorResponse = readErrorResponse(resp);
                    throw mapException(resp.getStatusCode(), resp.getHeaders(), errorResponse);
                })
                .body(AsrJobCreated.class);
    }

    private AsrJobResult executeGetTranscription(String transcriptionId, String correlationId) {
        var spec = restClient.get().uri("/transcriptions/{id}", transcriptionId);

        if (correlationId != null && !correlationId.isBlank()) {
            spec = spec.header("X-Request-Id", correlationId);
        }

        return spec.retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    AsrErrorResponse errorResponse = readErrorResponse(resp);
                    throw mapException(resp.getStatusCode(), resp.getHeaders(), errorResponse);
                })
                .body(AsrJobResult.class);
    }

    private <T> T executeWithRetry(Supplier<T> supplier) {
        Supplier<T> decorated = Retry.decorateSupplier(retry, supplier);
        try {
            return decorated.get();
        } catch (AsrException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new AsrTimeoutException("TIMEOUT", "ASR request timed out", ex);
            }
            throw new AsrException("INTERNAL_ERROR", "ASR request failed", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        } catch (RestClientException ex) {
            throw new AsrException("INTERNAL_ERROR", "ASR request failed", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }

    private HttpEntity<ByteArrayResource> buildFilePart(byte[] fileBytes, String fileName, String contentType) {
        String safeFileName = (fileName == null || fileName.isBlank()) ? "audio" : fileName;

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (IllegalArgumentException ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.formData()
                .name("file")
                .filename(safeFileName)
                .build());

        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return safeFileName;
            }
        };

        return new HttpEntity<>(resource, headers);
    }

    private AsrErrorResponse readErrorResponse(ClientHttpResponse response) {
        try {
            byte[] body = StreamUtils.copyToByteArray(response.getBody());
            if (body.length == 0) {
                return null;
            }
            return objectMapper.readValue(body, AsrErrorResponse.class);
        } catch (IOException ex) {
            return null;
        }
    }

    private AsrException mapException(HttpStatusCode statusCode, HttpHeaders headers, AsrErrorResponse errorResponse) {
        String code = errorResponse != null ? errorResponse.code() : null;
        String message = errorResponse != null
                        && errorResponse.message() != null
                        && !errorResponse.message().isBlank()
                ? errorResponse.message()
                : "ASR service error: HTTP " + statusCode.value();
        Integer retryAfter = parseRetryAfterSeconds(headers);

        if (code == null || code.isBlank()) {
            return fallbackException(statusCode, message, retryAfter);
        }

        return switch (code) {
            case "INVALID_FORMAT",
                    "UNSUPPORTED_LANGUAGE",
                    "INVALID_PARAMETER",
                    "MISSING_FILE",
                    "CORRUPTED_FILE" -> new AsrInvalidFormatException(code, message);
            case "AUDIO_TOO_LONG" -> new AsrAudioTooLongException(code, message);
            case "FILE_TOO_LARGE" -> new AsrFileTooLargeException(code, message);
            case "RATE_LIMIT_EXCEEDED" -> new AsrRateLimitedException(code, message, retryAfter);
            case "SERVICE_UNAVAILABLE" -> new AsrUnavailableException(code, message, retryAfter);
            case "NOT_FOUND" -> new AsrNotFoundException(code, message);
            case "UNAUTHORIZED", "FORBIDDEN" -> new AsrUnauthorizedException(code, message, statusCode);
            case "INFERENCE_TIMEOUT" -> new AsrTimeoutException(code, message, statusCode);
            case "INTERNAL_ERROR", "INFERENCE_ERROR" -> new AsrException(code, message, statusCode);
            default -> new AsrException(code, message, statusCode);
        };
    }

    private AsrException fallbackException(HttpStatusCode statusCode, String message, Integer retryAfter) {
        int statusValue = statusCode.value();
        if (statusValue == 429) {
            return new AsrRateLimitedException("RATE_LIMIT_EXCEEDED", message, retryAfter);
        }
        if (statusValue == 503) {
            return new AsrUnavailableException("SERVICE_UNAVAILABLE", message, retryAfter);
        }
        if (statusValue == 404) {
            return new AsrNotFoundException("NOT_FOUND", message);
        }
        if (statusValue == 401 || statusValue == 403) {
            return new AsrUnauthorizedException("UNAUTHORIZED", message, statusCode);
        }
        if (statusValue == 413) {
            return new AsrFileTooLargeException("FILE_TOO_LARGE", message);
        }
        if (statusValue == 400) {
            return new AsrInvalidFormatException("INVALID_FORMAT", message);
        }
        if (statusValue == 504) {
            return new AsrTimeoutException("TIMEOUT", message, statusCode);
        }
        return new AsrException("INTERNAL_ERROR", message, statusCode);
    }

    private Integer parseRetryAfterSeconds(HttpHeaders headers) {
        String value = headers.getFirst("Retry-After");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isTimeout(ResourceAccessException ex) {
        return ex.getCause() instanceof SocketTimeoutException;
    }

    private <T> T recordLatency(String phase, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        try {
            return supplier.get();
        } finally {
            metrics.recordLatency(phase, System.currentTimeMillis() - startTime);
        }
    }
}
