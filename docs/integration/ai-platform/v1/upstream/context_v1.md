# Context v1 Specification (Upstream)

> Canonical спецификация контекста запроса к AI Platform

## Endpoint

**URL:** `POST /decide`

## Request Structure

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `commandId` | UUID | Unique command identifier from client |
| `correlationId` | UUID | Distributed tracing ID |
| `commandType` | string | Command type: `create_task`, `complete_task`, `add_shopping_item` |
| `payload` | object | Command-specific payload |
| `requesterId` | UUID | User ID who initiated the command |
| `householdId` | UUID | Household context ID |

### Optional Fields

| Field | Type | Description |
|-------|------|-------------|
| `householdContext` | object | Household context for decision-making |
| `householdContext.members` | array | Household members |
| `householdContext.zones` | array | Household zones |

## Response Types

| Type | Description | Required Fields |
|------|-------------|-----------------|
| `start_job` | Execute actions immediately | `actions` |
| `propose_create_task` | Propose task creation | `actions` |
| `propose_add_shopping_item` | Propose shopping item | `actions` |
| `clarify` | Request user clarification | `question` |
| `reject` | Reject command | `reason` |

## Example Request

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
      {"id": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "name": "Иван", "role": "admin"},
      {"id": "8c9e6679-7425-40de-944b-e07fc1f90ae8", "name": "Мария", "role": "member"}
    ],
    "zones": [
      {"id": "a47ac10b-58cc-4372-a567-0e02b2c3d480", "name": "Кухня"},
      {"id": "b47ac10b-58cc-4372-a567-0e02b2c3d481", "name": "Гостиная"}
    ]
  }
}
```

## Versioning

Contract version follows semver: MAJOR.MINOR.PATCH

Breaking changes require MAJOR version bump.
