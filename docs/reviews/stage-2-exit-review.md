# Stage 2 Exit Review

**Date:** 2026-01-12
**Reviewer:** Claude (AI Assistant)
**Status:** PASS WITH NOTES
**Stage:** 2 (AI Platform Integration) → 3 (Decision Application + Guardrails)

---

## Summary

Stage 2 Exit Review validates that HomeTusk can safely proceed to Stage 3 (decision application with guardrails) without hidden architectural debt.

**Overall Verdict: PASS WITH NOTES**

The architecture is sound and follows the documented patterns. DecisionProvider abstraction is correctly implemented, fallback behavior is explicit, and contracts are aligned. However, **critical test coverage gaps** must be addressed before Stage 3 work begins.

### Critical Actions Required (Before Stage 3)

| # | Issue | Risk | Fix |
|---|-------|------|-----|
| 1 | No integration tests for AI Platform | Stage 3 features untestable | Add WireMock tests |
| 2 | No runtime validation of DecisionDTO | Invalid AI responses may crash | Add JSON Schema validation |
| 3 | householdContext is always empty | Guardrails won't have context | Populate in DecisionContext builder |

### Nice-to-Have (Can defer)

- CI integration for `validate-aiplatform-contracts.sh`
- Clarify `/decide` vs `/decision` with AI Platform team

---

## 1. Architecture Boundary Review

### Status: PASS

### Findings

#### DecisionProvider is the only decision entry point

**Location:** `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/`

The `DecisionProvider` interface is the sole abstraction for decision-making:

```java
// DecisionProvider.java:12
public interface DecisionProvider {
    DecisionResult decide(DecisionContext context);
    DecisionSource getSource();
    boolean isAvailable();
}
```

**Implementations:**
- `ManualDecisionProvider` - Rule-based fallback
- `AiPlatformDecisionProvider` - External AI Platform calls

**Selection:** `DecisionProviderSelector.java` routes based on config.

#### No domain services call AI Platform directly

Verified via grep search:
- `tasks/` - No RestClient/HttpClient imports
- `households/` - No RestClient/HttpClient imports
- `users/` - No RestClient/HttpClient imports

AI Platform client is **isolated** in `commands/pipeline/decision/client/`.

**Files containing AI Platform references:**
```
commands/pipeline/decision/client/AiPlatformClient.java
commands/pipeline/decision/client/AiPlatformException.java
commands/pipeline/decision/DecisionProviderSelector.java
commands/pipeline/decision/DecisionProvider.java
commands/pipeline/decision/AiPlatformDecisionProvider.java
```

#### DecisionResult handling is consistent

`CommandService.java:197-204` handles all three types via sealed interface pattern matching:

```java
return switch (result) {
    case DecisionResult.StartJob startJob -> handleStartJob(...);
    case DecisionResult.Clarify clarify -> handleClarify(...);
    case DecisionResult.Reject reject -> handleReject(...);
};
```

**Behavior:**
- `StartJob` → Execute actions, mark EXECUTED
- `Clarify` → Mark NEEDS_INPUT, return question to user, **no domain changes**
- `Reject` → Mark REJECTED, throw BusinessException

#### Fallback behavior is explicit

**Configuration defaults:**
- `decision.provider: manual` (application.yml:74)
- `decision.fallback.enabled: true` (application.yml:76)

**Logging (DecisionProviderSelector.java):**
```java
log.warn("AI Platform provider unavailable, falling back to manual");  // line 64
log.error("AI Platform decision failed, falling back to manual", e);   // line 70
```

**Source tracking:** Fallback returns `DecisionSource.FALLBACK` (line 85), visible in API response as `status: executed_degraded`.

### Issues Found

| Issue | Location | Risk | Recommendation |
|-------|----------|------|----------------|
| `householdContext` always empty | `CommandService.java:186` | Guardrails cannot evaluate member/zone context | Populate from MembershipRepository/ZoneRepository |

**Code reference:**
```java
// CommandService.java:186
.householdContext(Map.of()) // Minimal context for now
```

