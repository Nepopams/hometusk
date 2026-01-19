# MVP Gap Analysis — Deep Review

**Date:** 2026-01-19
**Reviewer:** Claude Code (Arch/BA)
**Reference:** `docs/planning/mvp.md` (updated version)

---

## Executive Summary

MVP **~75% complete**. Критические gaps требуют работы перед exit review:

| Priority | Gap | Impact |
|----------|-----|--------|
| **BLOCKER** | JDK/CI не настроен | Тесты невозможно запустить |
| **CRITICAL** | Clarification loop не завершён | NEEDS_INPUT команды нельзя продолжить |
| **MEDIUM** | start_task intent отсутствует | Нельзя перевести задачу в IN_PROGRESS |
| **LOW** | Метрики не валидированы | Нет формального подтверждения 80%/2s |

---

## Exit Criteria Analysis

### Must-have #1: Команда через API ✅ DONE
**Evidence:** `POST /api/v1/commands` реализован в `CommandController.java`
**Tests:** `MvpJourneyIntegrationTest`, `CommandPipelineTest`

### Must-have #2: Интенты приводят к изменениям ⚠️ PARTIAL

| Intent (MVP.md) | Status | Evidence |
|-----------------|--------|----------|
| создать задачу | ✅ DONE | `create_task` command |
| обновить статус задачи | ⚠️ PARTIAL | `complete_task` → DONE только. **НЕТ start_task → IN_PROGRESS** |
| добавить позиции в список покупок | ✅ DONE | `add_shopping_item` action + REST API |
| отметить купленным | ✅ DONE | `PATCH /shopping-items/{id}` + `markPurchased()` |

**Gap:** Нет команды для перевода задачи в `IN_PROGRESS`. Только `complete_task` (→ DONE).

### Must-have #3: Task scope + zone + assignee ✅ DONE
**Evidence:**
- `Task.householdId` enforced
- `zoneId` в `CreateTaskPayload`
- `ZoneOwnerFirstPolicy`, `MaxOpenTasksPerAssigneePolicy` в guardrails

### Must-have #4: Traceability ✅ DONE
**Evidence:**
- `DecisionLog` создаётся для каждой команды
- `correlationId` пропагируется через все слои
- Тесты проверяют `decisionLogRepository` entries

### Must-have #5: Degraded mode ✅ DONE
**Evidence:**
- `DecisionProviderSelector.decideWithFallback()`
- `AiPlatformIntegrationTest` Scenarios 4, 4b, 4c
- Response status `executed_degraded` + `degradedReason: ai_unavailable`

### Must-have #6: Idempotency ✅ DONE
**Evidence:**
- `CommandIdempotencyService`, `CommandIdempotency` entity
- `CommandIdempotencyIntegrationTest` — same key → replay, different payload → 409
- ADR-012: `docs/architecture/decisions/012-command-reliability-idempotency.md`

### Must-have #7: Invites ✅ DONE
**Evidence:**
- `HouseholdInviteController`, `InviteService`
- `HouseholdInviteIntegrationTest` — create, accept, expired, redeemed, 410 Gone
- ADR-010: `docs/architecture/decisions/010-household-invites.md`

### Must-have #8: Notifications ✅ DONE
**Evidence:**
- `NotificationController` — list, mark read
- `NotificationService` — events для task/shopping/invite
- `NotificationIntegrationTest`
- ADR-011: `docs/architecture/decisions/011-notifications-stub.md`

### Must-have #9: OpenAPI ⚠️ PARTIAL
**Evidence:** `docs/contracts/http/commands.openapi.yaml` существует
**Gap:** Контракт показывает только `create_task` и `complete_task`. Нет `start_task`.

### Must-have #10: Integration tests ✅ DONE (but can't run)
**Evidence:** 16+ integration test classes exist
**BLOCKER:** `JAVA_HOME is not set` — тесты невозможно запустить

---

## Critical Gaps Detail

### GAP-1: JDK/CI не настроен (BLOCKER)

