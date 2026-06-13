# ADR-016: Scheduled Command Execution with `scheduleAt`

**Status:** Accepted
**Date:** 2026-06-13
**Epic:** EP-033 - Structured Command Attributes & Scheduling

## Context

EP-033 adds explicit command attributes so HomeTusk users can control generated
tasks before execution. The remaining NOW gap is one-off scheduled commands:
clients can ask HomeTusk to accept a command now and execute it later.

This changes the command lifecycle and data model. The design must preserve core
HomeTusk invariants:
- commands remain first-class auditable entities;
- AI output is not source of truth and is still schema/business validated;
- domain rules are enforced in code at action time;
- idempotency continues to protect command submission;
- degraded mode still works if AI is unavailable when the scheduled command is due.

Existing routine scheduling already uses a feature-flagged Spring scheduler. The
scheduled command flow should reuse that operational posture without coupling
one-off commands to recurring routine generation.

## Decision

### 1. Command Lifecycle

Add `scheduleAt` as an optional command-level timestamp on `POST /api/v1/commands`.
When `scheduleAt` is absent, the command executes immediately as today.

When `scheduleAt` is present and in the future:
1. The backend creates the `Command`.
2. The backend validates schema and business invariants using the effective payload.
3. The backend stores the command with status `scheduled`.
4. The backend writes a `DecisionLog` entry with decision type `scheduled`.
5. The API returns status `scheduled` without executing actions.

When the command becomes due, a command scheduler executes the existing command
through the same decision, guardrails, action, and DecisionLog pipeline used by
immediate commands.

### 2. Validation Policy

Scheduled commands are validated twice:
- at submission time, to reject invalid assignee/zone/deadline/schedule values early;
- at due time, to re-check household boundaries and task state before action execution.

If due-time validation fails, the command is rejected and a validation failure is
logged. No task is created or mutated.

### 3. Scheduler Boundary

Create a separate command scheduler service/job:
- `CommandSchedulerService` finds due scheduled commands;
- `CommandSchedulerJob` invokes it under a separate feature flag;
- `CommandService` owns actual scheduled command execution so the command
  pipeline remains centralized.

The command scheduler is independent from `RoutineSchedulerService`. Both use
Spring scheduling and are disabled by default in local/dev configuration.

### 4. Concurrency and Idempotency

Submission idempotency remains request-hash based, so `scheduleAt` participates
in idempotency naturally.

Due-time processing uses database row locking on the command row before
execution. If the command is no longer `scheduled` or is not yet due, the
scheduler skips it. This is sufficient for the current single-instance posture
and avoids new infrastructure.

### 5. Scope

This ADR covers one-off scheduled execution of existing command types. It does
not introduce recurrence, reminders, priority, new AI intents, or local LLM
behavior.

## Consequences

### Positive

- Preserves command traceability from submission through delayed execution.
- Reuses existing schema/business validation, AI fallback, guardrails, and action execution.
- Keeps scheduled command operations separate from recurring routine generation.
- Maintains backward compatibility: `scheduleAt` is optional and immediate commands are unchanged.

### Negative

- Scheduled commands can fail later if household membership, zones, or task state changes.
- Commands may have multiple DecisionLog rows: one for scheduling and one for execution/rejection.
- v0 relies on single-instance scheduler posture plus DB row locking; multi-instance hardening remains future work.

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Duplicate due execution | Lock command row and skip commands no longer in `scheduled` state. |
| Stale assignee/zone/task state | Re-run business validation at due time. |
| Scheduler outage | Due commands remain in `scheduled` state and execute on the next run; no silent loss. |
| AI outage at due time | Existing degraded fallback behavior applies. |

## Alternatives Considered

### Execute Scheduling Outside The Command Pipeline

Rejected. It would bypass DecisionLog, guardrails, and degraded-mode behavior,
violating HomeTusk command traceability invariants.

### Store Scheduled Jobs In A New Scheduler Table

Rejected for v0. `Command` is already the lifecycle entity; a separate table
would add consistency and rollback complexity without clear benefit.

### Use Quartz Immediately

Rejected for v0. Spring scheduling plus DB state is enough for the current
single-service deployment and matches the existing routine scheduler posture.

## Migration and Rollback

Migration adds nullable `commands.schedule_at` and status value `scheduled`.
Rollback is safe by disabling the command scheduler flag and reverting the
runtime/contract changes. If migration has already been applied, rollback can
drop the nullable column after scheduled commands are resolved or rejected.

## Related

- Initiative: `docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md`
- Epic: `docs/planning/epics/EP-033/epic.md`
- Diagram: `docs/diagrams/sequence-scheduled-command-execution.md`
- Routine scheduler precedent: `docs/adr/013-routine-scheduler-design.md`
- Command reliability: `docs/architecture/decisions/012-command-reliability-idempotency.md`
