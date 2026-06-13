# Scheduled Command Execution

**Type**: Sequence
**Last Updated**: 2026-06-13
**Status**: current

## Purpose

Explain how `scheduleAt` commands move from accepted command submission to later
validated execution while preserving DecisionLog traceability.

## Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant CommandController
    participant CommandService
    participant CommandSchedulerService
    participant DecisionPipeline
    participant Domain
    participant DB as PostgreSQL

    Client->>CommandController: POST /commands with scheduleAt
    CommandController->>CommandService: execute(request)
    CommandService->>DB: INSERT Command(status=received, schedule_at)
    CommandService->>CommandService: Build effective payload
    CommandService->>CommandService: Schema + business validation
    CommandService->>DB: UPDATE Command(status=scheduled)
    CommandService->>DB: INSERT DecisionLog(type=scheduled)
    CommandService-->>CommandController: scheduled response
    CommandController-->>Client: 200 status=scheduled

    CommandSchedulerService->>DB: Find due scheduled command IDs
    CommandSchedulerService->>CommandService: executeScheduledCommand(commandId)
    CommandService->>DB: SELECT Command FOR UPDATE
    CommandService->>CommandService: Rebuild effective payload
    CommandService->>CommandService: Re-run schema + business validation
    CommandService->>DecisionPipeline: decide(context)
    DecisionPipeline-->>CommandService: StartJob / Clarify / Reject / Degraded
    CommandService->>Domain: Execute validated action when allowed
    CommandService->>DB: INSERT DecisionLog(execution outcome)
    CommandService->>DB: UPDATE Command(final status)
```

## Notes

- Submission and due-time execution are separate decisions in the audit trail.
- Due-time validation intentionally re-checks mutable household and task state.
- Scheduler outage does not lose commands; due commands remain queryable by
  `status=scheduled` and `schedule_at <= now`.
