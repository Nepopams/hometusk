# Codex APPLY Prompt: ST-901 + ST-902 — Points Ledger + Badges v0

## Mode
**APPLY** — Implementation allowed. Write files, run tests.

---

## Sources of Truth (Reference)
```
docs/planning/workpacks/ST-901-902/workpack.md
docs/planning/workpacks/ST-901-902/prompt-plan.md  # Approved PLAN
docs/planning/epics/EP-009/stories/ST-901-points-ledger.md
docs/planning/epics/EP-009/stories/ST-902-badges-achievements.md
docs/_governance/dod.md
```

---

## Approved PLAN Summary

Implement backend gamification foundation:
1. **V018 Migration** — badges, points_ledger, user_badges tables + seed 5 badges
2. **Entities** — PointsLedger, PointsReason, Badge, UserBadge
3. **Repositories** — with idempotency queries and aggregates
4. **PointsService** — award/reverse with idempotency key `(taskId, userId, reason)`
5. **BadgeService** — check criteria, award badges, notify
6. **GamificationController** — GET /progress, GET /badges
7. **Integration** — hook into ActionExecutor after task completion
8. **Tests** — unit + integration
9. **OpenAPI** — add endpoints and schemas

---

## Critical Constraints (MUST FOLLOW)

### Points Model
| Parameter | Value |
|-----------|-------|
| Base points | 10 per completed task |
| On-time bonus | +5 if `task.deadline != null` AND `completedAt < deadline` |
| No deadline | If `task.deadline = null` → NO on-time bonus |
| Overdue penalty | NONE |
| Reversible | Yes (full rollback on uncomplete) |

### Idempotency Key (CRITICAL)
```
Key = (taskId, userId, reason)
```
- Use UNIQUE index `idx_points_ledger_task_user_reason`
- On duplicate, catch `DataIntegrityViolationException` and return existing
- Handles reassignment: different userId = new award allowed

### Badges (5 — streak-free)
| Code | Name | Criteria |
|------|------|----------|
| FIRST_TASK | Task Starter | COUNT(TASK_COMPLETED) >= 1 |
| TEN_TASKS | Task Champion | COUNT(TASK_COMPLETED) >= 10 |
| WEEK_WARRIOR | Week Warrior | COUNT(TASK_COMPLETED in last 7 days) >= 7 |
| ZONE_SPECIALIST | Zone Specialist | MAX(COUNT per zone) >= 5 |
| ON_TIME_HERO | On-Time Hero | COUNT(ON_TIME_BONUS) >= 5 |

### Response Structure
```java
GamificationProgressResponse {
    // Per-user (my progress)
    UUID userId;
    int totalPoints;
    int pointsThisWeek;
    List<BadgeDto> earnedBadges;
    List<PointsEntryDto> recentActivity; // last 10

    // Household aggregate
    int householdTotalTasks;
    int householdTotalPoints;
}
```

---

## Files to Create

### Migration
```
services/backend/src/main/resources/db/migration/V018__create_gamification_tables.sql
```

### Domain (package: com.hometusk.gamification.domain)
```
services/backend/src/main/java/com/hometusk/gamification/domain/PointsLedger.java
services/backend/src/main/java/com/hometusk/gamification/domain/PointsReason.java
services/backend/src/main/java/com/hometusk/gamification/domain/Badge.java
services/backend/src/main/java/com/hometusk/gamification/domain/UserBadge.java
```

### Repository (package: com.hometusk.gamification.repository)
```
services/backend/src/main/java/com/hometusk/gamification/repository/PointsLedgerRepository.java
services/backend/src/main/java/com/hometusk/gamification/repository/BadgeRepository.java
services/backend/src/main/java/com/hometusk/gamification/repository/UserBadgeRepository.java
```

### Service (package: com.hometusk.gamification.service)
```
services/backend/src/main/java/com/hometusk/gamification/service/PointsService.java
services/backend/src/main/java/com/hometusk/gamification/service/BadgeService.java
```

### API (package: com.hometusk.gamification.api)
```
services/backend/src/main/java/com/hometusk/gamification/api/GamificationController.java
```

