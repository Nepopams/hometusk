# Codex PLAN Prompt: ST-1001 — Routine Entity + CRUD Endpoints

## Mode
**PLAN ONLY** — Read-only exploration. NO file modifications allowed.

## Objective
Analyze the codebase and create a detailed implementation plan for ST-1001 (Routine Entity + CRUD Endpoints) that will be reviewed before any code changes.

---

## Sources of Truth (MUST READ)

Read these files to understand requirements and constraints:

```
docs/planning/workpacks/ST-1001/workpack.md       # Implementation plan
docs/planning/epics/EP-010/stories/ST-1001-routine-entity-crud.md  # Story + ACs
docs/contracts/http/routines.openapi.yaml         # API contract
docs/adr/013-routine-scheduler-design.md          # Architecture decisions
docs/_governance/dod.md                           # Definition of Done
```

---

## Allowed Commands (Read-Only)

You may ONLY use these commands:
- `ls`, `find` — list files/directories
- `cat`, `head`, `tail` — read file contents
- `rg`, `grep` — search code
- `sed -n` — extract lines (no editing)
- `git status`, `git diff` — inspect state (no commits)

**FORBIDDEN:**
- Any file modifications (edit/write/move/delete)
- Any network access
- Package install / system changes
- `git commit`, `git push`
- Database operations

---

## Task

### 1. Analyze Existing Patterns

Read and understand these existing implementations:

```bash
# Task entity (to extend)
cat services/backend/src/main/java/com/hometusk/tasks/domain/Task.java

# Shopping domain (similar CRUD pattern)
ls services/backend/src/main/java/com/hometusk/shopping/
cat services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java
cat services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java

# Household boundary enforcement
cat services/backend/src/main/java/com/hometusk/users/service/MembershipService.java

# Existing migrations
ls services/backend/src/main/resources/db/migration/

# Error codes
cat services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java

# JSON type handling (hypersistence-utils)
rg "JsonType" services/backend/src/main/java/ -l
```

### 2. Verify Dependencies

Check that required dependencies exist:

```bash
# Hypersistence utils for JSONB
rg "hypersistence" services/backend/build.gradle

# Check Zone entity for reference
cat services/backend/src/main/java/com/hometusk/households/domain/Zone.java

# Check User entity
cat services/backend/src/main/java/com/hometusk/users/domain/User.java
```

### 3. Check Test Patterns

```bash
# Integration test patterns
ls services/backend/src/test/java/com/hometusk/integration/
cat services/backend/src/test/java/com/hometusk/integration/ShoppingControllerIntegrationTest.java | head -100

# Unit test patterns
ls services/backend/src/test/java/com/hometusk/
```

### 4. Create Implementation Plan

Based on your analysis, create a detailed plan covering:

1. **DB Migration V021**
   - Exact SQL statements
   - Table structure matching workpack
   - Constraints (CHECK, partial unique index)
   - Migration file naming convention

2. **Domain Layer**
   - `RoutineStatus` enum
   - `AssignmentPolicy` enum
   - `RecurrenceRule` sealed interface with Jackson annotations
   - `RoundRobinState` record
   - `Routine` entity with all fields and relationships

3. **Task Entity Extension**
   - Fields to add
   - Import changes needed

4. **Repository Layer**
   - Query methods needed
   - Household-scoped queries

5. **Service Layer**
   - CRUD methods
   - Validation logic
   - Error handling

6. **Controller Layer**
   - Endpoint mappings
   - Request/response handling
   - Security enforcement

7. **DTOs**
   - Match OpenAPI schemas exactly
   - Mapper methods

8. **Tests**
   - Unit test scenarios
   - Integration test scenarios (all 10 ACs)

---

## Output Format

Provide your plan as:

```markdown
# Implementation Plan: ST-1001

## 1. Analysis Summary
[What patterns you found, dependencies verified]

## 2. File-by-File Implementation Order
[Ordered list with exact paths and key implementation details]

## 3. Migration SQL
[Complete SQL for V021]

## 4. Key Code Snippets
[Critical implementations like RecurrenceRule, Routine entity]

## 5. Test Plan
[Test class names and scenarios]

## 6. Risks/Questions
[Any uncertainties or decisions needed]
```

---

## Critical Constraints (MUST FOLLOW)

1. **RecurrenceRule** must use Jackson `@JsonTypeInfo` with `property = "type"` for polymorphic JSON
2. **Partial unique index** must use Postgres syntax: `WHERE routine_id IS NOT NULL`
3. **CHECK constraint** must enforce: `(routine_id IS NULL) = (scheduled_date IS NULL)`
4. **Household boundary** must be enforced via `membershipService.requireMembership()` in ALL endpoints
5. **PATCH semantics**: null means "don't change", explicit null in JSON clears the field
6. **Soft delete**: DELETE sets status=DELETED, does not remove row

---

## STOP-THE-LINE

If you encounter any of these, STOP and report:
- Missing dependency (hypersistence-utils not in build.gradle)
- Conflicting patterns in existing code
- Unclear requirements not covered by workpack/story
- Migration naming conflict (V021 already exists)

Do NOT proceed with assumptions. Ask for clarification.

---

## Deliverable

A complete implementation plan ready for human review. After approval, the APPLY phase will execute the actual code changes.
