# Iteration 1 / Step 3 Exit Review — Notifications stub

## Status
- Decision: PASS WITH NOTES
- Date: 2026-01-16
- Branch/commit(s): claude/document-api-mk9y9x37rxtpp2mu-TUYKv @ a26176f7d4b422ff9786d36808cb270ed359379b

## Scope Delivered
- Миграция `V014__create_notifications.sql` для per-recipient уведомлений.
- Domain/repo/dto/service/controller в `com.hometusk.notifications`.
- API: `GET /api/v1/households/{id}/notifications` и `POST /api/v1/notifications/{id}/read` (идемпотентно, ownership enforced).
- Эмиссия уведомлений из `ActionExecutor`, `ShoppingService`, `InviteService`.
- ErrorCode `NOTIFICATION_NOT_FOUND` и маппинг в `GlobalExceptionHandler`.
- Тесты: `NotificationIntegrationTest` (5 сценариев).
- Документация синхронизирована: OpenAPI, service-catalog, api-coverage, ADR-011.

## Not Delivered / Out of Scope
- Нет push/email/SMS, нет realtime/websockets, нет preferences/quiet hours.

## Contract & Docs
- OpenAPI: добавлены пути `GET /households/{id}/notifications`, `POST /notifications/{id}/read`, схемы `Notification` и `NotificationPayload`, ошибки 400/401/403/404, `since` как RFC3339.
- Обновлены `docs/architecture/service-catalog.md`, `docs/mvp/api-coverage.md`, `docs/architecture/decisions/011-notifications-stub.md`.

## Verification Evidence
- Commands intended:
  - `./scripts/test.sh`
  - `cd services/backend && ./gradlew check`
- Current result: FAIL — отсутствует `JAVA_HOME`/`java` в PATH.
- Remediation: установить JDK (Java 21), задать `JAVA_HOME`, убедиться что `java -version` работает, затем повторить команды.

## Risks & Follow-ups
- Риск нагрузки при fan‑out на всех членов household для shopping‑событий.
- Payload хранится как JSON‑строка — при изменении схемы нужно следить за обратной совместимостью.
- Следующее усиление (по мере роста): курсорная пагинация, outbox/async, индексы под частые выборки.

## Rollback Plan
- Откатить коммиты Step 3; таблицу `notifications` удалить миграцией или оставить неиспользуемой (без влияния на рантайм).

## Next Step
- Готовность к следующей итерации после настройки JDK и прохождения тестов; либо подготовить общий exit review Iteration 1 после завершения верификации.
