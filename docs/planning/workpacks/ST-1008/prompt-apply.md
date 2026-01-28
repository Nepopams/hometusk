# Codex APPLY Prompt: ST-1008 — Security Boundaries + Integration Tests

## Mode: APPLY (Implementation)

You may create/edit files and run commands. Follow the plan strictly.

---

## From PLAN Phase Findings

**Confirmed by Codex PLAN exploration:**
- RoutineController has 5 endpoints with explicit `membershipService.requireMembership()` calls
- RoutineService uses `findByIdAndHousehold_Id()` — returns 404 if routine not in household
- Routine entity has `pausedAt` field (Instant) already defined
- RoutineStatus enum: ACTIVE, PAUSED, DELETED
- RecurrenceRuleParser exists with `getOccurrencesInRange(rule, fromDate, count)`
- Integration test pattern: extend IntegrationTestBase, use `jwt()` and `jwtForUser(testUser2)`
- MembershipService throws `AccessDeniedException` → 403
- No AGENTS.md — use CLAUDE.md as primary instruction source

**Clarifications:**
- DTO naming per OpenAPI: `UpcomingInstancesResponse` (wrapper), `UpcomingInstanceDto` (per-instance)
- For v0: skip `alreadyGenerated` flag (requires TaskRepository lookup) — mark as TODO

---

## Anchors (Sources of Truth)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1008/workpack.md
docs/planning/epics/EP-010/stories/ST-1008-security-boundaries.md
docs/contracts/http/routines.openapi.yaml
docs/_governance/dod.md
```

---

## Files to Create

### 1. UpcomingInstanceDto.java

**Path:** `services/backend/src/main/java/com/hometusk/routines/dto/UpcomingInstanceDto.java`

```java
package com.hometusk.routines.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Upcoming routine instance")
public record UpcomingInstanceDto(
        @Schema(description = "Scheduled date") LocalDate scheduledDate,
        @Schema(description = "Projected assignee") UserSummaryDto projectedAssignee) {}
```

### 2. UpcomingInstancesResponse.java

**Path:** `services/backend/src/main/java/com/hometusk/routines/dto/UpcomingInstancesResponse.java`

```java
package com.hometusk.routines.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Upcoming instances preview response")
public record UpcomingInstancesResponse(
        @Schema(description = "Routine ID") UUID routineId,
        @Schema(description = "Routine title") String routineTitle,
        @Schema(description = "Upcoming instances") List<UpcomingInstanceDto> instances) {}
```

### 3. RoutineSecurityIntegrationTest.java

**Path:** `services/backend/src/test/java/com/hometusk/integration/RoutineSecurityIntegrationTest.java`

**Test class structure:**
- Extend `IntegrationTestBase`
- Use `@DisplayName("Routine Security Integration Tests")`
- Create `otherHousehold` and `otherRoutine` in `@BeforeEach`
- 10 test methods mapping to AC-1 through AC-9

**Required test methods:**

| Test Method | AC | Assertion |
|-------------|-----|-----------|
| `listRoutines_notMember_returns403` | AC-1 | `status().isForbidden()` |
| `createRoutine_notMember_returns403` | AC-2 | `status().isForbidden()` |
| `getRoutine_notMember_returns403` | AC-3 | `status().isForbidden()` |
| `updateRoutine_notMember_returns403` | AC-4 | `status().isForbidden()` |
| `deleteRoutine_notMember_returns403` | AC-5 | `status().isForbidden()` |
| `pauseRoutine_notMember_returns403` | AC-6 | `status().isForbidden()` |
| `resumeRoutine_notMember_returns403` | AC-6 | `status().isForbidden()` |
| `upcoming_notMember_returns403` | AC-7 | `status().isForbidden()` |
| `listRoutines_crossHousehold_noLeaks` | AC-8 | Only own household routines |
| `getRoutine_wrongHousehold_returns404` | AC-9 | `status().isNotFound()` |

---

## Files to Modify

### 1. RoutineController.java

**Path:** `services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java`

**Add 3 endpoints after existing `deleteRoutine`:**

```java
@PostMapping("/routines/{routineId}/pause")
@Operation(summary = "Pause a routine")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Routine paused"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "403", description = "Not a member of this household"),
    @ApiResponse(responseCode = "404", description = "Routine not found"),
    @ApiResponse(responseCode = "409", description = "Cannot pause deleted routine")
})
public ResponseEntity<RoutineDto> pauseRoutine(
        @PathVariable UUID householdId, @PathVariable UUID routineId) {
    CurrentUser currentUser = userResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    Routine routine = routineService.pauseRoutine(routineId, householdId);
    return ResponseEntity.ok(RoutineDto.from(routine, objectMapper));
}

