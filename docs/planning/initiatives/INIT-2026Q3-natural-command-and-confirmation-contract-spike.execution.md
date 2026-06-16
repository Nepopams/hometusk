# INIT-2026Q3 Natural Command & Confirmation Contract Spike Execution Notes

**Status:** Completed; Gate A GO; Gate B GO; artifact gate GO/HOLD split; Gate C GO for docs-only APPLY; review gate GO; Gate D GO for initiative closure
**Date:** 2026-06-16
**Initiative:** `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
**Roadmap:** `docs/planning/strategy/roadmap.md`
**Delegation:** Human gates for this initiative are delegated to Codex. Every GO / NO-GO / HOLD decision is recorded here with evidence, risks, and rationale.

---

## Intake Summary

| Field | Decision |
| --- | --- |
| Request type | HomeTusk planning pipeline for `natural_command` and `needs_confirmation` contract spike |
| Scope anchor | `INIT-2026Q3-natural-command-and-confirmation-contract-spike` |
| Workflow path | `intake -> planning -> artifact gate -> workpack -> Codex PLAN -> delegated Gate C GO -> docs-only APPLY -> review gate -> delegated Gate D GO` |
| Change type | contract discovery, product architecture, backend API design, AI safety, mobile readiness, docs/process |
| Work level | initiative-level docs-only contract spike |
| Primary boundary | HomeTusk owns public API, execution authority, guardrails, pending confirmation state, and audit trail |
| Runtime posture | No runtime implementation in this initiative |
| Public API posture | Draft-only OpenAPI delta; no accepted public `/commands` contract change |

## Sources of Truth Read

### HomeTusk

| Artifact | Path |
| --- | --- |
| Active repo rules | `AGENTS.md` |
| Workflow | `docs/CODEX-WORKFLOW.md` |
| Product goal | `docs/planning/strategy/product-goal.md` |
| Roadmap | `docs/planning/strategy/roadmap.md` |
| MVP release scope | `docs/planning/releases/MVP.md` |
| DoR / DoD | `docs/_governance/dor.md`, `docs/_governance/dod.md` |
| Active initiative | `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md` |
| AI Platform 2.1 intake | `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`, `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.execution.md`, `docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-d.md` |
| Prior artifact gate | `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.execution.md`, `docs/research/ai-command-capabilities/domain-planner-v1-gate/natural-command-contract-v0-draft.md`, `docs/research/ai-command-capabilities/domain-planner-v1-gate/mobile-ai-state-matrix-v0.md` |
| Provider acceptance | `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/reject-confirm-answer-contract-posture.md`, `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/natural-command-readiness-decision.md`, `docs/research/ai-command-capabilities/provider-domain-planner-v1-acceptance/recommendation.md` |
| Current Commands contract | `docs/contracts/http/commands.openapi.yaml` |
| Contract index | `docs/_indexes/contracts-index.md` |
| AI Platform v2.1 integration | `docs/integration/ai-platform/v2.1/**` |
| Backend command DTOs/models | `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`, `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`, `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java`, `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java` |
| DecisionLog and decision pipeline | `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`, `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java` |
| Guardrails | `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailResult.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailOutcome.java` |
| Mobile command shell | `clients/mobile/src/api/types.ts`, `clients/mobile/src/features/command/**` |

### AI Platform Read-Only Evidence

Provider repository inspected read-only:

```text
C:/Users/user/Documents/projects/VR_AI_Platform
```

Provider handoff:

```text
C:/Users/user/Documents/projects/VR_AI_Platform/docs/planning/workpacks/ST-056/hometusk-handoff.md
```

Key evidence:

- Provider-side Gate D is GO for Domain Planner v1 contract + 50-scenario eval.
- Contract version is `2.1.0`.
- First-class `reject` is implemented.
- Non-executing `confirm` is schema-supported.
- `answer` remains blocked.
- 50 scenarios evaluated, 50 schema-valid decisions, 50 outcome matches, 0 unsupported auto-execute, 0 cross-household references, 0 blocker failure scenarios.
- Remaining non-blocker buckets: `wrong_intent=30`, `item_boundary_loss=2`.
- Provider repo status was clean during read-only inspection.

## Triage Classification

| Flag | Value | Evidence |
| --- | --- | --- |
| `contract_impact` | draft-only | Draft request/response/OpenAPI delta only; accepted `docs/contracts/http/commands.openapi.yaml` unchanged |
| `backend_impact` | no now | Future backend contract implementation is recommended separately |
| `mobile_impact` | no now | Mobile state dependencies only; no client files changed |
| `ai_platform_impact` | no | Provider repo read-only; no upstream writes |
| `security_sensitive` | yes | Confirmation, authorization, household-scoped actions, no-mutation approval path |
| `traceability_critical` | yes | Command, provider decision, guardrails, confirmation lifecycle, approval/cancel/expiry audit |
| `adr_needed` | no for this docs-only spike | Future runtime pending-confirmation ownership may require ADR if implementation changes service boundaries |
| `diagrams_needed` | no canonical diagram required now | Lifecycle is documented as text; future implementation may add canonical sequence/state diagrams |
| `cross_repo` | yes | AI Platform evidence read-only |

## Scope Boundary Preserved

In scope:

- Draft contract package under `docs/research/ai-command-capabilities/natural-command-contract-spike/**`.
- Roadmap update.
- Initiative execution notes.
- Implementation-ready workpack and staged PLAN/APPLY prompts for docs-only contract spike.
- Draft-only OpenAPI delta, explicitly non-binding.
- Contract index note for draft-only spike visibility.

Out of scope:

- Java/backend behavior change.
- Database migration.
- Accepted public OpenAPI change.
- `natural_command` execution.
- `needs_confirmation` public runtime implementation.
- Confirmation approve/cancel endpoint implementation.
- `answered` response.
- Mobile/web UI.
- Direct mobile/web to AI Platform.
- AI Platform repo changes.
- Production rollout/config change.

## Gate A Decision - GO

**Decision:** GO for making `INIT-2026Q3-natural-command-and-confirmation-contract-spike` the current roadmap initiative and closing the prior roadmap TBD anchor.

**Evidence:**

- `INIT-2026Q3-ai-platform-2-1-contract-intake` Gate D recommended this separate spike.
- AI Platform `2.1.0` intake gives HomeTusk safe provider `reject` and controlled provider `confirm` behavior, but no HomeTusk confirmation UX.
- Provider handoff reports 50 schema-valid provider decisions, 0 blocker failures, first-class `reject`, schema-level `confirm`, and blocked `answer`.
- The initiative has explicit docs-only scope, expected files, anti-scope, success metrics, flags, risks, and exit criteria.

**Rationale:**

HomeTusk can now define its own public contract boundary without approving runtime/mobile changes. The spike is the correct next initiative because runtime work would otherwise hide natural text in structured commands and flatten confirmation into rejection indefinitely.

**Risks accepted:**

- Provider intent evidence still has non-blocker mismatch buckets.
- HomeTusk lacks pending confirmation state and mobile confirmation cards today.
- The spike may identify future ADR/diagram needs, but must not implement them here.

## Gate B Decision - GO

**Decision:** GO for one docs-only workpack:

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/
```

**Committed scope:**

- Roadmap update and execution notes.
- Workpack, checklist, PLAN prompt, PLAN findings, Gate C, APPLY prompt, review gate, and Gate D evidence.
- All expected contract spike artifacts under `docs/research/ai-command-capabilities/natural-command-contract-spike/**`.
- Draft-only contract index note.

**DoR evidence:**

- Initiative title, outcome, owner, milestone, sources, expected files, acceptance criteria, flags, risks, out-of-scope, and exit criteria are present.
- Current `/commands` request/response, backend command states, DecisionLog, guardrails, AI Platform mapping, and mobile command state were inspected read-only.
- External provider evidence is available read-only and supports safe contract discovery, not runtime GO.

**Rationale:**

The work is bounded and demonstrable as docs-only contract discovery. It is not blocked by runtime implementation dependencies because implementation is explicitly out of scope.

## Artifact Gate Decision

### Contract Gate

**Decision:** GO for draft-only HomeTusk contract artifacts; HOLD for accepted public contract changes.

**Evidence:**

- Current public `CommandRequest.type` supports only `create_task` and `complete_task`.
- Current public `CommandResponse` variants are `executed`, `scheduled`, `needs_input`, `rejected`, and `executed_degraded`.
- Current mobile API types mirror those statuses and do not model `needs_confirmation` or `answered`.
- Current AI Platform `2.1.0` provider schema supports `confirm_payload`, but HomeTusk maps provider `confirm` to `AI_CONFIRMATION_UNSUPPORTED`.

**Rationale:**

Draft artifacts are needed before accepted contract APPLY. Accepted OpenAPI, DTO, endpoint, and mobile type changes remain a future initiative with its own Gate C.

### ADR / Diagram Gate

**Decision:** NO ARTIFACT CHANGE REQUIRED for this spike; HOLD for future runtime if pending confirmation state changes architecture boundaries.

**Evidence:**

- This initiative records lifecycle and ownership recommendations in research docs.
- No service boundary or runtime data model changes are applied here.

### Security / Traceability Gate

**Decision:** GO with mandatory future implementation evidence.

**Future implementation must prove:**

- No mutation occurs before explicit confirmation approval.
- Confirmation approval/cancel/expiry are household-scoped and actor-audited.
- Provider proposed actions are schema-validated and HomeTusk guardrail-checked as proposals before persistence.
- `DecisionLog.rawDecisionPayload` preserves raw provider payload, while pending confirmation state has an explicit source of truth outside `DecisionLog`.
- Raw audio is never stored as decision input.

## Codex PLAN Findings

Detailed findings are recorded in:

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/plan-findings.md
```

Key findings:

- Existing `/api/v1/commands` is the preferred compatibility surface; use future `type=natural_command` rather than a separate AI endpoint.
- `needs_confirmation` should be first-class and must not overload `needs_input`.
- `DecisionLog` is audit evidence, not pending confirmation state.
- Future confirmation runtime should use explicit pending confirmation/pending command ownership.
- `answer` remains blocked until a read-only answer contract starts.
- Provider `confirm` should map to `needs_confirmation` only after HomeTusk owns confirmation ids, expiry, authz, idempotency, and approval/cancel semantics.

## Gate C Decision - GO

**Decision:** GO for docs-only APPLY limited to the approved file list in the workpack and PLAN findings.

**Approved files:**

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/**`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/**`
- `docs/_indexes/contracts-index.md`

**Forbidden files:**

- `docs/contracts/http/commands.openapi.yaml`
- `services/backend/src/main/java/**`
- `services/backend/src/main/resources/db/migration/**`
- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`

**Rationale:**

The PLAN is decision-complete and names the draft artifacts, accepted evidence, and STOP conditions. Human Gate C is delegated, and the APPLY remains docs-only.

## APPLY Evidence

**Scope completed:**

- Roadmap updated to move the spike from NEXT to completed NOW and recommend the next backend contract implementation initiative.
- Initiative status updated to completed with delegated Gate D GO.
- Execution notes created.
- Workpack/checklist/PLAN findings/Gate C/APPLY prompt/review/Gate D artifacts created.
- Draft contract package created under `docs/research/ai-command-capabilities/natural-command-contract-spike/**`.
- Contract index updated with a draft-only, non-binding spike entry.

**Out-of-scope preserved:**

- No accepted public OpenAPI contract was changed.
- No backend Java, database migration, web/mobile, or AI Platform files were changed.
- No runtime behavior, rollout/config, or mobile UX was approved.

## Review Gate Decision - GO

**Decision:** GO.

**Artifact:**

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/review-gate.md
```

**Review evidence:**

- Expected spike files exist.
- Draft-only markers are present in README and OpenAPI delta.
- Forbidden scope scan shows no changes under backend, clients, accepted commands OpenAPI, or provider repo.
- Contract index entry is explicitly draft-only and non-binding.

## Gate D Decision - GO

**Decision:** GO for docs-only initiative closure.

**Implementation readiness:** **LIMITED-GO** for a separate backend contract implementation initiative.

**Artifact:**

```text
docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/gate-d.md
```

**Rationale:**

The spike delivered all expected contract artifacts without changing runtime/API/mobile/provider behavior. The contract direction is ready for a future backend implementation workpack, but provider intent quality and HomeTusk missing pending-confirmation runtime keep full product/mobile rollout out of scope.

**Next recommended action:**

Open a separate HomeTusk initiative:

```text
HomeTusk natural_command + needs_confirmation backend contract implementation
```

Non-goals for that next initiative unless separately gated: mobile UX, `answered`, broad planner actions, direct mobile/web AI Platform calls, production rollout, and AI Platform repo writes.
