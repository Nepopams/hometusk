# Story: ST-902 — Badges v0 (5 Milestones)

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Сделал 10 задач — получил бейдж 'Task Starter'. Приятное признание прогресса."

---

## Description
Implement 5 milestone badges that auto-unlock (streak-free, S08 compatible):
1. **Task Starter** — Complete first task
2. **Task Champion** — Complete 10 tasks
3. **Week Warrior** — Complete 7+ tasks in one week
4. **Zone Specialist** — Complete 5+ tasks in same zone
5. **On-Time Hero** — Complete 5 tasks before deadline (ledger-derived, no ST-903 dependency)

**Note:** Original "Consistent Contributor" (7-day streak) deferred to S09 with ST-903.

---

## Acceptance Criteria

### AC-1: Badge catalog exists
```
Given system initialized
Then Badge table contains 5 predefined badges
And each has: code, name, description, criteria
```

### AC-2: Badge auto-awarded on criteria met
```
Given user completes 10th task
When PointsLedger entry created
Then BadgeService checks criteria
And UserBadge entry created for "TASK_CHAMPION"
And Notification sent (if available)
```

### AC-3: Badge not awarded twice
```
Given user already has "TASK_CHAMPION" badge
When user completes 11th task
Then no duplicate badge awarded
```

### AC-4: API returns earned badges
```
Given user with 2 badges
When GET /households/{id}/gamification/progress
Then response includes:
  - badges: [{code, name, earnedAt}, ...]
```

### AC-5: Badge catalog endpoint
```
When GET /households/{id}/gamification/badges
Then returns all badges + earned status per user
```

---

## Badge Definitions (S08 — Streak-free)

| Code | Name | Criteria | Icon | Ledger Query |
|------|------|----------|------|--------------|
| FIRST_TASK | Task Starter | Complete 1 task | star | COUNT(TASK_COMPLETED) >= 1 |
| TEN_TASKS | Task Champion | Complete 10 tasks | trophy | COUNT(TASK_COMPLETED) >= 10 |
| WEEK_WARRIOR | Week Warrior | 7+ tasks in 7 days | fire | COUNT(TASK_COMPLETED WHERE created_at > NOW-7d) >= 7 |
| ZONE_SPECIALIST | Zone Specialist | 5+ tasks in same zone | target | COUNT(TASK_COMPLETED GROUP BY zone) >= 5 |
| ON_TIME_HERO | On-Time Hero | 5 tasks before deadline | clock | COUNT(ON_TIME_BONUS) >= 5 |

**Deferred to S09:** SEVEN_DAY_STREAK (requires ST-903)

---

## Domain Impact

### New Entities
```java
@Entity
public class Badge {
    @Id
    private UUID id;
    @Column(unique = true)
    private String code;
    private String name;
    private String description;
    private String criteria;
    private String iconName;
}

@Entity
public class UserBadge {
    @Id
    private UUID id;
    private UUID userId;
    private UUID householdId;
    private UUID badgeId;
    private Instant earnedAt;
}
```

### New Services
- `BadgeService`: check criteria, award badge
- `BadgeCriteriaChecker`: strategy per badge type

---

## Contract Impact
**Yes** — Add to progress response + badges endpoint

---

## DB Migration
```sql
CREATE TABLE badges (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    criteria VARCHAR(500),
    icon_name VARCHAR(50)
);

CREATE TABLE user_badges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    badge_id UUID NOT NULL REFERENCES badges(id),
    earned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id, badge_id)
);

-- Seed badges (S08 — streak-free)
INSERT INTO badges (id, code, name, description, criteria, icon_name) VALUES
    (gen_random_uuid(), 'FIRST_TASK', 'Task Starter', 'You completed your first task!', 'Complete 1 task', 'star'),
    (gen_random_uuid(), 'TEN_TASKS', 'Task Champion', 'You are on a roll!', 'Complete 10 tasks', 'trophy'),
    (gen_random_uuid(), 'WEEK_WARRIOR', 'Week Warrior', 'Productive week!', 'Complete 7+ tasks in one week', 'fire'),
    (gen_random_uuid(), 'ZONE_SPECIALIST', 'Zone Specialist', 'Master of your domain!', 'Complete 5+ tasks in one zone', 'target'),
    (gen_random_uuid(), 'ON_TIME_HERO', 'On-Time Hero', 'Beating the clock!', 'Complete 5 tasks before deadline', 'clock');
-- Note: SEVEN_DAY_STREAK deferred to S09 migration
```

---

## Test Notes

### Unit Tests
- BadgeCriteriaChecker for each badge type
- No duplicate badge award

### Integration Tests
- Complete task → badge earned → notification

---

## Points
**3 points**

---

## Flags
- contract_impact: yes
