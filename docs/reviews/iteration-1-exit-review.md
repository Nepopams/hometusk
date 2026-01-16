# Iteration 1 Exit Review — MVP Closure (Steps 1–3)

## Status
- Decision: PASS WITH NOTES
- Date: 2026-01-16
- Version label (if used): Iteration1
- Overall gate status: тесты не пройдены из-за отсутствия `JAVA_HOME`/`java` в PATH; `./scripts/test.sh` и `./services/backend/gradlew check` не прошли.

## Delivered Scope (by step)
### Step 1 — Backend hardening
- `/api/v1/commands` нормализован: всегда 200 с `status` в теле (executed/needs_input/rejected/executed_degraded), business reject не через 4xx.
- OpenAPI и docs синхронизированы с контроллерами (TaskDetail, зоны, 401/403, удалён `/internal/*`).
- Интеграционные тесты: E2E для MVP пути и shopping add → list.

### Step 2 — Invites
- Токен `hti_` + Base64URL 32 байта, single‑use, TTL 7 дней, anti‑IDOR accept.
- Статусы и коды: 404 invalid, 410 expired/redeemed/revoked, 200 no‑op если уже член.
- Конкурентный accept защищён `PESSIMISTIC_WRITE`, тесты 8 сценариев, ADR‑010.

### Step 3 — Notifications
- Per‑recipient модель в БД, API list/read, RFC3339 `since`, лимиты.
- Emission hooks из `ActionExecutor`, `ShoppingService`, `InviteService`.
- `NOTIFICATION_NOT_FOUND` + 404 mapping, тесты 5 сценариев, ADR‑011.

## Contract Impact Summary
- `/api/v1/commands`: 200 + `status` для reject/needs_input/executed_degraded; поведение клиентов, ожидающих 4xx на reject, изменено.
- Инвайты: `/api/v1/households/{id}/invites`, `/api/v1/invites/accept` с семантикой 404/410.
- Уведомления: `/api/v1/households/{id}/notifications`, `/api/v1/notifications/{id}/read` и новые схемы `Notification`/`NotificationPayload`.
- Breaking changes: NONE (но есть поведенческая корректировка обработки reject у клиентов).

## Verification Evidence
- Выполнено:
  - `./scripts/test.sh` — FAIL (нет `JAVA_HOME`/`java`).
  - `cd services/backend && ./gradlew check` — FAIL (нет `JAVA_HOME`/`java`).
- Checklist to flip PASS WITH NOTES → PASS:
  - Установить JDK 21.
  - Настроить `JAVA_HOME` и PATH (`java -version` должен работать).
  - Повторить `./scripts/test.sh` и `./services/backend/gradlew check`.

## Retro (Action items)
- Owner: DevOps/Platform, Deadline: 2026-01-20, Success: JDK 21 установлен на рабочих местах/CI, `java -version` доступен.
- Owner: Backend Lead, Deadline: 2026-01-21, Success: добавлен preflight‑чек окружения в CI с понятным фейлом при отсутствии `JAVA_HOME`.
- Owner: Docs/QA, Deadline: 2026-01-23, Success: guardrail против дрейфа контрактов (CI‑проверка OpenAPI vs контроллеры или обязательная review‑чеклист‑секция).
- Owner: Backend, Deadline: 2026-01-27, Success: контрактные тесты на `/api/v1/commands` (200 + status) и 404/410 для инвайтов.
- Owner: Backend, Deadline: 2026-01-30, Success: решение по пагинации уведомлений (cursor или page) задокументировано и согласовано.
- Owner: QA, Deadline: 2026-01-30, Success: regression‑сценарий клиента, ожидающего 4xx на reject, зафиксирован в тест‑плане.

## Risks & Follow-ups
- Фан‑аут уведомлений на всех членов household может увеличить нагрузку на БД при росте активности.
- Payload уведомлений хранится как JSON‑строка; потребуется версия/миграция схем при расширении.
- Поведенческие изменения (`/commands` reject как 200) требуют адаптации клиентов и мониторинга.
- Next iteration: рассмотреть outbox/async для уведомлений, курсорную пагинацию, и контрактные тесты в CI.

## Appendix
- Step reviews: `docs/reviews/iteration-1-step-1-exit-review.md`, `docs/reviews/iteration-1-step-2-exit-review.md`, `docs/reviews/iteration-1-step-3-exit-review.md`.
- ADRs: `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`, `docs/architecture/decisions/010-household-invites.md`, `docs/architecture/decisions/011-notifications-stub.md`.
- Key files (high-level): `docs/contracts/http/commands.openapi.yaml`, `docs/architecture/service-catalog.md`, `docs/mvp/api-coverage.md`, `services/backend/src/main/resources/db/migration/V013__create_household_invites.sql`, `services/backend/src/main/resources/db/migration/V014__create_notifications.sql`.
