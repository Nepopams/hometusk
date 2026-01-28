# Codex APPLY Prompt: ST-903 — Streaks v0

## Mode
**APPLY** — implement the approved plan.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-903/workpack.md`
- Story: `docs/planning/epics/EP-009/stories/ST-903-streaks.md`
- DoD: `docs/_governance/dod.md`

---

## PLAN Clarifications (Human Gate Approved)

1. **Migration version**: V020 (V019 is GamificationSettings)
2. **Task completion integration point**: `ActionExecutor.executeCompleteTask()` — after `pointsService.awardForTaskCompleted()` and `badgeService.checkAndAwardBadges()`
3. **Grace day reset strategy**: No cron — reset on activity only. Grace resets to false on consecutive-day or after streak reset.
4. **streakVisible**: Add to `GamificationSettings` entity/dto (NOT in StreakState) — all privacy settings in one place
5. **Timezone**: Use UTC for LocalDate (`ZoneOffset.UTC`)

---

## Critical Constraints

### Streak Behavior
- Streak unit: "days with ≥1 completed task"
- Per-user per-household
- 1 grace day (miss 1 day without losing streak)
- Best streak preserved on reset
- streakVisible for opt-out (user can hide from others)
- Check gamificationEnabled before updating streak

### No-Shame Design
| Scenario | Message |
|----------|---------|
| Streak = 1 | "Day 1! Great start!" |
| Streak continues | "Day {N}! Keep it up!" |
| Grace used | "Grace day saved your streak!" |
| Streak reset | "Fresh start! Your best: {N} days" |

### Anti-Compulsion Rules
- NO "Your streak is at risk!" notifications
- NO countdown timers
- User can disable streak tracking entirely

---

## Task 1: Backend — Entity

### File: `services/backend/src/main/java/com/hometusk/gamification/domain/StreakState.java`
```java
package com.hometusk.gamification.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "streak_states",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "household_id"}))
public class StreakState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "grace_used_today", nullable = false)
    private boolean graceUsedToday = false;

    // NOTE: streakVisible is in GamificationSettings, NOT here

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StreakState() {}

    public StreakState(User user, Household household) {
        this.user = user;
        this.household = household;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.graceUsedToday = false;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Household getHousehold() { return household; }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public LocalDate getLastActivityDate() { return lastActivityDate; }
    public boolean isGraceUsedToday() { return graceUsedToday; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    public void setLastActivityDate(LocalDate lastActivityDate) { this.lastActivityDate = lastActivityDate; }
    public void setGraceUsedToday(boolean graceUsedToday) { this.graceUsedToday = graceUsedToday; }
}
```

---

## Task 2: Backend — Repository

### File: `services/backend/src/main/java/com/hometusk/gamification/repository/StreakStateRepository.java`
```java
package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.StreakState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StreakStateRepository extends JpaRepository<StreakState, UUID> {
    Optional<StreakState> findByUser_IdAndHousehold_Id(UUID userId, UUID householdId);
}
```

---

## Task 3: Backend — Service

### File: `services/backend/src/main/java/com/hometusk/gamification/service/StreakService.java`
```java
package com.hometusk.gamification.service;

import com.hometusk.gamification.domain.StreakState;
import com.hometusk.gamification.repository.StreakStateRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    private final StreakStateRepository repository;
    private final GamificationSettingsService settingsService;

    public StreakService(StreakStateRepository repository, GamificationSettingsService settingsService) {
        this.repository = repository;
        this.settingsService = settingsService;
    }

    @Transactional
    public StreakState getOrCreate(User user, Household household) {
        return repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .orElseGet(() -> {
                    try {
                        StreakState state = new StreakState(user, household);
                        StreakState saved = repository.save(state);
                        log.info("Created streak state for user {} in household {}", user.getId(), household.getId());
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        log.debug("Streak state already created by concurrent request, re-fetching");
                        return repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                                .orElseThrow(() -> new IllegalStateException("Streak state should exist"));
                    }
                });
    }

    @Transactional
    public void updateStreak(User user, Household household, LocalDate activityDate) {
        // Check if gamification enabled
        if (!settingsService.isGamificationEnabled(user, household)) {
            log.debug("Gamification disabled for user {}, skipping streak update", user.getId());
            return;
        }

        StreakState state = getOrCreate(user, household);
        LocalDate lastActivity = state.getLastActivityDate();

        // Reset grace flag if this is a new day compared to last activity
        if (lastActivity != null && !lastActivity.equals(activityDate)) {
            state.setGraceUsedToday(false);
        }

        if (lastActivity == null) {
            // First activity ever
            state.setCurrentStreak(1);
            log.info("User {} started streak in household {}", user.getId(), household.getId());
        } else {
            long daysSince = ChronoUnit.DAYS.between(lastActivity, activityDate);

            if (daysSince == 0) {
                // Same day, no change to streak
                return;
            } else if (daysSince == 1) {
                // Consecutive day - increment streak
                state.setCurrentStreak(state.getCurrentStreak() + 1);
                log.info("User {} streak increased to {} in household {}",
                        user.getId(), state.getCurrentStreak(), household.getId());
            } else if (daysSince == 2 && !state.isGraceUsedToday()) {
                // Grace day scenario - missed 1 day but grace saves it
                state.setCurrentStreak(state.getCurrentStreak() + 1);
                state.setGraceUsedToday(true);
                log.info("User {} used grace day, streak preserved at {} in household {}",
                        user.getId(), state.getCurrentStreak(), household.getId());
            } else {
                // Streak broken - preserve best and reset
                if (state.getCurrentStreak() > state.getBestStreak()) {
                    state.setBestStreak(state.getCurrentStreak());
                }
                log.info("User {} streak reset from {} to 1 in household {} (best: {})",
                        user.getId(), state.getCurrentStreak(), household.getId(), state.getBestStreak());
                state.setCurrentStreak(1);
            }
        }

        state.setLastActivityDate(activityDate);
        repository.save(state);
    }

    public StreakState getStreakState(User user, Household household) {
        return getOrCreate(user, household);
    }
}
```

---

## Task 4: Backend — Migration

### File: `services/backend/src/main/resources/db/migration/V020__add_streak_states.sql`
```sql
-- Streak states table (streakVisible is in gamification_settings)
CREATE TABLE streak_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    household_id UUID NOT NULL REFERENCES households(id),
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    last_activity_date DATE,
    grace_used_today BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, household_id)
);

