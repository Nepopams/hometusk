# Domain Planner v1 Artifact Gate

Status: Accepted artifact gate package, 2026-06-15

Initiative: `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`

Recommendation: **LIMITED-GO**

## Purpose

This package closes the HomeTusk-owned artifact gap before any AI Platform
Domain Planner v1, HomeTusk `natural_command`, or Mobile AI Command UX APPLY
work.

It turns the audit baseline into consumable planning artifacts:

- accepted narrow decision/action taxonomy;
- draft natural-command contract direction;
- machine-readable seed golden scenarios;
- deterministic eval rubric;
- privacy and retention questions;
- future mobile AI state matrix;
- provider readiness checklist;
- HomeTusk integration-doc drift summary;
- provider initiative brief.

## Gate Posture

| Gate | Decision | Evidence | Rationale |
| --- | --- | --- | --- |
| Gate A | GO | Roadmap selected `INIT-2026Q3-ai-command-artifact-gate` as the NOW focus for this gate. | Initiative is docs-only, source-backed, and bounded by LIMITED-GO. |
| Gate B | GO | Expected artifact package is explicit in the initiative. | Committed scope is one artifact package, no runtime or contract mutation. |
| Gate C | GO | PLAN scope is limited to files under this package plus initiative execution notes. | Human gates are delegated; APPLY remains docs-only and reversible. |
| Gate D | GO | All expected files exist and no production/runtime/API/mobile/upstream files are changed by this gate package. | The provider-side follow-up can start from these artifacts, but implementation is still blocked. |

## Accepted

- The canonical decision outcomes for target planning are `execute`, `clarify`,
  `confirm`, `reject`, and `answer`.
- The narrow v0 auto-execute corridor is limited to `create_task` and
  `add_shopping_items`.
- Confidence is an audit signal, not execution permission.
- HomeTusk remains validation, execution, audit, and product acceptance
  authority.
- AI Platform remains a planner/proposer and must not mutate HomeTusk state.
- Mobile and web clients must call HomeTusk, not AI Platform directly.
- The first 10 golden scenarios are a seed set only.

## Draft Only

- `natural-command-contract-v0-draft.md` is not an OpenAPI or runtime contract.
- `confirm` and `answer` are target outcomes, not implemented response variants.
- The mobile state matrix describes future UX states, not approved mobile work.
- Provider outputs in this package are contract direction, not upstream schema
  changes.

## Blocked

- HomeTusk `natural_command` implementation.
- Backend response types `needs_confirmation` and `answered`.
- Mobile AI Command UX v1.
- Direct mobile-to-AI-Platform calls.
- AI Platform Domain Planner v1 implementation.
- Mixed task/shopping autonomous planning.
- Production multi-agent planner.

## Next Provider Initiative May Consume

- `decision-action-taxonomy-accepted-v0.md`
- `golden-scenarios-fixtures-v0/golden-scenarios-v0.yaml`
- `golden-scenarios-fixtures-v0/context-fixtures-v0.yaml`
- `eval-rubric-v0.md`
- `privacy-and-retention-questions.md`
- `provider-planner-readiness-checklist.md`
- `provider-initiative-brief.md`

## Must Not Be Implemented Yet

Do not use this package as approval to:

- modify `docs/contracts/**`;
- modify `docs/integration/ai-platform/v1/upstream/**`;
- add Java/TypeScript runtime behavior;
- add `natural_command` to public APIs;
- add mobile AI cards;
- broaden LIMITED-GO to GO.

## Artifact Index

| Artifact | Status | Purpose |
| --- | --- | --- |
| `decision-action-taxonomy-accepted-v0.md` | Accepted for narrow v0 | Decision/action taxonomy and trust corridor. |
| `natural-command-contract-v0-draft.md` | Draft | Future request/decision contract direction. |
| `golden-scenarios-fixtures-v0/` | Seed fixtures | Machine-readable product acceptance scenarios. |
| `eval-rubric-v0.md` | Accepted gate rubric | Deterministic and manual eval checks. |
| `privacy-and-retention-questions.md` | Required questions | Data boundary and provider governance posture. |
| `mobile-ai-state-matrix-v0.md` | Future-state matrix | Backend-dependent mobile UX states. |
| `provider-planner-readiness-checklist.md` | Provider gate | Minimum AI Platform Domain Planner v1 readiness. |
| `hometusk-ai-platform-integration-doc-drift.md` | Drift summary | Known HomeTusk integration documentation drift. |
| `provider-initiative-brief.md` | Handoff brief | Concise provider initiative input. |
