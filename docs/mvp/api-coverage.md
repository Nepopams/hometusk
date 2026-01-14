# MVP API Coverage Matrix

**Last Updated:** 2026-01-14
**Scope:** MVP Closure / Iteration 1 / Step 1

## User Journey Mapping

| Step | User Action | Endpoint | Method | Auth | Household Check | Status |
|------|-------------|----------|--------|------|-----------------|--------|
| 1 | Login | OIDC via Keycloak | - | External | N/A | Done |
| 2 | Create household | `/api/v1/households` | POST | JWT | N/A (creates new) | **Implemented** |
| 3 | List my households | `/api/v1/users/me` | GET | JWT | N/A (returns user's) | **Implemented** |
| 4 | Create zone | `/api/v1/households/{id}/zones` | POST | JWT | requireMembership | **Implemented** |
| 5 | Create task (manual) | `/api/v1/commands` | POST | JWT | In payload | Done |
| 6 | Complete task | `/api/v1/commands` | POST | JWT | In payload | Done |
| 7 | Run command (AI) | `/api/v1/commands` | POST | JWT | In payload | Done |
| 8 | View tasks list | `/api/v1/households/{id}/tasks` | GET | JWT | requireMembership | **Implemented** |
| 9a | List shopping items | `/api/v1/households/{id}/shopping-lists/{listId}/items` | GET | JWT | requireMembership | **Implemented** |
| 9b | Add shopping item | `/api/v1/households/{id}/shopping-lists/{listId}/items` | POST | JWT | requireMembership | **Implemented** |
| 9c | Mark purchased | `/api/v1/households/{id}/shopping-items/{itemId}` | PATCH | JWT | requireMembership + ownership | **Implemented** |
| 9d | Delete item | `/api/v1/households/{id}/shopping-items/{itemId}` | DELETE | JWT | requireMembership + ownership | **Implemented** |
| 10 | Task details | `/api/v1/households/{id}/tasks/{taskId}` | GET | JWT | requireMembership + ownership | **Implemented** |

## Endpoint Reference

### User Endpoints

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/users/me` | GET | - | `UserProfile` | 401 |

### Household Endpoints

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/households` | POST | `CreateHouseholdRequest` | `Household` (201) | 400, 401 |
| `/api/v1/households/{id}/members` | GET | - | `HouseholdMember[]` | 401, 403 |
| `/api/v1/households/{id}/zones` | GET | - | `Zone[]` | 401, 403 |
| `/api/v1/households/{id}/zones` | POST | `CreateZoneRequest` | `Zone` (201) | 400, 401, 403 |

### Task Endpoints

| Endpoint | Method | Query Params | Response | Errors |
|----------|--------|--------------|----------|--------|
| `/api/v1/households/{id}/tasks` | GET | `status`, `assigneeId`, `zoneId` | `Task[]` | 401, 403 |
| `/api/v1/households/{id}/tasks/{taskId}` | GET | - | `TaskDetail` | 401, 403, 404 |

### Shopping Endpoints

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/households/{id}/shopping-lists` | GET | - | `ShoppingList[]` | 401, 403 |
| `/api/v1/households/{id}/shopping-lists/{listId}/items` | GET | `purchased` | `ShoppingItem[]` | 401, 403, 404 |
| `/api/v1/households/{id}/shopping-lists/{listId}/items` | POST | `AddShoppingItemRequest` | `ShoppingItem` (201) | 400, 401, 403, 404 |
| `/api/v1/households/{id}/shopping-items/{itemId}` | PATCH | `UpdateShoppingItemRequest` | `ShoppingItem` | 401, 403, 404 |
| `/api/v1/households/{id}/shopping-items/{itemId}` | DELETE | - | 204 No Content | 401, 403, 404 |

### Command Endpoints (Existing)

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/commands` | POST | `CommandRequest` | `CommandResponse` | 400, 401, 403, 404, 409 |

## Query Parameters

### Task List Filters

| Param | Type | Description | Example |
|-------|------|-------------|---------|
| `status` | enum | Filter by task status | `?status=open` or `?status=completed` |
| `assigneeId` | UUID | Filter by assignee | `?assigneeId=uuid` |
| `zoneId` | UUID | Filter by zone | `?zoneId=uuid` |

### Shopping Item Filters

| Param | Type | Description | Example |
|-------|------|-------------|---------|
| `purchased` | boolean | Filter by purchase status | `?purchased=false` |

## Validation Rules

### CreateHouseholdRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, trimmed, 1-80 chars, non-blank |

### CreateZoneRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, trimmed, 1-255 chars, non-blank |

### AddShoppingItemRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, 1-255 chars |
| `quantity` | integer | Optional, default 1, min 1 |
| `unit` | string | Optional, max 50 chars |

### UpdateShoppingItemRequest

| Field | Type | Validation |
|-------|------|------------|
| `purchased` | boolean | Required |

## HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Successful GET/PATCH |
| 201 | Created | Successful POST (create) |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation error |
| 401 | Unauthorized | Missing/invalid JWT |
| 403 | Forbidden | Not a household member |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Idempotency conflict |

## Security

All endpoints require:
1. **JWT Authentication** - Valid Keycloak token
2. **Household Boundary Check** - User must be member of target household
3. **Resource Ownership** - For single-resource endpoints, resource must belong to household

See ADR-009 for Commands vs CRUD boundary decisions.
