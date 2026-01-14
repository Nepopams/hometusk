# CHANGELOG: MVP API Hardening (Iteration 1, Step 1)

**Date:** 2026-01-14
**Branch:** `claude/initial-setup-QIo2u`
**ADR:** [ADR-009](../architecture/decisions/009-mvp-commands-vs-crud-boundary.md)

---

## Summary

Backend hardening to provide all REST endpoints needed for the MVP Web UI user journey. This implements 12 new endpoints across 4 controllers, plus comprehensive security boundary tests.

---

## New Endpoints

### UserController (`/api/v1/users/me`)

| Method | Endpoint | Purpose | OpenAPI Ref |
|--------|----------|---------|-------------|
| GET | `/api/v1/users/me` | User profile with household memberships | `#/paths/~1api~1v1~1users~1me/get` |

### HouseholdController (`/api/v1/households`)

| Method | Endpoint | Purpose | OpenAPI Ref |
|--------|----------|---------|-------------|
| POST | `/api/v1/households` | Create new household (auto-add creator as admin) | `#/paths/~1api~1v1~1households/post` |
| GET | `/api/v1/households/{id}/members` | List household members | `#/paths/~1api~1v1~1households~1{householdId}~1members/get` |
| GET | `/api/v1/households/{id}/zones` | List household zones | `#/paths/~1api~1v1~1households~1{householdId}~1zones/get` |
| POST | `/api/v1/households/{id}/zones` | Create zone (idempotent by name) | `#/paths/~1api~1v1~1households~1{householdId}~1zones/post` |

### TaskController (`/api/v1/households/{id}/tasks`)

| Method | Endpoint | Purpose | OpenAPI Ref |
|--------|----------|---------|-------------|
| GET | `/api/v1/households/{id}/tasks` | List tasks (filters: status, assigneeId, zoneId) | `#/paths/~1api~1v1~1households~1{householdId}~1tasks/get` |
| GET | `/api/v1/households/{id}/tasks/{taskId}` | Task detail with linked shopping items | `#/paths/~1api~1v1~1households~1{householdId}~1tasks~1{taskId}/get` |

### ShoppingController (`/api/v1/households/{id}/shopping-*`)

| Method | Endpoint | Purpose | OpenAPI Ref |
|--------|----------|---------|-------------|
| GET | `/api/v1/households/{id}/shopping-lists` | List shopping lists with unpurchased counts | `#/paths/~1api~1v1~1households~1{householdId}~1shopping-lists/get` |
| GET | `/api/v1/households/{id}/shopping-lists/{listId}/items` | List items (filter: purchased) | `#/paths/~1api~1v1~1households~1{householdId}~1shopping-lists~1{listId}~1items/get` |
| POST | `/api/v1/households/{id}/shopping-lists/{listId}/items` | Add shopping item directly | `#/paths/~1api~1v1~1households~1{householdId}~1shopping-lists~1{listId}~1items/post` |
| PATCH | `/api/v1/households/{id}/shopping-items/{itemId}` | Update item (mark purchased/unpurchased) | `#/paths/~1api~1v1~1households~1{householdId}~1shopping-items~1{itemId}/patch` |
| DELETE | `/api/v1/households/{id}/shopping-items/{itemId}` | Delete shopping item | `#/paths/~1api~1v1~1households~1{householdId}~1shopping-items~1{itemId}/delete` |

---

## Files Created

### Controllers
- `services/backend/src/main/java/com/hometusk/users/api/UserController.java`
- `services/backend/src/main/java/com/hometusk/households/api/HouseholdController.java`
- `services/backend/src/main/java/com/hometusk/tasks/api/TaskController.java`
- `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`

### DTOs
- `services/backend/src/main/java/com/hometusk/users/dto/UserProfileDto.java`
- `services/backend/src/main/java/com/hometusk/users/dto/HouseholdSummaryDto.java`
- `services/backend/src/main/java/com/hometusk/households/dto/HouseholdDto.java`
- `services/backend/src/main/java/com/hometusk/households/dto/ZoneDto.java`
- `services/backend/src/main/java/com/hometusk/households/dto/HouseholdMemberDto.java`
- `services/backend/src/main/java/com/hometusk/households/dto/CreateHouseholdRequest.java`
- `services/backend/src/main/java/com/hometusk/households/dto/CreateZoneRequest.java`
- `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDto.java`
- `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDetailDto.java`
- `services/backend/src/main/java/com/hometusk/tasks/dto/UserSummaryDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingListDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/AddShoppingItemRequest.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateShoppingItemRequest.java`

### Documentation
- `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`
- `docs/mvp/api-coverage.md`
- `docs/mvp/CHANGELOG-api-hardening.md` (this file)

### Tests
- `services/backend/src/test/java/com/hometusk/integration/HouseholdBoundarySecurityTest.java`
- `services/backend/src/test/java/com/hometusk/integration/UserControllerTest.java`
- `services/backend/src/test/java/com/hometusk/integration/HouseholdControllerTest.java`
- `services/backend/src/test/java/com/hometusk/integration/TaskControllerTest.java`
- `services/backend/src/test/java/com/hometusk/integration/ShoppingControllerTest.java`

---

## Files Modified

### Service Layer
- `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java` - Added zoneId filter methods
- `services/backend/src/main/java/com/hometusk/tasks/service/TaskService.java` - Added zoneId filter support
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java` - Added direct add/update methods
- `services/backend/src/main/java/com/hometusk/activity/domain/ActivityType.java` - Added `SHOPPING_ITEM_DELETED`
- `services/backend/src/main/java/com/hometusk/activity/service/ActivityRecorder.java` - Added `recordShoppingItemDeleted()`

### Documentation
- `docs/contracts/http/commands.openapi.yaml` - Added all new endpoints
- `docs/architecture/service-catalog.md` - Added REST Controllers section
- `CLAUDE.md` - Added MVP User Journey Endpoints section

---

## Running Tests Locally

```bash
# Navigate to backend directory
cd services/backend

# Run all tests
./gradlew test

# Run only integration tests
./gradlew test --tests "com.hometusk.integration.*"

# Run specific test class
./gradlew test --tests "com.hometusk.integration.HouseholdBoundarySecurityTest"

# Run with verbose output
./gradlew test --info
```

---

## Security Boundary Checks

All household-scoped endpoints implement IDOR prevention via:

```java
membershipService.requireMembership(currentUser.id(), householdId);
```

This is verified by `HouseholdBoundarySecurityTest` with test cases for:
- Cross-household task access (403)
- Cross-household shopping list access (403)
- Cross-household zone access (403)
- Cross-household item modification (403)
- Resource access via wrong household URL (404)
- Unauthenticated access (401)

---

## OpenAPI Contract Location

**File:** `docs/contracts/http/commands.openapi.yaml`

All endpoints are documented with:
- Request/response schemas
- HTTP status codes (200, 201, 204, 400, 401, 403, 404)
- Parameter definitions
- Security requirements (Bearer JWT)
