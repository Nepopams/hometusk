# Codex PLAN Prompt: ST-901 + ST-902 — Points Ledger + Badges v0

## Mode
**PLAN ONLY** — Read-only exploration. NO edits, NO file writes.

---

## Allowed Commands (whitelist)
```
ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
```

## Forbidden
- Any file modifications (edit/write/move/delete)
- Any network access
- Package install / system changes
- git commit/push, migrations, DB operations

---

## Sources of Truth (MUST READ)
```
docs/planning/workpacks/ST-901-902/workpack.md
docs/planning/epics/EP-009/stories/ST-901-points-ledger.md
docs/planning/epics/EP-009/stories/ST-902-badges-achievements.md
docs/planning/epics/EP-009/epic.md
docs/_governance/dod.md
```

---

## Context

You are implementing the backend gamification foundation for HomeTusk:
- **ST-901:** Points Ledger — award points on task completion
- **ST-902:** Badges v0 — 5 milestone badges that auto-unlock

This is Sprint S08 "SAFE" scope: Points + Badges only (no streaks, no privacy toggle).

---

## Critical Constraints (MUST FOLLOW)

### Points Model
| Parameter | Value |
|-----------|-------|
| Base points | 10 per completed task |
| On-time bonus | +5 if task.deadline != null AND completedAt < deadline |
| Overdue penalty | NONE (anti-shame) |
| No deadline | If task.deadline = null → NO on-time bonus |
| Reversible | Yes (task uncomplete → full rollback) |

### Idempotency (CRITICAL)
```
Idempotency key = (taskId, recipientUserId, reason)
```
- Same (taskId, userId, TASK_COMPLETED) → return existing, no duplicate
- Handles reassignment: different userId = new award allowed
- Applies to all reasons: TASK_COMPLETED, ON_TIME_BONUS, TASK_UNCOMPLETED, ON_TIME_BONUS_REVERSED

### Badges (5 — streak-free)
| Code | Criteria | Query |
|------|----------|-------|
| FIRST_TASK | Complete 1 task | COUNT(TASK_COMPLETED) >= 1 |
| TEN_TASKS | Complete 10 tasks | COUNT(TASK_COMPLETED) >= 10 |
| WEEK_WARRIOR | 7+ tasks in 7 days | COUNT(TASK_COMPLETED WHERE created_at > NOW-7d) >= 7 |
| ZONE_SPECIALIST | 5+ tasks in same zone | COUNT per zone >= 5 |
| ON_TIME_HERO | 5 tasks before deadline | COUNT(ON_TIME_BONUS) >= 5 |

**NO SEVEN_DAY_STREAK** — requires ST-903 (deferred to S09).

### Integration Point
```java
// After TaskService.complete() returns successfully:
// 1. Award points to task.getAssignee()
// 2. Check and award badges
// Use @TransactionalEventListener or direct call
```

### Migration Number
Latest migration is V017. Use **V018** for gamification tables.

---

## Existing Patterns to Follow

### Entity Pattern
```
services/backend/src/main/java/com/hometusk/tasks/domain/Task.java
```
- Use UUID with @GeneratedValue(strategy = GenerationType.UUID)
- Use @ManyToOne(fetch = FetchType.LAZY) for relationships
- Protected no-arg constructor + business constructor

### Controller Pattern
```
services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java
```
- Use membershipService.requireMembership(userId, householdId)
- Use userResolver.resolveCurrentUser()
- Add @Tag, @Operation, @ApiResponses annotations

### Service Pattern
```
services/backend/src/main/java/com/hometusk/tasks/service/TaskService.java
```
- Use @Transactional for write operations
- Use @Transactional(readOnly = true) for reads
- Throw BusinessException for rule violations

---

## Your Task

1. **Read** the sources of truth listed above
2. **Explore** the existing codebase for patterns:
   - Entity structure (Task.java, Notification.java)
   - Repository queries (TaskRepository, NotificationRepository)
   - Controller structure (AnalyticsController)
   - Service structure (TaskService, NotificationService)
   - Integration tests (AnalyticsControllerIntegrationTest)
3. **Identify** integration point for task completion → points award
4. **Produce a detailed implementation plan** with:
   - File list (exact paths)
   - Class/method signatures
   - Key implementation details
   - Test cases to write
   - Migration SQL (V018)
   - OpenAPI additions

---

## Output Format

```markdown
# Implementation Plan: ST-901 + ST-902

## Step 1: Migration V018
[SQL content]

## Step 2: Entities
[File paths + class structure]

## Step 3: Repositories
[File paths + query methods]

## Step 4: PointsService
[Methods + idempotency logic]

## Step 5: BadgeService
[Methods + criteria checkers]

## Step 6: GamificationController
[Endpoints + response structure]

## Step 7: Integration with TaskService
[Event listener or direct call approach]

## Step 8: Tests
[Unit test cases + integration test cases]

## Step 9: OpenAPI
[Additions needed]

## Questions / Blockers
[If any critical info missing]
```

---

## STOP-THE-LINE Rules

If you encounter:
- Missing information about Task completion flow
- Unclear integration point
- Conflicting patterns in codebase

**STOP and list what's missing. Do not invent.**

---

## Verification (after PLAN approved)

The PLAN will be reviewed and approved before APPLY prompt is generated.
