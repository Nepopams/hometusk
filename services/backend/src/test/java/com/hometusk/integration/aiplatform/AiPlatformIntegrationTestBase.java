package com.hometusk.integration.aiplatform;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hometusk.integration.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for AI Platform integration tests using WireMock.
 *
 * <p>Provides WireMock server setup and common stubbing utilities.
 */
public abstract class AiPlatformIntegrationTestBase extends IntegrationTestBase {

    protected static WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @DynamicPropertySource
    static void configureAiPlatform(DynamicPropertyRegistry registry) {
        registry.add("decision.provider", () -> "aiplatform");
        registry.add("aiplatform.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("aiplatform.timeout-ms", () -> "5000");
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
        // Also mark health as unavailable
        stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(503)));
    }

    /**
     * Stubs AI Platform health check to fail.
     */
    protected void stubHealthCheckFailed() {
        stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(503)));
    }
}
