package com.hometusk.integration.aiplatform;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hometusk.integration.IntegrationTestBase;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for AI Platform integration tests using WireMock.
 *
 * <p>Provides WireMock server setup and common stubbing utilities.
 */
public abstract class AiPlatformIntegrationTestBase extends IntegrationTestBase {

    protected static WireMockServer wireMockServer;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @DynamicPropertySource
    static void configureAiPlatform(DynamicPropertyRegistry registry) {
        registry.add("decision.provider", () -> "aiplatform");
        registry.add("aiplatform.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("aiplatform.timeout-ms", () -> "300");
        registry.add("decision.fallback.enabled", () -> "true");
        registry.add("guardrails.enabled", () -> "true");
        registry.add("guardrails.max-open-tasks-per-assignee", () -> "10");
    }

    @BeforeEach
    void setUpWireMock() {
        wireMockServer.resetAll();
        // Default health check stub
        stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void tearDownWireMock() {
        wireMockServer.resetAll();
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.circuitBreaker("aiPlatform").reset();
        }
    }

    /**
     * Stubs AI Platform to return a start_job decision.
     */
    protected void stubStartJobDecision(String assigneeId, String title) {
        String responseBody =
                """
                {
                    "decisionId": "550e8400-e29b-41d4-a716-446655440000",
                    "type": "start_job",
                    "confidence": 0.95,
                    "actions": [
                        {
                            "actionType": "create_task",
                            "parameters": {
                                "title": "%s",
                                "assigneeId": "%s"
                            }
                        }
                    ]
                }
                """
                        .formatted(title, assigneeId);

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs AI Platform to return a clarify decision.
     */
    protected void stubClarifyDecision(String question, String... requiredFields) {
        StringBuilder fieldsJson = new StringBuilder("[");
        for (int i = 0; i < requiredFields.length; i++) {
            if (i > 0) fieldsJson.append(", ");
            fieldsJson.append("\"").append(requiredFields[i]).append("\"");
        }
        fieldsJson.append("]");

        String responseBody =
                """
                {
                    "decisionId": "550e8400-e29b-41d4-a716-446655440001",
                    "type": "clarify",
                    "confidence": 0.4,
                    "question": "%s",
                    "requiredFields": %s,
                    "suggestions": {}
                }
                """
                        .formatted(question, fieldsJson);

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs AI Platform to return an invalid response (schema validation failure).
     */
    protected void stubInvalidResponse() {
        String responseBody =
                """
                {
                    "decisionId": "550e8400-e29b-41d4-a716-446655440002",
                    "type": "start_job",
                    "confidence": 0.9
                }
                """
                        .stripIndent();
        // Missing required "actions" field for start_job type

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs AI Platform to timeout.
     */
    protected void stubTimeout() {
        stubFor(post(urlEqualTo("/decision")).willReturn(aResponse().withFixedDelay(10000)));
    }

    /**
     * Stubs AI Platform health check to fail.
     */
    protected void stubHealthCheckFailed() {
        stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(503)));
    }

    // --- Upstream contract stubs ---

    /**
     * Stubs upstream propose_create_task (mapped to start_job in HomeTusk).
     */
    protected void stubProposeCreateTaskDecision(String assigneeId, String title) {
        String responseBody =
                """
                {
                    "decisionId": "e390f1ee-7c54-4b01-90e6-d701748f0852",
                    "type": "propose_create_task",
                    "confidence": 0.75,
                    "actions": [
                        {
                            "actionType": "create_task",
                            "parameters": {
                                "title": "%s",
                                "assigneeId": "%s"
                            }
                        }
                    ]
                }
                """
                        .formatted(title, assigneeId);

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs upstream propose_add_shopping_item (now supported, executes immediately).
     */
    protected void stubProposeAddShoppingItemDecision() {
        String responseBody =
                """
                {
                    "decisionId": "f490f1ee-8c54-4b01-90e6-d701748f0853",
                    "type": "propose_add_shopping_item",
                    "confidence": 0.85,
                    "actions": [
                        {
                            "actionType": "add_shopping_item",
                            "parameters": {
                                "name": "Молоко",
                                "quantity": 2
                            }
                        }
                    ]
                }
                """;

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs unknown decision type (safe degradation → Reject).
     */
    protected void stubUnknownDecisionType() {
        String responseBody =
                """
                {
                    "decisionId": "a590f1ee-9c54-4b01-90e6-d701748f0854",
                    "type": "unknown_future_type",
                    "confidence": 0.9
                }
                """;

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    /**
     * Stubs upstream start_job with deadline and zone for guardrails test.
     */
    protected void stubStartJobWithFullParams(String assigneeId, String title, String zoneId) {
        String futureDeadline = java.time.Instant.now()
                .plus(7, java.time.temporal.ChronoUnit.DAYS)
                .toString();
        String responseBody =
                """
                {
                    "decisionId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                    "type": "start_job",
                    "confidence": 0.95,
                    "actions": [
                        {
                            "actionType": "create_task",
                            "parameters": {
                                "title": "%s",
                                "assigneeId": "%s",
                                "zoneId": "%s",
                                "deadline": "%s"
                            }
                        }
                    ]
                }
                """
                        .formatted(title, assigneeId, zoneId, futureDeadline);

        stubFor(post(urlEqualTo("/decision"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }
}
