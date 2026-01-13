# Stage 4: Upstream AI Platform Contract Alignment

**Status:** PLAN (awaiting arch-reviewer approval)
**Date:** 2026-01-13
**Author:** Claude Code

---

## Executive Summary

Выровнять HomeTusk с canonical upstream AI Platform контрактами.
Установить `docs/integration/ai-platform/v1/upstream/` как источник истины (read-only).

---

## Phase 1: Upstream Directory Setup

### Задача
Создать структуру upstream директории с canonical контрактами.

### Файлы для создания

```
docs/integration/ai-platform/v1/upstream/
├── README.md                         # Описание upstream контрактов
├── VERSION                           # Версия/хэш upstream
├── contracts/
│   └── schemas/
│       ├── command.schema.json       # Canonical request schema
│       └── decision.schema.json      # Canonical response schema
├── examples/
│   ├── start-job.json
│   ├── propose-create-task.json      # NEW: propose_create_task
│   ├── propose-add-shopping-item.json # NEW: unsupported action type
│   ├── clarify.json
│   └── reject.json
└── context_v1.md                     # Upstream context spec
```

### Upstream decision.schema.json (предполагаемый)

```json
{
  "type": {
    "enum": ["start_job", "propose_create_task", "propose_add_shopping_item", "clarify", "reject"]
  }
}
```

### Безопасность
- **НЕ ИЗМЕНЯТЬ** файлы в `upstream/` после создания
- Все изменения должны приходить от AI Platform team
- Добавить `.github/CODEOWNERS` для защиты директории

### Файлы для модификации
- `docs/integration/ai-platform/v1/README.md` — добавить ссылку на upstream

---

## Phase 2: Contract Comparison & Alignment

### Задача
Сравнить текущие локальные схемы с upstream и задокументировать расхождения.

### Текущие локальные схемы
- `docs/integration/ai-platform/v1/contracts/schemas/command.schema.json`
- `docs/integration/ai-platform/v1/contracts/schemas/decision.schema.json`

### Ожидаемые расхождения

| Поле | Локальная | Upstream | Действие |
|------|-----------|----------|----------|
| `decision.type` | `start_job, clarify, reject` | `start_job, propose_create_task, propose_add_shopping_item, clarify, reject` | Adapter layer |
| `action.actionType` | `create_task, complete_task` | Возможно другие | Adapter layer |
| Endpoint | `/decision` | `/decide` | Configurable |

### Решение: Wrapper Schema vs Direct Copy

**Рекомендация:** Оставить локальные схемы как **HomeTusk-specific wrappers**.

**Причина:**
1. Upstream схема может содержать типы, которые HomeTusk не поддерживает
2. Локальная схема документирует, что HomeTusk **фактически принимает**
3. Adapter layer преобразует upstream → local

### Файлы для модификации
- `docs/integration/ai-platform/v1/contracts/VERSION` — добавить `upstream_ref`
- `docs/integration/ai-platform/v1/README.md` — документировать различия

---

## Phase 3: Adapter Layer Implementation

### Задача
Создать явный adapter для преобразования между upstream и internal форматами.

### Новые файлы

```
services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/
├── adapter/
│   ├── PlatformCommandAdapter.java       # InternalCommand → PlatformCommandDTO
│   ├── PlatformDecisionAdapter.java      # PlatformDecisionDTO → DecisionResult
│   ├── PlatformCommandDTO.java           # Matches upstream command.schema.json
│   └── PlatformDecisionDTO.java          # Matches upstream decision.schema.json
```

### PlatformDecisionAdapter Logic

