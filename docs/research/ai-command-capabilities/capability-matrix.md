# Capability Matrix

Status: Initial baseline, 2026-06-15

## Sources of Truth

- `services/backend/src/main/java/com/hometusk/commands/**`
- `clients/mobile/src/features/command/**`
- `clients/web/src/routes/Commands.tsx`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/v1/**`
- `../../VR_AI_Platform/contracts/schemas/**`
- `../../VR_AI_Platform/graphs/core_graph.py`
- `../../VR_AI_Platform/routers/v2.py`
- `../../VR_AI_Platform/routers/assist/**`
- `../../VR_AI_Platform/routers/partial_trust_*.py`
- `../../VR_AI_Platform/tests/test_planner_multi_item.py`
- `../../VR_AI_Platform/tests/test_quality_eval.py`

## Matrix

| Capability | Current observed maturity | Required maturity for good UX | Product value | Implementation risk | Safety risk | Priority | Owner | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ASR transcript | Medium | Transcript capture with explicit user send and safe errors | Medium | Medium | Medium | P2 | Both | HomeTusk voice BFF and provider ASR docs/code exist; ASR is separate from decisioning. |
| Language normalization | Low/experimental | Robust typo/noise normalization without changing meaning | Medium | Medium | Medium | P2 | AI Platform | Assist normalization is flag-gated and off by default. |
| Intent classification | Low/medium | Stable household intent taxonomy with golden regression | High | Medium | High | P0 | AI Platform | Provider deterministic keyword intent detection is narrow. |
| Entity extraction | Medium for shopping, low for tasks | Extract items, task title, assignee, zone, time with confidence and gaps | High | High | High | P0 | AI Platform | V2 multi-item tests exist; assignee/date extraction is weak. |
| Multi-item shopping extraction | Medium | Correct item split, quantity/unit, list/source/category support | High | Medium | Medium | P0 | AI Platform + HomeTusk | Provider V2 can emit multiple shopping actions; HomeTusk maps item name/quantity/unit/list only. |
| Date/time normalization | Low | Locale/timezone-aware windows and ambiguity handling | High | High | High | P0 | AI Platform + HomeTusk | HomeTusk validates future deadlines; provider has no strong date parser evidence. |
| Task creation planning | Medium | Title, optional zone/assignee/deadline, clarify/confirm for ambiguity | High | Medium | Medium | P0 | Both | Supported by provider and HomeTusk, but narrow. |
| Task completion matching | Low | Match natural references to one task or clarify | High | High | High | P1 | HomeTusk + AI Platform | HomeTusk requires task id; mobile does local title/id matching. |
| Assignment recommendation | Low | Member grounding, fairness/workload, confirmation for inferred assignments | High | High | High | P1 | Both | HomeTusk default requester/zone owner; provider default assignee only. |
| Workload/fairness reasoning | Low | Deterministic policy plus explainable planner hints | Medium | High | High | P2 | HomeTusk | HomeTusk has workload score and max-open guardrail; no planner fairness. |
| Zone grounding | Medium | Map natural zone names to household zones or clarify | Medium | Medium | Medium | P1 | Both | HomeTusk provides zones in context and validates zone id. |
| Shopping list grounding | Medium | Default list, named list/source, clarify if ambiguous | High | Medium | Medium | P0 | Both | Provider uses default list; source/list naming gap remains. |
| Task-shopping linkage planning | Low | Explicit plan to link created or existing task and items | High | High | Medium | P1 | Both | HomeTusk can link items if created with task; provider does not prove mixed plan. |
| Multi-action planning | Low/medium | Bounded action graph with plan-level validation and confirmation | High | High | High | P1 | AI Platform + HomeTusk | Provider V2 supports multiple shopping actions; mixed domain actions unproven. |
| Clarification question generation | Medium | Specific, safe, field-backed questions | High | Medium | Low | P0 | AI Platform + HomeTusk | Provider clarify exists; assist clarify is flag-gated with safety checks. |
| Confirmation decisioning | Missing | First-class `confirm` outcome and UX contract | High | Medium | High | P0 | Both | No provider or HomeTusk confirm response type. |
| Structured result explanation | Low | Action summary, why, source, confidence, what changed | High | Medium | Medium | P1 | HomeTusk | Current mobile/web show status/result but not full planned rationale. |
| Confidence scoring | Low/medium | Calibrated signal used for audit, not execution permission | Medium | High | High | P1 | AI Platform | Provider returns fixed/deterministic confidence and partial trust threshold. |
| Fallback/degradation | High for manual fallback | Deterministic fallback with explicit audit and UX | High | Low | Low | P0 | HomeTusk | DecisionProviderSelector falls back to manual provider. |
| Audit/traceability | High | DecisionLog for all commands plus scenario replay metadata | High | Medium | Medium | P0 | HomeTusk | DecisionLog stores raw provider payload and validation flags. |
| Golden scenario regression testing | Low/medium | HomeTusk-owned scenario suite tied to product taxonomy | High | Medium | Medium | P0 | Both | Provider has quality eval tests; HomeTusk lacks accepted scenario catalog before this pack. |

## Priority Interpretation

- P0: required before Domain Planner v1 can be trusted.
- P1: required before broad natural command or Mobile AI UX.
- P2: important but can follow the narrow v1 corridor.

## Ownership Summary

AI Platform should own:

- intent classification;
- entity extraction;
- date/time normalization proposals;
- decision/action taxonomy support;
- golden scenario runner against provider decisions.

HomeTusk should own:

- accepted action taxonomy;
- API/adapter contracts;
- guardrails and domain validation;
- execution and audit;
- mobile/web UX states;
- product golden scenarios and final GO/NO-GO gates.
