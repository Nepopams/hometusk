# Initiative: INIT-2026Q2-email-notification-platform
Status: **DONE** (closed 2026-06-13 via branch codex/email-notification-platform; ADR-018, V030 outbox migration, sender abstraction, retry worker, metrics, runbook, and backend tests)
Owner: Planning/Architecture

## Goal

Создать безопасную email notification platform внутри HomeTusk: outbox, sender abstraction, retry/idempotency и delivery status. Письма не должны отправляться напрямую из доменных операций и не должны ломать создание задач/покупок при сбое SMTP/provider.

## Why now

В продукте уже есть in-app/realtime notification direction, а следующий MVP-сценарий — email по назначенным задачам. Если сразу встроить `sendEmail()` в task assignment, получим дубли при retry, побочные эффекты внутри транзакций и неуправляемую деградацию.

## In Scope

- **Notification outbox:**
  - таблица `email_notification_outbox` или расширение существующей notification model, если она уже подходит;
  - статусы: `PENDING`, `SENT`, `FAILED`, `RETRY_SCHEDULED`, `CANCELLED`;
  - idempotency key на уровне business event.
- **Sender abstraction:**
  - интерфейс `EmailSender`;
  - SMTP/dev implementation;
  - конфигурация provider credentials вне git.
- **Async delivery:**
  - scheduled job/worker, который отправляет pending emails;
  - retry policy с лимитом попыток;
  - safe degradation: доменная операция не падает из-за email provider outage.
- **Templates MVP:**
  - простой text/html template renderer без тяжёлого template-engine, если не нужен;
  - обязательные поля: recipient, subject, body, correlation/task context.
- **Observability:**
  - логирование попыток отправки;
  - metrics: pending, sent, failed, retry count.

## Out of Scope

- Конкретные продуктовые email use-cases, кроме тестового/system notification.
- Push/SMS/APNs/FCM.
- Bounce webhooks.
- Preference center/unsubscribe management.
- Массовые рассылки/marketing emails.
- Персонализация/AI-generated email copy.

## Deliverables

- Backend:
  - migration для outbox;
  - `EmailNotificationOutbox` entity/repository;
  - `EmailNotificationService.enqueue(...)`;
  - `EmailSender` interface + SMTP/dev implementation;
  - scheduled sender job with retry policy;
  - idempotency strategy.
- Config:
  - `email.enabled`, SMTP host/port/from/retry config;
  - dev mode, при котором письма логируются или отправляются в local SMTP sink.
- Docs:
  - ADR: outbox over direct send;
  - runbook локальной проверки;
  - error/degraded mode semantics.
- Tests:
  - enqueue idempotency;
  - retry behavior;
  - provider failure does not break domain operation.

## Exit Criteria

1) Можно поставить email notification в outbox без немедленной отправки.
2) Sender job отправляет pending emails и обновляет status.
3) Повторное enqueue с тем же idempotency key не создаёт дубль.
4) SMTP/provider outage не ломает доменную операцию.
5) Failed emails имеют понятный статус и retry limit.
6) Есть локальный способ проверить отправку без внешнего production provider.
7) Метрики/логи позволяют понять, сколько писем pending/sent/failed.

## Success Metrics

- 0 duplicate emails по одному idempotency key.
- 100% provider failures не влияют на успешность task/shopping command execution.
- 95% pending emails в dev/stage переходят в terminal/retry state в пределах configured interval.

## Dependencies

- Initiative `INIT-2026Q2-email-validation`.
- Существующий notification/event publishing слой, если он уже реализован.
- Конфигурация SMTP/dev mail sink.

## Risks

- **Duplicate delivery:** решается idempotency key + unique constraint.
- **Transactional inconsistency:** enqueue делать после успешного domain decision/application или через transactional outbox semantics.
- **Provider lock-in:** держать provider за `EmailSender`, без vendor-specific leakage в domain services.
- **Spam/noise:** платформа не решает “кому и когда писать”; это задача use-case initiatives.