CREATE INDEX idx_streak_states_user_household
    ON streak_states(user_id, household_id);

-- Add streakVisible to gamification_settings
ALTER TABLE gamification_settings
    ADD COLUMN streak_visible BOOLEAN NOT NULL DEFAULT TRUE;
```

---

## Task 5: Backend — DTO Update

### Modify: `GamificationProgressResponse.java`

Add streak fields:
```java
public record GamificationProgressResponse(
        UUID userId,
        int totalPoints,
        int pointsThisWeek,
        List<BadgeDto> earnedBadges,
        List<PointsEntryDto> recentActivity,
        int householdTotalTasks,
        int householdTotalPoints,
        int currentStreak,        // NEW
        int bestStreak,           // NEW
        boolean graceAvailable    // NEW: true if grace not used today
) {}
```

---

## Task 6: Backend — Controller Update

### Modify: `GamificationController.java`

Add StreakService injection:
```java
private final StreakService streakService;
```

Update getProgress() to include streak:
```java
@GetMapping("/progress")
public ResponseEntity<GamificationProgressResponse> getProgress(@PathVariable UUID householdId) {
    // ... existing code ...

    // Get streak state
    StreakState streakState = streakService.getStreakState(user, household);

    return ResponseEntity.ok(new GamificationProgressResponse(
            // ... existing fields ...
            streakState.getCurrentStreak(),
            streakState.getBestStreak(),
            !streakState.isGraceUsedToday()
    ));
}
```

---

## Task 7: Backend — Integration with Task Completion

### Modify: `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`

Add StreakService injection:
```java
private final StreakService streakService;
```

In `executeCompleteTask()` and `executeCompleteTaskFromAction()`, after badges:
```java
// After badgeService.checkAndAwardBadges(...)
LocalDate activityDate = task.getCompletedAt()
        .atZone(ZoneOffset.UTC)
        .toLocalDate();
streakService.updateStreak(task.getAssignee(), task.getHousehold(), activityDate);
```

---

## Task 8: Backend — Update GamificationSettings Entity

### Modify: `services/backend/src/main/java/com/hometusk/gamification/domain/GamificationSettings.java`

Add streakVisible field:
```java
@Column(name = "streak_visible", nullable = false)
private boolean streakVisible = true;
```

Add to constructor:
```java
this.streakVisible = true;
```

Add getter/setter:
```java
public boolean isStreakVisible() { return streakVisible; }
public void setStreakVisible(boolean streakVisible) { this.streakVisible = streakVisible; }
```

---

## Task 9: Backend — Update GamificationSettingsDto

### Modify: `services/backend/src/main/java/com/hometusk/gamification/dto/GamificationSettingsDto.java`

Add streakVisible:
```java
@Schema(description = "Gamification privacy settings")
public record GamificationSettingsDto(
        @Schema(description = "Show progress to other household members") boolean showProgressToOthers,
        @Schema(description = "Enable gamification (points, badges, streaks)") boolean gamificationEnabled,
        @Schema(description = "Show streak to other household members") boolean streakVisible) {

    public static GamificationSettingsDto from(GamificationSettings settings) {
        return new GamificationSettingsDto(
                settings.isShowProgressToOthers(),
                settings.isGamificationEnabled(),
                settings.isStreakVisible());
    }

    public static GamificationSettingsDto defaults() {
        return new GamificationSettingsDto(true, true, true);
    }
}
```

---

## Task 10: Backend — Update GamificationSettingsService

### Modify: `services/backend/src/main/java/com/hometusk/gamification/service/GamificationSettingsService.java`

Update `update()` method to include streakVisible:
```java
@Transactional
public GamificationSettings update(User user, Household household, GamificationSettingsDto request) {
    GamificationSettings settings = getOrCreate(user, household);
    settings.setShowProgressToOthers(request.showProgressToOthers());
    settings.setGamificationEnabled(request.gamificationEnabled());
    settings.setStreakVisible(request.streakVisible());
    return repository.save(settings);
}

