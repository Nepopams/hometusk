# AI Platform Integration Package v1

> Документация интеграции HomeTusk ↔ AI Platform

## Обзор

HomeTusk является **потребителем** внешнего AI Platform для принятия решений по командам пользователей.
AI Platform анализирует команды на естественном языке и возвращает структурированные решения.

## Структура пакета

```
v1/
├── README.md                          # Этот файл
├── context_v1.md                      # Спецификация контекста запроса
├── contracts/
│   ├── VERSION                        # Версия контрактов
│   └── schemas/
│       ├── command.schema.json        # JSON Schema для запроса
│       └── decision.schema.json       # JSON Schema для ответа
├── examples/
│   ├── start-job-response.json        # Пример: создание задачи
│   ├── clarify-response.json          # Пример: уточнение
│   └── reject-response.json           # Пример: отклонение
└── mapping/
    └── hometusk-to-aiplatform.md      # Маппинг полей HomeTusk → AI Platform
```

## Endpoint

**Decision API:** `POST /decision`

> **Примечание:** AI Platform Integration Package определяет endpoint как `/decide`,
> однако текущая реализация HomeTusk использует `/decision`.
> См. [mapping/hometusk-to-aiplatform.md](mapping/hometusk-to-aiplatform.md) для деталей.

## Типы решений

| Тип | Описание | Действие HomeTusk |
|-----|----------|-------------------|
| `start_job` | Выполнить предложенные действия | Создать/выполнить задачу |
| `clarify` | Нужно уточнение от пользователя | Показать вопрос пользователю |
| `reject` | Невозможно обработать команду | Показать причину отказа |

## Связанные документы

- [OpenAPI контракт](../../../contracts/external/ai-platform.decision.openapi.yaml)
- [ADR-004: AI Platform Integration](../../../architecture/decisions/004-stage2-ai-platform-integration.md)
- [Service Catalog](../../../architecture/service-catalog.md)

## Клиентский код

Реализация клиента: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/`

Основные классы:
- `AiPlatformClient.java` — HTTP клиент
- `AiDecisionRequest.java` — модель запроса
- `AiDecisionResponse.java` — модель ответа
- `AiDecisionResponseMapper.java` — маппинг ответа в DecisionResult

## Конфигурация

```yaml
aiplatform:
  base-url: ${AI_PLATFORM_URL:http://localhost:8090}
  timeout-ms: ${AI_PLATFORM_TIMEOUT_MS:5000}
  api-key: ${AI_PLATFORM_API_KEY:}
```

## Валидация контрактов

```bash
./scripts/validate-aiplatform-contracts.sh
```
