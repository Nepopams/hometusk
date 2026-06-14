package com.hometusk.voice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.voice.config.VoiceProperties;
import com.hometusk.voice.dto.VoiceAsrTranscription;
import com.hometusk.voice.dto.VoiceAsrUpstreamErrorResponse;
import com.hometusk.voice.dto.VoiceAsrUpstreamResponse;
import com.hometusk.voice.exception.VoiceAsrException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
public class VoiceAsrClientImpl implements VoiceAsrClient {

    private static final Logger log = LoggerFactory.getLogger(VoiceAsrClientImpl.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final VoiceProperties.AsrProperties properties;

    public VoiceAsrClientImpl(VoiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties.asr();
        this.objectMapper = objectMapper;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(this.properties.connectTimeoutMs());
        requestFactory.setReadTimeout(this.properties.readTimeoutMs());

        var builder = RestClient.builder().baseUrl(this.properties.baseUrl()).requestFactory(requestFactory);
        if (this.properties.apiKey() != null && !this.properties.apiKey().isBlank()) {
            builder.defaultHeader("X-API-Key", this.properties.apiKey());
        }
        this.restClient = builder.build();

        log.info(
                "Voice ASR client initialized: baseUrl={}, transcribePath={}, connectTimeoutMs={}, readTimeoutMs={}",
                this.properties.baseUrl(),
                this.properties.transcribePath(),
                this.properties.connectTimeoutMs(),
                this.properties.readTimeoutMs());
    }

    @Override
    public VoiceAsrTranscription transcribe(
            byte[] fileBytes, String fileName, String contentType, String correlationId) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", buildFilePart(fileBytes, fileName, contentType));

        try {
            var spec = restClient
                    .post()
                    .uri(properties.transcribePath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("X-Request-Id", correlationId)
                    .header("X-Correlation-ID", correlationId);

            VoiceAsrUpstreamResponse response = spec.body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, upstreamResponse) -> {
                        VoiceAsrUpstreamErrorResponse errorResponse = readErrorResponse(upstreamResponse);
                        throw mapException(upstreamResponse.getStatusCode(), errorResponse);
                    })
                    .body(VoiceAsrUpstreamResponse.class);

            if (response == null) {
                throw VoiceAsrException.badUpstreamResponse("ASR provider returned an empty response");
            }

            String transcript = response.transcript() != null ? response.transcript() : response.text();
            if (transcript == null) {
                throw VoiceAsrException.badUpstreamResponse("ASR provider response is missing transcript");
            }

            String traceId = firstNonBlank(response.traceId(), response.requestId(), correlationId);
            return new VoiceAsrTranscription(transcript, traceId, response.latencyMs());
        } catch (VoiceAsrException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw VoiceAsrException.timeout("ASR provider request timed out", ex);
            }
            throw VoiceAsrException.upstreamUnavailable("ASR provider is unavailable", ex);
        } catch (RestClientException ex) {
            throw VoiceAsrException.upstreamUnavailable("ASR provider request failed", ex);
        }
    }

    private HttpEntity<ByteArrayResource> buildFilePart(byte[] fileBytes, String fileName, String contentType) {
        String safeFileName = fileName == null || fileName.isBlank() ? "audio" : fileName;
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            mediaType = MediaType.parseMediaType(contentType);
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

    private VoiceAsrUpstreamErrorResponse readErrorResponse(ClientHttpResponse response) {
        try {
            byte[] body = StreamUtils.copyToByteArray(response.getBody());
            if (body.length == 0) {
                return null;
            }
            return objectMapper.readValue(body, VoiceAsrUpstreamErrorResponse.class);
        } catch (IOException ex) {
            return null;
        }
    }

    private VoiceAsrException mapException(HttpStatusCode statusCode, VoiceAsrUpstreamErrorResponse errorResponse) {
        String providerCode = errorResponse != null ? normalizeCode(errorResponse.code()) : null;
        String message = errorResponse != null
                        && errorResponse.message() != null
                        && !errorResponse.message().isBlank()
                ? errorResponse.message()
                : "ASR provider error";

        if ("missing_audio_file".equals(providerCode)) {
            return new VoiceAsrException(
                    "missing_audio_file", message, org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if ("file_too_large".equals(providerCode)) {
            return VoiceAsrException.fileTooLarge(message);
        }
        if ("unsupported_media".equals(providerCode)) {
            return VoiceAsrException.unsupportedMedia(message);
        }
        if ("asr_config_error".equals(providerCode)) {
            return VoiceAsrException.configError(message);
        }
        if ("auth_error".equals(providerCode)) {
            return VoiceAsrException.authError(message);
        }
        if ("bad_upstream_response".equals(providerCode)) {
            return VoiceAsrException.badUpstreamResponse(message);
        }
        if ("timeout".equals(providerCode)) {
            return VoiceAsrException.timeout(message, null);
        }

        int status = statusCode.value();
        if (status == 400) {
            return VoiceAsrException.invalidMultipart(message);
        }
        if (status == 401 || status == 403) {
            return VoiceAsrException.authError(message);
        }
        if (status == 413) {
            return VoiceAsrException.fileTooLarge(message);
        }
        if (status == 415) {
            return VoiceAsrException.unsupportedMedia(message);
        }
        if (status == 504) {
            return VoiceAsrException.timeout(message, null);
        }
        return VoiceAsrException.upstreamUnavailable(message, null);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (code.trim().toLowerCase()) {
            case "invalid_multipart", "invalid_parameter", "invalid_format", "corrupted_file" -> "invalid_multipart";
            case "missing_audio_file", "missing_file" -> "missing_audio_file";
            case "file_too_large", "audio_too_long" -> "file_too_large";
            case "unsupported_media", "unsupported_media_type", "unsupported_language" -> "unsupported_media";
            case "asr_config_error" -> "asr_config_error";
            case "auth_error", "unauthorized", "forbidden" -> "auth_error";
            case "bad_upstream_response" -> "bad_upstream_response";
            case "timeout", "inference_timeout" -> "timeout";
            default -> code.trim().toLowerCase();
        };
    }

    private boolean isTimeout(ResourceAccessException ex) {
        return ex.getCause() instanceof SocketTimeoutException;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "voice-asr";
    }
}
