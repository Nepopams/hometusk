# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] - Stage 5: Task ↔ Shopping Linkage

### Added
- **Shopping Module (Stage 5)**
  - `ShoppingList` and `ShoppingItem` JPA entities
  - `ShoppingService` with operations: addItem, markPurchased, deleteItem, linkItemsToTask
  - `ShoppingListRepository` and `ShoppingItemRepository`
  - Migration V012 adding `linked_task_id` and `idempotency_key` columns

- **AI Platform Integration**
  - Enabled `add_shopping_item` action type in `ActionExecutor`
  - Enabled `propose_add_shopping_item` decision type in `AiDecisionResponseMapper`
  - Shopping lists included in AI context (`ContextBuilder`)
  - Task-shopping linking when both created in same decision

- **Guardrails**
  - `ShoppingItemValidationPolicy` - validates item name (non-empty, max 255 chars)

- **Activity Recording**
  - `recordShoppingItemAdded` method in `ActivityRecorder`
  - `recordShoppingItemPurchased` method in `ActivityRecorder`

- **Integration Tests**
  - `ShoppingIntegrationTest` with 6 scenarios:
    - Task + shopping items linked
    - Shopping items without task
    - Idempotent retry (no duplicates)
    - Invalid schema rejected
    - Household boundary violation
    - Full-chain test

- **Documentation**
  - ADR-008: Stage 5 Task-Shopping Linkage
  - Updated service-catalog.md with shopping module

### Changed
- `CommandService` now tracks created entities for task-shopping linking
- `ActionExecutor` uses generic `entityId` instead of `taskId` in `ActionResult`
- `ContextBuilder` includes `shopping_lists` in AI context

### Technical Details
- **Idempotency Strategy**: SHA-256 hash of (command_id + list_id + normalized_name + linked_task_id)
- **Linking Semantics**: Items linked to task if both created in same decision
- **Safe Behavior**: Items added unlinked if task lookup fails

---

## [Stage 4] - 2026-01-13 - Context-driven Autodelegation

### Added
- Workload-based autodelegation with `workload_score` in AI context
- Zone ownership tracking (schema only, `owner_id` column)
- `DeadlineSanityPolicy` guardrail
- `ZoneOwnerFirstPolicy` guardrail
- Comprehensive observability metrics (`DecisionMetrics`)
- 200 OK with `needs_input` status for clarification (not 422)

### Changed
- `ContextBuilder` calculates workload scores for members
- Guardrails pipeline extended with new policies

See [ADR-007](docs/architecture/decisions/007-stage4-context-driven-autodelegation.md) for details.

---

## [Stage 3] - Guardrails Foundation

### Added
- `GuardrailsOrchestrator` for policy evaluation before action execution
- `MaxOpenTasksPerAssigneePolicy` guardrail
- `HouseholdSnapshot` for guardrails context
- Feature flag for guardrails enable/disable

---

## [Stage 2] - AI Platform Integration

### Added
- External AI Platform consumer via RestClient
- `AiPlatformDecisionProvider` with WireMock tests
- `AiDecisionResponseMapper` for upstream contract mapping
- Schema validation for AI responses
- Fallback to `ManualDecisionProvider` when AI unavailable

See [ADR-004](docs/architecture/decisions/004-stage2-ai-platform-integration.md) for details.

---

## [Stage 1] - MVP Foundation

### Added
- Commands API (`POST /api/v1/commands`)
- Command types: `create_task`, `complete_task`
- Domain entities: Household, User, Membership, Zone, Task
- Command pipeline: validation → decision → execution → logging
- `DecisionLog` for audit trail
- `TaskActivity` for event history
- Keycloak JWT authentication
- Testcontainers for integration tests

See [ADR-002](docs/architecture/decisions/002-mvp-text-command-scenario.md) and [ADR-003](docs/architecture/decisions/003-stage1-commands-api.md) for details.
