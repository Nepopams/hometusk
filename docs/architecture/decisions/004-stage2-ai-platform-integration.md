# ADR-004: Stage 2 AI Platform Integration

**Status:** Accepted
**Date:** 2026-01-12
**Deciders:** Development team
**Stage:** 2

## Context

Stage 1 implemented the Commands API with rule-based decisions (assign to initiator if no assignee specified). Stage 2 needs to integrate with an external AI Platform for intelligent decision-making while maintaining backward compatibility and degraded mode support.

Key constraints:
- HomeTusk is a **consumer** of an external AI Platform (no LLM code in this repo)
- Keep `/api/v1/commands` as the main entry point
- Maintain the existing pipeline: SchemaValidator -> BusinessValidator -> Decision -> ActionExecutor -> ActivityRecorder
- Abstract the decision step behind a provider interface
- Support graceful degradation when AI Platform is unavailable

## Decision

### 1. DecisionProvider Abstraction

Introduced a `DecisionProvider` interface to abstract decision-making:

```java
public interface DecisionProvider {
    DecisionResult decide(DecisionContext context);
    DecisionSource getSource();
    boolean isAvailable();
}
```

**Implementations:**
- `ManualDecisionProvider` - Rule-based logic extracted from Stage 1 `DecisionEngine`
- `AiPlatformDecisionProvider` - HTTP client to external AI Platform

**Selection:** Via configuration `decision.provider=manual|aiplatform` (default: manual)

### 2. Decision Result Types

AI Platform can return three result types, modeled as a sealed interface:

```java
sealed interface DecisionResult {
    record StartJob(source, confidence, actions) implements DecisionResult {}
    record Clarify(source, confidence, question, requiredFields) implements DecisionResult {}
    record Reject(source, confidence, reason, errorCode) implements DecisionResult {}
}
```

- `StartJob` - Execute proposed actions (maps to existing behavior)
- `Clarify` - Need user clarification (new `NEEDS_INPUT` command status)
- `Reject` - Cannot process command (throws `BusinessException`)

### 3. Fallback Strategy

When AI Platform is unavailable, the system falls back to `ManualDecisionProvider`:

1. Log the failure with correlation ID
2. Execute using manual (rule-based) logic
3. Set `DecisionSource.FALLBACK`
4. Return HTTP 207 with `status: executed_degraded` and `degradedReason: ai_unavailable`

This aligns with CLAUDE.md rule 4: "System must work if AI is unavailable."

### 4. Traceability

All decisions include audit fields in `DecisionLog`:
- `externalDecisionId` - AI Platform's decision ID for cross-system correlation
- `rawDecisionPayload` - Full response from AI Platform for audit trail
- Existing fields: `source`, `confidence`, `correlationId`

### 5. External Contract

External AI Platform contract defined at `docs/contracts/external/ai-platform.decision.openapi.yaml`:
- `POST /decision` - Request AI decision
- `GET /health` - Health check

## Consequences

### Positive
- Clean abstraction allows easy switching between providers
- Full traceability maintained per CLAUDE.md requirements
- Graceful degradation ensures system availability
- Existing tests continue to work (manual provider as default)
- No breaking changes to API contract

### Negative
- Added complexity in decision flow
- External dependency introduces latency when using AI Platform
- Need to handle `Clarify` flow in UI (future Stage 3 work)

### Deferred to Stage 3
- Retry with exponential backoff
- Circuit breaker pattern
- Async command processing
- UI for clarification flow

## Implementation

### New Files Created
```
com.hometusk.commands.pipeline.decision/
├── DecisionProvider.java
├── DecisionContext.java
├── DecisionResult.java
├── ManualDecisionProvider.java
├── AiPlatformDecisionProvider.java
├── DecisionProviderSelector.java
├── DecisionProviderUnavailableException.java
└── client/
    ├── AiPlatformClient.java
    ├── AiDecisionRequest.java
    ├── AiDecisionResponse.java
    ├── AiDecisionResponseMapper.java
    └── AiPlatformException.java
```

### Modified Files
- `CommandService.java` - Use DecisionProviderSelector instead of DecisionEngine
- `CommandController.java` - Return CommandResponseBase (polymorphic response)
- `ActionExecutor.java` - Added `executeAction(ProposedAction)` method
- `DecisionLog.java` - Added `externalDecisionId`, `rawDecisionPayload` fields
- `DecisionSource.java` - Added `MANUAL`, `AI_PLATFORM` values
- `CommandStatus.java` - Added `NEEDS_INPUT` status
- `ErrorCode.java` - Added `AI_REJECTED`, `AI_UNAVAILABLE`

### Database Migration
`V010__add_ai_platform_fields.sql`:
- Added `external_decision_id UUID` column
- Added `raw_decision_payload JSONB` column
- Updated source constraint for new values

### Configuration
```yaml
decision:
  provider: manual  # or aiplatform
  fallback:
    enabled: true

aiplatform:
  base-url: ${AI_PLATFORM_URL}
  timeout-ms: 5000
  api-key: ${AI_PLATFORM_API_KEY}
```

## References

- [ADR-003: Stage 1 Commands API](003-stage1-commands-api.md)
- [AI Platform Contract](../../contracts/external/ai-platform.decision.openapi.yaml)
- [CLAUDE.md - Architectural Rules](../../../CLAUDE.md)
