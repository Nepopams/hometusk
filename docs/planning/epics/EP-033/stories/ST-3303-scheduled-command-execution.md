# Story: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Status: DONE
**Epic:** EP-033 | **Priority:** P0 | **Points:** 8

Readiness: ADR/diagram artifact gate, Codex PLAN, delegated Gate C, APPLY, review gate, and delegated Gate D are complete on 2026-06-13.

## Description
Allow clients to submit commands with a future `scheduleAt` timestamp. Scheduled commands are validated and accepted immediately, remain in `scheduled` state, and execute later through the existing command pipeline when due.

## User Value
Users can queue a one-off command to run later while keeping HomeTusk's command audit trail, validation, and degraded-mode behavior intact.

## In Scope
- Add optional `CommandRequest.scheduleAt`.
- Add `scheduled` command status and scheduled response shape.
- Persist nullable `commands.schedule_at`.
- Validate `scheduleAt` is in the future when submitted.
- For scheduled submissions, validate payload/attributes and write a scheduling DecisionLog without creating a task immediately.
- Add command scheduler service/job behind a disabled-by-default feature flag.
- Execute due scheduled commands once through the existing validation, decision, guardrails, action, and DecisionLog path.
- Revalidate business invariants at due time.
- Extend active web command composer with optional schedule-at control.
- Update OpenAPI, contract index, service catalog, ADR/diagram indexes, and planning evidence.

## Out of Scope
- Recurrence.
- Reminder notifications.
- Priority.
- Calendar integrations.
- New AI Platform upstream contract fields.
- Quartz or external scheduler infrastructure.

## Acceptance Criteria

### AC-1: Scheduled Submission
Given a valid command request includes future `scheduleAt`
When submitted
Then the API returns status `scheduled`
And no task is created immediately
And the command stores `scheduleAt` with status `scheduled`
And DecisionLog records the scheduling decision.

### AC-2: Invalid Schedule Rejection
Given `scheduleAt` is in the past or invalid
When submitted
Then the request is rejected
And no task is created
And DecisionLog records failed validation when a command entity is created.

### AC-3: Due Execution
Given a scheduled command is due
When the command scheduler runs
Then the command executes through the existing command pipeline
And the created task reflects explicit due date, assignee, and zone attributes
And command status becomes `executed`.

### AC-4: Due-Time Revalidation
Given scheduled command attributes become invalid before due time
When the scheduler runs
Then the command is rejected without action execution
And DecisionLog records the due-time validation failure.

### AC-5: Idempotency
Given the same Idempotency-Key and identical request including `scheduleAt`
When the request is replayed
Then the stored scheduled response is returned.

Given the same Idempotency-Key and a changed `scheduleAt`
When submitted
Then the API returns `409 IDEMPOTENCY_CONFLICT`.

### AC-6: Web Scheduling Control
Given a household member uses the active Commands page
When they set a future schedule time
Then the request sends top-level `scheduleAt`.

Given they set a past schedule time
When they submit
Then the UI blocks submission before POST.

## Test Strategy
- Backend integration tests for scheduled submission, due execution, due-time rejection, and idempotency.
- Focused scheduler service test coverage if needed for skip/error counts.
- Web build/lint and browser verification for request shape and responsive layout.

## Flags
- contract_impact: yes.
- data_impact: yes.
- adr_needed: yes, completed by ADR-016.
- diagrams_needed: yes, completed by scheduled command sequence diagram.
- security_sensitive: medium.
- traceability_critical: high.

## Dependencies
- ST-3301 Gate D GO.
- ST-3302 Gate D GO.
- ADR-016 accepted.

## Gate Notes
- Artifact gate: GO, ADR and diagram accepted.
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3303/gate-c.md`.
- Review gate: GO, recorded in `docs/planning/workpacks/ST-3303/review-gate.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3303/gate-d.md`.
