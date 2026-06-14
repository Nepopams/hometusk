# Initiative: INIT-2026Q2-task-assignment-email-notifications
Status: **DONE** (closed on 2026-06-13 via branch `codex/task-assignment-email-notifications`)
Owner: Planning/Architecture

## Goal

Отправлять пользователю email-уведомление, когда задача назначена на него, независимо от того, назначение произошло вручную, через command pipeline, через AI Platform decision или через guardrails fallback.

## Why now

Назначенная задача без уведомления превращается в тихую запись в backlog. Для HomeTusk как “Jira для быта” важно закрыть командный контур: intent → task assignment → assignee реально узнаёт о назначении.

## In Scope

- **Task assignment event:**
  - выделить единое событие `TASK_ASSIGNED`;
  - не размазывать email logic по `ActionExecutor`, `TaskService`, guardrails и REST controllers.
- **Notification rule:**
  - отправлять только если `assignee_id != null`;
  - assignee является участником household;
  - assignee имеет verified email;
  - email notifications enabled для пользователя/household, если такая настройка уже есть;
  - не отправлять самому себе, если включено MVP-правило `skipSelfNotifications=true`.
- **Outbox enqueue:**
  - использовать email notification platform/outbox;
  - idempotency key: `TASK_ASSIGNED:{task_id}:{assignee_id}:{assignment_version|assigned_at}` или другой детерминированный вариант;
  - не отправлять письмо напрямую внутри task transaction.
- **Template MVP:**
  - subject: задача назначена;
  - body: название задачи, household, zone/due date при наличии, ссылка/placeholder на UI.
- **Tests:**
  - task created with assignee → email queued;
  - task created without assignee → no email;
  - unverified/missing email → no email;
  - retry/idempotency → no duplicate queued emails;
  - AI/upstream decision → adapter → guardrails → action execution → email queued.

## Out of Scope

- Email reminders before deadline.
- Digest emails.
- Invite-by-email.
- Push notifications.
- User preference UI, если настройки ещё не существуют.
- Rich template editor/localization.
- Notification analytics beyond basic metrics.

## Deliverables

- Backend:
  - task assignment event publication;
  - `TaskAssignmentEmailNotificationHandler` or equivalent application handler;
  - integration with `EmailNotificationService.enqueue(...)`;
  - idempotency key strategy;
  - template renderer for task assignment email.
- API/UI:
  - no mandatory new UI for MVP;
  - optional display in `/api/v1/users/me`: email notification eligibility state.
- Docs:
  - ADR or initiative note: when task assignment email is produced;
  - update API coverage / MVP roadmap;
  - documented skip rules.
- Tests:
  - integration tests across manual command and AI decision path;
  - negative tests for missing/unverified email and self-assignment.

## Implementation Notes

- `ActionExecutor` calls `TaskAssignmentNotificationService` after task creation
  so manual command, AI Platform, fallback, and guardrails-modified paths use one
  notification boundary.
- `TaskAssignmentNotificationService` keeps the existing in-app notification and
  publishes `TaskAssignedEvent` for email.
- `TaskAssignmentEmailNotificationHandler` listens after commit, verifies
  household membership and email eligibility, applies the MVP self-assignment
  skip rule, and enqueues through `EmailNotificationService`.
- Idempotency key format:
  `TASK_ASSIGNED:{task_id}:{assignee_id}:{assignment_timestamp_ms}`.
- Email enqueue runs in a separate `REQUIRES_NEW` transaction and catches runtime
  failures, so task assignment remains successful if email enqueue fails.
- No HTTP contract change is required for this initiative.

## Closure Evidence

- Backend implementation delivered `TaskAssignedEvent`,
  `TaskAssignmentNotificationService`, and
  `TaskAssignmentEmailNotificationHandler`.
- Manual command path, duplicate/idempotency behavior, missing/unverified email,
  self-assignment skip, degraded enqueue behavior, and AI Platform path are
  covered by integration tests.
- Sequence documentation added:
  `docs/diagrams/sequence-task-assignment-email-notification.md`.

## Exit Criteria

1) Назначение задачи на пользователя создаёт pending email notification в outbox.
2) Повторное применение той же команды/decision не создаёт duplicate email.
3) Missing/unverified email не приводит к отправке и не ломает создание задачи.
4) Email notification не отправляется напрямую из domain operation.
5) AI Platform decision path и manual command path ведут себя одинаково.
6) Есть documented degraded behavior: если email platform disabled/down, task assignment still succeeds.

## Success Metrics

- 95% task assignment emails переходят из pending в sent/retry state по configured SLA в stage.
- 0 duplicate assignment emails для одного assignment event.
- 0 failed task creations из-за email delivery failure.

## Dependencies

- Initiative `INIT-2026Q2-email-validation`.
- Initiative `INIT-2026Q2-email-notification-platform`.
- Стабильный command/action execution pipeline.
- Task assignment semantics and guardrails pipeline.

## Risks

- **Event duplication:** action retry/command retry может повторить событие → idempotency key обязателен.
- **Assignment semantics drift:** если guardrails меняет assignee, email должен уйти финальному assignee, а не промежуточному кандидату.
- **Noise:** уведомления самому себе и массовые batch операции могут раздражать → skip/grouping rules держать явно.
- **Link to UI:** если web route ещё нестабилен, в письме использовать безопасный placeholder/configured base URL.