### DTO (package: com.hometusk.gamification.dto)
```
services/backend/src/main/java/com/hometusk/gamification/dto/GamificationProgressResponse.java
services/backend/src/main/java/com/hometusk/gamification/dto/BadgeCatalogResponse.java
services/backend/src/main/java/com/hometusk/gamification/dto/PointsEntryDto.java
services/backend/src/main/java/com/hometusk/gamification/dto/BadgeDto.java
```

### Tests
```
services/backend/src/test/java/com/hometusk/gamification/service/PointsServiceTest.java
services/backend/src/test/java/com/hometusk/gamification/service/BadgeServiceTest.java
services/backend/src/test/java/com/hometusk/gamification/api/GamificationControllerIntegrationTest.java
```

---

## Files to Modify

### Integration with Task Completion
```
services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java
```
After `taskService.complete(...)`:
```java
// Award points and check badges
pointsService.awardForTaskCompleted(task, actor);
badgeService.checkAndAwardBadges(task.getAssignee(), task.getHousehold());
```

### Notification Type (add BADGE_EARNED)
```
services/backend/src/main/java/com/hometusk/notifications/domain/NotificationType.java
```
Add: `BADGE_EARNED`

### OpenAPI
```
docs/contracts/http/commands.openapi.yaml
```
Add:
- Tag: Gamification
- Paths: `/households/{householdId}/gamification/progress`, `/households/{householdId}/gamification/badges`
- Schemas: GamificationProgressResponse, BadgeCatalogResponse, PointsEntry, BadgeDto

---

## Implementation Details

### V018 Migration SQL
```sql
-- V018__create_gamification_tables.sql

CREATE TABLE badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    criteria VARCHAR(500),
    icon_name VARCHAR(50)
);

CREATE TABLE points_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    points INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    note VARCHAR(500),
    CONSTRAINT points_ledger_reason_check CHECK (
        reason IN ('TASK_COMPLETED', 'ON_TIME_BONUS', 'TASK_UNCOMPLETED', 'ON_TIME_BONUS_REVERSED')
    )
);

CREATE TABLE user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id, badge_id)
);

-- Idempotency index (CRITICAL)
CREATE UNIQUE INDEX idx_points_ledger_task_user_reason
ON points_ledger(task_id, user_id, reason);

CREATE INDEX idx_points_ledger_user_household_created
ON points_ledger(user_id, household_id, created_at DESC);

CREATE INDEX idx_points_ledger_household_created
ON points_ledger(household_id, created_at DESC);

CREATE INDEX idx_user_badges_user_household
ON user_badges(user_id, household_id);

-- Seed badges (S08 streak-free)
INSERT INTO badges (id, code, name, description, criteria, icon_name) VALUES
    (gen_random_uuid(), 'FIRST_TASK', 'Task Starter', 'You completed your first task!', 'Complete 1 task', 'star'),
    (gen_random_uuid(), 'TEN_TASKS', 'Task Champion', 'You are on a roll!', 'Complete 10 tasks', 'trophy'),
    (gen_random_uuid(), 'WEEK_WARRIOR', 'Week Warrior', 'Productive week!', 'Complete 7+ tasks in one week', 'fire'),
    (gen_random_uuid(), 'ZONE_SPECIALIST', 'Zone Specialist', 'Master of your domain!', 'Complete 5+ tasks in one zone', 'target'),
    (gen_random_uuid(), 'ON_TIME_HERO', 'On-Time Hero', 'Beating the clock!', 'Complete 5 tasks before deadline', 'clock');

-- Add BADGE_EARNED to notifications type
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN (
        'INVITE_ACCEPTED',
        'TASK_ASSIGNED',
        'TASK_COMPLETED',
        'SHOPPING_ITEM_ADDED',
        'SHOPPING_ITEM_PURCHASED',
        'BADGE_EARNED'
    )
);
```

