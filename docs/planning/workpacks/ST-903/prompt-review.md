# Codex REVIEW Prompt: ST-903 — Streaks v0

## Mode
**REVIEW** — verify implementation against acceptance criteria.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-903/workpack.md`
- Story: `docs/planning/epics/EP-009/stories/ST-903-streaks.md`
- DoD: `docs/_governance/dod.md`

---

## Review Checklist

### Backend Entity & Repository
- [ ] `StreakState.java` exists with @ManyToOne for User and Household
- [ ] `StreakStateRepository.java` exists
- [ ] `findByUser_IdAndHousehold_Id` method present
- [ ] Unique constraint on (user_id, household_id)
- [ ] @PreUpdate for updatedAt

### Backend Service
- [ ] `StreakService.java` exists
- [ ] `getOrCreate(User, Household)` method with race condition handling
- [ ] `updateStreak(User, Household, LocalDate)` method
- [ ] Checks gamificationEnabled before updating
- [ ] Grace day logic correct (daysSince == 2)
- [ ] Best streak preserved on reset

### Backend Migration
- [ ] V020 migration file exists
- [ ] Table `streak_states` created
- [ ] Index on (user_id, household_id) created

### Backend DTO
- [ ] `GamificationProgressResponse` includes `currentStreak`
- [ ] `GamificationProgressResponse` includes `bestStreak`
- [ ] `GamificationProgressResponse` includes `graceAvailable`

### Backend Controller
- [ ] `getProgress()` returns streak fields
- [ ] StreakService injected

### Backend Integration
- [ ] ActionExecutor calls `streakService.updateStreak()` after points/badges
- [ ] Uses UTC for LocalDate conversion

### Backend GamificationSettings Update
- [ ] `GamificationSettings.java` has `streakVisible` field
- [ ] `GamificationSettingsDto` includes `streakVisible`
- [ ] `GamificationSettingsService.update()` handles `streakVisible`

### Backend Tests
- [ ] `StreakServiceTest.java` exists
- [ ] Test: First activity sets streak to 1
- [ ] Test: Consecutive day increments streak
- [ ] Test: Same day no change
- [ ] Test: Grace day preserves streak
- [ ] Test: Streak resets after 3+ days
- [ ] Test: Best streak preserved

### Web Types
- [ ] `GamificationProgress` includes `currentStreak`
- [ ] `GamificationProgress` includes `bestStreak`
- [ ] `GamificationProgress` includes `graceAvailable`
- [ ] `GamificationSettings` includes `streakVisible`

### Web UI
- [ ] Streak displayed on Progress page (PersonalProgressCard)
- [ ] Best streak shown when applicable
- [ ] Grace available indicator
- [ ] PrivacySettingsCard has streakVisible toggle

---

## Verification Commands

```bash
# Backend tests
cd services/backend
./gradlew test --tests "*StreakService*" --info
./gradlew test --tests "*Gamification*"

# Check migration applied
./gradlew flywayInfo

# Spotless
./gradlew spotlessCheck

# Web build
cd clients/web
npm run build

# Web lint
npm run lint
```

---

## Test Scenarios to Verify

### AC-1: Streak increments on daily task
- User with streak=5, lastActivityDate=yesterday
- Complete task today
- Expected: streak=6, lastActivityDate=today

### AC-2: Grace day prevents reset
- User with streak=5, lastActivityDate=2 days ago, graceUsedToday=false
- Complete task today
- Expected: streak=6, graceUsedToday=true

### AC-3: Streak resets after grace exhausted
- User with streak=5, lastActivityDate=3 days ago
- Complete task today
- Expected: streak=1, bestStreak=max(5, previous)

### AC-4: Best streak preserved
- User with bestStreak=10, currentStreak=5
- Streak resets
- Expected: bestStreak remains 10

### AC-5: Streak visible in progress
- GET /progress
- Response includes: currentStreak, bestStreak, graceAvailable

### AC-6: Gamification disabled skips streak
- User with gamificationEnabled=false
- Complete task
- Expected: streak NOT updated

---

## Expected Files

### Created
- `services/backend/src/main/java/com/hometusk/gamification/domain/StreakState.java`
- `services/backend/src/main/java/com/hometusk/gamification/repository/StreakStateRepository.java`
- `services/backend/src/main/java/com/hometusk/gamification/service/StreakService.java`
- `services/backend/src/main/resources/db/migration/V020__add_streak_states.sql`
- `services/backend/src/test/java/com/hometusk/gamification/service/StreakServiceTest.java`

### Modified
- `GamificationProgressResponse.java` — add streak fields
- `GamificationController.java` — include streak in response, inject StreakService
- `ActionExecutor.java` — call StreakService on task completion
- `GamificationSettings.java` — add streakVisible field
- `GamificationSettingsDto.java` — add streakVisible
- `GamificationSettingsService.java` — handle streakVisible
- `clients/web/src/types/api.ts` — add streak fields + streakVisible
- `clients/web/src/components/gamification/PersonalProgressCard.tsx` — show streak
- `clients/web/src/components/gamification/PrivacySettingsCard.tsx` — add streakVisible toggle
- `clients/web/src/routes/Progress.css` — streak styles

---

## GO/NO-GO Decision

### Must-Fix (blocks merge)
- Streak logic incorrect
- Grace day not working
- Best streak not preserved
- gamificationEnabled check missing
- Build fails

### Should-Fix (follow-up ticket)
- CSS polish
- Streak animation
- Better messaging for grace day

---

## Output Format

```
## Review Result: [GO / NO-GO]

### Must-Fix Issues
- [list or "None"]

### Should-Fix Issues
- [list or "None"]

### Evidence
- Backend tests: [PASS/FAIL]
- Web build: [PASS/FAIL]
- Streak increments: [VERIFIED/FAILED]
- Grace day works: [VERIFIED/FAILED]
- Best streak preserved: [VERIFIED/FAILED]
- Gamification check: [VERIFIED/FAILED]

### Recommendation
[Approve for merge / Block with required fixes]
```
