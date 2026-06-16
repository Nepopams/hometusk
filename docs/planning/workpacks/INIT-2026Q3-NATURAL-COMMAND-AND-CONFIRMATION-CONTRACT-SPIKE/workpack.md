# Workpack: INIT-2026Q3 Natural Command & Confirmation Contract Spike

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- Execution notes: `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- MVP release scope: `docs/planning/releases/MVP.md`
- Workflow: `AGENTS.md`, `docs/CODEX-WORKFLOW.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Current Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- AI Platform v2.1 integration: `docs/integration/ai-platform/v2.1/**`
- Prior research: `docs/research/ai-command-capabilities/**`
- Current backend command models, DecisionLog, guardrails, and mobile command shell inspected read-only.
- Provider read-only source: `C:/Users/user/Documents/projects/VR_AI_Platform`
- Provider handoff: `C:/Users/user/Documents/projects/VR_AI_Platform/docs/planning/workpacks/ST-056/hometusk-handoff.md`
- Provider eval: `C:/Users/user/Documents/projects/VR_AI_Platform/docs/planning/workpacks/ST-052/local-50-scenario-eval-report.json`

## Status

**DONE - GATE D GO.** Gate A/B/C/D are delegated to Codex and recorded in the execution notes. Docs-only APPLY and review evidence are complete as of 2026-06-16.

## Objective

Produce implementation-ready draft contract artifacts for future HomeTusk `natural_command` and `needs_confirmation` runtime work without changing runtime behavior, accepted public OpenAPI, backend, mobile, or AI Platform repositories.

Target result:

```text
natural_command request draft
+ command response outcome draft
+ needs_confirmation response draft
+ confirmation lifecycle
+ provider confirm mapping
+ guardrails policy
+ DecisionLog traceability requirements
+ mobile state dependencies
+ non-binding OpenAPI delta
+ implementation readiness decision
```

## In Scope

- Update roadmap to make this initiative the closed NOW result.
- Record delegated Gate A/B/artifact/C/review/D decisions.
- Create a workpack package with checklist, PLAN prompt, findings, Gate C, APPLY prompt, review gate, and Gate D.
- Create all expected draft contract spike files under `docs/research/ai-command-capabilities/natural-command-contract-spike/`.
- Add a draft-only contract index entry so future implementers can find the spike without treating it as accepted API.

## Out of Scope

- Java/backend behavior change.
- Database migration.
- Accepted public OpenAPI change.
- `natural_command` runtime execution.
- `needs_confirmation` public runtime implementation.
- Confirmation approve/cancel endpoint implementation.
- `answered` response.
- Mobile/web UI.
- Direct mobile/web calls to AI Platform.
- AI Platform repo changes.
- Production rollout/config changes.

## Impact Flags

| Flag | Value | Notes |
| --- | --- | --- |
| `contract_impact` | draft-only | Non-binding OpenAPI delta; accepted commands OpenAPI unchanged |
| `backend_impact` | no now | Future implementation recommended separately |
| `mobile_impact` | no now | Dependencies only |
| `ai_platform_impact` | no | Provider repo read-only |
| `security_sensitive` | yes | Confirmation, authz, no-mutation approval path |
| `traceability_critical` | yes | DecisionLog and confirmation lifecycle evidence |
| `adr_needed` | no for spike; maybe for runtime | Pending confirmation ownership may need future ADR |
| `diagrams_needed` | no for spike; maybe for runtime | Lifecycle is documented as text |
| `cross_repo` | yes | Provider evidence read-only |

## Acceptance Criteria

- [x] Natural command request draft exists.
- [x] Response outcome model draft exists.
- [x] `needs_confirmation` contract draft exists.
- [x] Confirmation lifecycle model exists.
- [x] Provider confirm mapping rules exist.
- [x] Guardrails policy matrix exists.
- [x] Date/time normalization policy is addressed.
- [x] DecisionLog/traceability requirements are addressed.
- [x] Mobile state dependencies are addressed.
- [x] OpenAPI delta draft exists and is clearly non-binding.
- [x] Final implementation readiness decision is recorded.
- [x] No runtime/backend/mobile/AI Platform implementation is made.
- [x] Next recommended initiative is explicit.

## Files Changed / Created

Planning:

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/workpack.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/checklist.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/prompt-plan.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/plan-findings.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/gate-c.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/prompt-apply.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/review-gate.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/gate-d.md`

Research / draft contract package:

- `docs/research/ai-command-capabilities/natural-command-contract-spike/README.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/natural-command-request-contract-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/command-response-outcomes-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/needs-confirmation-contract-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/confirmation-lifecycle-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/provider-confirm-mapping-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/guardrails-policy-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/decisionlog-traceability-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/mobile-state-contract-dependencies-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/openapi-delta-draft.yaml`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/implementation-readiness-decision.md`

