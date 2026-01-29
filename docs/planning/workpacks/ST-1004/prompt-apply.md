# Codex APPLY Prompt: ST-1004 — Assignment Policies (Fixed/Round-Robin/Manual)

## Mode: APPLY (Implementation)

**CRITICAL:** This is the implementation phase. You MAY edit files.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1004/workpack.md
docs/planning/epics/EP-010/stories/ST-1004-assignment-policies.md
docs/planning/epics/EP-010/epic.md
docs/_governance/dod.md
```

---

## PLAN Phase Findings (incorporated)

### Verified State
- `RoundRobinState` record already exists: `services/backend/src/main/java/com/hometusk/routines/domain/RoundRobinState.java`
- `RoutineSchedulerService` exists with `createTaskForDate()` method
- `MembershipRepository.findByHousehold_IdWithUser()` available for getting members
- `@Lock` pattern exists in `HouseholdInviteRepository.findByInviteTokenForUpdate()`
- `Task.setAssignee(User)` method available
- `ObjectMapper` injected via constructor in RoutineService

### Clarifications Applied
1. **Round-robin sorting:** By `joinedAt` ascending, then by UUID for determinism
2. **Member removal:** Filter to current members (don't reset state), skip removed members
3. **Lock scope:** Lock by `id` only (simpler for v0)
4. **RoundRobinState:** Already exists, DO NOT recreate

### Adjusted Scope
- SKIP Step 1 (RoundRobinState already exists)
- IMPLEMENT Steps 2-7

---

## Implementation Steps

### Step 2: Create AssignmentPolicyService

**File:** `services/backend/src/main/java/com/hometusk/routines/service/AssignmentPolicyService.java`

```java
package com.hometusk.routines.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RoundRobinState;
import com.hometusk.routines.domain.Routine;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssignmentPolicyService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentPolicyService.class);

    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    public AssignmentPolicyService(MembershipRepository membershipRepository, ObjectMapper objectMapper) {
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Determine assignee based on routine's assignment policy.
     * For ROUND_ROBIN, also updates the routine's roundRobinStateJson.
     *
     * @return User to assign, or null for MANUAL policy
     */
    public User determineAssignee(Routine routine) {
        return switch (routine.getAssignmentPolicy()) {
            case FIXED -> routine.getFixedAssignee();
            case MANUAL -> null;
            case ROUND_ROBIN -> determineRoundRobinAssignee(routine);
        };
    }

    private User determineRoundRobinAssignee(Routine routine) {
        List<User> currentMembers = getCurrentMembers(routine.getHouseholdId());
        if (currentMembers.isEmpty()) {
            log.warn("No members in household {} for round-robin", routine.getHouseholdId());
            return null;
        }

        RoundRobinState state = parseState(routine.getRoundRobinStateJson());
        List<UUID> memberIds = currentMembers.stream().map(User::getId).toList();

        UUID nextAssigneeId = findNextAssignee(state, memberIds);
        User nextAssignee = currentMembers.stream()
                .filter(u -> u.getId().equals(nextAssigneeId))
                .findFirst()
                .orElse(currentMembers.get(0));

        // Update state
        RoundRobinState newState = new RoundRobinState(nextAssignee.getId(), memberIds);
        routine.setRoundRobinStateJson(serializeState(newState));

        return nextAssignee;
    }

    private List<User> getCurrentMembers(UUID householdId) {
        return membershipRepository.findByHousehold_IdWithUser(householdId).stream()
                .sorted(Comparator.comparing(Membership::getJoinedAt)
                        .thenComparing(m -> m.getUser().getId()))
                .map(Membership::getUser)
                .toList();
    }

    private UUID findNextAssignee(RoundRobinState state, List<UUID> currentMemberIds) {
        if (state == null || state.lastAssignedUserId() == null) {
            // First assignment: start with first member
            return currentMemberIds.get(0);
        }

        UUID lastAssigned = state.lastAssignedUserId();
        int lastIndex = currentMemberIds.indexOf(lastAssigned);

        if (lastIndex == -1) {
            // Last assigned user left household, start from beginning
            return currentMemberIds.get(0);
        }

        // Next member with wrap-around
        int nextIndex = (lastIndex + 1) % currentMemberIds.size();
        return currentMemberIds.get(nextIndex);
    }

    private RoundRobinState parseState(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RoundRobinState.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse round-robin state, will reinitialize: {}", e.getMessage());
            return null;
        }
    }

    private String serializeState(RoundRobinState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize round-robin state", e);
        }
    }
}
```

### Step 3: Add Pessimistic Lock Method to RoutineRepository

**File:** `services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java`

**Add method:**
```java
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Find routine by ID with pessimistic write lock (for round-robin state update).
 */
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Routine r WHERE r.id = :id")
Optional<Routine> findByIdForUpdate(@Param("id") UUID id);
```

### Step 4: Integrate with RoutineSchedulerService

**File:** `services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java`

**Changes:**
1. Inject `AssignmentPolicyService` via constructor
2. In `generateTasksForRoutine()`: lock routine before processing dates
3. In `createTaskForDate()`: call `assignmentPolicyService.determineAssignee(routine)`
4. Set `task.setAssignee(assignee)` if not null
5. Save routine after round-robin state update (routine is managed, will be persisted at commit)

**Updated code structure:**
```java
// Add to constructor
private final AssignmentPolicyService assignmentPolicyService;