```java
public class PlatformDecisionAdapter {

    public DecisionResult adapt(PlatformDecisionDTO upstream) {
        return switch (upstream.type()) {
            case "start_job" -> mapStartJob(upstream);
            case "propose_create_task" -> mapProposeCreateTask(upstream);
            case "propose_add_shopping_item" -> unsupportedAction(upstream);
            case "clarify" -> mapClarify(upstream);
            case "reject" -> mapReject(upstream);
            default -> unknownType(upstream);
        };
    }

    // SAFE DEGRADATION: unsupported action types return Clarify
    private DecisionResult unsupportedAction(PlatformDecisionDTO upstream) {
        log.warn("Unsupported action type: {}", upstream.type());
        return new DecisionResult.Clarify(
            DecisionSource.AI_PLATFORM,
            BigDecimal.ZERO,
            upstream.decisionId(),
            "...",
            "Действие '" + upstream.type() + "' пока не поддерживается. Попробуйте переформулировать.",
            List.of(),
            Map.of("unsupported_type", upstream.type())
        );
    }

    // SAFE DEGRADATION: unknown type returns Reject
    private DecisionResult unknownType(PlatformDecisionDTO upstream) {
        log.error("Unknown decision type: {}", upstream.type());
        return new DecisionResult.Reject(
            DecisionSource.AI_PLATFORM,
            BigDecimal.ZERO,
            upstream.decisionId(),
            "...",
            "Unknown decision type from AI Platform",
            "UNKNOWN_DECISION_TYPE"
        );
    }
}
```

### Файлы для модификации
- `AiPlatformDecisionProvider.java` — использовать adapter вместо mapper
- `AiDecisionResponseMapper.java` — deprecate или удалить

---

## Phase 4: Endpoint Configuration

### Задача
Сделать endpoint конфигурируемым. Default: `/decide` (upstream).

### Текущее состояние
- Hardcoded: `/decision` в `AiPlatformClient.java:56`

### Изменения

```yaml
# application.yml
aiplatform:
  base-url: ${AI_PLATFORM_URL:http://localhost:8090}
  endpoint: ${AI_PLATFORM_ENDPOINT:/decide}  # NEW: default to upstream
  timeout-ms: 5000
```

### Файлы для модификации
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiPlatformClient.java`
- `services/backend/src/main/resources/application.yml`
- `docs/contracts/external/ai-platform.decision.openapi.yaml`

---

## Phase 5: Bidirectional Validation

### Задача
Валидировать запросы и ответы против upstream схем.

### 5.1 Outgoing Request Validation

**Новый компонент:** `RequestSchemaValidator`

```java
@Component
public class RequestSchemaValidator {
    // Validates AiDecisionRequest against upstream command.schema.json
    public ValidationResult validateRequest(AiDecisionRequest request);
}
```

**Поведение при ошибке валидации запроса:**
- Log error
- Throw `IllegalStateException` (это баг в нашем коде, не в AI Platform)
- НЕ отправлять невалидный запрос

### 5.2 Incoming Response Validation (существует)

**Текущий:** `AiResponseSchemaValidator`

**Изменения:**
- Переименовать в `ResponseSchemaValidator`
- Валидировать против **upstream** схемы (не локальной)
- На ошибку: вернуть `DecisionResult.Reject` (уже реализовано)

### Файлы для создания
- `RequestSchemaValidator.java`

### Файлы для модификации
- `AiResponseSchemaValidator.java` → `ResponseSchemaValidator.java`
- `src/main/resources/schemas/ai-decision-response.schema.json` — заменить на upstream copy

---

## Phase 6: WireMock Tests Update

### Задача
Использовать upstream examples для тестов.

### Текущие тесты
- `AiPlatformIntegrationTest.java` — 5 сценариев

### Новые сценарии

| Сценарий | Upstream Example | Expected Result |
|----------|------------------|-----------------|
| start_job success | `upstream/examples/start-job.json` | Task created |
| propose_create_task | `upstream/examples/propose-create-task.json` | Task created (mapped) |
| propose_add_shopping_item | `upstream/examples/propose-add-shopping-item.json` | **Clarify** (unsupported) |
| clarify | `upstream/examples/clarify.json` | NEEDS_INPUT |
| reject | `upstream/examples/reject.json` | REJECTED |
| invalid schema | malformed JSON | **Reject** (validation fail) |
| missing required fields | partial JSON | **Reject** (validation fail) |

### Файлы для модификации
- `AiPlatformIntegrationTestBase.java` — добавить stubs из upstream examples
- `AiPlatformIntegrationTest.java` — добавить 3 новых сценария

---

## Phase 7: Documentation Update

### Задача
Обновить документацию для отражения upstream-first подхода.

### 7.1 mapping/hometusk-to-aiplatform.md

Добавить секции:
- **Upstream vs Local schemas** — объяснить различия
- **Safe degradation matrix** — какие типы как обрабатываются
- **Endpoint migration path** — `/decision` → `/decide`

### 7.2 CLAUDE.md

Добавить правило:
```markdown
### 7. AI Platform Contracts are Canonical

