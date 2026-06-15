# Provider Planner Readiness Checklist

Status: Required checklist for future AI Platform Domain Planner v1

Date: 2026-06-15

Future provider initiative:
`AI Platform Domain Planner v1 - Narrow Household Command Corridor`

Repository owner: `vr_ai_platform`

## Required Before Provider Gate C

- [ ] Consume HomeTusk golden fixtures from this artifact package.
- [ ] Preserve schema validation at request and response boundaries.
- [ ] Map provider output to accepted HomeTusk taxonomy:
  `execute`, `clarify`, `reject`, and optionally non-executing `confirm`.
- [ ] Support simple `create_task` planning.
- [ ] Support multi-item `add_shopping_items` planning.
- [ ] Preserve item boundaries for shopping commands.
- [ ] Propose date/time with timezone or clarify.
- [ ] Clarify when member, zone, list, task, or date is ambiguous.
- [ ] Reject unsupported, unsafe, cross-household, or unverifiable requests.
- [ ] Avoid direct mutation; HomeTusk executes only after validation.
- [ ] Provide planner version, decision version, schema version, decision id,
  and trace id.
- [ ] Expose deterministic eval output for HomeTusk acceptance.
- [ ] Document privacy/retention posture for prompt and response data.

## Optional for Narrow v1

- [ ] Emit `confirm` as a non-executing outcome for risky but understood plans.
- [ ] Include alternatives for audit without using them as execution permission.
- [ ] Emit normalized date/time confidence with explicit ambiguity flags.

## Must Not Be Included in Provider v1

- [ ] Broad multi-agent production planner.
- [ ] Direct HomeTusk data mutation.
- [ ] Direct client/mobile integration.
- [ ] Full household workload optimization.
- [ ] Natural reschedule auto-execute.
- [ ] Natural completion auto-execute.
- [ ] Read-only answer implementation without HomeTusk answer contract.
- [ ] Prompt-only behavior with no schema/eval gate.

## Required Eval Evidence

Provider must produce:

- scenario-by-scenario result file;
- aggregate pass/fail metrics;
- failure bucket counts;
- list of unsupported scenarios;
- exact fixture source path/version;
- planner version;
- decision version;
- prompt/model version if applicable;
- run command and environment flags;
- zero blocker failures for seed scenarios.

## HomeTusk Acceptance Gates

| Gate | Required evidence |
| --- | --- |
| Provider Gate A | Initiative scope references this artifact package and keeps LIMITED-GO boundaries. |
| Provider Gate B | Committed provider scope excludes broad planner/mobile/direct mutation. |
| Provider Gate C | Read-only PLAN names exact provider files, eval commands, and schema impact. |
| Provider Gate D | Review evidence shows fixtures pass, unsafe/cross-household cases do not execute, and retention questions are answered or explicitly blocked. |

## Handoff Inputs

- `decision-action-taxonomy-accepted-v0.md`
- `natural-command-contract-v0-draft.md`
- `golden-scenarios-fixtures-v0/`
- `eval-rubric-v0.md`
- `privacy-and-retention-questions.md`
- `hometusk-ai-platform-integration-doc-drift.md`
