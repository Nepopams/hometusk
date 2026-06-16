# Gate D: Natural Command & Confirmation Contract Spike

## Decision

GO for docs-only initiative closure.

Implementation readiness decision: **LIMITED-GO**.

## Decider

Codex, under delegated human-gate authority from the user goal.

## Date

2026-06-16

## Rationale

The APPLY delivered the full draft contract spike package inside Gate C scope without changing runtime/backend/mobile/provider behavior or accepted public contracts.

The result is strong enough to start a separate backend contract implementation initiative, but not strong enough for product/runtime/mobile rollout. Provider evidence still has non-blocker intent mismatch buckets, HomeTusk still needs explicit pending confirmation state, and `answer` remains blocked.

## Evidence

- Roadmap now anchors the closed spike and recommends the next backend contract implementation initiative.
- Execution notes record Gate A/B/artifact/C/review/D.
- Workpack/checklist/prompt-plan/plan-findings/gate-c/prompt-apply/review-gate exist.
- Draft research package exists with all expected files.
- Draft OpenAPI delta is clearly non-binding.
- Contract index marks the spike as draft-only.
- No accepted `docs/contracts/http/commands.openapi.yaml` change.
- No backend Java, migration, mobile/web, or provider repo changes.

## Verification

| Check | Result |
| --- | --- |
| Gate marker scan | PASS |
| Draft marker scan | PASS |
| YAML parse for `openapi-delta-draft.yaml` | PASS |
| Trailing whitespace scan for new files | PASS |
| Forbidden scope scan | PASS |
| Provider repo status | PASS |
| `git diff --check` | PASS |

## Residual Risks

- Future runtime needs a durable pending confirmation model; `DecisionLog` is not sufficient as source of truth.
- Provider `confirm` is schema-supported but observed provider eval still often clarifies confirmation-like scenarios.
- Provider intent matching has non-blocker gaps (`wrong_intent=30`, `item_boundary_loss=2`).
- Mobile AI UX remains blocked until backend contract implementation and review.
- `answered` remains blocked until a separate read-only answer contract initiative.

## Next Recommended Action

Open a separate HomeTusk initiative:

```text
HomeTusk natural_command + needs_confirmation backend contract implementation
```

Recommended non-goals for that next initiative:

- no mobile/web UI;
- no `answered`;
- no broad planner actions;
- no direct mobile/web AI Platform calls;
- no production rollout/config;
- no AI Platform repo writes.
