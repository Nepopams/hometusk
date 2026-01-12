# Context v1 Specification

> Спецификация контекста, передаваемого в AI Platform для принятия решений

## Версия

**Context Version:** 1.0

## Структура контекста

### Обязательные поля

| Поле | Тип | Описание |
|------|-----|----------|
| `commandId` | UUID | Уникальный идентификатор команды в HomeTusk |
| `correlationId` | UUID | ID для распределённой трассировки |
| `commandType` | string | Тип команды: `create_task`, `complete_task` |
| `payload` | object | Payload команды (зависит от типа) |
| `requesterId` | UUID | ID пользователя, инициировавшего команду |
| `householdId` | UUID | ID домохозяйства |

### Опциональные поля

| Поле | Тип | Описание |
|------|-----|----------|
| `householdContext` | object | Контекст домохозяйства для принятия решения |
| `householdContext.members` | array | Список членов домохозяйства |
| `householdContext.zones` | array | Список зон домохозяйства |

## Household Context

### Members

```json
{
  "members": [
    {
      "id": "uuid",
      "name": "string"
    }
  ]
}
```

Передаётся минимальный набор данных:
- Только ID и имя
- Не передаются: email, роли, настройки
- AI Platform не должен иметь доступ к чувствительным данным

### Zones

```json
{
  "zones": [
    {
      "id": "uuid",
      "name": "string"
    }
  ]
}
```

## Payload по типам команд

### create_task

```json
{
  "title": "Убрать кухню",
  "description": "Помыть посуду и протереть столы",
  "zoneId": "uuid (optional)",
  "assigneeId": "uuid (optional)",
  "deadline": "2025-01-15T18:00:00Z (optional)"
}
```

### complete_task

```json
{
  "taskId": "uuid"
}
```

## Пример полного контекста

```json
{
  "commandId": "550e8400-e29b-41d4-a716-446655440000",
  "correlationId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "commandType": "create_task",
  "payload": {
    "title": "Убрать кухню сегодня вечером"
  },
  "requesterId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "householdId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "householdContext": {
    "members": [
      {"id": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "name": "Иван"},
      {"id": "8c9e6679-7425-40de-944b-e07fc1f90ae8", "name": "Мария"}
    ],
    "zones": [
      {"id": "a47ac10b-58cc-4372-a567-0e02b2c3d480", "name": "Кухня"},
      {"id": "b47ac10b-58cc-4372-a567-0e02b2c3d481", "name": "Гостиная"}
    ]
  }
}
```

## Построение контекста в HomeTusk

Контекст формируется в `DecisionContext.java`:

1. `commandId` — из созданной сущности Command
2. `correlationId` — из HTTP заголовка `X-Correlation-ID` или генерируется
3. `commandType` — из `Command.type`
4. `payload` — из `Command.payload`
5. `requesterId` — из JWT токена (поле `sub`)
6. `householdId` — из запроса
7. `householdContext` — загружается из БД по householdId

## Валидация

Контекст валидируется:
1. **В HomeTusk** — перед отправкой в AI Platform
2. **В AI Platform** — при получении (согласно `command.schema.json`)

## Версионирование

При изменении структуры контекста:
1. Создать новую папку `v2/`
2. Обновить `context_v2.md`
3. Поддерживать обратную совместимость в переходный период