- Upstream contracts at `docs/integration/ai-platform/v1/upstream/` are READ-ONLY
- Any contract change requires ADR and coordination with AI Platform team
- HomeTusk adapts to upstream, never the reverse
```

### 7.3 Service Catalog

Обновить External Dependencies секцию:
- Добавить ссылку на upstream contracts
- Документировать endpoint configuration

### Файлы для модификации
- `docs/integration/ai-platform/v1/mapping/hometusk-to-aiplatform.md`
- `CLAUDE.md`
- `docs/architecture/service-catalog.md`

---

## Phase 8: ADR Creation

### Задача
Создать ADR-006 для фиксации решения.

### ADR-006: Upstream Contract Alignment

**Decision:**
- Upstream contracts are source of truth
- HomeTusk uses adapter layer for compatibility
- Unsupported decision types degrade safely to Clarify
- Endpoint is configurable (default: `/decide`)

### Файлы для создания
- `docs/architecture/decisions/006-upstream-contract-alignment.md`

---

## Safety Notes (НЕ ИЗМЕНЯТЬ)

### Файлы, которые НЕ ДОЛЖНЫ модифицироваться

1. **Upstream contracts (после создания)**
   - `docs/integration/ai-platform/v1/upstream/**/*`

2. **Domain entities**
   - `Task.java`, `Command.java`, `DecisionLog.java` — НЕ менять структуру

3. **Public API contracts**
   - `POST /api/v1/commands` — request/response format

4. **Existing guardrails**
   - `GuardrailPolicy` implementations — Stage 3 работает

### Принципы безопасности

| Ситуация | Поведение |
|----------|-----------|
| Unknown decision type | `Reject` с errorCode |
| Unsupported action type | `Clarify` с объяснением |
| Schema validation fail (response) | `Reject` с errorCode `AI_RESPONSE_INVALID` |
| Schema validation fail (request) | `IllegalStateException` (это наш баг) |
| Network error | Fallback to `ManualDecisionProvider` |

---

## Implementation Order

| Phase | Зависимости | Риск |
|-------|-------------|------|
| 1. Upstream setup | - | Low |
| 2. Contract comparison | Phase 1 | Low |
| 3. Adapter layer | Phase 2 | Medium |
| 4. Endpoint config | - | Low |
| 5. Validation | Phase 3 | Medium |
| 6. Tests | Phase 3, 5 | Low |
| 7. Documentation | Phase 1-6 | Low |
| 8. ADR | Phase 1-7 | Low |

**Estimated files:**
- **New:** 8-10 files
- **Modified:** 10-12 files
- **Deprecated:** 1 file (`AiDecisionResponseMapper.java`)

---

## Exit Criteria

1. [ ] Upstream directory exists with canonical schemas
2. [ ] Adapter layer handles all upstream decision types
3. [ ] Unsupported types degrade safely (no exceptions)
4. [ ] Endpoint is configurable, defaults to `/decide`
5. [ ] Bidirectional validation implemented
6. [ ] WireMock tests use upstream examples
7. [ ] Documentation reflects upstream-first approach
8. [ ] ADR-006 created and approved

---

## Questions for Upstream Team

1. Где получить актуальные canonical schemas?
2. Есть ли версионирование контрактов (semver, commit hash)?
3. Какие action types планируются в будущем?
4. Есть ли breaking changes в roadmap?
