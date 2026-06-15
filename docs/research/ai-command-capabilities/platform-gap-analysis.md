# AI Platform Gap Analysis

Status: Initial baseline, 2026-06-15

## Sources of Truth

- `../../VR_AI_Platform/contracts/schemas/command.schema.json`
- `../../VR_AI_Platform/contracts/schemas/decision.schema.json`
- `../../VR_AI_Platform/app/models/api_models.py`
- `../../VR_AI_Platform/app/routes/decide.py`
- `../../VR_AI_Platform/app/services/decision_service.py`
- `../../VR_AI_Platform/graphs/core_graph.py`
- `../../VR_AI_Platform/routers/v2.py`
- `../../VR_AI_Platform/routers/assist/**`
- `../../VR_AI_Platform/routers/partial_trust_*.py`
- `../../VR_AI_Platform/agent_registry/**`
- `../../VR_AI_Platform/docs/adr/ADR-000-ai-platform-intent-decision-engine.md`
- `../../VR_AI_Platform/docs/adr/ADR-004-partial-trust-corridor.md`
- `../../VR_AI_Platform/docs/adr/ADR-005-internal-agent-contract-v0.md`

## Gap Summary

| ID | Gap | Severity | Blocks |
| --- | --- | --- | --- |
| P-GAP-001 | No accepted `confirm` outcome in DecisionDTO. | High | Mobile AI UX, risky mutations |
| P-GAP-002 | No accepted `answer` outcome in DecisionDTO. | High | Status/query commands |
| P-GAP-003 | Provider action enum lacks task completion, reschedule, explicit linkage. | High | Broad natural command |
| P-GAP-004 | Intent classification is narrow and keyword-oriented. | High | Domain Planner v1 quality |
| P-GAP-005 | Date/time normalization is not production-grade. | High | deadlines/scheduling |
| P-GAP-006 | Assignment reasoning is default-assignee-first. | Medium | fairness/household planning |
| P-GAP-007 | Shopping extraction does not cover HomeTusk `category`, `source`, `linkedTaskId`. | Medium | shopping UX |
| P-GAP-008 | Mixed task plus shopping planning is unproven. | High | task-shopping linkage scenario |
| P-GAP-009 | Partial trust is narrow, sampled, and shopping-only. | Medium | production planner readiness |
| P-GAP-010 | Agent registry is internal-only and disabled by default. | Medium | relying on agents for UX |
| P-GAP-011 | Golden scenario suite is not HomeTusk-owned. | High | release confidence |
| P-GAP-012 | Provider `reject` is not a clean first-class action. | Medium | taxonomy clarity |

## Detailed Gaps

### P-GAP-001: Missing `confirm`

Current provider schema supports actions `start_job`, `propose_create_task`,
`propose_add_shopping_item`, and `clarify`.

Impact:

- HomeTusk cannot distinguish "safe to execute" from "understood but needs user
  confirmation".
- Risky commands either execute too early or degrade to clarify/reject.

Required before:

- broad multi-action plans;
- inferred non-requester assignment;
- reschedule;
- Mobile AI Command UX.

### P-GAP-002: Missing `answer`

Current provider response has no read-only answer outcome.

Impact:

- Queries such as household status must become clarify/reject or be handled
  outside AI-command taxonomy.

Required before:

- status/query style commands;
- mobile answer cards.

### P-GAP-003: Action Coverage

Provider actions cover task creation and shopping addition. They do not cover:

- `complete_task`;
- `reschedule_task`;
- `link_task_shopping`;
- `answer_status`;
- `confirm_action`.

Impact:

- Current provider cannot be the production planner for the full golden scenario
  catalog.

### P-GAP-004: Intent Classification

Observed code in `graphs/core_graph.py` uses narrow keyword matching.
Router V2 improves shape but still depends on deterministic intent detection
and optional flag-gated assist.

Impact:

- Ambiguous household commands and Russian morphology are not reliably covered.

Required:

- accepted intent taxonomy;
- golden scenario regression;
- strict clarify behavior on low confidence.

### P-GAP-005: Date/Time Normalization

Provider has no strong evidence of locale/timezone-aware normalization for
"today evening", "tomorrow", or "weekend".

Impact:

- Deadlines may be absent, wrong, or must be clarified.
- HomeTusk can validate future deadlines but cannot infer accepted windows.

### P-GAP-006: Assignment Reasoning

Provider defaults assignment to requester/default assignee. HomeTusk can adjust
zone owner and max open tasks deterministically.

Impact:

- Fairness-aware assignment is not planner-ready.
- Named assignee extraction is not proven.

### P-GAP-007: Shopping Metadata

Provider supports `name`, `quantity`, `unit`, and `list_id`. HomeTusk shopping
domain also supports category/source/task linkage.

Impact:

- Scenario "добавь в ашан ..." cannot be faithfully represented without contract
  expansion or explicit source/list semantics.

### P-GAP-008: Mixed Domain Planning

Provider V2 can emit multiple shopping item actions. It does not prove a mixed
plan that creates a task and shopping items and links them.

Impact:

- Task plus shopping linkage scenario should not auto-execute.

### P-GAP-009: Partial Trust Scope

Partial trust is limited to `add_shopping_item`, feature-flagged, sampled, and
accepted only after strict validation.

Impact:

- It is a useful safety pattern, not a general planner.

### P-GAP-010: Agent Registry Runtime Readiness

Agent registry capabilities exist, but sample registry agents are disabled by
default and internal-only.

Impact:

- Agent registry should not be treated as production household planner evidence.

### P-GAP-011: Golden Scenario Ownership

Provider has tests for planner behavior and quality metrics, but HomeTusk did
not have a product-owned scenario catalog before this initiative.

Impact:

- Provider readiness cannot be accepted solely by provider unit tests.

### P-GAP-012: Reject Taxonomy

Provider schema has `status=error`; integration docs mention `reject`.

Impact:

- Cross-repo decision taxonomy is unclear.

## Required AI Platform Next Initiative

Recommended next provider-side initiative:

`AI Platform Domain Planner v1 - Narrow Household Command Corridor`

Minimum scope:

- adopt accepted decision taxonomy or explicitly map provider status/action into it;
- support `execute`, `clarify`, `reject`, and optionally `confirm` for narrow v1;
- support `create_task` and multi-item `add_shopping_items`;
- define date/time proposal behavior with timezone;
- preserve safe fallback and schema validation;
- run HomeTusk golden scenarios as regression fixtures;
- avoid broad multi-agent planner as the first production brain.
