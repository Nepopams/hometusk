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
 */
public abstract class AiPlatformIntegrationTestBase extends IntegrationTestBase {

    protected static final String DECIDE_PATH = "/v1/decide";
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
        registry.add("aiplatform.decision-path", () -> DECIDE_PATH);
        registry.add("aiplatform.timeout-ms", () -> "300");
        registry.add("decision.fallback.enabled", () -> "true");
        registry.add("guardrails.enabled", () -> "true");
        registry.add("guardrails.max-open-tasks-per-assignee", () -> "10");
    }

    @BeforeEach
    void setUpWireMock() {
        wireMockServer.resetAll();
        stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void tearDownWireMock() {
        wireMockServer.resetAll();
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.circuitBreaker("aiPlatform").reset();
        }
    }

    protected void stubStartJobDecision(String assigneeId, String title) {
        stubStartJobDecisionWithTitleAssertion(assigneeId, title, null);
    }

    protected void stubStartJobDecisionWithTitleAssertion(String assigneeId, String title, String expectedText) {
        String responseBody =
                """
                {
                    "decision_id": "550e8400-e29b-41d4-a716-446655440000",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.95,
                    "payload": {
                        "job_id": "job-test",
                        "job_type": "create_task",
                        "proposed_actions": [
                            {
                                "action": "propose_create_task",
                                "payload": {
                                    "task": {
                                        "title": "%s",
                                        "assignee_id": "%s"
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Task creation accepted.",
                    "trace_id": "trace-test-start-job",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(title, assigneeId);

        var mapping = post(urlEqualTo(DECIDE_PATH))
                .withRequestBody(matchingJsonPath("$.command_id"))
                .withRequestBody(matchingJsonPath("$.user_id"))
                .withRequestBody(matchingJsonPath("$.text"))
                .withRequestBody(matchingJsonPath("$.capabilities[?(@ == 'start_job')]"))
                .withRequestBody(matchingJsonPath("$.context.household.members[0].user_id"));
        if (expectedText != null) {
            mapping = mapping.withRequestBody(matchingJsonPath("$.text", equalTo(expectedText)));
        }

        stubFor(mapping.willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)));
    }

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
                    "decision_id": "550e8400-e29b-41d4-a716-446655440001",
                    "command_id": "cmd-test",
                    "status": "clarify",
                    "action": "clarify",
                    "confidence": 0.4,
                    "payload": {
                        "question": "%s",
                        "missing_fields": %s
                    },
                    "explanation": "More input is required.",
                    "trace_id": "trace-test-clarify",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(question, fieldsJson);

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubInvalidResponse() {
        String responseBody =
                """
                {
                    "decision_id": "550e8400-e29b-41d4-a716-446655440002",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.9,
                    "explanation": "Missing required payload.",
                    "trace_id": "trace-test-invalid",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .stripIndent();

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubMalformedJsonResponse() {
        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"decision_id\":\"malformed\",\"action\":\"reject\"")));
    }

    protected void stubTimeout() {
        stubFor(post(urlEqualTo(DECIDE_PATH)).willReturn(aResponse().withFixedDelay(10000)));
    }

    protected void stubDecisionEndpointUnavailable() {
        stubFor(post(urlEqualTo(DECIDE_PATH)).willReturn(aResponse().withStatus(503)));
    }

    protected void stubHealthCheckFailed() {
        stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(503)));
    }

    protected void stubProposeCreateTaskDecision(String assigneeId, String title) {
        String responseBody =
                """
                {
                    "decision_id": "e390f1ee-7c54-4b01-90e6-d701748f0852",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "propose_create_task",
                    "confidence": 0.75,
                    "payload": {
                        "task": {
                            "title": "%s",
                            "assignee_id": "%s"
                        }
                    },
                    "explanation": "Task proposal accepted.",
                    "trace_id": "trace-test-propose-task",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(title, assigneeId);

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubProposeAddShoppingItemDecision() {
        String responseBody =
                """
                {
                    "decision_id": "f490f1ee-8c54-4b01-90e6-d701748f0853",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.85,
                    "payload": {
                        "job_id": "job-shopping",
                        "job_type": "add_shopping_item",
                        "proposed_actions": [
                            {
                                "action": "propose_add_shopping_item",
                                "payload": {
                                    "item": {
                                        "name": "Milk",
                                        "quantity": "2"
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Shopping item accepted.",
                    "trace_id": "trace-test-shopping",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """;

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubUnknownDecisionType() {
        String responseBody =
                """
                {
                    "decision_id": "a590f1ee-9c54-4b01-90e6-d701748f0854",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "unknown_future_type",
                    "confidence": 0.9,
                    "payload": {},
                    "explanation": "Future action.",
                    "trace_id": "trace-test-unknown",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """;

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubRejectDecision() {
        String responseBody =
                """
                {
                    "decision_id": "b690f1ee-9c54-4b01-90e6-d701748f0855",
                    "command_id": "cmd-test",
                    "status": "error",
                    "action": "reject",
                    "decision_outcome": "reject",
                    "confidence": 0.2,
                    "payload": {
                        "code": "unsupported_or_unsafe_command",
                        "reason": "Outside the supported household command corridor.",
                        "ui_message": "I cannot safely handle this request.",
                        "details": {
                            "category": "unsupported"
                        }
                    },
                    "explanation": "Unsupported command is rejected without proposed mutation.",
                    "trace_id": "trace-test-reject",
                    "schema_version": "2.1.0",
                    "decision_version": "mvp1-graph-0.1",
                    "created_at": "2026-06-15T00:00:00Z"
                }
                """;

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubConfirmDecision() {
        String responseBody =
                """
                {
                    "decision_id": "c790f1ee-9c54-4b01-90e6-d701748f0856",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "confirm",
                    "decision_outcome": "confirm",
                    "confidence": 0.73,
                    "payload": {
                        "confirmation_id": "conf-test",
                        "summary": "Create a task for another household member.",
                        "reasons": [
                            "Non-requester assignment requires HomeTusk confirmation."
                        ],
                        "proposed_actions": [
                            {
                                "action": "propose_create_task",
                                "payload": {
                                    "task": {
                                        "title": "Clean kitchen"
                                    }
                                }
                            }
                        ],
                        "expires_at": "2026-06-15T01:00:00Z",
                        "ui_message": "Please confirm before I do this."
                    },
                    "explanation": "Confirmation required.",
                    "trace_id": "trace-test-confirm",
                    "schema_version": "2.1.0",
                    "decision_version": "mvp1-graph-0.1",
                    "created_at": "2026-06-15T00:00:00Z"
                }
                """;

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .withRequestBody(matchingJsonPath("$.capabilities[?(@ == 'reject')]"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    protected void stubStartJobWithFullParams(String assigneeId, String title, String zoneId) {
        String futureDeadline = java.time.Instant.now()
                .plus(7, java.time.temporal.ChronoUnit.DAYS)
                .toString();
        String responseBody =
                """
                {
                    "decision_id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                    "command_id": "cmd-test",
                    "status": "ok",
                    "action": "start_job",
                    "confidence": 0.95,
                    "payload": {
                        "job_id": "job-full",
                        "job_type": "create_task",
                        "proposed_actions": [
                            {
                                "action": "propose_create_task",
                                "payload": {
                                    "task": {
                                        "title": "%s",
                                        "assignee_id": "%s",
                                        "zone_id": "%s",
                                        "due": "%s"
                                    }
                                }
                            }
                        ]
                    },
                    "explanation": "Task creation accepted.",
                    "trace_id": "trace-test-full",
                    "schema_version": "1.0.0",
                    "decision_version": "test-1",
                    "created_at": "2026-06-14T00:00:00Z"
                }
                """
                        .formatted(title, assigneeId, zoneId, futureDeadline);

        stubFor(post(urlEqualTo(DECIDE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }
}
