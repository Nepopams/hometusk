# HomeTusk ↔ Upstream Contract Mapping

> Документ описывает различия между HomeTusk реализацией и canonical upstream контрактами AI Platform

**Upstream Version:** 1.0.0 (см. `upstream/VERSION`)
**HomeTusk Version:** 0.1.0 (см. `contracts/VERSION`)
**Last Updated:** 2026-01-13

---

## Сводка различий

| Аспект | Upstream | HomeTusk | Решение |
|--------|----------|----------|---------|
| Endpoint | `POST /decide` | `POST /decision` | Configurable (default: `/decision`) |
| Decision types | 5 типов | 3 типа | Adapter с safe degradation |
| Action types | 3 типа | 2 типа | Unsupported → Clarify |
| Schema $id | `ai-platform.example.com` | `hometusk.app` | HomeTusk-specific wrapper |

---

## 1. Endpoint Mapping

### Расхождение

| Source | Endpoint |
|--------|----------|
| **Upstream (canonical)** | `POST /decide` |
| **HomeTusk (current)** | `POST /decision` |

### Решение

Endpoint настраивается через конфигурацию:

```yaml
aiplatform:
  base-url: ${AI_PLATFORM_URL:http://localhost:8090}
  decision-path: ${AI_PLATFORM_DECISION_PATH:/decision}  # default: HomeTusk legacy
  # Для upstream: /decide
```

### Код

`AiPlatformClient.java` использует `aiplatform.decision-path` для построения URL.

---

## 2. Decision Types Mapping

### Сравнение

| Upstream Type | HomeTusk Support | Mapping |
|---------------|------------------|---------|
| `start_job` | Полная | → `DecisionResult.StartJob` |
| `propose_create_task` | Partial | → `DecisionResult.StartJob` (execute immediately) |
| `propose_add_shopping_item` | **Не поддерживается** | → `DecisionResult.Clarify` (safe degradation) |
| `clarify` | Полная | → `DecisionResult.Clarify` |
| `reject` | Полная | → `DecisionResult.Reject` |

### Safe Degradation Strategy

```java
// UpstreamDecisionAdapter.java
switch (upstream.type()) {
    case "start_job" -> mapStartJob(upstream);
    case "propose_create_task" -> mapProposeCreateTask(upstream);  // → StartJob
    case "propose_add_shopping_item" -> unsupportedActionClarify(upstream);
    case "clarify" -> mapClarify(upstream);
    case "reject" -> mapReject(upstream);
    default -> unknownTypeReject(upstream);
}
```

**Правила:**
- Неизвестный тип → `Reject` с errorCode `UNKNOWN_DECISION_TYPE`
- Неподдерживаемый тип → `Clarify` с объяснением пользователю

---

## 3. Action Types Mapping

### Сравнение

| Upstream Action | HomeTusk Support | Mapping |
|-----------------|------------------|---------|
| `create_task` | Полная | → `ActionExecutor.createTask()` |
| `complete_task` | Полная | → `ActionExecutor.completeTask()` |
| `add_shopping_item` | **Не поддерживается** | → `Clarify` (safe degradation) |

### Обработка неподдерживаемых action types

```java
// Если actions содержит add_shopping_item
return new DecisionResult.Clarify(
    source,
    confidence,
    decisionId,
    rawPayload,
    "Добавление в список покупок пока не поддерживается. Попробуйте создать задачу.",
    List.of(),
    Map.of("unsupported_action", "add_shopping_item")
);
```

---

## 4. Field Mapping

### Command Request

| HomeTusk Field | Upstream Field | Notes |
|----------------|----------------|-------|
| `commandId` | `commandId` | UUID, 1:1 |
| `correlationId` | `correlationId` | UUID, 1:1 |
| `commandType` | `commandType` | enum mapping (см. ниже) |
| `payload` | `payload` | object, 1:1 |
| `requesterId` | `requesterId` | UUID, 1:1 |
| `householdId` | `householdId` | UUID, 1:1 |
| `householdContext.members` | `householdContext.members` | 1:1 |
| `householdContext.zones` | `householdContext.zones` | 1:1 |

#### commandType enum

| HomeTusk | Upstream |
|----------|----------|
| `create_task` | `create_task` |
| `complete_task` | `complete_task` |
| — | `add_shopping_item` (not supported) |

### Decision Response

| Upstream Field | HomeTusk Field | Notes |
|----------------|----------------|-------|
| `decisionId` | `externalDecisionId` | UUID, stored in DecisionLog |
| `type` | Mapped to sealed class | См. Decision Types |
| `confidence` | `confidence` | BigDecimal |
| `actions` | `proposedActions` | List mapping |
| `question` | `question` | String (for clarify) |
| `requiredFields` | `requiredFields` | List<String> (for clarify) |
| `suggestions` | `suggestions` | Map (for clarify) |
| `reason` | `reason` | String (for reject) |
| `errorCode` | `errorCode` | String (for reject) |

---

## 5. Schema Differences

### HomeTusk Wrapper Schema

HomeTusk использует **wrapper schema** (`docs/integration/ai-platform/v1/contracts/schemas/`), которая:

1. Документирует то, что HomeTusk **фактически принимает**
2. Содержит только поддерживаемые типы
3. Имеет HomeTusk-specific `$id`

### Upstream Canonical Schema

Upstream schema (`docs/integration/ai-platform/v1/upstream/contracts/schemas/`) содержит:

1. Полный набор типов AI Platform
2. Canonical `$id`
3. Все возможные action types

### Валидация

- **Response validation:** против upstream schema (до маппинга)
- **HomeTusk internal:** против wrapper schema (после маппинга)

---

## 6. Traceability

| Field | Source | Storage |
|-------|--------|---------|
| `correlationId` | HTTP header `X-Correlation-ID` | Command, DecisionLog, TaskActivity |
| `externalDecisionId` | `response.decisionId` | DecisionLog |
| `rawDecisionPayload` | Full JSON response | DecisionLog (JSONB) |
| `source` | `AI_PLATFORM` enum | DecisionLog |

---

## 7. Migration Path

### From /decision to /decide

1. Получить подтверждение от AI Platform team
2. Изменить конфиг: `aiplatform.decision-path=/decide`
3. Обновить мониторинг (новый endpoint в dashboards)
4. Удалить legacy path после переходного периода

### Adding new action types

1. Обновить upstream snapshot
2. Добавить маппинг в `UpstreamDecisionAdapter`
3. Реализовать ActionExecutor method
4. Обновить тесты
5. Обновить этот документ

---

## Связанные документы

- [Upstream README](../upstream/README.md)
- [ADR-006: Upstream Contract Alignment](../../../../architecture/decisions/006-upstream-contract-alignment.md)
- [Service Catalog](../../../../architecture/service-catalog.md)
