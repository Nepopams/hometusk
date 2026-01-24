# Story: ST-901 — Points Ledger + Task Completion

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Закончил задачу — получил очки. Вижу свой total в профиле. Простая и понятная механика."

---

## Description
Implement points ledger system:
- Event-driven: task completed → points added
- Reversible: task uncompleted → points subtracted
- Audit trail: all point changes logged
- API: get user points total

---

## Acceptance Criteria

### AC-1: Points awarded on task completion
```
Given user completes a task
When task status changes to DONE
Then PointsLedger entry created with:
  - userId = assignee
  - points = 10 (base)
  - reason = TASK_COMPLETED
  - taskId = task.id
And user's total points increases by 10
```

### AC-2: On-time bonus (if deadline set)
```
Given task has deadline (deadline != null)
And task completed before deadline
When task marked DONE
Then additional entry created:
  - points = 5
  - reason = ON_TIME_BONUS
```

### AC-2a: No bonus if no deadline
```
Given task has no deadline (deadline = null)
When task marked DONE
Then only base points awarded (10)
And NO on-time bonus entry created
```

### AC-3: Points reversed on uncomplete (full rollback)
```
Given user uncompletes a task (status back to OPEN)
And task previously earned 10 base + 5 on-time bonus
When status change processed
Then TWO reversal entries created:
  - entry1: points = -10, reason = TASK_UNCOMPLETED
  - entry2: points = -5, reason = ON_TIME_BONUS_REVERSED (if bonus was awarded)
And user's total decreases by sum of original awards
```

### AC-3a: Reversal is idempotent
```
Given task was already uncompleted (reversal entries exist)
When uncomplete event received again
Then no new reversal entries created (deduplicated)
```

### AC-4: No points for unassigned tasks
```
Given task has no assignee
When task completed
Then no points awarded
```

### AC-4a: Idempotency — no double-award
```
Given task completion event received
And PointsLedger already has entry for (taskId, recipientUserId, TASK_COMPLETED)
When same event processed again
Then no new entry created (deduplicated)
And existing response returned
```

### AC-4b: Idempotency key strategy
```
Dedup key = (taskId, recipientUserId, reason)
- Handles reassignment: if task reassigned to different user, new user can earn points
- Handles retries: same (task, user, reason) → no duplicate
- TASK_COMPLETED: one per (task, user)
- ON_TIME_BONUS: one per (task, user) if applicable
- TASK_UNCOMPLETED: one per (task, user)
- ON_TIME_BONUS_REVERSED: one per (task, user) if applicable
```

### AC-5: API returns user points
```
Given authenticated user in household
When GET /households/{id}/gamification/progress
Then response includes:
  - totalPoints: number
  - pointsThisWeek: number
  - recentActivity: PointsLedger[] (last 10)
```

### AC-6: Household boundary enforced
```
Given user NOT member of household
When requesting gamification progress
Then 403 Forbidden
```

---

## Domain Impact

### New Entities
```java
@Entity
public class PointsLedger {
    @Id
    private UUID id;
    private UUID userId;
    private UUID householdId;
    private UUID taskId; // nullable
    private int points;
    private PointsReason reason;
    private Instant createdAt;
    private UUID createdBy; // for manual adj
    private String note;
}

public enum PointsReason {
    TASK_COMPLETED,        // +10 base
    ON_TIME_BONUS,         // +5 if before deadline
    TASK_UNCOMPLETED,      // -10 reversal
    ON_TIME_BONUS_REVERSED // -5 reversal (if bonus was awarded)
    // MANUAL_ADJUSTMENT deferred to S09+
}
```

### New Services
- `PointsService`: calculate, award, reverse points
- Integration with `TaskService` (event listener or domain event)

### New Endpoints
- `GET /api/v1/households/{id}/gamification/progress`

---

## Contract Impact
**Yes** — New endpoint + schema in OpenAPI

```yaml
GamificationProgress:
  type: object
  properties:
    userId:
      type: string
      format: uuid
    totalPoints:
      type: integer
    pointsThisWeek:
      type: integer
    recentActivity:
      type: array
      items:
        $ref: '#/components/schemas/PointsEntry'
```

---

## DB Migration
```sql
CREATE TABLE points_ledger (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    task_id UUID REFERENCES tasks(id),
    points INTEGER NOT NULL,
    reason VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    note VARCHAR(500)
);

CREATE INDEX idx_points_user_household ON points_ledger(user_id, household_id);
CREATE INDEX idx_points_household_created ON points_ledger(household_id, created_at);
```

---

## Test Notes

### Unit Tests
- PointsService.awardPoints()
- PointsService.reversePoints()
- On-time bonus calculation

### Integration Tests
- Complete task → verify points entry
- Uncomplete task → verify reversal
- 403 for non-member

---

## Points
**5 points**

---

## Flags
- contract_impact: yes
- security_sensitive: yes
