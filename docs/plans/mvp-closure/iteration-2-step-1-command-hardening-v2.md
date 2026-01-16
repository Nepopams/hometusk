# Iteration 2 / Step 1 Plan — Command pipeline hardening v2

## 1) Executive summary
Цель: сделать `/api/v1/commands` детерминированным и устойчивым к повторам и сбоям AI Platform.
Основные направления:
- Ввести idempotency для команд (повтор запроса не создаёт дубликаты).
- Усилить клиент AI Platform (таймауты/ретраи/сircuit breaker) и чётко фиксировать деградацию.
- Гарантировать корректность контрактов: JSON Schema валидация AI ответа + guardrails до выполнения действий.
- Улучшить трассировку/наблюдаемость без расширения сервиса.

Ограничения: без новых микросервисов, без новых типов команд, без UI работ.

## 2) Scope (In / Out)
**In:**
- Надёжность `/api/v1/commands` (executed / needs_input / rejected / executed_degraded).
- Idempotency-Key для POST `/api/v1/commands`.
- Харднинг AI клиента (timeouts/retry/circuit breaker) и детерминированный fallback.
- JSON Schema validation AI ответов + guardrails до выполнения действий.
- Трассировка/корреляция через весь цикл команды.
- Интеграционные тесты на retry/degraded/schema/idempotency.
- Обновления OpenAPI + service-catalog + api-coverage + ADR.

**Out:**
- Новые микросервисы, брокеры, outbox.
- Новые доменные фичи (invites/notifications уже в Iteration 1).
- Расширение RBAC.
- Локальная AI логика сверх «валидировать + потребить внешний ответ».

## 3) Current state assessment (with file pointers)
**Command API/flow**
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`: принимает `X-Correlation-ID`, валидирует JWT и membership, нет `Idempotency-Key`.
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`: создаёт `Command`, валидирует schema + business, вызывает AI (DecisionProviderSelector), применяет guardrails, пишет DecisionLog.
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`: `correlation_id` UNIQUE, нет idempotency полей.
- `services/backend/src/main/resources/db/migration/V007__create_commands.sql`: таблица `commands`, уникальный `correlation_id`.
- `services/backend/src/main/java/com/hometusk/commands/repository/CommandRepository.java`: поиск по correlationId, без idempotency.

**AI Platform client**
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`: RestClient без явных timeouts/retry/circuit breaker; `timeout-ms` есть в config, но не применён.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionProviderSelector.java`: fallback на manual при ошибках/healthCheck; при fallback disabled выбрасывает исключение.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java`: валидирует ответ через `AiResponseSchemaValidator`, при ошибке — `DecisionResult.Reject`.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiResponseSchemaValidator.java`: JSON Schema валидация, схема `/schemas/ai-decision-response.schema.json`.
- `services/backend/src/main/resources/application.yml`: конфиг `aiplatform.*`, `decision.provider`, `decision.fallback.enabled`.

**Guardrails + validation**
- `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java`: JSON Schema для payload команд.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailsOrchestrator.java`: оценивает policies, не выполняет action при неполном контексте.

**Tracing / DecisionLog**
- `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java`: пишет DecisionLog, поддерживает `external_decision_id` и `raw_decision_payload`.
- `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`: хранит correlationId и raw AI payload.

**Error envelope / codes**
- `services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java`: 400 для validation, 409 для `IDEMPOTENCY_CONFLICT`, 403/404/410 по правилам.
- `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java`: есть `IDEMPOTENCY_CONFLICT`.

**Existing tests (relevant)**
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java`.
- `services/backend/src/test/java/com/hometusk/integration/guardrails/Stage4GuardrailsIntegrationTest.java`.
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`.
- WireMock base: `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`.

## 4) Proposed changes by phase (3 phases, small commits)

### Phase 1 — Idempotency foundation (commit 1)
**Objective:** безопасные повторы `POST /api/v1/commands`.

**Tasks:**
- Выбрать подход хранения:
  - Предпочтительно: отдельная таблица `command_idempotency` с `idempotency_key`, `initiator_user_id`, `request_hash`, `stored_response_json`, `stored_http_status`, `created_at`, `expires_at`.
  - Уникальность: `(idempotency_key, initiator_user_id)`.
- Определить политику:
  - Совпадает key + hash → вернуть сохранённый ответ (200).
  - Совпадает key + hash различается → 409 `IDEMPOTENCY_CONFLICT`.
  - TTL для идемпотентности: 24h (зафиксировать в ADR и OpenAPI).
- В `CommandController` читать `Idempotency-Key`, передавать в сервис.
- В `CommandService`/новом `CommandIdempotencyService` реализовать dedupe (с блокировкой по уникальному ключу).

**Files:**
- Flyway migration (новая версия после V014) при добавлении таблицы.
- Новый домен/репозиторий/сервис для идемпотентности.
- `CommandController`, `CommandService`.
- `docs/contracts/http/commands.openapi.yaml` (Idempotency-Key header, TTL).

**Risks:** ложное совпадение hash → неверный ответ; гонки при параллельных запросах.
**Rollback:** откат коммита; таблица остаётся неиспользуемой.