@PostMapping("/routines/{routineId}/resume")
@Operation(summary = "Resume a paused routine")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Routine resumed"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "403", description = "Not a member of this household"),
    @ApiResponse(responseCode = "404", description = "Routine not found"),
    @ApiResponse(responseCode = "409", description = "Cannot resume deleted routine")
})
public ResponseEntity<RoutineDto> resumeRoutine(
        @PathVariable UUID householdId, @PathVariable UUID routineId) {
    CurrentUser currentUser = userResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    Routine routine = routineService.resumeRoutine(routineId, householdId);
    return ResponseEntity.ok(RoutineDto.from(routine, objectMapper));
}

@GetMapping("/routines/{routineId}/upcoming")
@Operation(summary = "Preview upcoming task instances")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "List of upcoming instances"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "403", description = "Not a member of this household"),
    @ApiResponse(responseCode = "404", description = "Routine not found")
})
public ResponseEntity<UpcomingInstancesResponse> getUpcomingInstances(
        @PathVariable UUID householdId,
        @PathVariable UUID routineId,
        @RequestParam(defaultValue = "7") int days) {
    CurrentUser currentUser = userResolver.resolveCurrentUser();
    membershipService.requireMembership(currentUser.id(), householdId);

    UpcomingInstancesResponse response = routineService.getUpcomingInstances(routineId, householdId, days);
    return ResponseEntity.ok(response);
}
```

**Add import:**
```java
import com.hometusk.routines.dto.UpcomingInstancesResponse;
```

### 2. RoutineService.java

**Path:** `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java`

**Add dependency injection for RecurrenceRuleParser:**

In constructor, add parameter and field:
```java
private final RecurrenceRuleParser recurrenceRuleParser;

public RoutineService(
        RoutineRepository routineRepository,
        // ... existing params ...
        RecurrenceRuleParser recurrenceRuleParser) {
    // ... existing assignments ...
    this.recurrenceRuleParser = recurrenceRuleParser;
}
```

**Add 3 methods after `deleteRoutine`:**

```java
@Transactional
public Routine pauseRoutine(UUID routineId, UUID householdId) {
    Routine routine = getRoutine(routineId, householdId);

    if (routine.getStatus() == RoutineStatus.DELETED) {
        throw new BusinessException(
                ErrorCode.INVALID_STATE,
                "Cannot pause a deleted routine",
                List.of(new BusinessException.Violation("ROUTINE_DELETED", "Routine is deleted")));
    }

    if (routine.getStatus() != RoutineStatus.PAUSED) {
        routine.setStatus(RoutineStatus.PAUSED);
        routine.setPausedAt(Instant.now());
        routine = routineRepository.save(routine);
        log.info("Routine paused: id={}, householdId={}", routineId, householdId);
    }

    return routine;
}

