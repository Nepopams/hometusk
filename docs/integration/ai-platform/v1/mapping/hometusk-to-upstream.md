# HomeTusk ↔ Upstream Contract Mapping

> Документ описывает различия между HomeTusk реализацией и canonical upstream контрактами AI Platform

**Upstream Version:** 1.0.0 (см. `upstream/VERSION`)
**HomeTusk Version:** 0.1.0 (см. `contracts/VERSION`)
**Last Updated:** 2026-01-13

---

## Сводка различий

| Аспект | Upstream | HomeTusk | Решение |
|--------|----------|----------|---------|
| Endpoint | `POST /decide` | `POST /v1/decide` | Configurable (default: `/v1/decide`) |
| Decision types | 5 типов | 3 типа | Adapter с safe degradation |
| Action types | 3 типа | 3 типа | Adapter maps upstream proposed actions |
| Schema $id | `ai-platform.example.com` | `hometusk.app` | HomeTusk-specific wrapper |

---

## 1. Endpoint Mapping

### Расхождение

| Source | Endpoint |
|--------|----------|
| **Upstream (canonical)** | `POST /decide` |
| **HomeTusk (current UAT/default path)** | `POST /v1/decide` |

### Решение

Endpoint настраивается через конфигурацию:

```yaml
aiplatform:
  base-url: ${AI_PLATFORM_URL:http://localhost:8090}
  decision-path: ${AI_PLATFORM_DECISION_PATH:/v1/decide}
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
| `propose_add_shopping_item` | Полная | → `DecisionResult.StartJob` with `add_shopping_item` |
| `clarify` | Полная | → `DecisionResult.Clarify` |
| `reject` | Полная | → `DecisionResult.Reject` |

### Safe Degradation Strategy

```java
// UpstreamDecisionAdapter.java
switch (upstream.type()) {
    case "start_job" -> mapStartJob(upstream);
    case "propose_create_task" -> mapProposeCreateTask(upstream);  // → StartJob
    case "propose_add_shopping_item" -> mapProposeAddShoppingItem(upstream);
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
| `add_shopping_item` | Полная | → `ActionExecutor.addShoppingItem()` |

### Proposed action mapping

```java
propose_create_task -> create_task
propose_add_shopping_item -> add_shopping_item
```

---

## 4. Field Mapping

### Command Request

> Runtime note (2026-06-14): HomeTusk now sends the upstream snake_case
> envelope directly: `command_id`, `user_id`, `timestamp`, `text`,
> `capabilities`, `context.household`, and `context.defaults`. The legacy
> camelCase table below is retained as migration context only.

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
| `householdContext.default_list_id` | `context.defaults.default_list_id` | First/oldest household shopping list when available |
| `requesterId` | `context.defaults.default_assignee_id` | Requesting user as deterministic default |

#### commandType enum

| HomeTusk | Upstream |
|----------|----------|
| `create_task` | `create_task` |
| `complete_task` | `complete_task` |
| — | `add_shopping_item` (not supported) |

### Decision Response

> Runtime note (2026-06-14): HomeTusk validates and maps the upstream response
> envelope: `decision_id`, `status`, `action`, `payload`, `explanation`, and
> trace/version fields. Nested `payload.proposed_actions[]` is converted into
> internal `create_task` / `add_shopping_item` actions.

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
2. Изменить конфиг: `aiplatform.decision-path=/v1/decide`
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
