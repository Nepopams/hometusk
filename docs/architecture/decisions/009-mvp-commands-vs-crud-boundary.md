# ADR-009: Commands vs CRUD Boundary for MVP Hardening

**Status:** Accepted
**Date:** 2026-01-14
**Context:** MVP Closure / Iteration 1 / Step 1

## Context

HomeTusk follows an intent-driven architecture where users submit commands via `POST /api/v1/commands` rather than CRUD operations. This is documented in CLAUDE.md Rule #2:

> "Users submit **commands**, not CRUD operations. Endpoint: `POST /api/v1/commands` (not `POST /tasks`)"

For the MVP Web UI, we need REST endpoints for:
1. User profile and household discovery
2. Household/zone administration
3. Task list viewing
4. Shopping list management

This creates tension between architectural purity and MVP pragmatism.

## Decision

We adopt a **pragmatic hybrid approach** with clear boundaries:

### Layer 1: Admin/Setup Operations (Direct REST)

These operations are administrative setup, not domain actions:

| Operation | Endpoint | Rationale |
|-----------|----------|-----------|
| Create household | `POST /api/v1/households` | One-time setup, not a "task" |
| Create zone | `POST /api/v1/households/{id}/zones` | Configuration, not command |
| Get user profile | `GET /api/v1/users/me` | Read-only |
| List members | `GET /api/v1/households/{id}/members` | Read-only |

### Layer 2: Domain Actions (Commands API)

These operations MUST go through `/api/v1/commands`:

| Operation | Command Type | Rationale |
|-----------|--------------|-----------|
| Create task | `create_task` | Core domain action, AI-involved |
| Complete task | `complete_task` | State transition, traced |
| AI-coordinated shopping | `add_shopping_item` | NL/AI flow |

### Layer 3: UX Pragmatic CRUD (Direct REST for MVP)

Shopping item mutations use direct REST for better UX:

| Operation | Endpoint | Rationale |
|-----------|----------|-----------|
| Create shopping list manually | `POST /api/v1/households/{id}/shopping-lists` | User-visible container creation from empty state |
| Add item manually | `POST .../items` | Simple form submit |
| Mark purchased | `PATCH .../items/{id}` | Toggle, no NL needed |
| Link/unlink shopping item to task manually | `PATCH .../items/{id}` | Direct UX correction; backend still validates same-household task |
| Delete item | `DELETE .../items/{id}` | Simple action |

**Trade-off accepted:** These writes bypass the commands pipeline but:
- Still record Activity for audit trail
- Are household-scoped with boundary checks
- Reject invalid manual task links with `404 TASK_NOT_FOUND` rather than silently unlinking
- Can be migrated to commands in future if needed

**Manual vs command task-link semantics:** command/AI `add_shopping_item`
keeps ADR-008 safe degradation and adds an item unlinked when a referenced task
cannot be validated. Manual REST create/update is stricter because the user is
choosing an explicit task link in UI; invalid or cross-household `linkedTaskId`
returns 404 and does not mutate the item.

### Read Operations (Direct REST)

All read operations use direct REST:

| Operation | Endpoint |
|-----------|----------|
| List tasks | `GET /api/v1/households/{id}/tasks` |
| Get task detail | `GET /api/v1/households/{id}/tasks/{taskId}` |
| List shopping lists | `GET /api/v1/households/{id}/shopping-lists` |
| Create shopping list | `POST /api/v1/households/{id}/shopping-lists` |
| List shopping items | `GET .../shopping-lists/{listId}/items` |

## Consequences

### Positive

1. **MVP ships faster** - No forced NL for simple toggles
2. **Better UX** - "Mark purchased" doesn't need AI interpretation
3. **Clear boundaries** - Documented which operations require commands
4. **Extensible** - Can migrate to commands later if needed

### Negative

1. **Dual write paths** - Shopping items can be added via REST or commands
2. **Reduced traceability** - REST writes don't create DecisionLog entries
3. **Architectural tension** - Deviates from pure intent-driven model

### Mitigations

1. **Activity recording** - All shopping mutations record TaskActivity events
2. **Correlation ID** - REST endpoints propagate X-Correlation-ID
3. **Documentation** - This ADR documents the boundary clearly
4. **Future path** - Stage 3+ can unify under commands if needed

## Boundary Summary

```
┌─────────────────────────────────────────────────────────────┐
│                    /api/v1/commands                          │
│   - create_task, complete_task, add_shopping_item (AI)      │
│   - Full traceability: Command → DecisionLog → Activity     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    REST Endpoints                            │
│   READS: /users/me, /households/*/tasks, /*/shopping-*      │
│   ADMIN: POST /households, POST /*/zones                    │
│   UX CRUD: POST/PATCH/DELETE shopping items                 │
│   - Household boundary checks enforced                      │
│   - Activity recorded for mutations                         │
└─────────────────────────────────────────────────────────────┘
```

## References

- CLAUDE.md - Architectural Rules
- ADR-003 - Stage 1 Commands API
- ADR-008 - Stage 5 Task-Shopping Linkage
