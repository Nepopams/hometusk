# Story: ST-3302 - Command Attribute Confirmation UI

## Status: DONE
**Epic:** EP-033 | **Priority:** P0 | **Points:** 3

Completed on 2026-06-13. Gate C and Gate D were delegated by the user goal and recorded in the ST-3302 workpack. `scheduleAt` and backend behavior changes remain out of scope.

## Description
Expose editable command attributes in the active web command composer so users can set due date, assignee, and zone before executing an immediate `create_task` command.

## User Value
Users can keep using the natural-language command box while explicitly controlling the task attributes that are most likely to be guessed incorrectly.

## In Scope
- Extend the active commands route composer to display optional controls for `dueDate`, `assigneeId`, and `zoneId`.
- Load assignee options from existing household members and zone options from existing household zones.
- Send selected attributes as command-level `CommandRequest` fields, not duplicated inside `payload`.
- Preserve the current result states, command history, idempotency behavior, and payload-only title creation path.
- Add frontend validation that selected due date is in the future.
- Update web API types and localized labels as needed.

## Out of Scope
- `scheduleAt` UI or scheduled command execution.
- Backend, migration, or OpenAPI changes beyond the already completed ST-3301 contract.
- Replacing the active commands route with the unused `CommandInput` component.
- Voice input flow changes.
- New members or zones endpoints.

## Acceptance Criteria

### AC-1: Attribute Controls Are Visible
Given a household member opens the active Commands page
When the create-task composer is shown
Then they can set optional due date, assignee, and zone before running the command.

### AC-2: Request Uses Command-Level Fields
Given the user selects due date, assignee, or zone
When the command is submitted
Then the request contains top-level `dueDate`, `assigneeId`, and/or `zoneId`
And the `payload` still contains the task title only.

### AC-3: Optional Path Remains Compatible
Given the user leaves all attribute controls blank
When the command is submitted
Then the request remains compatible with the previous payload-only create-task path.

### AC-4: Household Options Are Scoped
Given the page loads options
When assignee and zone controls render
Then options come from the current household members and zones endpoints.

### AC-5: Past Due Date Is Blocked Client-side
Given the user selects a past due date
When the command is submitted
Then the UI blocks submission and shows validation feedback.

## Test Strategy
- TypeScript build covers `CommandRequest` top-level fields.
- Add focused unit tests only if the route already has a nearby test harness; otherwise use build/lint plus manual browser verification.
- Use the in-app browser after frontend changes to verify the composer renders without overlap on desktop and mobile widths.

## Flags
- contract_impact: no new contract; consumes ST-3301 contract.
- data_impact: no.
- adr_needed: no.
- diagrams_needed: no.
- security_sensitive: medium, because controls must only use current household members/zones.
- traceability_critical: medium, because requests must continue through command history/idempotency.

## Dependencies
- ST-3301 Gate D GO.
- Existing `getMembers`, `getZones`, `useMembers`, and `useZones`.
- Active route: `clients/web/src/routes/Commands.tsx`.

## Gate Notes
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3302/gate-c.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3302/gate-d.md`.
