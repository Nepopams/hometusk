# HomeTusk → AI Platform Mapping

> Документ описывает маппинг между внутренними сущностями HomeTusk и API AI Platform

## Endpoint

### Расхождение: `/decide` vs `/decision`

| Источник | Endpoint |
|----------|----------|
| AI Platform Integration Package (spec) | `POST /decide` |
| HomeTusk реализация (текущая) | `POST /decision` |

**Решение:** HomeTusk использует `/decision` как указано в текущем OpenAPI контракте.

**Причина:** На момент реализации Stage 2, API AI Platform был определён с endpoint `/decision`.
Если AI Platform мигрирует на `/decide`, потребуется:

1. Обновить `AiPlatformClient.java`
2. Обновить `docs/contracts/external/ai-platform.decision.openapi.yaml`
3. Обновить интеграционные тесты

**TODO:** Уточнить у команды AI Platform актуальный endpoint и при необходимости создать ADR для миграции.

---

## Маппинг идентификаторов

### Command ID → command_id

| HomeTusk | AI Platform | Описание |
|----------|-------------|----------|
| `Command.id` | `commandId` | UUID команды в HomeTusk |

**Код:** `AiDecisionRequest.java:22`

```java
public AiDecisionRequest(DecisionContext context) {
    this.commandId = context.getCommandId();
    ...
}
```

### Correlation ID → trace_id

| HomeTusk | AI Platform | Описание |
|----------|-------------|----------|
| `correlationId` (HTTP header `X-Correlation-ID`) | `correlationId` | ID для распределённой трассировки |

**Поток:**
1. Клиент отправляет `X-Correlation-ID` в HTTP заголовке
2. `CorrelationIdFilter` извлекает или генерирует ID
3. ID передаётся через `DecisionContext`
4. `AiDecisionRequest` включает его в запрос к AI Platform

**Код:** `AiDecisionRequest.java:23`

```java
this.correlationId = context.getCorrelationId();
```

---

## Маппинг пользователя

### JWT `sub` → user_id

| HomeTusk | AI Platform | Описание |
|----------|-------------|----------|
| JWT claim `sub` | `requesterId` | ID пользователя |

**Поток:**
1. Keycloak выдаёт JWT с claim `sub` (subject)
2. `UserResolver` извлекает `sub` из токена
3. Находит `User` в БД по `externalId = sub`
4. `User.id` передаётся как `requesterId`

**Код:** `UserResolver.java`

```java
String externalId = jwt.getClaimAsString("sub");
User user = userRepository.findByExternalId(externalId);
```

---

## Построение Context v1 из доменных сущностей

### Источники данных

| Поле Context | Источник в HomeTusk |
|--------------|---------------------|
| `commandId` | `Command.id` |
| `correlationId` | HTTP header или сгенерированный UUID |
| `commandType` | `Command.type` (enum) |
| `payload` | `Command.payload` (JSONB) |
| `requesterId` | `User.id` (из JWT) |
| `householdId` | `CommandRequest.householdId` |
| `householdContext.members` | `MembershipRepository.findByHouseholdId()` |
| `householdContext.zones` | `ZoneRepository.findByHouseholdId()` |

### Построение householdContext

```java
// DecisionContext.Builder
public Builder withHouseholdContext(UUID householdId) {
    List<Membership> memberships = membershipRepository
        .findByHouseholdId(householdId);
    List<Zone> zones = zoneRepository
        .findByHouseholdId(householdId);

    this.householdContext = new HouseholdContext(
        memberships.stream()
            .map(m -> new MemberDto(m.getUserId(), m.getUser().getName()))
            .toList(),
        zones.stream()
            .map(z -> new ZoneDto(z.getId(), z.getName()))
            .toList()
    );
    return this;
}
```

### Минимизация данных

В `householdContext` передаётся **только необходимый минимум**:

| Передаётся | НЕ передаётся |
|------------|---------------|
| `member.id` | `member.email` |
| `member.name` | `member.role` |
| `zone.id` | `zone.description` |
| `zone.name` | Временные данные |

**Причина:** AI Platform не должен иметь доступ к чувствительным данным пользователей.

---

## Маппинг ответа AI Platform

### Decision Response → DecisionResult

| AI Platform | HomeTusk | Тип |
|-------------|----------|-----|
| `type: start_job` | `DecisionResult.StartJob` | Sealed class |
| `type: clarify` | `DecisionResult.Clarify` | Sealed class |
| `type: reject` | `DecisionResult.Reject` | Sealed class |

**Код:** `AiDecisionResponseMapper.java`

```java
public DecisionResult map(AiDecisionResponse response, String rawPayload) {
    return switch (response.type()) {
        case "start_job" -> new DecisionResult.StartJob(
            response.actions(),
            response.confidence(),
            response.decisionId(),
            rawPayload
        );
        case "clarify" -> new DecisionResult.Clarify(
            response.question(),
            response.requiredFields(),
            response.suggestions(),
            response.confidence(),
            response.decisionId(),
            rawPayload
        );
        case "reject" -> new DecisionResult.Reject(
            response.reason(),
            response.errorCode(),
            response.confidence(),
            response.decisionId(),
            rawPayload
        );
        default -> throw new AiPlatformException("Unknown decision type");
    };
}
```

### Сохранение для аудита

| Поле | Источник | Таблица |
|------|----------|---------|
| `external_decision_id` | `response.decisionId` | `decision_logs` |
| `raw_decision_payload` | Полный JSON ответа | `decision_logs` |
| `source` | `'ai_platform'` | `decision_logs` |
| `confidence` | `response.confidence` | `decision_logs` |

---

## Связанные документы

- [Context v1 Specification](../context_v1.md)
- [OpenAPI Contract](../../../../contracts/external/ai-platform.decision.openapi.yaml)
- [ADR-004: AI Platform Integration](../../../../architecture/decisions/004-stage2-ai-platform-integration.md)