### Phase 2 — AI client hardening + contract validation (commit 2)
**Objective:** устойчивый AI вызов, детерминированная деградация, строгая валидация до execution.

**Tasks:**
- Добавить таймауты в `AiPlatformClient` (connect/read) с использованием `aiplatform.timeout-ms`.
- Retry + circuit breaker:
  - Поскольку зависимостей нет в `build.gradle.kts`, выбрать минимальный путь:
    - Вариант A: добавить `resilience4j-spring-boot3` (обоснование в ADR-012), или
    - Вариант B: ограниченный retry на уровне клиента + простое in-memory состояние доступности.
  - Ретрай: 1–2 попытки с jitter/backoff, без thundering herd.
- Детерминированный degraded путь:
  - Таймаут / open circuit / 5xx → возврат fallback (executed_degraded или needs_input) по политике команды.
  - Запрет 500 на бизнес-отказы.
- Уточнить и закрепить порядок: AI schema validation → Decision mapping → Guardrails → Execution.
- Трассировка: корреляция должна идти через request → AI call → DecisionLog → response.
  - Если OpenTelemetry не используется (сейчас нет зависимостей), оставить MDC + correlationId.

**Files:**
- `AiPlatformClient`, `DecisionProviderSelector`, конфиги в `application.yml`.
- Возможные новые классы: `AiPlatformResilienceConfig`, retry/circuit breaker wrappers.
- `CommandService` (обработка деградации и отказов без 500).
- `DecisionLogWriter` (если нужен raw payload/ошибки).

**Risks:** увеличение latency из-за retries; неверная деградация при нестабильном AI.
**Rollback:** откат; оставить feature flags (если добавлены) выключенными.

### Phase 3 — Tests + docs sync + ADR (commit 3)
**Objective:** зафиксировать контракт и проверить устойчивость.

**Tests (blocking):**
1) Idempotency happy: повтор с тем же ключом → тот же ответ, действий не больше 1.
2) Idempotency conflict: тот же ключ + другой payload → 409 + `IDEMPOTENCY_CONFLICT`.
3) AI timeout → deterministic degraded response.
4) AI invalid schema → `rejected` (200) и no execution.
5) Circuit breaker open → degraded path.
6) CorrelationId сохраняется и возвращается (минимальная проверка).

**Docs:**
- OpenAPI: `Idempotency-Key` header, 409 conflict, TTL policy, degraded semantics.
- `docs/architecture/service-catalog.md` и `docs/mvp/api-coverage.md`.
- ADR-012: «Command reliability & idempotency» (timeout/retry/circuit breaker, idempotency semantics, ordering schema/guardrails, observability).

**Rollback:** откат набора коммитов; миграция остаётся, описать последствия.

## 5) Contract impact (OpenAPI changes; breaking changes = NONE)
- `POST /api/v1/commands`: добавить `Idempotency-Key` header (описание, TTL, уникальность).
- Добавить 409 для конфликтного использования ключа (errorCode `IDEMPOTENCY_CONFLICT`).
- Уточнить degraded/rejected semantics (status в теле, 200).
- Проверить 401/403 и error envelope consistency.

## 6) DoD checklist
- [ ] Повторы с Idempotency-Key не создают дубликаты (интеграционные тесты).
- [ ] AI ошибки/таймауты приводят к deterministic degraded или rejected, без 500.
- [ ] AI ответы валидируются по JSON Schema до execution.
- [ ] Guardrails применяются до выполнения действий.
- [ ] OpenAPI + service-catalog + api-coverage синхронизированы.
- [ ] ADR-012 добавлен.
- [ ] Тесты проходят.

## 7) Open questions
Нет блокирующих. Решение принято: 409 для idempotency conflict и TTL 24h.

---

## Files consulted
- `AGENTS.MD`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/architecture/service-catalog.md`
- `docs/mvp/api-coverage.md`
- `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`
- `docs/architecture/decisions/010-household-invites.md`
- `docs/architecture/decisions/011-notifications-stub.md`
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionProviderSelector.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiResponseSchemaValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- `services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java`
- `services/backend/src/main/resources/db/migration/V007__create_commands.sql`
- `services/backend/src/main/resources/db/migration/V010__add_ai_platform_fields.sql`
- `services/backend/src/main/resources/application.yml`
- `services/backend/build.gradle.kts`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`

## Files to change (planned)
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- New: `services/backend/src/main/java/com/hometusk/commands/idempotency/*` (entity/repo/service)
- New migration: `services/backend/src/main/resources/db/migration/V0xx__create_command_idempotency.sql`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`
- `services/backend/src/main/resources/application.yml`
- `services/backend/build.gradle.kts` (если добавляем resilience dependency)
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`
- New tests: `services/backend/src/test/java/com/hometusk/integration/CommandIdempotencyIntegrationTest.java`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/architecture/service-catalog.md`
- `docs/mvp/api-coverage.md`
- `docs/architecture/decisions/012-command-reliability-idempotency.md`

## Verification commands
- `./scripts/test.sh`
- `cd services/backend && ./gradlew check`