public RoutineSchedulerService(
        RoutineRepository routineRepository,
        TaskRepository taskRepository,
        RecurrenceRuleParser recurrenceRuleParser,
        RoutineService routineService,
        AssignmentPolicyService assignmentPolicyService) {
    // ... existing fields
    this.assignmentPolicyService = assignmentPolicyService;
}

// In generateTasksForRoutine():
private RoutineResult generateTasksForRoutine(Routine routine) {
    // Lock the routine for this transaction
    Routine lockedRoutine = routineRepository.findByIdForUpdate(routine.getId())
            .orElseThrow(() -> new IllegalStateException("Routine not found: " + routine.getId()));

    LocalDate today = LocalDate.now();
    int windowDays = lockedRoutine.getGenerationWindowDays();
    // ... rest of the method uses lockedRoutine instead of routine
}

// In createTaskForDate():
private void createTaskForDate(Routine routine, LocalDate date) {
    Task task = new Task(routine.getHousehold(), routine.getTitle(), routine.getCreatedBy());
    task.setDescription(routine.getDescription());
    task.setZone(routine.getZone());
    task.setRoutine(routine);
    task.setScheduledDate(date);

    Instant deadline = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
    task.setDeadline(deadline);
    task.setCreatedVia("scheduler");

    // Assignment policy integration
    User assignee = assignmentPolicyService.determineAssignee(routine);
    if (assignee != null) {
        task.setAssignee(assignee);
    }

    taskRepository.save(task);
    // Routine state (for round-robin) is updated in determineAssignee()
    // It will be persisted when transaction commits (routine is managed entity)
}
```

**IMPORTANT:** State only advances when task INSERT succeeds. If existsByRoutine_IdAndScheduledDate returns true, we skip createTaskForDate(), so round-robin state is NOT advanced.

### Step 5: Unit Tests for AssignmentPolicyService

**File:** `services/backend/src/test/java/com/hometusk/routines/service/AssignmentPolicyServiceTest.java`

Create unit tests:
- `fixed_assignsToConfiguredUser`
- `manual_returnsNull`
- `roundRobin_firstAssignment_assignsFirstMember`
- `roundRobin_rotatesCorrectly`
- `roundRobin_wrapsAround`
- `roundRobin_handlesMemberRemoval`
- `roundRobin_emptyHousehold_returnsNull`
- `roundRobin_updatesStateJson`

### Step 6: Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/RoutineSchedulerIntegrationTest.java`

Add tests:
- `scheduler_withFixedPolicy_assignsToFixedUser`
- `scheduler_withManualPolicy_noAssignee`
- `scheduler_withRoundRobin_rotatesMembers`
- `scheduler_withRoundRobin_statePersistedAcrossRuns`

---

## Files to Create/Modify Summary

| File | Action |
|------|--------|
| `AssignmentPolicyService.java` | CREATE |
| `RoutineRepository.java` | ADD lock method |
| `RoutineSchedulerService.java` | MODIFY (inject AssignmentPolicyService, add locking, add assignment) |
| `AssignmentPolicyServiceTest.java` | CREATE |
| `RoutineSchedulerIntegrationTest.java` | MODIFY (add policy tests) |

---

## Verification Commands

```bash
# Format code
cd services/backend && ./gradlew spotlessApply

# Run unit tests
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.AssignmentPolicyServiceTest"

# Run scheduler integration tests
cd services/backend && ./gradlew test --tests "com.hometusk.integration.RoutineSchedulerIntegrationTest"

# Full build
cd services/backend && ./gradlew build
```

---

## STOP-THE-LINE Rules

Stop and report if:
- `MembershipRepository.findByHousehold_IdWithUser()` doesn't exist
- `Membership.getJoinedAt()` doesn't exist
- `@Lock` import not available
- Any circular dependency issues with AssignmentPolicyService
- Compilation errors

---

## DoD Checklist (verify at end)

- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing (8+ test cases)
- [ ] Integration tests written and passing (4+ test cases)
- [ ] FIXED policy assigns to configured user
- [ ] ROUND_ROBIN rotates through members correctly
- [ ] MANUAL policy leaves assignee null
- [ ] Round-robin state persisted atomically with task
- [ ] Pessimistic lock prevents concurrent state corruption
- [ ] All tests pass: `./gradlew build`
