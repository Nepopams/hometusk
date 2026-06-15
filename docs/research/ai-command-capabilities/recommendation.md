# Recommendation

Status: Initial recommendation, 2026-06-15

## Decision

**LIMITED-GO**

Proceed only to a narrow, contract-first AI Platform Domain Planner v1 initiative
for simple task creation and shopping item addition.

Do not proceed yet to:

- broad natural command execution;
- HomeTusk `natural_command` implementation;
- Mobile AI Command Center;
- mixed task/shopping autonomous planning;
- multi-agent production planner.

## Why Not GO

The current stack has valuable foundations:

- HomeTusk command lifecycle, idempotency, guardrails, degraded mode, and
  DecisionLog;
- AI Platform schema validation and deterministic baseline;
- V2 provider planning for multi-item shopping;
- ASR capture/edit/send path;
- mobile command shell.

But the evidence does not prove production readiness for full AI-command UX:

- no accepted `confirm` outcome;
- no accepted `answer` outcome;
- no explicit `natural_command` contract;
- provider planner is narrow and mostly deterministic;
- date/time, assignment, reschedule, answer, and linkage planning are incomplete;
- HomeTusk integration wrapper docs are stale relative to runtime;
- mobile UX lacks structured confirmation/answer/proposed-action states.

## Recommended Next Initiative

`AI Platform Domain Planner v1 - Narrow Household Command Corridor`

Suggested scope:

- support accepted v0 outcomes for `execute`, `clarify`, `reject`;
- optionally define `confirm` if needed for narrow corridor risk;
- support `create_task` and `add_shopping_items`;
- support multi-item shopping extraction with default list grounding;
- return schema-valid structured actions;
- avoid direct domain mutation;
- run against HomeTusk golden scenarios v0;
- keep HomeTusk as validation/execution authority.

## Required HomeTusk Follow-Up Before APPLY

1. Contract/artifact gate:
   - align AI Platform mapping docs with current `/v1/decide` runtime;
   - update or supersede stale wrapper schemas;
   - approve decision/action taxonomy.

2. Workpack:
   - define exact files for machine-readable scenarios;
   - define validation commands;
   - define owner split for provider versus HomeTusk changes.

3. Codex PLAN:
   - read-only exploration of the approved workpack;
   - no code writes before Gate C.

## Trust Corridor

Auto-execute only:

- `create_task` with clear title and grounded optional attributes;
- `add_shopping_items` where item boundaries are clear and list/default list is
  grounded.

Clarify:

- ambiguous item/task/member/zone/list/date;
- missing capability;
- incomplete household context.

Confirm:

- inferred assignment to non-requester;
- task plus shopping linkage;
- reschedule;
- broad batch planning.

Reject:

- unsafe overload;
- unsupported action type;
- cross-household reference;
- unverifiable domain entity;
- direct AI execution bypassing HomeTusk.

Answer:

- blocked until read-only answer contract exists.

## Non-Goals

- generic assistant chat;
- LLM inside HomeTusk;
- mobile to AI Platform direct calls;
- prompt tuning hidden as implementation;
- multi-agent production orchestration;
- broad household planning without confirm/clarify;
- production code changes under this audit.

## Minimum Contract Changes Required Later

No contract changes are required by this audit itself.

Future implementation likely needs:

- HomeTusk accepted decision taxonomy doc;
- updated AI Platform integration mapping;
- machine-readable golden scenario fixtures;
- future `natural_command` request contract;
- future `needs_confirmation` response;
- future `answered` response;
- expanded provider action schema for accepted v1 actions.

## Human Gate Recommendation

Gate A: approve this audit baseline as the initiative research result.

Gate before implementation: require artifact gate for any future external
behavior change.

Gate C: require Codex PLAN before any HomeTusk APPLY work.

Gate D: require read-only review verifying:

- no production code in audit-only work;
- contracts updated only when explicitly approved;
- no upstream snapshot mutation;
- golden scenarios map to accepted taxonomy;
- command traceability and degraded mode remain intact.
