# Story: ST-3301 - Command Structured Attributes Contract/Backend Foundation

## Status: DONE
**Epic:** EP-033 | **Priority:** P0 | **Points:** 5

Completed on 2026-06-13. Human Gate B, artifact gate, Gate C, and Gate D were delegated by the user goal and recorded in the ST-3301 workpack. Scheduling execution remains out of scope.

## Description
Add a backward-compatible foundation for explicit command-level task attributes on `POST /api/v1/commands`. New clients can send `dueDate`, `assigneeId`, and `zoneId` at the command level for immediate `create_task` commands; existing clients that only send those fields inside `payload` continue to work.

## User Value
Users can set the key task attributes before execution instead of relying only on inferred payload fields, while HomeTusk keeps validation, household boundaries, idempotency, and audit traceability intact.

## In Scope
- Update Commands OpenAPI for optional `CommandRequest.dueDate`, `CommandRequest.assigneeId`, and `CommandRequest.zoneId`.
- Add compatibility notes and contract index material note.
- Add backend DTO fields for command-level attributes.
- Add nullable command storage columns for those attributes.
- Normalize effective `create_task` payload so top-level attributes participate in the existing schema/business validation and execution path.
- Persist explicit attributes on `Command`.
- Preserve payload-only behavior for existing clients.
- Preserve idempotency semantics: same key + same full request replays; same key + different attribute value conflicts.
- Add integration tests for happy path, backward compatibility, invalid household boundaries, and idempotency.

## Out of Scope
- `scheduleAt` acceptance or scheduled execution.
- Recurrence, reminders, priority, or bulk command support.
- Frontend confirmation UI changes.
- AI Platform upstream contract changes.
- New local LLM behavior.

## Acceptance Criteria

### AC-1: Command-Level Attributes Create A Task
```
Given a household member submits POST /api/v1/commands with type=create_task
And the request includes top-level dueDate, assigneeId, and zoneId
When the command executes
Then the created task has deadline=dueDate
And the created task assignee is assigneeId
And the created task zone is zoneId
And the command stores those explicit attributes
And DecisionLog is written for the command
```

### AC-2: Existing Payload-Only Clients Still Work
```
Given an existing client sends assigneeId, zoneId, and deadline only inside payload
When the command executes
Then the task is created with the same attributes as before
And no new top-level field is required
```

### AC-3: Attribute Conflicts Are Rejected
```
Given a create_task request has top-level dueDate or assigneeId or zoneId
And payload contains a different value for the same effective attribute
When the command is submitted
Then the command is rejected with a validation/business error
And no task is created
And DecisionLog records the failed validation
```

### AC-4: Household Boundaries Are Enforced
```
Given top-level assigneeId is not a member of the household
Or top-level zoneId is not in the household
When the command is submitted
Then the request is rejected with BUSINESS_RULE_VIOLATION
And no cross-household data is exposed
```

### AC-5: Due Date Must Be Future
```
Given top-level dueDate is in the past
When the command is submitted
Then the request is rejected with DEADLINE_MUST_BE_FUTURE
And no task is created
```

### AC-6: Idempotency Includes Attributes
```
Given the same Idempotency-Key and identical request including top-level attributes
When the request is replayed
Then the stored response is returned and no duplicate task is created

Given the same Idempotency-Key and a changed dueDate, assigneeId, or zoneId
When the request is submitted
Then the API returns 409 IDEMPOTENCY_CONFLICT
```

### AC-7: Contract Is Backward-Compatible
```
Given clients omit the new fields
When they use the existing contract
Then request and response compatibility is preserved
And OpenAPI documents the new optional fields and conflict semantics
```

## Test Strategy

**Integration Tests**
- `CommandStructuredAttributesIntegrationTest` or focused additions to `CommandPipelineTest`:
  - command-level attributes create task with assignee/zone/deadline;
  - payload-only command remains valid;
  - top-level/payload conflicts reject and create no task;
  - invalid assignee and invalid zone reject;
  - past dueDate rejects.
- `CommandIdempotencyIntegrationTest`:
  - identical top-level attributes replay;
  - changed top-level attribute conflicts.

**Unit Tests**
- Add focused tests only if normalization is extracted from `CommandService`.

## Flags
- contract_impact: yes
- data_impact: yes
- adr_needed: no
- diagrams_needed: no
- security_sensitive: medium
- traceability_critical: high

## Dependencies
- Existing command pipeline and idempotency implementation.
- Existing household membership and zone validation.
- Existing task deadline/assignee/zone fields.

## Gate Notes
- Artifact gate: GO for non-breaking Commands API delta. Contract docs must be updated in the APPLY before runtime code is considered complete.
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3301/gate-c.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3301/gate-d.md`.