@Transactional
public Routine resumeRoutine(UUID routineId, UUID householdId) {
    Routine routine = getRoutine(routineId, householdId);

    if (routine.getStatus() == RoutineStatus.DELETED) {
        throw new BusinessException(
                ErrorCode.INVALID_STATE,
                "Cannot resume a deleted routine",
                List.of(new BusinessException.Violation("ROUTINE_DELETED", "Routine is deleted")));
    }

    if (routine.getStatus() != RoutineStatus.ACTIVE) {
        routine.setStatus(RoutineStatus.ACTIVE);
        routine.setPausedAt(null);
        routine = routineRepository.save(routine);
        log.info("Routine resumed: id={}, householdId={}", routineId, householdId);
    }

    return routine;
}

@Transactional(readOnly = true)
public UpcomingInstancesResponse getUpcomingInstances(UUID routineId, UUID householdId, int days) {
    Routine routine = getRoutine(routineId, householdId);

    RecurrenceRule rule;
    try {
        rule = objectMapper.readValue(routine.getRecurrenceRuleJson(), RecurrenceRule.class);
    } catch (JsonProcessingException e) {
        throw new IllegalStateException("Invalid recurrence rule JSON", e);
    }

    int count = Math.min(Math.max(days, 1), 30);
    List<LocalDate> dates = recurrenceRuleParser.getOccurrencesInRange(rule, LocalDate.now(), count);

    UserSummaryDto assignee = routine.getAssignmentPolicy() == AssignmentPolicy.FIXED
            ? UserSummaryDto.from(routine.getFixedAssignee())
            : null;

    List<UpcomingInstanceDto> instances = dates.stream()
            .map(date -> new UpcomingInstanceDto(date, assignee))
            .toList();

    return new UpcomingInstancesResponse(routine.getId(), routine.getTitle(), instances);
}
```

**Add imports:**
```java
import com.hometusk.routines.dto.UpcomingInstanceDto;
import com.hometusk.routines.dto.UpcomingInstancesResponse;
import com.hometusk.routines.dto.UserSummaryDto;
import java.time.Instant;
import java.time.LocalDate;
```

---

## Forbidden Paths (DO NOT MODIFY)

- `services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java`
- `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java`
- `services/backend/src/main/java/com/hometusk/routines/domain/Routine.java` (pausedAt already exists)
- Any database migrations
- Any files outside routines module (except test)

---

## Implementation Order

1. Create `UpcomingInstanceDto.java`
2. Create `UpcomingInstancesResponse.java`
3. Modify `RoutineService.java` — add RecurrenceRuleParser dependency + 3 methods
4. Modify `RoutineController.java` — add 3 endpoints
5. Create `RoutineSecurityIntegrationTest.java`
6. Run: `cd services/backend && ./gradlew spotlessApply`
7. Run: `cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSecurityIntegrationTest"`
8. Run: `cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineControllerIntegrationTest"`
9. Run: `cd services/backend && ./gradlew build`

---

## Verification Commands

```bash
# Apply formatting first
cd services/backend && ./gradlew spotlessApply

# Run security tests
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSecurityIntegrationTest"

# Run existing routine tests (regression)
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineControllerIntegrationTest"

# Full build
cd services/backend && ./gradlew build
```

**Expected:** All tests pass, build succeeds.

---

## STOP-THE-LINE Rule

If ANY of the following occurs, STOP immediately and report:
- Tests fail and you cannot determine why
- Need to modify forbidden files
- ErrorCode.INVALID_STATE does not exist (check ErrorCode enum first)
- Unexpected codebase state

Do NOT guess or improvise. Report and wait for clarification.

---

## Output Checklist

After implementation, confirm:
- [ ] `UpcomingInstanceDto.java` created
- [ ] `UpcomingInstancesResponse.java` created
- [ ] `RoutineService.java` — RecurrenceRuleParser injected + 3 methods added
- [ ] `RoutineController.java` — 3 endpoints added
- [ ] `RoutineSecurityIntegrationTest.java` — 10 test methods
- [ ] `./gradlew spotlessApply` applied
- [ ] All security tests pass
- [ ] All routine controller tests pass
- [ ] `./gradlew build` succeeds
