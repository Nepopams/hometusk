# Task Assignment Email Notification

**Type**: Sequence
**Last Updated**: 2026-06-13
**Status**: current

## Purpose

Explain how manual command, AI Platform, and guardrails-modified task assignment
paths converge on one `TaskAssignedEvent`, then enqueue email through the email
outbox without direct SMTP calls from task execution.

## Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant CommandController
    participant CommandService
    participant Decision as DecisionProvider/Guardrails
    participant ActionExecutor
    participant TaskService
    participant Notifications as TaskAssignmentNotificationService
    participant EmailHandler as TaskAssignmentEmailNotificationHandler
    participant Outbox as EmailNotificationService
    participant DB as PostgreSQL

    Client->>CommandController: POST /commands create_task
    CommandController->>CommandService: execute(request)
    CommandService->>Decision: manual, AI Platform, or fallback decision
    Decision-->>CommandService: final create_task action with assigneeId
    CommandService->>ActionExecutor: execute create_task
    ActionExecutor->>TaskService: create task(final assignee)
    TaskService->>DB: INSERT task
    ActionExecutor->>Notifications: notifyTaskAssigned(task, actor)
    Notifications->>DB: create in-app notification when applicable
    Notifications-->>EmailHandler: publish TaskAssignedEvent
    CommandService->>DB: INSERT DecisionLog and UPDATE Command
    CommandService-->>CommandController: executed response
    CommandController-->>Client: 200

    EmailHandler->>EmailHandler: after commit: verify member, email eligibility, skip-self rule
    alt eligible assignee
        EmailHandler->>Outbox: enqueue TASK_ASSIGNED email
        Outbox->>DB: INSERT email_notification_outbox(status=PENDING)
    else skipped or enqueue failure
        EmailHandler-->>EmailHandler: log skip/failure without rolling back task
    end
```

## Notes

- The email is queued for the final assignee after manual decision, AI Platform
  decision, and guardrails modifications are applied.
- Missing, unverified, non-member, or self assignments do not enqueue email.
- Email enqueue uses a task assignment idempotency key and never sends SMTP
  directly from command or task execution.
- If email enqueue fails after task assignment commits, the failure is logged and
  the task assignment remains successful.
