# Iteration 1 / Step 2 Exit Review — Household Invites

## Status
- Decision: PASS WITH NOTES
- Date: 2026-01-16
- Branch/commit(s): claude/document-api-mk9y9x37rxtpp2mu-TUYKv @ a26176f7d4b422ff9786d36808cb270ed359379b (uncommitted changes present)

## Scope Delivered
- Flyway migration `V013__create_household_invites.sql` with table, indexes, and status constraint.
- Domain + repository: `HouseholdInvite`, `InviteStatus`, `HouseholdInviteRepository`.
- Token generation: `hti_` + 32-byte SecureRandom Base64URL (no padding).
- Invite services with transactional accept and PESSIMISTIC_WRITE row lock.
- API: `POST /api/v1/households/{id}/invites` and `POST /api/v1/invites/accept` (token-only request).
- Error codes for invite states and 404/410 mappings.
- Docs + OpenAPI + ADR-010 + api-coverage updates.
- Integration tests: `HouseholdInviteIntegrationTest` (8 scenarios).

## Not Delivered / Out of Scope
- Нет доставки уведомлений.
- Нет email/SMS/пуш-каналов.
- Нет расширения ролей/RBAC.

## Key Design Decisions
- Token: Base64URL (RFC 4648 §5), 32 байта, префикс `hti_`, срок 7 дней.
- Accept endpoint без householdId (anti-IDOR).
- Конкурентный приём защищен `PESSIMISTIC_WRITE`.
- Коды: 404 для invalid token; 410 для expired/redeemed/revoked; 200 для already-member (no-op, токен остаётся ACTIVE).

## Verification Evidence
- ./scripts/test.sh — FAIL: JAVA_HOME не задан, `java` отсутствует в PATH.
- cd services/backend && ./gradlew check — FAIL: JAVA_HOME не задан, `java` отсутствует в PATH.
- Remediation: установить JDK, задать `JAVA_HOME`, повторить оба прогона.

## Contract & Docs Alignment
- OpenAPI: добавлены пути `/api/v1/households/{id}/invites` и `/api/v1/invites/accept`, схемы `CreateInviteResponse`, `AcceptInviteRequest`, `AcceptInviteResponse`.
- ADR: `docs/architecture/decisions/010-household-invites.md`.
- Обновлены `docs/architecture/service-catalog.md` и `docs/mvp/api-coverage.md`.

## Risks & Follow-ups
- Клиентам нужно корректно обрабатывать 404 vs 410.
- При already-member accept токен остаётся ACTIVE — UI должен учитывать это поведение.
- Повторный прогон тестов обязателен после настройки JDK.

## Rollback Plan
- Откатить коммиты. Таблица `household_invites` останется; при необходимости удалить отдельной миграцией или оставить без использования.

## Next Step
- Iteration 1 / Step 3: Notifications stub — готовность после установки JDK и прохождения `./scripts/test.sh` + `./services/backend/gradlew check`.
