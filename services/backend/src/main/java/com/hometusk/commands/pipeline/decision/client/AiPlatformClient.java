package com.hometusk.commands.pipeline.decision.client;

import com.hometusk.shared.logging.MdcKeys;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP client for external AI Platform.
 * Only instantiated when decision.provider=aiplatform.
 *
 * <p>Endpoint configuration:
 * <ul>
 *   <li>aiplatform.decision-path=/decision (default, HomeTusk legacy)</li>
 *   <li>aiplatform.decision-path=/decide (upstream canonical)</li>
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

    public AiPlatformClient(
            @Value("${aiplatform.base-url}") String baseUrl,
            @Value("${aiplatform.timeout-ms:5000}") int timeoutMs,
            @Value("${aiplatform.api-key:}") String apiKey,
            @Value("${aiplatform.decision-path:/decision}") String decisionPath,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.timeoutMs = timeoutMs;
        this.decisionPath = decisionPath;

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
        log.info("AI Platform client initialized: baseUrl={}, decisionPath={}, timeoutMs={}",
                baseUrl, decisionPath, timeoutMs);
    }

    /**
     * Requests a decision from AI Platform.
     *
     * @param request Decision request
     * @return Decision response
     * @throws AiPlatformException if request fails
     */
    public AiDecisionResponse requestDecision(AiDecisionRequest request) {
        log.debug("Requesting decision from AI Platform: commandId={}, path={}",
                request.commandId(), decisionPath);

        try {
            Supplier<AiDecisionResponse> supplier = () -> executeRequest(request);
            Supplier<AiDecisionResponse> decorated =
                    CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier));
            AiDecisionResponse response = decorated.get();

            log.debug(
                    "Received decision from AI Platform: decisionId={}, type={}",
                    response.decisionId(),
                    response.type());

            return response;

        } catch (RestClientException e) {
            log.error("AI Platform request failed", e);
            throw new AiPlatformException("AI Platform request failed: " + e.getMessage(), e);
        }
    }

    private AiDecisionResponse executeRequest(AiDecisionRequest request) {
        var spec = restClient.post().uri(decisionPath);
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            spec = spec.header("X-Correlation-ID", correlationId);
        }

        return spec.body(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (req, resp) -> {
                            throw new AiPlatformException(
                                    "AI Platform returned error: " + resp.getStatusCode(), resp.getStatusCode());
                        })
                .body(AiDecisionResponse.class);
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
}
