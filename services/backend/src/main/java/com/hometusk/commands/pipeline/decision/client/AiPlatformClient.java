package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hometusk.shared.logging.MdcKeys;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP client for external AI Platform.
 * Only instantiated when decision.provider=aiplatform.
 *
 * <p>Endpoint configuration:
 * <ul>
 *   <li>aiplatform.decision-path=/v1/decide (upstream canonical in UAT)</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "decision.provider", havingValue = "aiplatform")
public class AiPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(AiPlatformClient.class);

    private final RestClient restClient;
    private final int timeoutMs;
    private final String decisionPath;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;

    public AiPlatformClient(
            @Value("${aiplatform.base-url}") String baseUrl,
            @Value("${aiplatform.timeout-ms:5000}") int timeoutMs,
            @Value("${aiplatform.api-key:}") String apiKey,
            @Value("${aiplatform.decision-path:/v1/decide}") String decisionPath,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            ObjectMapper objectMapper) {
        this.timeoutMs = timeoutMs;
        this.decisionPath = decisionPath;
        this.objectMapper = objectMapper;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        var builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        this.restClient = builder.build();
        this.retry = retryRegistry.retry("aiPlatform");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiPlatform");
        log.info(
                "AI Platform client initialized: baseUrl={}, decisionPath={}, timeoutMs={}",
                baseUrl,
                decisionPath,
                timeoutMs);
    }

    /**
     * Requests a decision from AI Platform.
     *
     * @param request Decision request
     * @return Decision response with the exact raw response payload
     * @throws AiPlatformException if request fails
     */
    public AiDecisionClientResponse requestDecision(AiDecisionRequest request) {
        log.debug("Requesting decision from AI Platform: commandId={}, path={}", request.commandId(), decisionPath);

        try {
            Supplier<String> supplier = () -> executeRequest(request);
            Supplier<String> decorated =
                    CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier));
            String rawPayload = decorated.get();

            log.debug(
                    "Received raw decision from AI Platform: commandId={}, bytes={}",
                    request.commandId(),
                    rawPayload != null ? rawPayload.length() : 0);

            return new AiDecisionClientResponse(rawPayload);

        } catch (RestClientException e) {
            log.error("AI Platform request failed", e);
            throw new AiPlatformException("AI Platform request failed: " + e.getMessage(), e);
        }
    }

    private String executeRequest(AiDecisionRequest request) {
        var spec = restClient.post().uri(decisionPath);
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            spec = spec.header("X-Correlation-ID", correlationId);
        }

        return spec.body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    String responseBody = sanitizedErrorBody(resp);
                    throw new AiPlatformException(
                            "AI Platform returned error: " + resp.getStatusCode() + ", body=" + responseBody,
                            resp.getStatusCode());
                })
                .body(String.class);
    }

    private String sanitizedErrorBody(ClientHttpResponse response) {
        try {
            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            if (body == null || body.isBlank()) {
                return "<empty>";
            }
            return truncate(redactInputFields(body));
        } catch (IOException e) {
            return "<unreadable: " + e.getClass().getSimpleName() + ">";
        }
    }

    private String redactInputFields(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            redactInputFields(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return body;
        }
    }

    private void redactInputFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof ObjectNode objectNode) {
            objectNode.remove("input");
            objectNode.fields().forEachRemaining(entry -> redactInputFields(entry.getValue()));
            return;
        }
        if (node.isArray()) {
            node.forEach(this::redactInputFields);
        }
    }

    private String truncate(String value) {
        int maxLength = 2000;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...<truncated>";
    }

    /**
     * Checks if AI Platform is healthy.
     *
     * @return true if healthy, false otherwise
     */
    public boolean healthCheck() {
        try {
            var response = restClient.get().uri("/health").retrieve().toBodilessEntity();

            return response.getStatusCode() == HttpStatus.OK;
        } catch (RestClientException e) {
            log.warn("AI Platform health check failed", e);
            return false;
        }
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public record AiDecisionClientResponse(String rawPayload) {}
}