### PointsService Key Methods
```java
@Service
public class PointsService {

    private static final int BASE_POINTS = 10;
    private static final int ON_TIME_BONUS = 5;

    @Transactional
    public List<PointsLedger> awardForTaskCompleted(Task task, User actor) {
        if (task.getAssignee() == null) {
            return List.of();
        }

        List<PointsLedger> entries = new ArrayList<>();
        User assignee = task.getAssignee();
        Household household = task.getHousehold();

        // Base points (idempotent)
        PointsLedger base = awardPointsIdempotent(
            assignee, household, task, BASE_POINTS, PointsReason.TASK_COMPLETED
        );
        if (base != null) entries.add(base);

        // On-time bonus (only if deadline exists and met)
        if (task.getDeadline() != null &&
            task.getCompletedAt() != null &&
            task.getCompletedAt().isBefore(task.getDeadline())) {

            PointsLedger bonus = awardPointsIdempotent(
                assignee, household, task, ON_TIME_BONUS, PointsReason.ON_TIME_BONUS
            );
            if (bonus != null) entries.add(bonus);
        }

        return entries;
    }

    private PointsLedger awardPointsIdempotent(User user, Household household,
            Task task, int points, PointsReason reason) {
        // Check if already exists
        Optional<PointsLedger> existing = repository.findByTask_IdAndUser_IdAndReason(
            task.getId(), user.getId(), reason
        );
        if (existing.isPresent()) {
            return null; // Already awarded
        }

        try {
            PointsLedger entry = new PointsLedger(user, household, task, points, reason);
            return repository.save(entry);
        } catch (DataIntegrityViolationException e) {
            // Race condition - already inserted
            return null;
        }
    }

    @Transactional
    public void reverseForTaskUncompleted(Task task, User actor) {
        if (task.getAssignee() == null) return;

        User assignee = task.getAssignee();
        Household household = task.getHousehold();

        // Reverse base points
        if (repository.findByTask_IdAndUser_IdAndReason(
                task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED).isPresent()) {
            awardPointsIdempotent(assignee, household, task, -BASE_POINTS, PointsReason.TASK_UNCOMPLETED);
        }

        // Reverse on-time bonus if it was awarded
        if (repository.findByTask_IdAndUser_IdAndReason(
                task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS).isPresent()) {
            awardPointsIdempotent(assignee, household, task, -ON_TIME_BONUS, PointsReason.ON_TIME_BONUS_REVERSED);
        }
    }
}
```

### BadgeService Key Methods
```java
@Service
public class BadgeService {

    @Transactional
    public void checkAndAwardBadges(User user, Household household) {
        if (user == null) return;

        UUID userId = user.getId();
        UUID householdId = household.getId();

        // Get already earned badge codes
        Set<String> earned = userBadgeRepository.findBadgeCodesByUserAndHousehold(userId, householdId);

        // Check each badge criteria
        checkAndAward("FIRST_TASK", earned, userId, householdId, user, household,
            () -> pointsLedgerRepository.countByUserAndReason(userId, householdId, PointsReason.TASK_COMPLETED) >= 1);

        checkAndAward("TEN_TASKS", earned, userId, householdId, user, household,
            () -> pointsLedgerRepository.countByUserAndReason(userId, householdId, PointsReason.TASK_COMPLETED) >= 10);

        checkAndAward("WEEK_WARRIOR", earned, userId, householdId, user, household,
            () -> pointsLedgerRepository.countTasksCompletedInLastDays(userId, householdId, 7) >= 7);

        checkAndAward("ZONE_SPECIALIST", earned, userId, householdId, user, household,
            () -> pointsLedgerRepository.maxTasksCompletedInSameZone(userId, householdId) >= 5);

        checkAndAward("ON_TIME_HERO", earned, userId, householdId, user, household,
            () -> pointsLedgerRepository.countByUserAndReason(userId, householdId, PointsReason.ON_TIME_BONUS) >= 5);
    }

    private void checkAndAward(String badgeCode, Set<String> earned,
            UUID userId, UUID householdId, User user, Household household,
            Supplier<Boolean> criteriaCheck) {
        if (earned.contains(badgeCode)) return;
        if (!criteriaCheck.get()) return;

        Badge badge = badgeRepository.findByCode(badgeCode).orElse(null);
        if (badge == null) return;

        try {
            UserBadge userBadge = new UserBadge(user, household, badge);
            userBadgeRepository.save(userBadge);

            // Notify
            notificationService.notifyBadgeEarned(user, household, badge);
        } catch (DataIntegrityViolationException e) {
            // Already awarded (race condition)
        }
    }
}
```

