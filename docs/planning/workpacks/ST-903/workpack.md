# Workpack: ST-903 — Streaks v0

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- Story: `docs/planning/epics/EP-009/stories/ST-903-streaks.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — S09 Scope

**Previous:** Deferred from S08 to avoid "streak pressure" early.

---

## Outcome
Streak tracking with no-shame design:
- Daily streak increments on task completion
- Grace day prevents immediate reset
- Best streak preserved separately
- User can hide streak visibility

---

## Key Decisions (Pending Human Gate)
- Streak unit: "days with ≥1 completed task"
- Per-user (not per-household)
- 1 grace day (configurable)
- Best streak preserved on reset
- User can opt-out of streak display

---

## Scope

### In Scope
- StreakState entity + repository
- StreakService (update logic)
- Grace day handling
- Best streak preservation
- Add streak to progress response
- Integration with task completion

### Out of Scope
- Household streaks
- Multiple grace days / freeze tokens
- Streak-based badge (handled by BadgeService)

---

## Files to Create

| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/gamification/domain/StreakState.java` | Entity |
| `services/backend/src/main/java/com/hometusk/gamification/repository/StreakStateRepository.java` | Repository |
| `services/backend/src/main/java/com/hometusk/gamification/service/StreakService.java` | Streak logic |

### Files to Modify
| Path | Changes |
|------|---------|
| `GamificationProgressResponse.java` | Add streak fields |
| `GamificationController.java` | Include streak in response |
| Migration file | Add streak_states table |

---

## Streak Logic

```java
public void updateStreak(UUID userId, UUID householdId, LocalDate activityDate) {
    StreakState state = getOrCreate(userId, householdId);
    LocalDate lastActivity = state.getLastActivityDate();

    if (lastActivity == null) {
        // First activity
        state.setCurrentStreak(1);
    } else {
        long daysSince = ChronoUnit.DAYS.between(lastActivity, activityDate);

        if (daysSince == 0) {
            // Same day, no change
            return;
        } else if (daysSince == 1) {
            // Consecutive day
            state.setCurrentStreak(state.getCurrentStreak() + 1);
        } else if (daysSince == 2 && !state.isGraceUsedToday()) {
            // Grace day scenario
            state.setCurrentStreak(state.getCurrentStreak() + 1);
            state.setGraceUsedToday(true);
        } else {
            // Streak broken
            state.setBestStreak(Math.max(state.getBestStreak(), state.getCurrentStreak()));
            state.setCurrentStreak(1);
        }
    }

    state.setLastActivityDate(activityDate);
    state.setUpdatedAt(Instant.now());
    repository.save(state);
}
```

---

## DB Migration Addition

```sql
-- Add to V20 or create V21

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

## Verification Commands

```bash
./gradlew test --tests "*StreakServiceTest*"
./gradlew test --tests "*Gamification*"
```

---

## No-Shame Wording (for UI layer)
| Scenario | Message |
|----------|---------|
| Streak = 1 | "Day 1! Great start!" |
| Streak continues | "Day {N}! Keep it up!" |
| Grace used | "Grace day saved your streak!" |
| Streak reset | "Fresh start! Your best: {N} days" |

---

## Risks
| Risk | Mitigation |
|------|------------|
| Timezone issues | Use user's local date or UTC consistently |
| Grace day confusion | Clear messaging in UI |
| Race conditions | Transactional + idempotent |

---

## DoD Checklist
- [ ] StreakState entity created
- [ ] StreakService logic correct
- [ ] Grace day works
- [ ] Best streak preserved
- [ ] Progress response includes streak
- [ ] Unit tests pass
- [ ] Integration tests pass