---

## 2. Contract & Integration Compliance Review

### Status: PASS WITH NOTES

### Findings

#### CommandDTO matches contract schema

**Schema:** `docs/integration/ai-platform/v1/contracts/schemas/command.schema.json`

**Java DTO:** `AiDecisionRequest.java` matches required fields:
- `commandId` (UUID) ✓
- `correlationId` (UUID) ✓
- `commandType` (String: "create_task", "complete_task") ✓
- `payload` (Map<String, Object>) ✓
- `requesterId` (UUID) ✓
- `householdId` (UUID) ✓
- `householdContext` (Map<String, Object>) ✓

#### DecisionDTO matches response schema

**Schema:** `docs/integration/ai-platform/v1/contracts/schemas/decision.schema.json`

**Java DTO:** `AiDecisionResponse.java` matches:
- `decisionId` (UUID) ✓
- `type` (String: "start_job", "clarify", "reject") ✓
- `confidence` (BigDecimal) ✓
- Conditional fields (actions/question/reason) ✓

#### Validation script runs successfully

```bash
./scripts/validate-aiplatform-contracts.sh
# Output: All validations passed!
```

The script validates:
- `start-job-response.json` against `decision.schema.json`
- `clarify-response.json` against `decision.schema.json`
- `reject-response.json` against `decision.schema.json`
- `command.schema.json` syntax

#### Endpoint mismatch documented

| Source | Endpoint |
|--------|----------|
| AI Platform spec mentions | `/decide` |
| HomeTusk implementation | `/decision` |
| OpenAPI contract | `/decision` |

**Status:** Documented in `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md:7-23`

**Policy:** HomeTusk uses `/decision` as defined in current OpenAPI. Migration to `/decide` requires coordinated change with AI Platform team.

### Issues Found

| Issue | Location | Risk | Recommendation |
|-------|----------|------|----------------|
| No runtime JSON Schema validation | `AiDecisionResponseMapper.java` | Invalid AI response may cause NPE or wrong behavior | Add networknt/json-schema-validator |
| Script not in CI | `.github/` missing | Contract drift undetected | Add to CI workflow |

---

## 3. Guardrails Readiness Review (Stage 3 Prep)

### Status: PASS

### Findings

#### BusinessValidator enforces boundaries BEFORE actions

**Location:** `services/backend/src/main/java/com/hometusk/commands/pipeline/BusinessValidator.java`

**Rules enforced:**
- `ASSIGNEE_MUST_BE_MEMBER` - line 54-58
- `ZONE_MUST_EXIST` - line 62-66
- `DEADLINE_MUST_BE_FUTURE` - line 70-75
- `TASK_NOT_FOUND` (IDOR prevention) - line 99-104
- `TASK_ALREADY_COMPLETED` - line 110-111

#### Clarify path does NOT create domain entities

**Verification:** `CommandService.handleClarify()` (lines 263-305):
1. Writes to `decision_logs` (audit only)
2. Updates `commands.status = NEEDS_INPUT`
3. Returns `ClarifyResponse` to client
4. **Does NOT call** `ActionExecutor` or `TaskService`

#### Recommended guardrail insertion points

| Guardrail | Package/Class | Integration Point |
|-----------|---------------|-------------------|
| `zone_owner_first` | `commands.pipeline.guardrails.ZoneOwnerPolicy` | Before ActionExecutor.executeAction() |
| `load_balancing` | `commands.pipeline.guardrails.LoadBalancingPolicy` | In DecisionProviderSelector after AI decision |
| `quiet_hours` | `commands.pipeline.guardrails.AvailabilityPolicy` | In HouseholdContext enrichment |
| `max_open_tasks` | `commands.pipeline.guardrails.TaskLimitPolicy` | BusinessValidator extension |

**Recommended architecture:**

```
commands/pipeline/guardrails/
├── GuardrailsValidator.java       # Orchestrator
├── GuardrailPolicy.java           # Interface
├── ZoneOwnerPolicy.java
├── LoadBalancingPolicy.java
├── AvailabilityPolicy.java
└── TaskLimitPolicy.java
```

