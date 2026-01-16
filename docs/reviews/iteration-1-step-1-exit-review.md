# Iteration 1 / Step 1 Exit Review — Backend hardening (Web MVP)

## Status
- Decision: PASS WITH NOTES
- Date: 2026-01-16
- Branch/commit(s): claude/document-api-mk9y9x37rxtpp2mu-TUYKv @ a26176f7d4b422ff9786d36808cb270ed359379b (uncommitted changes present)

## Scope Delivered
- /api/v1/commands возвращает HTTP 200 для executed / needs_input / rejected / executed_degraded с явным status в теле.
- Публичный OpenAPI синхронизирован с контроллерами: /internal/* удален, POST /households/{householdId}/zones добавлен, TaskDetail для GET task detail, 401/403 отражены.
- Документы обновлены: api-coverage и service-catalog соответствуют факту.
- Интеграционные тесты: добавлен E2E путь Web MVP и проверка shopping add -> list contains; обновлены ожидания статусов и reject.
- scripts/test.sh переведен на LF для запуска в bash.

## Not Delivered / Out of Scope
- Инвайты домохозяйств и уведомления не входят в Step 1 и не реализованы.

## Key Changes (Changelog)
- Командные отклонения (AI/guardrails) больше не возвращают 4xx, а идут как 200 + status=rejected с errorCode/reason.
- Добавлен DTO для rejected-ответа и единый контракт oneOf для командных ответов.
- Валидационные 4xx сохранены через error envelope в GlobalExceptionHandler.

## Verification Evidence
- Commands executed (exact):
  - ./scripts/test.sh
  - ./services/backend/gradlew check
- Results:
  - ./scripts/test.sh: FAIL — JAVA_HOME не задан, java отсутствует в PATH.
  - ./services/backend/gradlew check: NOT RUN — блокирующая причина та же (Java отсутствует).
- Remediation: установить JDK, задать JAVA_HOME, затем повторно выполнить оба прогона.

## Contract & Docs Alignment
- OpenAPI: /commands → CommandResponse (executed/needs_input/rejected/executed_degraded), /internal/* удалены, POST /households/{householdId}/zones добавлен, task detail → TaskDetail, 401/403 добавлены, X-Idempotency-Key удален.
- Docs updated: `docs/mvp/api-coverage.md`, `docs/architecture/service-catalog.md`, `docs/contracts/http/commands.openapi.yaml`.
- ADR: ADR-009 unchanged; reviewed and still valid.

## Risks & Follow-ups
- Клиенты, ожидающие 4xx при reject, должны перейти на обработку 200 + status=rejected.
- Верификация не завершена: необходим повторный прогон тестов после установки Java.

## Rollback Plan
- Откатить изменения через revert соответствующих коммитов; миграций БД нет.

## Next Step
- Iteration 1 / Step 2: Household Invites — готовность после успешного прогона `./scripts/test.sh` и `./services/backend/gradlew check`.