Index:

- `docs/_indexes/contracts-index.md`

Forbidden files preserved:

- `docs/contracts/http/commands.openapi.yaml`
- `services/backend/src/main/java/**`
- `services/backend/src/main/resources/db/migration/**`
- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`

## Implementation Plan

### Commit 1 - Planning and gates

Steps:

1. Update roadmap and initiative status.
2. Create execution notes.
3. Create workpack/checklist/PLAN findings/Gate C/APPLY/review/Gate D planning artifacts.

Verification:

- `rg -n "Gate A|Gate B|Gate C|Gate D|LIMITED-GO" docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE`

### Commit 2 - Draft contract spike package

Steps:

1. Create all expected research files.
2. Mark README and OpenAPI delta as draft-only / non-binding.
3. Define request, response, confirmation, lifecycle, mapping, guardrails, traceability, mobile dependencies, and final readiness decision.

Verification:

- `rg -n "DRAFT ONLY|Draft only|No runtime|No mobile|LIMITED-GO" docs/research/ai-command-capabilities/natural-command-contract-spike`
- Parse `openapi-delta-draft.yaml`.

### Commit 3 - Index and review evidence

Steps:

1. Add draft-only contract index entry.
2. Run forbidden scope scans.
3. Record review gate and Gate D.

Verification:

- `git diff --check`
- `git status --short`
- forbidden scope scan for `docs/contracts/http/commands.openapi.yaml`, `services/backend`, `clients`, and provider repo.

## Contract Impact

- Provider: HomeTusk public Commands API as future consumer-facing surface.
- Consumer: Web/native mobile clients through HomeTusk only.
- External provider: AI Platform `2.1.0` as decision provider.
- Current accepted HomeTusk public API: unchanged.
- Draft delta: future `type=natural_command`, future `needs_confirmation`, possible future approve/cancel endpoints.
- Compatibility: additive direction; existing `create_task`, `complete_task`, `needs_input`, `rejected`, `executed`, `scheduled`, and `executed_degraded` remain compatible.
- Breaking change: none in this initiative.

## Tests / Checks

- [x] Planning artifact existence and gate marker scan.
- [x] Draft package existence scan.
- [x] OpenAPI delta YAML parse.
- [x] Trailing whitespace scan for new files.
- [x] Forbidden scope scan.
- [x] Provider repo status read-only check.
- [x] `git diff --check`.

## Verification Commands

- `rg -n "Gate A|Gate B|Gate C|Gate D|LIMITED-GO" docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE` - expected: gate markers present.
- `rg -n "DRAFT ONLY|Draft only|No runtime|No mobile|LIMITED-GO" docs/research/ai-command-capabilities/natural-command-contract-spike` - expected: draft and non-scope markers present.
- YAML parse for `openapi-delta-draft.yaml` - expected: parse succeeds.
- `git diff --check` - expected: pass.
- `git -C C:/Users/user/Documents/projects/VR_AI_Platform status --short` - expected: no output.

## DoD Checklist

- [x] Expected planning artifacts created.
- [x] Expected contract spike artifacts created.
- [x] Draft-only markers are explicit.
- [x] Accepted public OpenAPI unchanged.
- [x] Runtime/backend/mobile/provider files unchanged.
- [x] Gate A/B/C/D decisions recorded with evidence, risks, and rationale.
- [x] Next recommended initiative explicit.

## Risks

| Risk | Mitigation |
| --- | --- |
| Draft is mistaken for accepted API | Mark README/OpenAPI/index as draft-only and non-binding |
| Confirmation runtime starts without state ownership | Require explicit pending confirmation model and Gate C in next initiative |
| Provider `confirm` treated as execute | Mapping doc requires no mutation before approval |
| `needs_input` overloaded for confirmation | Draft response outcome model forbids overloading |
| Mobile UX starts too early | Mobile dependency doc states no implementation until backend contract is accepted and implemented |
| `answer` scope creep | Final decision keeps `answer` blocked |

## Rollback

- Revert docs-only commits from this workpack.
- No database migration, runtime behavior, accepted public API, mobile client, or provider repo rollback is required.

## Prompt Pack

- PLAN: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/plan-findings.md`
- Gate C: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/gate-c.md`
- APPLY: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/prompt-apply.md`
- Review gate: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/review-gate.md`
- Gate D: `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-AND-CONFIRMATION-CONTRACT-SPIKE/gate-d.md`
