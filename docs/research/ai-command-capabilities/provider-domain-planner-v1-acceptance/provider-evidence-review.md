# Provider Evidence Review

Status: Completed, 2026-06-15

Decision: **LIMITED-GO**

## Scope Reviewed

Provider repository was inspected read-only:

```text
C:/Users/user/Documents/projects/VR_AI_Platform
```

Revision inspected:

```text
b1ca7235dfacb1faee35e042d6a072976c640d35
```

The review covers provider closure evidence only. It does not approve HomeTusk
runtime, API, mobile, contract, or production rollout work.

## Provider Closure Summary

Provider initiative:
`docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.md`

Provider status:

- Done, provider-side closure complete.
- HomeTusk acceptance explicitly separate.
- Narrow corridor preserved.
- HomeTusk files were not modified or copied into the provider repo.

Provider handoff:
`docs/planning/epics/EP-016/domain-planner-v1-closure-handoff.md`

Provider result:

- simple `create_task` provider decisions are schema-valid;
- multi-item shopping decisions are represented as repeated
  `propose_add_shopping_item` actions;
- missing, ambiguous, contextual, confirm-required, unsupported, unsafe, or
  impossible scenarios produce non-executing clarify or safe rejection mapping;
- ASR remains transcription-only;
- `/v1/decide` remains the provider entrypoint.

## Gate Evidence

| Gate / story | Provider evidence | HomeTusk interpretation |
| --- | --- | --- |
| ST-048 | `docs/planning/workpacks/ST-048/review-report.md` | Provider artifact gate GO; no runtime approval by itself. |
| ST-049 | `docs/planning/workpacks/ST-049/review-report.md` | Eval runner GO; early baseline still had blocker failures before ST-050. |
| ST-050 | `docs/planning/workpacks/ST-050/review-report.md` | Runtime adaptation GO inside current-schema boundary. |
| ST-051 | `docs/planning/workpacks/ST-051/review-report.md` | Provider closure GO; HomeTusk acceptance remains separate. |
| Execution notes | `docs/planning/initiatives/INIT-2026Q3-domain-planner-v1-narrow-household-command-corridor.execution.md` | Provider gates recorded with evidence and stop conditions. |

## Runtime Changes and Non-Changes

Provider runtime files reviewed as evidence:

- `graphs/core_graph.py`
- `routers/v2.py`
- `tests/test_domain_planner_v1_corridor.py`

Observed provider behavior:

- shopping item splitting supports comma and conjunction separators;
- shopping list grounding clarifies when no default list exists;
- unsafe broad/bulk assignment maps to non-executing current-schema rejection;
- mixed task/shopping and contextual shopping clarify instead of executing;
- `/v1/asr/transcribe` does not call the decision service.

Important non-changes:

- no HomeTusk runtime files changed;
- no provider `contracts/**` schema/version/public API change;
- no first-class `reject`, `confirm`, or `answer`;
- no direct plural `add_shopping_items` action enum;
- no direct HomeTusk mutation;
- no direct mobile/web integration;
- no production rollout/config change.

## Eval Evidence

Provider eval report:
`docs/planning/workpacks/ST-049/local-seed-eval-report.json`

Provider seed eval summary after ST-050/ST-051:

| Metric | Value |
| --- | --- |
| Fixture versions | `golden-scenarios-v0`, `golden-context-v0` |
| HomeTusk source revision recorded by provider | `d924c631c80895995c65f22bec6f77dc0a0347b7` |
| Total scenarios | 10 |
| Evaluated scenarios | 10 |
| Schema-valid decisions | 10 |
| Outcome matches | 10 |
| Intent matches | 3 |
| Unsupported auto-execute | 0 |
| Cross-household references | 0 |
| Blocker failure scenarios | 0 |
| Non-blocker buckets | `wrong_intent=7`, `item_boundary_loss=2` |

HomeTusk interpretation:

- Safety gates are promising: no unsupported auto-execute, cross-household, or
  blocker failures in the 10-scenario seed suite.
- The evidence is not enough for full product GO because the suite is small and
  intent/item-boundary non-blockers remain visible.
- The expanded 50-scenario HomeTusk suite in this package must be run before any
  runtime acceptance claim.

## Contract / Schema Review

Provider schema facts:

- `CommandDTO.capabilities` supports `start_job`,
  `propose_create_task`, `propose_add_shopping_item`, and `clarify`.
- `DecisionDTO.action` supports `start_job`, `propose_create_task`,
  `propose_add_shopping_item`, and `clarify`.
- `DecisionDTO.status` supports `ok`, `clarify`, and `error`.
- `start_job.payload.proposed_actions[]` supports repeated
  `propose_add_shopping_item` actions.
- Provider schema has no first-class `reject`, `confirm`, `answer`, or direct
  plural `add_shopping_items`.

HomeTusk interpretation:

- Current-schema repeated singular shopping actions are acceptable for the
  narrow provider corridor.
- `reject_mapped_to_error` is acceptable only as provider evidence of safe
  no-execute behavior; it is not acceptable as a HomeTusk product/runtime
  contract.
- `confirm` is required before non-requester assignment, reschedule,
  task-shopping linkage, batch planning, or broad workload redistribution.
- `answer` is blocked until HomeTusk has a grounded read-only answer contract.

## Privacy / Retention Review

Provider privacy posture:
`docs/guides/domain-planner-v1-privacy-retention.md`

Evidence:

- raw audio is outside `/v1/decide`;
- ASR remains transcription-only;
- raw command text logging is opt-in through `LOG_USER_TEXT`, default false;
- provider planning/review/eval artifacts avoid raw HomeTusk scenario text;
- retention period, zero-data-retention behavior, region, access policy,
  deletion workflow, and external LLM training-use posture remain HOLD items.

HomeTusk interpretation:

- Privacy posture is sufficient for provider evidence review.
- Production prompt/response retention remains unresolved for any future
  external LLM or raw text retention path.

## ASR Boundary

Provider evidence includes a regression test proving `/v1/asr/transcribe` does
not call the decision service.

HomeTusk interpretation:

- This preserves the HomeTusk rule that voice is input capture only.
- Voice transcript still requires user review before any command execution.

## HomeTusk Non-Impact Evidence

Provider closure states:

- no HomeTusk files were edited or copied;
- no HomeTusk backend/mobile/OpenAPI/runtime/integration changes were made;
- no direct HomeTusk state mutation was introduced;
- no direct mobile/web call path to AI Platform was introduced.

HomeTusk review of this initiative also kept changes docs-only in the HomeTusk
repository.

## Review Conclusion

Provider evidence is useful and safety-positive, but not sufficient for full
HomeTusk product acceptance.

Decision:

```text
LIMITED-GO
```

Accepted only for a narrower next step: provider contract/eval follow-up and
HomeTusk contract discovery, with runtime/mobile/API work still blocked.
