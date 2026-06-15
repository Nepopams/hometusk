# AI Platform Integration Package v1

> Документация интеграции HomeTusk ↔ AI Platform

## Обзор

HomeTusk является **потребителем** внешнего AI Platform для принятия решений по командам пользователей.
AI Platform анализирует команды на естественном языке и возвращает структурированные решения.

**Upstream-first подход:** Canonical контракты хранятся в `upstream/`. HomeTusk адаптируется к upstream.

## Структура пакета

```
v1/
├── README.md                          # Этот файл
├── context_v1.md                      # Спецификация контекста запроса
├── upstream/                          # ← ИСТОЧНИК ИСТИНЫ (read-only)
│   ├── README.md                      # Описание vendor snapshot
│   ├── VERSION                        # Версия upstream контрактов (1.0.0)
│   ├── context_v1.md                  # Upstream спецификация
│   ├── contracts/schemas/             # Canonical JSON Schemas
│   └── examples/                      # Upstream примеры
├── contracts/
│   ├── VERSION                        # Версия HomeTusk wrapper (0.2.0)
│   └── schemas/                       # HomeTusk wrapper schemas
├── examples/                          # HomeTusk примеры
└── mapping/
    ├── hometusk-to-aiplatform.md      # Legacy маппинг
    └── hometusk-to-upstream.md        # Upstream маппинг и различия
```

## Endpoint

**Upstream canonical:** `POST /decide`
**HomeTusk UAT/default path:** `POST /v1/decide`

Endpoint настраивается через `aiplatform.decision-path`:
```yaml
aiplatform:
  decision-path: /v1/decide
```

## Типы решений (Upstream)

| Тип | Описание | Поддержка HomeTusk |
|-----|----------|-------------------|
| `start_job` | Выполнить предложенные действия | Полная |
| `propose_create_task` | Предложить создание задачи | Маппится на start_job |
| `propose_add_shopping_item` | Предложить добавление в список | Маппится на `add_shopping_item` |
| `clarify` | Нужно уточнение от пользователя | Полная |
| `reject` | Невозможно обработать команду | Полная |

## Safe Degradation

- Неподдерживаемые типы → `Clarify` с понятным сообщением
- Неизвестные типы → `Reject` с errorCode `UNKNOWN_DECISION_TYPE`
- Ошибка валидации схемы → `Reject`

## Связанные документы

- [Upstream README](upstream/README.md)
- [Upstream ↔ HomeTusk Mapping](mapping/hometusk-to-upstream.md)
- [ADR-004: AI Platform Integration](../../../architecture/decisions/004-stage2-ai-platform-integration.md)
- [ADR-006: Upstream Contract Alignment](../../../architecture/decisions/006-upstream-contract-alignment.md)
- [Service Catalog](../../../architecture/service-catalog.md)

## Клиентский код

Реализация клиента: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/`

Основные классы:
- `AiPlatformClient.java` — HTTP клиент (configurable endpoint)
- `AiDecisionRequest.java` — модель запроса
- `AiDecisionResponse.java` — модель ответа
- `AiDecisionResponseMapper.java` — маппинг ответа в DecisionResult (safe degradation)
- `AiResponseSchemaValidator.java` — валидация против upstream schema

## Конфигурация

```yaml
aiplatform:
  base-url: ${AI_PLATFORM_URL:http://localhost:8090}
  decision-path: ${AI_PLATFORM_DECISION_PATH:/v1/decide}
  timeout-ms: ${AI_PLATFORM_TIMEOUT_MS:5000}
  api-key: ${AI_PLATFORM_API_KEY:}
```

## Валидация контрактов

```bash
./scripts/validate-aiplatform-contracts.sh
```
