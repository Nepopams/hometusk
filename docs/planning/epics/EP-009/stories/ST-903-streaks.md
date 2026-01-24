# Story: ST-903 — Streaks v0 (Grace Day + Opt-out)

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Deferred** — OUT OF SCOPE for S08 (planned for S09)

**Rationale:** Avoid "streak pressure" early; validate value with Points + Badges first.

## User Value
> "Держу серию 5 дней. Вчера не успел — но grace day спас серию. Стресса нет, мотивация есть."

---

## Description
Implement streak tracking with no-shame design:
- Daily streak: "days with ≥1 completed task"
- Grace day: miss 1 day without losing streak
- Best streak: preserved separately ("Your best: 14 days")
- Opt-out: user can hide streak from UI

---

## Acceptance Criteria

### AC-1: Streak increments on daily task
```
Given user has currentStreak = 5
And lastActivityDate = yesterday
When user completes task today
Then currentStreak = 6
And lastActivityDate = today
```

### AC-2: Grace day prevents reset
```
Given user has currentStreak = 5
And lastActivityDate = 2 days ago
And graceUsedToday = false
When user completes task today
Then currentStreak = 6 (streak preserved)
And graceUsedToday = true
```

### AC-3: Streak resets after grace exhausted
```
Given user has currentStreak = 5
And lastActivityDate = 3 days ago
When user completes task today
Then currentStreak = 1 (reset)
And bestStreak = max(5, previous best)
```

### AC-4: Best streak preserved
```
Given user's bestStreak = 10
And currentStreak = 5
When streak resets
Then bestStreak remains 10
```

### AC-5: Streak visible in progress
```
When GET /households/{id}/gamification/progress
Then response includes:
  - currentStreak: number
  - bestStreak: number
  - streakGraceAvailable: boolean
```

### AC-6: User can hide streak
```
Given user sets streakVisible = false
When household view requested
Then user's streak not shown to others
But user can still see own streak
```

---

## No-Shame Design

### Wording Rules
| Scenario | Message |
|----------|---------|
| Streak continues | "Day {N}! Keep it up!" |
| Grace day used | "Grace day activated! Your streak is safe." |
| Streak reset | "Starting fresh! Your best was {N} days." |
| No activity | (no push notification — no guilt) |

### Anti-Compulsion
- NO "Your streak is at risk!" notifications
- NO countdown timers
- User can disable streak tracking entirely

---

## Domain Impact

### New Entity
```java
@Entity
public class StreakState {
    @Id
    private UUID id;
    private UUID userId;
    private UUID householdId;
    private int currentStreak;
    private int bestStreak;
    private LocalDate lastActivityDate;
    private boolean graceUsedToday;
    private boolean streakVisible;
    private Instant updatedAt;
}
```

### New Service
- `StreakService`: update streak on task completion, handle grace logic

---

## Contract Impact
**Yes** — Add streak fields to progress response

---

## DB Migration
```sql
CREATE TABLE streak_states (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    last_activity_date DATE,
    grace_used_today BOOLEAN NOT NULL DEFAULT FALSE,
    streak_visible BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id)
);
```

---

## Test Notes

### Unit Tests
- StreakService: increment, grace day, reset
- Best streak preservation
- Multi-day gap handling

### Integration Tests
- Complete task → streak updated
- Grace day scenario
- Reset scenario

---

## Points
**5 points**

---

## Flags
- contract_impact: yes
