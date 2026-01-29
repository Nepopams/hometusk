# Codex APPLY Prompt: ST-1007 — Task Card "From Routine" Indicator

## Mode: APPLY (Implementation)

**CRITICAL:** This is the implementation phase. You MAY edit files.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1007/workpack.md
docs/planning/epics/EP-010/stories/ST-1007-task-routine-indicator.md
docs/_governance/dod.md
```

---

## PLAN Phase Findings (incorporated)

### Verified State
- TaskDto: record with 13 fields, static `from(Task)` method
- TaskDetailDto: same + linkedShoppingItems, static `from(Task, List<ShoppingItem>)` method
- Task.routine: `@ManyToOne(fetch = FetchType.LAZY)`, nullable
- RoutineStatus: ACTIVE, PAUSED, DELETED
- UserSummaryDto pattern: record with static `from()` returning null for null input
- OpenAPI: RoutineSummary exists but needs `status` field added

### Clarifications Applied
1. **Add status to RoutineSummary** — required for AC-7 (deleted routine indicator)
2. **Place RoutineSummaryDto in tasks.dto** — consistent with UserSummaryDto
3. **Accept lazy loading for v0** — N+1 optimization deferred

---

## Implementation Steps

### Step 1: Create RoutineSummaryDto

**File:** `services/backend/src/main/java/com/hometusk/tasks/dto/RoutineSummaryDto.java`

```java
package com.hometusk.tasks.dto;

import com.hometusk.routines.domain.Routine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Summary of a routine")
public record RoutineSummaryDto(
        @Schema(description = "Routine ID") UUID id,
        @Schema(description = "Routine title") String title,
        @Schema(description = "Routine status") String status) {

    public static RoutineSummaryDto from(Routine routine) {
        if (routine == null) {
            return null;
        }
        return new RoutineSummaryDto(
                routine.getId(),
                routine.getTitle(),
                routine.getStatus().name());
    }
}
```

### Step 2: Update TaskDto

**File:** `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDto.java`

**Changes:**
1. Add `RoutineSummaryDto routine` field to record
2. Update `from(Task)` to include `RoutineSummaryDto.from(task.getRoutine())`

**Updated record signature:**
```java
public record TaskDto(
        UUID id,
        UUID householdId,
        String title,
        String description,
        String status,
        UserSummaryDto assignee,
        ZoneDto zone,
        Instant deadline,
        UserSummaryDto createdBy,
        UUID commandId,
        String createdVia,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        @Schema(description = "Source routine if auto-generated") RoutineSummaryDto routine)
```

**Updated from() method:**
```java
public static TaskDto from(Task task) {
    return new TaskDto(
            task.getId(),
            task.getHouseholdId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus().name().toLowerCase(),
            UserSummaryDto.from(task.getAssignee()),
            task.getZone() != null ? ZoneDto.from(task.getZone()) : null,
            task.getDeadline(),
            UserSummaryDto.from(task.getCreatedBy()),
            task.getCommandId(),
            task.getCreatedVia(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            task.getCompletedAt(),
            RoutineSummaryDto.from(task.getRoutine()));
}
```

### Step 3: Update TaskDetailDto

**File:** `services/backend/src/main/java/com/hometusk/tasks/dto/TaskDetailDto.java`

**Changes:**
1. Add `RoutineSummaryDto routine` field to record
2. Update `from(Task, List<ShoppingItem>)` to include routine mapping

**Updated record:** Add `RoutineSummaryDto routine` after `completedAt`, before `linkedShoppingItems`.

### Step 4: Update OpenAPI Contract

**File:** `docs/contracts/http/commands.openapi.yaml`

**Changes:**
1. Ensure `RoutineSummary` schema has `status` field:
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
      description: Routine status (DELETED if routine was removed)
```

2. Ensure `Task` schema has `routine` field:
```yaml
Task:
  properties:
    # ... existing fields ...
    routine:
      $ref: '#/components/schemas/RoutineSummary'
      nullable: true
      description: Source routine if this task was auto-generated
```

### Step 5: Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/tasks/dto/RoutineSummaryDtoTest.java`

Create unit tests:
- `from_routine_mapsCorrectly` — verifies id/title/status mapping
- `from_null_returnsNull` — verifies null handling
- `from_deletedRoutine_hasDeletedStatus` — verifies DELETED status mapping

### Step 6: Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/TaskControllerTest.java`

Add tests:
- `getTask_withRoutine_includesRoutineSummary` — task from routine has routine info
- `getTask_manualTask_routineIsNull` — manual task has routine=null
- `listTasks_includesRoutineSummary` — task list includes routine info

---

## Files to Create/Modify Summary

| File | Action |
|------|--------|
| `RoutineSummaryDto.java` | CREATE |
| `TaskDto.java` | MODIFY (add routine field) |
| `TaskDetailDto.java` | MODIFY (add routine field) |
| `commands.openapi.yaml` | MODIFY (ensure status in RoutineSummary) |
| `RoutineSummaryDtoTest.java` | CREATE |
| `TaskControllerTest.java` | MODIFY (add routine tests) |

---

## Verification Commands

```bash
# Format code
cd services/backend && ./gradlew spotlessApply

# Run unit tests
cd services/backend && ./gradlew test --tests "*RoutineSummaryDtoTest"

# Run integration tests
cd services/backend && ./gradlew test --tests "*TaskControllerTest"

# Full build
cd services/backend && ./gradlew build
```

---

## STOP-THE-LINE Rules

Stop and report if:
- TaskDto record cannot be extended (Kotlin/Java record limitation)
- Circular import between tasks.dto and routines.domain
- OpenAPI validation fails
- Compilation errors

---

## DoD Checklist (verify at end)

- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied
- [ ] RoutineSummaryDto created with from() method
- [ ] TaskDto includes routine field
- [ ] TaskDetailDto includes routine field
- [ ] OpenAPI contract updated with status field
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] All tests pass: `./gradlew build`
