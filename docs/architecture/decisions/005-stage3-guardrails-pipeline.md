# ADR-005: Stage 3 Guardrails Pipeline

**Status:** Accepted
**Date:** 2026-01-13
**Deciders:** Development team
**Stage:** 3

## Context

Stage 2 integrated with an external AI Platform for intelligent decision-making. However, critical issues were identified:

1. **householdContext was empty** - AI Platform received no context about household members and zones
2. **No JSON Schema validation** - AI responses were not validated before mapping
3. **No guardrails** - No safety policies to prevent problematic decisions before execution

Stage 3 needs to address these issues by:
- Implementing ContextBuilder to provide household context
- Adding JSON Schema validation for AI responses
- Creating a composable Guardrails pipeline

## Decision

### 1. ContextBuilder and HouseholdSnapshot

Created a `ContextBuilder` component that builds context for both AI Platform requests and guardrails evaluation:

```java
@Component
public class ContextBuilder {
    HouseholdSnapshot buildSnapshot(UUID householdId, UUID correlationId);
    Map<String, Object> buildHouseholdContextForAi(UUID householdId, UUID correlationId);
}
```

`HouseholdSnapshot` is an immutable record containing:
- Members with roles (admin/member)
- Zones (with optional owner)
- Open task count per assignee

**Fail-safe fallback:** If context cannot be fully loaded (database error, no members found), the snapshot is marked as `incomplete()` and the guardrails pipeline will return `NeedsClarification` - **never continue with incomplete data**.

### 2. JSON Schema Validation

AI Platform responses are validated against JSON Schema before mapping:

**Schema:** `src/main/resources/schemas/ai-decision-response.schema.json`

```java
@Component
public class AiResponseSchemaValidator {
    ValidationResult validate(AiDecisionResponse response);
}
```

- On validation failure: Return `DecisionResult.Reject` with errorCode `AI_RESPONSE_INVALID`
- Per CLAUDE.md Rule 1: "AI output MUST be schema-validated before use"

### 3. Guardrails Pipeline

Introduced a policy-based guardrails system that evaluates decisions before execution:

```java
public interface GuardrailPolicy {
    GuardrailOutcome evaluate(GuardrailContext context);
    String getName();
    int getOrder();
}

sealed interface GuardrailOutcome {
    record Accept() implements GuardrailOutcome {}
    record Modify(List<ProposedAction> modifiedActions, String reason) implements GuardrailOutcome {}
    record Clarify(String question, List<String> requiredFields, Map<String, Object> suggestions) {}
    record Reject(String reason, String errorCode) implements GuardrailOutcome {}
}
```

**Orchestration logic:**
1. Check if snapshot is complete (fail-safe: CLARIFY if not)
2. Evaluate policies in order (sorted by `getOrder()`)
3. Aggregate outcomes:
   - Any REJECT → final REJECT
   - Any CLARIFY (no REJECT) → final CLARIFY
   - All ACCEPT/MODIFY → apply modifications and proceed

### 4. Implemented Policies

| Policy | Order | Purpose |
|--------|-------|---------|
| `ZoneOwnerFirstPolicy` | 100 | Assign zone owner when no assignee specified |
| `MaxOpenTasksPerAssigneePolicy` | 200 | Request clarification if assignee has >= max open tasks |

### 5. Configuration

```yaml
guardrails:
  enabled: true
  max-open-tasks-per-assignee: 10
```

## Consequences

### Positive
- AI Platform now receives full household context (members, zones)
- AI responses are schema-validated before processing
- Safety policies prevent problematic decisions
- Composable policy pattern allows easy extension
- Fail-safe behavior ensures no decisions with incomplete context

### Negative
- Additional database queries for snapshot building
- Slight latency increase from guardrails evaluation
- More complex pipeline logic

### Deferred to Stage 4
- Observability metrics for guardrails outcomes
- Database index on (assignee_id, status) for performance
- Additional policies (availability, quiet hours)

## Implementation

### New Files Created
```
com.hometusk.commands.pipeline/
├── ContextBuilder.java
└── guardrails/
    ├── GuardrailPolicy.java
    ├── GuardrailOutcome.java
    ├── GuardrailContext.java
    ├── GuardrailResult.java
    ├── GuardrailsOrchestrator.java
    ├── GuardrailsConfig.java
    ├── HouseholdSnapshot.java
    ├── MaxOpenTasksPerAssigneePolicy.java
    └── ZoneOwnerFirstPolicy.java

com.hometusk.commands.pipeline.decision.client/
└── AiResponseSchemaValidator.java

src/main/resources/schemas/
└── ai-decision-response.schema.json

src/test/java/com/hometusk/integration/aiplatform/
├── AiPlatformIntegrationTestBase.java
└── AiPlatformIntegrationTest.java (5 test scenarios)
```

### Modified Files
- `CommandService.java` - Integrated ContextBuilder and GuardrailsOrchestrator
- `AiPlatformDecisionProvider.java` - Added JSON Schema validation
- `TaskRepository.java` - Added `countTasksByAssigneeAndStatuses` query
- `ErrorCode.java` - Added `GUARDRAILS_REJECTED`
- `application.yml` - Added guardrails configuration

### Database
- `TaskRepository.countTasksByAssigneeAndStatuses` - Batch query for open task counts

## References

- [ADR-004: Stage 2 AI Platform Integration](004-stage2-ai-platform-integration.md)
- [Service Catalog](../service-catalog.md)
- [CLAUDE.md - Architectural Rules](../../../CLAUDE.md)