```
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

**Impact:** Exit review невозможен. Все тесты не могут быть выполнены.

**Fix:** Установить JDK 21, настроить JAVA_HOME, или добавить CI pipeline.

**Files:** Нет изменений кода, только окружение.

---

### GAP-2: Clarification loop не завершён (CRITICAL)

**Текущее состояние:**
- Command может вернуть `status: needs_input` с `question` и `requiredFields`
- Command сохраняется в статусе `NEEDS_INPUT`
- **НО:** Нет endpoint для продолжения команды с дополнительным input

**Ожидаемый flow (не реализован):**
```
POST /commands → { status: "needs_input", commandId: X, question: "..." }
POST /commands/{X}/continue → { additionalInput: {...} } → { status: "executed" }
```

**Evidence:**
- `CommandNeedsInputResponse` возвращает `commandId`
- НО в OpenAPI нет endpoint `POST /commands/{id}/continue`
- НО в тестах нет сценария "continue after needs_input"

**Impact:** Пользователь получает вопрос но не может ответить. Команда "застревает".

**Fix required:**
1. Добавить `POST /commands/{commandId}/continue` endpoint
2. Добавить `ContinueCommandRequest` DTO
3. Реализовать логику в `CommandService`
4. Добавить интеграционный тест
5. Обновить OpenAPI contract

**Estimated effort:** 2-3 story points

---

### GAP-3: start_task intent отсутствует (MEDIUM)

**MVP.md говорит:** "обновить статус задачи"
**TaskStatus enum:** `OPEN, IN_PROGRESS, DONE, CANCELLED`

**Текущее состояние:**
- `create_task` → создаёт задачу в статусе `OPEN`
- `complete_task` → переводит в `DONE`
- **НЕТ команды для перевода в `IN_PROGRESS`**

**Impact:** Невозможно начать работу над задачей через commands API.

**Options:**
1. Добавить `start_task` command type
2. Добавить generic `update_task_status` command
3. Clarify MVP: "обновить статус" = только complete (DONE)

**Fix required (if option 1):**
1. Add `START_TASK` to `CommandType`
2. Add `StartTaskPayload` DTO
3. Add schema validation
4. Add business validation (task exists, not already in_progress/done)
5. Add `executeStartTask` in `ActionExecutor`
6. Update OpenAPI contract
7. Add tests

**Estimated effort:** 2 story points

---

## Metrics Status

| Metric | Target | Current Status |
|--------|--------|----------------|
| 80%+ intent accuracy | 80% | **NOT VALIDATED** — нет формального теста |
| p95 latency (degraded) | < 2s | **NOT VALIDATED** — нет performance теста |
| p95 latency (AI path) | < 5s | **NOT VALIDATED** |
| 100% traceability | 100% | ✅ DONE — DecisionLog для каждой команды |
| 0 cross-household leaks | 0 | ✅ DONE — `HouseholdBoundarySecurityTest` |

**Note:** Метрики могут быть валидированы вручную после настройки JDK.

---

## Existing ADRs (Reference)

| ADR | Title | Status |
|-----|-------|--------|
| 001 | MVP Voice Task Scenario | Accepted |
| 002 | MVP Text Command Scenario | Accepted |
| 003 | Stage 1 Commands API | Accepted |
| 004 | Stage 2 AI Platform Integration | Accepted |
| 005 | Stage 3 Guardrails Pipeline | Accepted |
| 006 | Upstream Contract Alignment | Accepted |
| 007 | Stage 4 Context-Driven Autodelegation | Accepted |
| 008 | Stage 5 Task-Shopping Linkage | Accepted |
| 009 | Commands vs CRUD Boundary | Accepted |
| 010 | Household Invites | Accepted |
| 011 | Notifications Stub | Accepted |
| 012 | Command Reliability (Idempotency) | Accepted |

---

## Recommended Action Plan

### Iteration 2 (Immediate — before exit review)

| # | Story | Points | Depends On |
|---|-------|--------|------------|
| 1 | Setup JDK/CI environment | 1 | — |
| 2 | Implement clarification loop (continue endpoint) | 3 | #1 |
| 3 | Decide on start_task scope (clarify MVP or implement) | 1 | — |

### Iteration 3 (If start_task required)

| # | Story | Points | Depends On |
|---|-------|--------|------------|
| 4 | Implement start_task command | 2 | #3 |
| 5 | End-to-end journey test (full lifecycle) | 1 | #2, #4 |

### Exit Review

| # | Story | Points | Depends On |
|---|-------|--------|------------|
| 6 | Run all tests, document results | 1 | #1 |
| 7 | Validate metrics (manual if needed) | 1 | #6 |
| 8 | MVP Closure documentation | 1 | #7 |

---

## Summary

**Реальный статус:** MVP ~75% готов с критическими gaps.

**Blockers:**
1. JDK/CI — без этого exit review невозможен
2. Clarification loop — ключевой UX flow не работает

**Scope decisions needed:**
- `start_task` — нужен для MVP или defer?

**Positive findings:**
- Invites, Notifications, Shopping, Idempotency — полностью реализованы
- Guardrails pipeline работает
- Degraded mode работает
- Traceability и security покрыты тестами
