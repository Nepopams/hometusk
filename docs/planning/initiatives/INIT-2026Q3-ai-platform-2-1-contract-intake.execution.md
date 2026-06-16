# INIT-2026Q3 AI Platform 2.1 Contract Intake Execution Notes

**Status:** Completed; Gate A GO; Gate B GO; artifact gate GO/HOLD split; Gate C GO for approved APPLY scope; Gate D GO for initiative closure
**Date:** 2026-06-15
**Initiative:** `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
**Roadmap:** `docs/planning/strategy/roadmap.md`
**Delegation:** Human gates for this initiative are delegated to Codex. Every GO / NO-GO / HOLD decision is recorded here with evidence, risks, and rationale.

---

## Intake Summary

| Field | Decision |
| --- | --- |
| Request type | HomeTusk planning pipeline for AI Platform 2.1 contract intake |
| Scope anchor | `INIT-2026Q3-ai-platform-2-1-contract-intake` |
| Current workflow step | `intake -> planning -> artifact gate -> workpack -> Codex PLAN -> delegated Gate C GO -> APPLY -> review gate -> delegated Gate D GO` |
| Change type | contract-change, backend integration, docs/process, AI safety, traceability |
| Work level | initiative-level implementation workpack |
| Primary boundary | HomeTusk consumes AI Platform 2.1.0; HomeTusk remains execution, guardrail, audit, runtime, mobile/API, and product authority |
| Runtime posture | Backend adapter APPLY completed within Gate C scope; no public runtime/API expansion |
| Public API posture | No public `/commands` contract change is approved by this initiative |

## Sources of Truth Read

### HomeTusk

| Artifact | Path |
| --- | --- |
| Active repo rules | `AGENTS.md`, `docs/planning/AGENTS.md` |
| Workflow | `docs/CODEX-WORKFLOW.md` |
| Product goal | `docs/planning/strategy/product-goal.md` |
| Roadmap | `docs/planning/strategy/roadmap.md` |
| MVP release scope | `docs/planning/releases/MVP.md` |
| DoR / DoD | `docs/_governance/dor.md`, `docs/_governance/dod.md` |
| Active initiative | `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md` |
| Parent artifact gate | `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`, `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.execution.md` |
| Provider acceptance review | `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`, `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.execution.md` |
| Current Commands contract | `docs/contracts/http/commands.openapi.yaml` |
| Contract index | `docs/_indexes/contracts-index.md` |
| Service catalog | `docs/architecture/service-catalog.md` |
| Current AI Platform integration docs | `docs/integration/ai-platform/v1/**` except `docs/integration/ai-platform/v1/upstream/**` treated as read-only |
| Backend adapter code | `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/**` |
| Decision log code | `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`, `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java` |
| AI Platform tests | `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java`, `services/backend/src/test/java/com/hometusk/integration/aiplatform/**` |

### AI Platform Read-Only Evidence

Provider repository path inspected read-only:

```text
C:/Users/user/Documents/projects/VR_AI_Platform
```

Provider revision inspected:

```text
5c2eb8c5fbdd75e5bc8d0a9d56333ee756354bb1
```

Provider files inspected:

- `contracts/schemas/command.schema.json`
- `contracts/schemas/decision.schema.json`
- `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-contract-and-50-scenario-eval.md`
- `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-contract-and-50-scenario-eval.execution.md`
- `docs/planning/workpacks/ST-052/local-50-scenario-eval-report.json`
- `docs/planning/workpacks/ST-056/hometusk-handoff.md`
- `graphs/core_graph.py`
- `routers/v2.py`

## Triage Classification

| Field | Value |
| --- | --- |
| `contract_impact` | yes-external-provider-snapshot; no public HomeTusk API expected |
| `backend_impact` | yes |
| `mobile_impact` | no |
| `ai_platform_impact` | no; provider repo read-only |
| `security_sensitive` | yes |
| `traceability_critical` | yes |
| `adr_needed` | no for planning; HOLD/reassess if APPLY requires architecture policy beyond documented mapping |
| `diagrams_needed` | no for planning; HOLD/reassess if APPLY changes integration flow materially |
| `cross_repo` | yes; AI Platform read-only evidence |

## Scope Boundary Preserved

In scope for the future APPLY:

- Import and document AI Platform `2.1.0` provider snapshot in HomeTusk-owned integration docs.
- Supersede stale HomeTusk-owned v1 mapping language where needed without editing `v1/upstream/**`.
- Update HomeTusk backend AI Platform adapter/schema validation/mapping/tests for `reject` and `confirm`.
- Send `reject` capability by default.
- Do not send `confirm` capability by default.
- Treat unexpected provider `confirm` as controlled non-execution: rejected with `AI_CONFIRMATION_UNSUPPORTED`.
- Preserve raw provider response payload for audit where possible and document public response limitations.
- Keep existing execute/clarify behavior stable.

Out of scope:

- HomeTusk `natural_command`.
- Public `/commands` contract change.
- `needs_confirmation`.
- `answered`.
- Mobile AI Command UX or confirmation/answer cards.
- Direct mobile/web to AI Platform calls.
- AI Platform repository edits.
- Production rollout/config changes.
- Provider `confirm` execution.
- Broadening provider evidence into product GO.

## Gate A Decision - GO

**Decision:** GO for making `INIT-2026Q3-ai-platform-2-1-contract-intake` the current HomeTusk roadmap initiative.

**Evidence:**

- The initiative is the direct successor to the closed HomeTusk provider acceptance review and the provider-side `2.1.0` handoff.
- Provider read-only evidence shows contract version `2.1.0`, 50 evaluated scenarios, 50 schema-valid decisions, 50 outcome matches, 0 unsupported auto-execute, 0 cross-household references, and 0 blocker failure scenarios.
- The initiative explicitly keeps HomeTusk `natural_command`, `needs_confirmation`, `answered`, mobile AI UX, direct mobile-to-AI-Platform calls, production rollout, and AI Platform edits out of scope.
- HomeTusk remains final execution, guardrail, audit, runtime, mobile/API, and product acceptance authority.

**Rationale:**

The roadmap can move from provider evidence review to HomeTusk contract intake because provider evidence is now sufficient as input, but not sufficient as HomeTusk product/runtime acceptance.

**Risks accepted:**

- Provider `confirm` is schema-supported but not HomeTusk-runtime-supported.
- HomeTusk currently cannot expose all provider reject/confirm metadata in the public `rejected` response.
- Current adapter code must be changed carefully so `confirm` can never execute.

## Gate B Decision - GO

**Decision:** GO for one implementation-ready workpack:

```text
docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/
```

**Committed scope:**

- Versioned AI Platform `v2.1` integration docs and provider snapshot.
- HomeTusk-owned v1 mapping supersession notes for stale drift.
- Backend adapter compatibility for provider `2.1.0`.
- `reject` and `confirm` safe mapping.
- Capability negotiation policy.
- Backend fixtures/tests for execute, clarify, reject, confirm, unknown/invalid, failure/degraded, and raw payload traceability.
- Service catalog and contract index updates if the snapshot/index changes.
- Roadmap/planning artifacts.

**DoR evidence:**

- Title, description, owner, target milestone, sources, acceptance criteria, files, flags, risks, and explicit anti-scope are present in the initiative.
- Exact current-state code files and test files were inspected before creating the workpack.
- Provider evidence is available read-only and the provider repo is clean.

**Rationale:**

The work is small enough for one focused backend/integration APPLY while still preserving contract-first gates and explicit stop conditions.

## Artifact Gate Decision

### Contract Gate

**Decision:** GO for external provider snapshot and HomeTusk-owned integration mapping docs; HOLD for public HomeTusk API contract changes.

**Evidence:**

- Provider `command.schema.json` and `decision.schema.json` support `reject`, `confirm`, and optional `decision_outcome` under `2.1.0`.
- HomeTusk public `/commands` response can already return `status=rejected` with `errorCode` and `reason`.
- Extra provider metadata can be preserved in `DecisionLog.rawDecisionPayload`; it does not need a public response schema change in this initiative.

**Rationale:**

This initiative is a consumer adapter intake, not a public HomeTusk API expansion. Any `needs_confirmation`, `answered`, or `natural_command` contract must remain a separate initiative.

### ADR / Diagram Gate

**Decision:** NO ARTIFACT CHANGE REQUIRED for planning; HOLD/reassess during APPLY if integration flow or service boundary changes materially.

**Evidence:**

- Default mapping posture is already captured in the initiative.
- The expected APPLY changes adapter behavior within the existing command pipeline and AI Platform dependency boundary.

### Security / Traceability Gate

**Decision:** GO with required tests/evidence.

**Required evidence in APPLY:**

- `reject` and `confirm` produce no domain mutation.
- `confirm` never executes even if provider emits it unexpectedly.
- `DecisionLog.rawDecisionPayload` contains provider trace/version/decision fields for reject/confirm.
- No extra sensitive provider payload is logged beyond existing DecisionLog policy.

## Codex PLAN Findings

Detailed findings are recorded in:

```text
docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/plan-findings.md
```

Key findings:

- `AiDecisionRequest.DEFAULT_CAPABILITIES` currently sends `start_job`, `propose_create_task`, `propose_add_shopping_item`, and `clarify`; it does not send `reject` or `confirm`.
- `services/backend/src/main/resources/schemas/ai-decision-response.schema.json` currently accepts only `start_job`, `propose_create_task`, `propose_add_shopping_item`, and `clarify`.
- `AiDecisionResponseMapper` currently has no `reject` or `confirm` branch.
- `AiDecisionResponse` currently has no `decision_outcome` field.
- `AiPlatformClient` currently deserializes directly to `AiDecisionResponse`, so exact raw provider JSON is not preserved before DTO mapping.
- `CommandService.handleReject` already maps `DecisionResult.Reject` to public `status=rejected`, writes `DecisionLog`, and does not execute actions.
- Public rejected response carries only `errorCode` and `reason`; provider code/reason/ui/trace/version details should be preserved in `DecisionLog`.

## Gate C Decision - GO

**Decision:** GO for a future APPLY limited to the approved file list in `gate-c.md` and `plan-findings.md`.

**Evidence:**

- PLAN is decision-complete and names exact current files/classes to inspect/change.
- Confirm strategy is explicitly A + C:
  - do not advertise `confirm` capability by default;
  - if `confirm` still arrives, map to `rejected / AI_CONFIRMATION_UNSUPPORTED`;
  - preserve provider raw payload in `DecisionLog`;
  - defer `needs_confirmation` to a future contract initiative.
- `reject` strategy is explicit:
  - advertise `reject`;
  - map provider `reject` to `DecisionResult.Reject`;
  - no action execution;
  - preserve provider metadata in `DecisionLog`.
- STOP conditions are explicit.

**Rationale:**

Human Gate C is delegated. The future APPLY scope is bounded to HomeTusk-owned planning/docs/backend/test files and does not approve public HomeTusk API, mobile, AI Platform, production rollout, or natural-command runtime changes.

## APPLY Evidence

**Scope completed:**

- HomeTusk-owned AI Platform `2.1.0` integration docs and provider snapshot.
- HomeTusk-owned v1 AI Platform mapping supersession notes.
- Backend AI Platform request capability update: advertise `reject`, do not advertise `confirm`.
- Backend response DTO/schema/client/provider/mapper compatibility for provider `reject`, `confirm`, and optional `decision_outcome`.
- Raw provider payload validation before DTO mapping.
- Safe malformed non-JSON provider output handling: `AI_RESPONSE_INVALID`, no fallback mutation, auditable JSON wrapper in `DecisionLog.rawDecisionPayload`.
- Backend fixtures and tests for provider `2.1.0`.
- Service catalog, contract index, roadmap, workpack, checklist, review gate, and Gate D evidence.

**Out-of-scope preserved:**

- No public `/commands` contract change.
- No HomeTusk `natural_command`.
- No `needs_confirmation`.
- No `answered`.
- No mobile/web UX changes.
- No direct mobile/web to AI Platform.
- No AI Platform repo writes.
- No production rollout/config change.

## Review Gate Decision - GO

**Decision:** GO.

**Artifact:**

```text
docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/review-gate.md
```

**Review evidence:**

- No remaining Must-fix findings.
- One pre-Gate-D safety gap was found and fixed before GO: raw provider responses are now schema-validated before DTO parsing, and malformed non-JSON provider output rejects safely without fallback execution.
- Diff stayed inside approved Gate C files.
- Forbidden scope scans passed for `docs/integration/ai-platform/v1/upstream/**`, `clients/**`, and `docs/contracts/**`.

## Gate D Decision - GO

**Decision:** GO for initiative closure.

**Artifact:**

```text
docs/planning/workpacks/INIT-2026Q3-AI-PLATFORM-2-1-CONTRACT-INTAKE/gate-d.md
```

**Rationale:**

The APPLY completed the AI Platform `2.1.0` contract intake inside the approved Gate C boundary. HomeTusk can now safely consume provider `reject` and understand provider `confirm` as a controlled non-executing rejection. Provider evidence remains input only; this is not product/runtime/mobile GO.

**Final recommendation:**

LIMITED-GO for a separate HomeTusk contract spike covering `natural_command` and `needs_confirmation`. Do not start runtime/mobile UX or production rollout from this initiative.

## Planning Verification

| Check | Result |
| --- | --- |
| `git diff --check` | Passed; reported LF-to-CRLF warnings only for existing modified markdown files. |
| Trailing whitespace scan for new execution/workpack files | Passed. |
| `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short` | Passed; provider repo remained clean/read-only. |
| Gate marker scan for Gate A/B/C/D and `AI_CONFIRMATION_UNSUPPORTED` | Passed. |
| Workpack artifact existence check | Passed for `workpack.md`, `plan-findings.md`, and `gate-c.md`. |

## APPLY Verification

| Check | Result |
| --- | --- |
| `git diff --check` | Passed; LF-to-CRLF warnings only. |
| JSON parse for v2.1 docs/test fixtures | Passed; 14 JSON files. |
| Provider snapshot JSON-equivalence | Passed for `command.schema.json` and `decision.schema.json`. |
| Focused adapter tests | Passed. |
| Focused AI Platform integration tests | Passed. |
| Full backend Gradle test suite | Passed: 496 tests, 0 failures, 0 errors, 5 skipped. |
| `spotlessCheck` | Passed. |
| `./scripts/test.sh` | Blocked by missing WSL `/bin/bash`; equivalent full Gradle test command passed. |
| Provider repo status | Passed; no changes. |
| Forbidden scope scan | Passed; no changes under v1 upstream, clients, or public contracts. |

## Next Recommended Action

Open a separate HomeTusk contract spike for:

```text
natural_command + needs_confirmation
```

This next initiative must run through contract governance before any runtime,
mobile UX, `answered`, direct mobile/web AI Platform calls, or production rollout.
