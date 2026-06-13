# MVP API Coverage Matrix

**Last Updated:** 2026-06-13
**Scope:** MVP Closure / Manual Shopping Flow
**Source of truth:** `docs/contracts/http/commands.openapi.yaml`

## User Journey Mapping

| Step | User Action | Endpoint | Method | Auth | Household Check | Status |
|------|-------------|----------|--------|------|-----------------|--------|
| 1 | Login | OIDC via Keycloak | - | External | N/A | Done |
| 2 | Create household | `/api/v1/households` | POST | JWT | N/A (creates new) | **Implemented** |
| 3 | List my households | `/api/v1/users/me` | GET | JWT | N/A (returns user's) | **Implemented** |
| 3a | Create invite | `/api/v1/households/{id}/invites` | POST | JWT | requireMembership | **Implemented** |
| 3b | Accept invite | `/api/v1/invites/accept` | POST | JWT | N/A (token-based) | **Implemented** |
| 4 | Create zone | `/api/v1/households/{id}/zones` | POST | JWT | requireMembership | **Implemented** |
| 5 | Create task (manual) | `/api/v1/commands` | POST | JWT | In payload | **Implemented** |
| 6 | Complete task | `/api/v1/commands` | POST | JWT | In payload | **Implemented** |
| 7 | Run command (AI) | `/api/v1/commands` | POST | JWT | In payload | **Implemented** |
| 8 | View tasks list | `/api/v1/households/{id}/tasks` | GET | JWT | requireMembership | **Implemented** |
| 9a | List shopping lists | `/api/v1/households/{id}/shopping-lists` | GET | JWT | requireMembership | **Implemented** |
| 9b | Create shopping list | `/api/v1/households/{id}/shopping-lists` | POST | JWT | requireMembership | **Implemented** |
| 9c | List shopping items | `/api/v1/households/{id}/shopping-lists/{listId}/items` | GET | JWT | requireMembership + household-scope | **Implemented** |
| 9d | Add shopping item | `/api/v1/households/{id}/shopping-lists/{listId}/items` | POST | JWT | requireMembership + household-scope + linked task household-scope | **Implemented** |
| 9e | Update item / mark purchased / link-unlink task | `/api/v1/households/{id}/shopping-items/{itemId}` | PATCH | JWT | requireMembership + household-scope + linked task household-scope | **Implemented** |
| 9f | Delete item | `/api/v1/households/{id}/shopping-items/{itemId}` | DELETE | JWT | requireMembership + household-scope | **Implemented** |
| 10 | Task details | `/api/v1/households/{id}/tasks/{taskId}` | GET | JWT | requireMembership + household-scope | **Implemented** |
| 11 | View notifications | `/api/v1/households/{id}/notifications` | GET | JWT | requireMembership | **Implemented** |
| 12 | Mark notification read | `/api/v1/notifications/{id}/read` | POST | JWT | ownership enforced | **Implemented** |

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
| `/api/v1/households/{id}/invites` | POST | - | `CreateInviteResponse` (201) | 401, 403 |

### Invite Endpoints

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/invites/accept` | POST | `AcceptInviteRequest` | `AcceptInviteResponse` | 400, 401, 404, 410 |

### Task Endpoints

| Endpoint | Method | Query Params | Response | Errors |
|----------|--------|--------------|----------|--------|
| `/api/v1/households/{id}/tasks` | GET | `status`, `assigneeId`, `zoneId` | `Task[]` | 401, 403 |
| `/api/v1/households/{id}/tasks/{taskId}` | GET | - | `TaskDetail` | 401, 403, 404 |

### Shopping Endpoints

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/households/{id}/shopping-lists` | GET | - | `ShoppingList[]` | 401, 403 |
| `/api/v1/households/{id}/shopping-lists` | POST | `CreateShoppingListRequest` | `ShoppingList` (201) | 400, 401, 403 |
| `/api/v1/households/{id}/shopping-lists/{listId}/items` | GET | `purchased`, `category`, `source` | `ShoppingItem[]` | 400, 401, 403, 404 |
| `/api/v1/households/{id}/shopping-lists/{listId}/items` | POST | `AddShoppingItemRequest` | `ShoppingItem` (201) | 400, 401, 403, 404 |
| `/api/v1/households/{id}/shopping-items/{itemId}` | PATCH | `UpdateShoppingItemRequest` | `ShoppingItem` | 400, 401, 403, 404 |
| `/api/v1/households/{id}/shopping-items/{itemId}` | DELETE | - | 204 No Content | 401, 403, 404 |

### Notification Endpoints

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/households/{id}/notifications` | GET | `since`, `limit` | `Notification[]` | 400, 401, 403 |
| `/api/v1/notifications/{id}/read` | POST | - | `Notification` | 401, 404 |

### Command Endpoints (Existing)

| Endpoint | Method | Request | Response | Errors |
|----------|--------|---------|----------|--------|
| `/api/v1/commands` | POST | `CommandRequest` (+ optional `Idempotency-Key`) | `CommandResponse` (200: executed / needs_input / rejected / executed_degraded) | 400, 401, 403, 409 |

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
| `category` | enum | Filter by shopping category | `?category=groceries` |
| `source` | string | Exact source/store match after trim | `?source=Ozon` |

### Notification List Filters

| Param | Type | Description | Example |
|-------|------|-------------|---------|
| `since` | RFC3339 timestamp | Return notifications created after this time | `?since=2026-01-16T12:00:00Z` |
| `limit` | integer | Max results (default 50, max 200) | `?limit=100` |

## Validation Rules

### CreateHouseholdRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, trimmed, 1-80 chars, non-blank |

### CreateZoneRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, trimmed, 1-255 chars, non-blank |

### CreateShoppingListRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, trimmed, 1-80 chars |

### AddShoppingItemRequest

| Field | Type | Validation |
|-------|------|------------|
| `name` | string | Required, 1-255 chars |
| `quantity` | integer | Optional, default 1, min 1 |
| `unit` | string | Optional, max 50 chars |
| `category` | enum | Optional, one of `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other` |
| `source` | string | Optional, trimmed, blank -> null, max 120 chars |
| `linkedTaskId` | UUID | Optional, task must belong to same household |

### UpdateShoppingItemRequest

| Field | Type | Validation |
|-------|------|------------|
| `purchased` | boolean | Optional; if present cannot be null |
| `category` | enum/null | Optional, null clears category |
| `source` | string/null | Optional, null/blank clears source, max 120 chars |
| `linkedTaskId` | UUID/null | Optional; omitted keeps link, null unlinks, UUID must belong to same household |

### AcceptInviteRequest

| Field | Type | Validation |
|-------|------|------------|
| `inviteToken` | string | Required, non-blank |

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
| 409 | Conflict | Idempotency conflict (key reuse with different payload) |
| 410 | Gone | Invite expired, redeemed, or revoked |

## Security

All endpoints require:
1. **JWT Authentication** - Valid Keycloak token
2. **Household Boundary Check** - User must be member of target household
3. **Household Scope** - Resource must belong to the household in the URL

See ADR-009 for Commands vs CRUD boundary decisions.