**Integration in CommandService:**
```java
// After decision, before action
DecisionResult guardedResult = guardrailsValidator.apply(result, context);
return handleDecisionResult(guardedResult, ...);
```

### Issues Found

| Issue | Location | Risk | Recommendation |
|-------|----------|------|----------------|
| No POST-decision validation | `ActionExecutor.java` | AI may propose invalid assignee/zone | Add re-validation before action |

Currently `BusinessValidator` runs BEFORE decision. AI Platform may propose:
- Non-existent assignee (should re-validate)
- Non-existent zone (should re-validate)

**Recommendation:** Add lightweight validation in `ActionExecutor.executeCreateTaskFromAction()` at line 121.

---

## 4. Test Coverage Review

### Status: CRITICAL GAPS

### Findings

#### Existing test coverage

**File:** `CommandPipelineTest.java` (377 lines)

| Scenario | Covered |
|----------|---------|
| create_task minimal | ✓ |
| create_task full payload | ✓ |
| create_task validation errors | ✓ |
| complete_task success | ✓ |
| complete_task not found | ✓ |
| Authorization (non-member) | ✓ |
| Correlation ID propagation | ✓ |

**All tests use ManualDecisionProvider** (default config).

#### Missing test coverage (CRITICAL)

| Scenario | Priority | Framework |
|----------|----------|-----------|
| AI Platform → start_job → action executed | CRITICAL | WireMock |
| AI Platform → clarify → NEEDS_INPUT, no domain changes | CRITICAL | WireMock |
| AI Platform timeout → fallback executed | CRITICAL | WireMock |
| AI Platform error 5xx → fallback executed | HIGH | WireMock |
| AI Platform invalid response → graceful handling | HIGH | WireMock |

#### Test infrastructure gaps

**Current dependencies (build.gradle.kts):**
```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test")
testImplementation("org.springframework.security:spring-security-test")
testImplementation("org.testcontainers:testcontainers:1.19.3")
testImplementation("org.testcontainers:postgresql:1.19.3")
```

**Missing:**
```kotlin
testImplementation("org.wiremock:wiremock-standalone:3.3.1")
// OR
testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
```

### Recommended Test Plan

**New file:** `AiPlatformIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AiPlatformIntegrationTest extends IntegrationTestBase {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureAiPlatform(DynamicPropertyRegistry registry) {
        registry.add("decision.provider", () -> "aiplatform");
        registry.add("aiplatform.base-url", wireMock::baseUrl);
    }

    @Test
    void aiPlatform_startJob_taskCreated() {
        // Stub AI Platform response
        wireMock.stubFor(post("/decision")
            .willReturn(okJson("""
                {"decisionId":"...", "type":"start_job", "confidence":0.95,
                 "actions":[{"actionType":"create_task","parameters":{...}}]}
                """)));

        // Execute command
        mockMvc.perform(post("/api/v1/commands")...)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("executed"));

        // Verify task created
        assertThat(taskRepository.count()).isEqualTo(1);
    }

    @Test
    void aiPlatform_clarify_needsInput() {
        wireMock.stubFor(post("/decision")
            .willReturn(okJson("""
                {"decisionId":"...", "type":"clarify", "confidence":0.4,
                 "question":"Кому назначить?", "requiredFields":["assigneeId"]}
                """)));

        mockMvc.perform(post("/api/v1/commands")...)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("needs_input"));

        // Verify NO task created
        assertThat(taskRepository.count()).isEqualTo(0);
    }

    @Test
    void aiPlatform_timeout_fallback() {
        wireMock.stubFor(post("/decision")
            .willReturn(aResponse().withFixedDelay(10000))); // 10s delay

        mockMvc.perform(post("/api/v1/commands")...)
            .andExpect(status().is(207))
            .andExpect(jsonPath("$.status").value("executed_degraded"));
    }
}
```

**Files to create/modify:**

