# Workpack: ST-1007 — Task Card "From Routine" Indicator

## Sources of Truth
- Story: `docs/planning/epics/EP-010/stories/ST-1007-task-routine-indicator.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Task DTOs: `services/backend/src/main/java/com/hometusk/tasks/dto/`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`

---

## Goal
Add routine information to Task API response so UI can display "From Routine" indicator:
- Task list includes routine summary
- Task detail includes routine summary
- Handle deleted routines gracefully

## Scope: In / Out

### In Scope (Backend API)
- Create `RoutineSummaryDto` record (id, title, status)
- Add `routine` field to `TaskDto` (for task list)
- Add `routine` field to `TaskDetailDto` (for task detail)
- Update OpenAPI contract
- Unit tests for mapping
- Integration tests for API response

### Out of Scope (separate implementation)
- Web UI task card badge (requires web client)
- Web UI task detail routine section (requires web client)
- Navigation to routine (web client routing)

---

## Anchors (non-negotiables)
| Artifact | Path |
|----------|------|
| Story Spec | `docs/planning/epics/EP-010/stories/ST-1007-task-routine-indicator.md` |
| TaskDto | `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDto.java` |
| TaskDetailDto | `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDetailDto.java` |
| Task entity | `services/backend/src/main/java/com/hometusk/tasks/domain/Task.java` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |

---

## Plan Steps

### Step 1: Create RoutineSummaryDto

**Description:** Create a shared DTO for routine summary in task responses.

**Expected Result:**
```java
package com.hometusk.tasks.dto;

public record RoutineSummaryDto(
    UUID id,
    String title,
    String status
) {
    public static RoutineSummaryDto from(Routine routine) {
        if (routine == null) return null;
        return new RoutineSummaryDto(
            routine.getId(),
            routine.getTitle(),
            routine.getStatus().name()
        );
    }
}
```

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/tasks/dto/RoutineSummaryDto.java`

### Step 2: Add routine field to TaskDto

**Description:** Extend TaskDto with routine summary.

**Expected Result:**
- Add `RoutineSummaryDto routine` field to record
- Update `from(Task task)` to include routine mapping

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDto.java`

### Step 3: Add routine field to TaskDetailDto

**Description:** Extend TaskDetailDto with routine summary.

**Expected Result:**
- Add `RoutineSummaryDto routine` field to record
- Update `from(Task task, List<ShoppingItem> items)` to include routine mapping

**Files touched:**
- MODIFY: `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDetailDto.java`

### Step 4: Update OpenAPI contract

**Description:** Add RoutineSummary schema and update Task schema.

**Expected Result:**
```yaml
RoutineSummary:
  type: object
  properties:
    id:
      type: string
      format: uuid
    title:
      type: string
    status:
      type: string
      enum: [ACTIVE, PAUSED, DELETED]

Task:
  properties:
    routine:
      $ref: '#/components/schemas/RoutineSummary'
      nullable: true
```

**Files touched:**
- MODIFY: `docs/contracts/http/commands.openapi.yaml`

### Step 5: Unit Tests

**Description:** Unit tests for RoutineSummaryDto mapping.

**Expected Result:**
- `RoutineSummaryDto_fromRoutine_mapsCorrectly`
- `RoutineSummaryDto_fromNull_returnsNull`
- `TaskDto_withRoutine_includesRoutineSummary`
- `TaskDto_withoutRoutine_routineIsNull`

**Files touched:**
- CREATE: `services/backend/src/test/java/com/hometusk/tasks/dto/RoutineSummaryDtoTest.java`

### Step 6: Integration Tests

**Description:** Integration tests for API response.

**Expected Result:**
- `getTask_withRoutine_includesRoutineSummary`
- `getTask_withDeletedRoutine_includesRoutineSummaryWithDeletedStatus`
- `getTask_manualTask_routineIsNull`
- `listTasks_includesRoutineSummary`

**Files touched:**
- MODIFY: `services/backend/src/test/java/com/hometusk/integration/TaskControllerIntegrationTest.java` (or create new)

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `RoutineSummaryDto.java` | CREATE | Routine summary for task responses |
| `TaskDto.java` | MODIFY | Add routine field |
| `TaskDetailDto.java` | MODIFY | Add routine field |
| `commands.openapi.yaml` | MODIFY | Add RoutineSummary schema |
| `RoutineSummaryDtoTest.java` | CREATE | Unit tests |
| Integration tests | MODIFY | API response tests |

---

## Tests & Checks

### Required Test Methods
| Test Class | Method | AC |
|------------|--------|-----|
| RoutineSummaryDtoTest | `from_routine_mapsCorrectly` | AC-1 |
| RoutineSummaryDtoTest | `from_null_returnsNull` | AC-6 |
| RoutineSummaryDtoTest | `from_deletedRoutine_hasDeletedStatus` | AC-7 |
| Integration | `getTask_withRoutine_includesRoutineSummary` | AC-1 |
| Integration | `listTasks_includesRoutineSummary` | AC-2 |

### Commands to Run
```bash
cd services/backend && ./gradlew spotlessApply
cd services/backend && ./gradlew test --tests "*RoutineSummaryDtoTest"
cd services/backend && ./gradlew test --tests "*TaskControllerIntegrationTest"
cd services/backend && ./gradlew build
```

---

## Contract Impact
**Yes** — Task schema extended with `routine` field.

### Schema Changes
- Add `RoutineSummary` component
- Add `routine` field to `Task` response (nullable)

### Backward Compatibility
- Field is nullable, existing clients ignore unknown fields
- No breaking changes

---

## Docs Updates
None required (contract update included in workpack).

---

## Rollout / Rollback

### Rollout
- Deploy backend with new field
- Existing clients continue to work (ignore new field)
- Web UI can use field when ready

### Rollback Steps
- Revert code changes
- Field disappears from response
- No data changes needed

---

## Done Criteria

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Task API includes routine info | Integration test |
| AC-2 | Task list includes routine indicator | Integration test |
| AC-6 | Manual tasks show no indicator | Integration test |
| AC-7 | Deleted routine shows with status | Unit test |

**Note:** AC-3, AC-4, AC-5 (UI components) are out of scope for this workpack.

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| N+1 queries for routine | Performance | Task.routine is LAZY, test query count |
| Missing routine FK | NPE | Null check in from() method |

---

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-1007/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-1007/prompt-apply.md`