### GamificationController
```java
@RestController
@RequestMapping("/api/v1/households/{householdId}/gamification")
@Tag(name = "Gamification", description = "Gamification progress and badges")
public class GamificationController {

    @GetMapping("/progress")
    @Operation(summary = "Get user's gamification progress")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progress data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<GamificationProgressResponse> getProgress(
            @PathVariable UUID householdId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        GamificationProgressResponse response = GamificationProgressResponse.builder()
            .userId(currentUser.id())
            .totalPoints(pointsService.getTotalPoints(currentUser.id(), householdId))
            .pointsThisWeek(pointsService.getPointsThisWeek(currentUser.id(), householdId))
            .earnedBadges(badgeService.getEarnedBadges(currentUser.id(), householdId))
            .recentActivity(pointsService.getRecentActivity(currentUser.id(), householdId, 10))
            .householdTotalTasks(pointsService.getHouseholdTotalTasks(householdId))
            .householdTotalPoints(pointsService.getHouseholdTotalPoints(householdId))
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/badges")
    @Operation(summary = "Get badge catalog with earned status")
    public ResponseEntity<BadgeCatalogResponse> getBadges(
            @PathVariable UUID householdId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        return ResponseEntity.ok(badgeService.getBadgeCatalog(currentUser.id(), householdId));
    }
}
```

---

## Test Cases

### PointsServiceTest (Unit)
- `awardForTaskCompleted_withAssignee_awardsBasePoints`
- `awardForTaskCompleted_withDeadlineAndOnTime_awardsBonusPoints`
- `awardForTaskCompleted_withDeadlineButLate_noBonusPoints`
- `awardForTaskCompleted_withNoDeadline_noBonusPoints`
- `awardForTaskCompleted_withNoAssignee_noPoints`
- `awardForTaskCompleted_duplicate_idempotent`
- `reverseForTaskUncompleted_reversesBaseAndBonus`
- `reverseForTaskUncompleted_idempotent`

### BadgeServiceTest (Unit)
- `checkAndAwardBadges_firstTask_awardsBadge`
- `checkAndAwardBadges_tenTasks_awardsBadge`
- `checkAndAwardBadges_weekWarrior_7tasksIn7days_awardsBadge`
- `checkAndAwardBadges_zoneSpecialist_5tasksInZone_awardsBadge`
- `checkAndAwardBadges_onTimeHero_5onTimeBonuses_awardsBadge`
- `checkAndAwardBadges_alreadyEarned_noDuplicate`

### GamificationControllerIntegrationTest
- `getProgress_asMember_returnsProgress`
- `getProgress_asNonMember_returns403`
- `getProgress_afterTaskComplete_showsPoints`
- `getBadges_asMember_returnsCatalog`
- `getBadges_showsEarnedStatus`

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Format
./gradlew spotlessApply

# Build
./gradlew build

# Unit tests
./gradlew test --tests "*PointsServiceTest*"
./gradlew test --tests "*BadgeServiceTest*"

# Integration tests
./gradlew test --tests "*GamificationControllerIntegrationTest*"

# All gamification tests
./gradlew test --tests "*gamification*"

# Full test suite
./gradlew test
```

---

## DoD Checklist

- [ ] V018 migration created and applies successfully
- [ ] PointsLedger entity with idempotency
- [ ] PointsReason enum (4 values)
- [ ] Badge entity
- [ ] UserBadge entity
- [ ] PointsService with award/reverse methods
- [ ] BadgeService with criteria checking
- [ ] GamificationController with 2 endpoints
- [ ] DTOs: GamificationProgressResponse, BadgeCatalogResponse, PointsEntryDto, BadgeDto
- [ ] Integration with ActionExecutor (task completion hook)
- [ ] NotificationType.BADGE_EARNED added
- [ ] Unit tests pass (PointsServiceTest, BadgeServiceTest)
- [ ] Integration tests pass (GamificationControllerIntegrationTest)
- [ ] 403 for non-members verified
- [ ] Idempotency verified (no duplicate points)
- [ ] On-time bonus edge case verified (no deadline = no bonus)
- [ ] OpenAPI updated
- [ ] Spotless applied
- [ ] Build passes

---

## STOP-THE-LINE Rules

If you encounter:
- Test failures you cannot resolve
- Missing dependencies or unclear imports
- Conflicting patterns in codebase

**STOP and report. Do not force or skip.**
