# PLAN Findings: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Result
**Gate C recommendation: GO.** ST-3303 is implementable as an additive command lifecycle extension using the existing command pipeline and a feature-flagged scheduler job.

## Current-State Findings
- `CommandRequest` supports ST-3301 attributes but not `scheduleAt`.
- `CommandStatus` has no `SCHEDULED` value and the DB check constraint lacks `scheduled`.
- `Command` has no scheduled execution timestamp.
- `RoutineSchedulerJob` already establishes the local pattern: Spring scheduled job, disabled by default via property.
- `CommandService` owns validation, decision, guardrails, action execution, and DecisionLog writing; scheduled due-time execution should stay centralized there.
- The active web route now supports due date, assignee, and zone controls; it can add schedule-at as another optional command-level field.

## Implementation Decision
1. Add `scheduleAt` to request/response/domain/migration.
2. If `scheduleAt` is present and future, validate effective payload, mark command `scheduled`, write a scheduling DecisionLog, and return status `scheduled`.
3. Add `CommandSchedulerService` to find due command IDs and ask `CommandService` to execute each due command.
4. Add row-lock repository method for due-time execution.
5. Re-run schema/business validation at due time before decision/action execution.
6. Keep scheduler disabled by default under `hometusk.command-scheduler.enabled=false`.
7. Add active web schedule-at control with future-time validation.

## File List For APPLY
See `docs/planning/workpacks/ST-3303/workpack.md`.

## Tests
- Extend command integration/idempotency tests for scheduled submission/replay/conflict.
- Add scheduler integration coverage for due execution and due-time rejection.
- Run focused command tests and web checks before full project test script.

## STOP-THE-LINE
- Recurrence or reminders become required.
- New scheduler infrastructure is required.
- Due-time execution cannot reuse the existing command pipeline.
- DecisionLog cannot be written for both scheduling and execution/rejection.
