# Reject / Confirm / Answer Contract Posture

Status: Completed, 2026-06-15

Decision: **LIMITED-GO with contract follow-up required**

## Summary

Provider current-schema evidence is accepted for safe no-execute behavior in the
seed evaluation. It is not accepted as a HomeTusk product/runtime contract for
`reject`, `confirm`, or `answer`.

## Decisions

| Question | Decision | Rationale |
| --- | --- | --- |
| Is `reject_mapped_to_error` acceptable for HomeTusk `natural_command` v0? | No for runtime/product contract; yes only as temporary provider evidence. | Product UX and HomeTusk mapping need a clear rejection reason/status, not an overloaded provider error/clarify representation. |
| Is first-class `reject` required before runtime integration? | Yes. | Unsupported, unsafe, cross-household, and unverifiable requests need explicit no-execute semantics and audit. |
| Is first-class `confirm` required before assignment, reschedule, or task + shopping linkage? | Yes. | These flows can mutate social/workload state or wrong objects; clarify alone is safe but not enough for a usable UX. |
| Is first-class `answer` required before status/query commands? | Yes, before any answer/status UX. | Answer must be grounded, read-only, and sourced from HomeTusk read models. |
| Is direct plural `add_shopping_items` required? | Not for the next provider eval step. | Repeated singular `propose_add_shopping_item` actions are acceptable if item boundaries are preserved and HomeTusk validates each action. |

## Accepted Current-Schema Corridor

The provider may continue using current schema for:

- clear `create_task` proposals;
- clear multi-item shopping as repeated `propose_add_shopping_item` actions;
- `clarify` when context is missing, ambiguous, unsupported, or outside the
  safe corridor;
- temporary `reject_mapped_to_error` as safety evidence only.

## Required Before HomeTusk Runtime Integration

### First-class `reject`

Required fields should include:

- canonical outcome/status;
- stable error code;
- user-safe reason;
- provider trace id;
- schema/decision/planner version;
- no proposed mutation.

### First-class `confirm`

Required fields should include:

- proposed actions;
- risk/reason labels;
- confirmation token or pending command id;
- expiry;
- approval/cancel semantics;
- no mutation before explicit approval.

### First-class `answer`

Required fields should include:

- read-only summary;
- referenced HomeTusk entities;
- source read model;
- generated timestamp;
- no proposed mutation;
- no hallucinated household state.

## Blocked Until Contract Governance

- `needs_confirmation` HomeTusk response.
- `answered` HomeTusk response.
- Mobile confirmation cards.
- Mobile answer/status cards.
- Natural reschedule auto-execute.
- Natural completion auto-execute.
- Task-shopping linkage auto-execute.
- Non-requester assignment auto-execute.

## Next Contract Action

Open a provider-side contract workpack for:

1. first-class `reject`;
2. optional non-executing `confirm`;
3. planner/version/provenance fields if not already reliably present;
4. explicit current-schema compatibility notes for repeated shopping actions.

Open a separate HomeTusk contract spike later for:

1. `natural_command` request shape;
2. `needs_confirmation` response shape;
3. `answered` response shape;
4. provider adapter mapping;
5. mobile/web API type changes.
