# AI Provider Domain Planner v1 Acceptance Review

Status: Completed HomeTusk acceptance review, 2026-06-15

Initiative:
`docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`

Execution log:
`docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.execution.md`

Final decision: **LIMITED-GO**

## Purpose

This package records the HomeTusk-side product acceptance review for the
provider-side AI Platform Domain Planner v1 narrow household command corridor.

Provider closure is treated as evidence, not automatic HomeTusk product
acceptance.

## Decision Summary

HomeTusk accepts the provider evidence only as sufficient input for a narrower
next step:

1. Run the expanded HomeTusk product scenario suite against the provider.
2. Open provider contract governance for first-class `reject` and non-executing
   `confirm` before HomeTusk runtime integration.
3. Keep `answer` and status/query semantics blocked until a HomeTusk read-only
   answer contract exists.
4. Keep HomeTusk `natural_command`, Mobile AI Command UX, OpenAPI/backend/mobile
   APPLY, production rollout, and direct mobile-to-AI-Platform calls blocked.

## Artifact Index

| Artifact | Purpose |
| --- | --- |
| `provider-evidence-review.md` | Narrative review of provider closure evidence, tests, evals, contracts, privacy, and non-impact claims. |
| `provider-eval-evidence-index.md` | HomeTusk-owned index of provider evidence with trust level, sufficiency, and unresolved questions. |
| `expanded-golden-scenarios-v1/` | Product-owned 50-scenario machine-readable suite for future provider acceptance. |
| `reject-confirm-answer-contract-posture.md` | Contract posture decision for `reject`, `confirm`, `answer`, and plural shopping semantics. |
| `natural-command-readiness-decision.md` | Readiness decision for future HomeTusk `natural_command` work. |
| `recommendation.md` | Final LIMITED-GO recommendation and next action. |

## Sources of Truth

HomeTusk:

- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/initiatives/INIT-2026Q3-ai-provider-domain-planner-v1-acceptance-review.md`
- `docs/research/ai-command-capabilities/domain-planner-v1-gate/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/integration/ai-platform/**`

Provider read-only:

- Repository: `C:/Users/user/Documents/projects/VR_AI_Platform`
- Revision inspected: `b1ca7235dfacb1faee35e042d6a072976c640d35`
- Evidence listed in `provider-eval-evidence-index.md`

## What Is Accepted

- Provider-side evidence is strong enough to continue with a gated, narrow
  follow-up.
- The provider preserved `/v1/decide` as the entrypoint.
- The provider kept ASR transcription-only.
- Provider seed eval reports 10/10 schema-valid decisions, 10/10 outcome
  matches, 0 unsupported auto-execute cases, 0 cross-household references, and
  0 blocker failure scenarios.
- Repeated `propose_add_shopping_item` actions are acceptable as the current
  provider representation for multi-item shopping, if HomeTusk validation
  preserves item boundaries.

## What Remains Blocked

- Full HomeTusk product GO.
- HomeTusk `natural_command` runtime implementation.
- Mobile AI Command UX.
- OpenAPI/backend/mobile changes.
- Provider schema changes from this repository.
- Direct mobile/web calls to AI Platform.
- Production rollout.
- `confirm` or `answer` runtime states.

## Residual Risks

- Provider seed coverage is only 10 scenarios; this package expands HomeTusk
  ownership to 50 scenarios but does not prove provider pass on those scenarios.
- `reject` is still represented by current-schema safe error/clarify mapping.
- Provider has no first-class `confirm` or `answer`.
- Remaining non-blocker provider buckets are visible: `wrong_intent=7` and
  `item_boundary_loss=2`.
- Production prompt/response retention remains HOLD if an external LLM or raw
  text retention is introduced.

## Next Recommended Action

Create a provider-side follow-up workpack to:

- consume `expanded-golden-scenarios-v1`;
- run deterministic eval against all 50 scenarios;
- decide first-class `reject` and non-executing `confirm` contract changes;
- keep `answer` blocked until HomeTusk answer contract governance exists.