public boolean isStreakVisible(User user, Household household) {
    return repository.findByUser_IdAndHousehold_Id(user.getId(), household.getId())
            .map(GamificationSettings::isStreakVisible)
            .orElse(true); // Default visible
}
```

---

## Task 11: Backend — Unit Tests

### File: `services/backend/src/test/java/com/hometusk/gamification/service/StreakServiceTest.java`
```java
// Test cases:
// 1. First activity sets streak to 1
// 2. Consecutive day increments streak
// 3. Same day activity doesn't change streak
// 4. Grace day preserves streak (daysSince = 2)
// 5. Streak resets after 3+ days gap
// 6. Best streak preserved on reset
// 7. Grace flag resets on new day
// 8. Gamification disabled skips update
```

---

## Task 12: Web — Types Update

### Modify: `clients/web/src/types/api.ts`

Update GamificationProgress:
```typescript
export interface GamificationProgress {
  userId: string;
  totalPoints: number;
  pointsThisWeek: number;
  earnedBadges: Badge[];
  recentActivity: PointsEntry[];
  householdTotalTasks: number;
  householdTotalPoints: number;
  currentStreak: number;      // NEW
  bestStreak: number;         // NEW
  graceAvailable: boolean;    // NEW
}
```

Update GamificationSettings (add streakVisible):
```typescript
export interface GamificationSettings {
  showProgressToOthers: boolean;
  gamificationEnabled: boolean;
  streakVisible: boolean;     // NEW
}
```

---

## Task 13: Web — PrivacySettingsCard Update

### Modify: `clients/web/src/components/gamification/PrivacySettingsCard.tsx`

Add streakVisible toggle:
```tsx
<div className="privacy-settings__option">
  <label className="privacy-settings__label">
    <input
      type="checkbox"
      checked={settings.streakVisible}
      onChange={(e) => onUpdate({ streakVisible: e.target.checked })}
      disabled={isUpdating}
    />
    <span>Show my streak to household members</span>
  </label>
</div>
```

---

## Task 14: Web — PersonalProgressCard UI Update

### Modify: `clients/web/src/components/gamification/PersonalProgressCard.tsx`

Add streak display:
```tsx
// After points display
<div className="personal-progress__streak">
  <span className="streak-value">{progress.currentStreak}</span>
  <span className="streak-label">day streak</span>
  {progress.bestStreak > 0 && progress.bestStreak > progress.currentStreak && (
    <span className="streak-best">Best: {progress.bestStreak} days</span>
  )}
  {progress.graceAvailable && progress.currentStreak > 0 && (
    <span className="streak-grace">Grace day available</span>
  )}
</div>
```

### Add CSS to `Progress.css`:
```css
/* Streak */
.personal-progress__streak {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--color-surface-secondary);
  border-radius: 8px;
  margin-top: 16px;
}

.streak-value {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-primary);
}

.streak-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.streak-best {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.streak-grace {
  font-size: var(--font-size-xs);
  color: var(--color-success);
  margin-top: 4px;
}
```

---

## Verification Commands

```bash
# Backend
cd services/backend
./gradlew test --tests "*StreakService*"
./gradlew test --tests "*Gamification*"
./gradlew spotlessApply

# Web
cd clients/web
npm run build
npm run lint
```

---

## DoD Checklist

- [ ] StreakState entity created (with @ManyToOne)
- [ ] StreakStateRepository created
- [ ] StreakService with streak logic (UTC dates)
- [ ] Migration V020 applied (streak_states + ALTER gamification_settings)
- [ ] Grace day logic works
- [ ] Best streak preserved on reset
- [ ] GamificationSettings entity has streakVisible
- [ ] GamificationSettingsDto includes streakVisible
- [ ] GamificationProgressResponse includes streak fields
- [ ] Controller returns streak data
- [ ] ActionExecutor calls StreakService on task completion
- [ ] gamificationEnabled check in StreakService
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Web GamificationProgress type updated
- [ ] Web GamificationSettings type has streakVisible
- [ ] PrivacySettingsCard has streakVisible toggle
- [ ] PersonalProgressCard shows streak
- [ ] Spotless applied
- [ ] Web builds

---

## STOP-THE-LINE
If any of the following, STOP and report:
- Migration version conflict
- Task completion integration point unclear
- GamificationProgressResponse structure differs from expected
- Required dependencies missing