| File | Action |
|------|--------|
| `build.gradle.kts` | Add WireMock dependency |
| `AiPlatformIntegrationTest.java` | Create |
| `IntegrationTestBase.java` | Add AI Platform config helper |

---

## 5. Documentation Sync Review

### Status: PASS

### Findings

#### CLAUDE.md alignment

**Rule:** "Decision logic lives in external AI Platform (Stage 2+)"

**Code compliance:**
- `DecisionProvider` interface exists ✓
- `AiPlatformDecisionProvider` calls external service ✓
- No LLM code in repository ✓
- AI output logged to `raw_decision_payload` ✓

#### service-catalog.md alignment

**Section:** "External Dependencies → AI Platform (Stage 2)"

**Content:** Correctly describes:
- Integration Package location ✓
- OpenAPI contract location ✓
- Endpoints called (`POST /decision`, `GET /health`) ✓
- Configuration example ✓
- Validation script reference ✓

#### Integration package alignment

**Endpoint documentation:**

| Document | States |
|----------|--------|
| `README.md` | "/decide" mentioned as spec, "/decision" as implementation |
| `mapping/hometusk-to-aiplatform.md` | Divergence documented with resolution |
| `ai-platform.decision.openapi.yaml` | `/decision` (matches code) |

**Recommendation:** Update README.md to state `/decision` as canonical.

### Issues Found

| Issue | Location | Risk | Recommendation |
|-------|----------|------|----------------|
| Confusing endpoint mention | `docs/integration/ai-platform/v1/README.md:33-35` | Developer confusion | Simplify to state only `/decision` |

---

## Action List

### Critical (Block Stage 3)

| # | Action | Files | Effort |
|---|--------|-------|--------|
| 1 | Add WireMock dependency | `services/backend/build.gradle.kts` | 5 min |
| 2 | Create AI Platform integration tests | `AiPlatformIntegrationTest.java` | 2-3 hrs |
| 3 | Populate householdContext | `CommandService.java:186`, new `HouseholdContextBuilder` | 1 hr |

### High Priority (Before first guardrail)

| # | Action | Files | Effort |
|---|--------|-------|--------|
| 4 | Add runtime DecisionDTO validation | `AiDecisionResponseMapper.java` | 1 hr |
| 5 | Add post-decision validation in ActionExecutor | `ActionExecutor.java:121` | 30 min |

### Nice-to-Have

| # | Action | Files | Effort |
|---|--------|-------|--------|
| 6 | Add contract validation to CI | `.github/workflows/ci.yml` | 30 min |
| 7 | Simplify endpoint documentation | `docs/integration/ai-platform/v1/README.md` | 10 min |

---

## Appendix: File References

| File | Purpose | Lines Reviewed |
|------|---------|----------------|
| `DecisionProvider.java` | Interface definition | 1-31 |
| `DecisionProviderSelector.java` | Provider selection + fallback | 1-115 |
| `AiPlatformDecisionProvider.java` | AI Platform adapter | 1-53 |
| `AiPlatformClient.java` | HTTP client | 1-99 |
| `AiDecisionRequest.java` | Request DTO | 1-29 |
| `AiDecisionResponse.java` | Response DTO | 1-26 |
| `AiDecisionResponseMapper.java` | Response mapping | 1-78 |
| `DecisionResult.java` | Sealed result types | 1-71 |
| `CommandService.java` | Pipeline orchestration | 1-348 |
| `BusinessValidator.java` | Domain rule validation | 1-130 |
| `ActionExecutor.java` | Action execution | 1-201 |
| `CommandPipelineTest.java` | Integration tests | 1-377 |
| `application.yml` | Configuration | 72-82 |
| `command.schema.json` | Request schema | 1-127 |
| `decision.schema.json` | Response schema | 1-103 |
| `hometusk-to-aiplatform.md` | Field mapping | 1-196 |
| `service-catalog.md` | Service registry | 219-262 |

---

**Review completed:** 2026-01-12
**Next review:** After Stage 3 implementation (guardrails)
