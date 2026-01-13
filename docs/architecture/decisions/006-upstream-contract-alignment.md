# ADR-006: Upstream AI Platform Contract Alignment

**Status:** Accepted
**Date:** 2026-01-13
**Deciders:** Development team
**Stage:** 2 Enhancement

## Context

Stage 2 introduced AI Platform integration with a local contract definition. However:

1. HomeTusk contracts diverged from canonical AI Platform contracts
2. Endpoint mismatch: HomeTusk used `/decision`, upstream uses `/decide`
3. New upstream decision types (`propose_create_task`, `propose_add_shopping_item`) not handled
4. No clear "source of truth" for contracts

This led to potential compatibility issues when AI Platform evolves.

## Decision

### 1. Upstream-First Contract Approach

**Canonical contracts live at:** `docs/integration/ai-platform/v1/upstream/`

This directory is a **vendor snapshot** of AI Platform contracts and is treated as read-only. HomeTusk code adapts to upstream, not vice versa.

**HomeTusk wrapper schemas** remain at `docs/integration/ai-platform/v1/contracts/` and document what HomeTusk actually supports.

### 2. Minimal Adapter Layer

Instead of creating an abstraction framework, we use a single mapper class with explicit field mappings:

```java
// AiDecisionResponseMapper.java
public DecisionResult toDecisionResult(AiDecisionResponse response) {
    return switch (response.type()) {
        case "start_job" -> mapToStartJob(response, rawPayload);
        case "propose_create_task" -> mapProposeCreateTask(response, rawPayload);
        case "propose_add_shopping_item" -> unsupportedDecisionType(response, rawPayload);
        case "clarify" -> mapToClarify(response, rawPayload);
        case "reject" -> mapToReject(response, rawPayload);
        default -> unknownDecisionType(response, rawPayload);
    };
}
```

**No new classes, interfaces, or frameworks** — just explicit switch-case mapping.

### 3. Safe Degradation Strategy

| Upstream Type | HomeTusk Support | Degradation |
|---------------|------------------|-------------|
| `start_job` | Full | → StartJob |
| `propose_create_task` | Mapped | → StartJob (execute immediately) |
| `propose_add_shopping_item` | Unsupported | → Clarify with user message |
| `clarify` | Full | → Clarify |
| `reject` | Full | → Reject |
| Unknown | N/A | → Reject with errorCode |

**Rules:**
- Unsupported but known types → `Clarify` (user can understand)
- Unknown types → `Reject` (potential contract violation)
- Never throw exceptions for valid upstream responses

### 4. Configurable Endpoint

```yaml
aiplatform:
  decision-path: /decision  # default (HomeTusk legacy)
  # or /decide (upstream canonical)
```

This allows gradual migration to upstream endpoint without code changes.

### 5. Schema Validation

Response validation uses upstream-aligned schema that accepts all upstream types:
- `start_job`, `propose_create_task`, `propose_add_shopping_item`, `clarify`, `reject`
- Action types: `create_task`, `complete_task`, `add_shopping_item`

Invalid responses (missing required fields) result in `Reject`.

## Consequences

### Positive

- Clear source of truth for contracts (upstream/)
- Safe handling of future AI Platform types
- No breaking changes to existing functionality
- Minimal code changes (single mapper class)
- Easy to add support for new types

### Negative

- Some upstream types return user-facing messages about "not supported"
- Need to manually update upstream snapshot when AI Platform changes

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Upstream schema changes break validation | Schema validation rejects invalid, doesn't crash |
| New type not handled | Default case returns Reject safely |
| Mapper loses fields for guardrails | Integration test covers adapter → guardrails flow |

## Test Strategy

9 integration test scenarios covering:
1. start_job → Task created
2. clarify → NEEDS_INPUT
3. Invalid schema → REJECTED
4. Timeout → Fallback
5. Guardrails CLARIFY → NEEDS_INPUT
6. propose_create_task → Task created (mapped)
7. propose_add_shopping_item → NEEDS_INPUT (safe degradation)
8. Unknown type → REJECTED
9. **Critical:** Adapter → Guardrails flow (proves mapping preserves assigneeId, zoneId)

## How to Update Upstream Snapshot

1. Obtain new contracts from AI Platform team
2. Copy files to `docs/integration/ai-platform/v1/upstream/`
3. Update `upstream/VERSION`
4. Compare with `mapping/hometusk-to-upstream.md`
5. Update `AiDecisionResponseMapper` if new types
6. Update tests
7. Create ADR if breaking changes

## Breaking Change Policy

Changes are **breaking** if:
- Required field removed from response
- Enum value renamed or removed
- Semantic meaning of field changed

Breaking changes require:
- New ADR
- Migration path documented
- Feature flag for gradual rollout

## Implementation

### New Files
- `docs/integration/ai-platform/v1/upstream/` (vendor snapshot)
- `docs/integration/ai-platform/v1/mapping/hometusk-to-upstream.md`

### Modified Files
- `AiDecisionResponseMapper.java` — safe degradation
- `AiPlatformClient.java` — configurable endpoint
- `ai-decision-response.schema.json` — upstream-aligned
- `AiPlatformIntegrationTest.java` — 4 new test scenarios
- `application.yml` — decision-path property
- `CLAUDE.md` — Rule 7
- `service-catalog.md` — upstream info

## References

- [ADR-004: Stage 2 AI Platform Integration](004-stage2-ai-platform-integration.md)
- [Upstream README](../../integration/ai-platform/v1/upstream/README.md)
- [Mapping Documentation](../../integration/ai-platform/v1/mapping/hometusk-to-upstream.md)
- [CLAUDE.md Rule 7](../../../CLAUDE.md)
