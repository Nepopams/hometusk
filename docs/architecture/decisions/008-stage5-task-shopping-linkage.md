# ADR-008: Stage 5 Task-Shopping Linkage

**Status:** Accepted
**Date:** 2026-01-13
**Deciders:** HomeTusk Development Team
**Related:** ADR-004 (AI Platform Integration), ADR-007 (Stage 4 Context Autodelegation)

---

## Context and Problem Statement

HomeTusk MVP includes two value pillars:
1. **Tasks** - AI-coordinated household task management (Stage 1-4)
2. **Shopping** - AI-coordinated shopping list management (Stage 5)

Stage 5 delivers the "shopping" pillar with UX-visible linkage between tasks and shopping items:
- A task can trigger shopping needs (e.g., "Clean bathroom" → needs cleaning supplies)
- Shopping items can show which task they relate to
- Items can also exist standalone (not linked to any task)

**Key requirements:**
1. Enable `add_shopping_item` action type in AI Platform decision processing
2. Link shopping items to tasks when both created in same decision
3. Support standalone shopping items (no task link)
4. Idempotent item creation (no duplicates on retry)
5. Safe behavior: if task link fails, still add items unlinked

**Constraints:**
- No upstream contract changes (docs/integration/ai-platform/v1/upstream is READ-ONLY)
- No AI/LLM logic inside HomeTusk
- Backward compatible API changes only

---

## Decision Drivers

- **Business Value:** Natural language → task + shopping items linked
- **User Experience:** Clear connection between tasks and needed purchases
- **Data Integrity:** Idempotent operations prevent duplicate items
- **Safety:** Items added even if task linkage fails (graceful degradation)
- **Simplicity:** Direct FK relationship, no complex join tables

---

## Considered Options

### Option 1: Direct FK (`linked_task_id`) (CHOSEN)
**Description:** Add nullable FK column `linked_task_id` directly to `shopping_items` table.

**Pros:**
- Simple 1:N relationship (task has many items)
- Efficient queries ("get items for task")
- ON DELETE SET NULL handles task deletion safely
- No additional tables or complexity

**Cons:**
- Item can only link to one task (acceptable for MVP)

**Decision:** CHOSEN. 1:N relationship is sufficient for MVP scope.

### Option 2: Join Table (`task_shopping_items`) (REJECTED)
**Description:** Create many-to-many join table for task-item relationships.

**Pros:**
- Supports many-to-many if needed in future
- Additional metadata per relationship

**Cons:**
- Overengineered for current requirements
- More complex queries
- Additional table to maintain

**Decision:** REJECTED. MVP requires 1:N only; YAGNI principle applies.

---

## Decision: Idempotency Strategy

### Chosen: Idempotency Key (Hash-based)

**Implementation:**
```
idempotency_key = SHA-256(command_id + list_id + normalized_name + linked_task_id|null)
```

**Behavior:**
1. Before creating item, generate idempotency key
2. Check if key exists in database
3. If exists → return existing item (idempotent)
4. If not exists → create new item with key

**Why this approach:**
- Deterministic: same inputs always produce same key
- Command-scoped: different commands can add same item
- Retry-safe: exact same request returns existing item
- Normalized: name is lowercased and trimmed for comparison

**Alternatives considered:**
- DB unique constraint on (list_id, name, quantity, unit) → too restrictive, prevents intentional duplicates
- Client-provided idempotency key → not available in AI decision flow

---

## Decision: Linking Semantics

**Deterministic rules:**
1. If task created in same decision → link items to that new task_id
2. If decision explicitly references existing task_id → link to it (after household validation)
3. If no task info in decision → add items WITHOUT link (safe behavior)
4. If task_id provided but not found/not in household → add items WITHOUT link (graceful degradation, log warning)

**Why not CLARIFY on missing task:**
- Violates "safe behavior" principle
- Blocking shopping for missing task is poor UX
- Items can be linked manually later

---

## Technical Decision: Migration Safety

**Migration V012:**
- Uses `IF NOT EXISTS` guards for idempotent reruns
- `DO $$ ... END $$` blocks for safe column additions
- `ON DELETE SET NULL` for FK cascade
- Indexes created with `IF NOT EXISTS`

---

## Consequences

### Positive
- Clear task ↔ shopping relationship visible in UI
- AI decisions can create linked task+items in single command
- Retry-safe operations via idempotency keys
- Safe degradation when task lookup fails

### Negative
- Item can only link to one task (acceptable for MVP)
- Idempotency key adds 64-char column overhead
- Additional index on linked_task_id

### Neutral
- Shopping tables already exist (V006), only adding columns
- JPA entities now materialized from existing schema

---

## Verification

After implementation:
1. Run `./gradlew test` - all tests pass including new shopping integration tests
2. Verify 6 test scenarios:
   - Task + shopping items linked
   - Shopping items without task link
   - Idempotent retry (no duplicates)
   - Invalid schema rejected
   - Household boundary violation rejected
   - Full-chain test (upstream → persisted links)

---

## References

- [ADR-004: AI Platform Integration](./004-stage2-ai-platform-integration.md)
- [ADR-007: Stage 4 Context Autodelegation](./007-stage4-context-driven-autodelegation.md)
- [V006 Migration: Shopping Tables](../../../services/backend/src/main/resources/db/migration/V006__create_shopping.sql)
- [V012 Migration: Task-Shopping Linkage](../../../services/backend/src/main/resources/db/migration/V012__add_shopping_task_linkage.sql)
