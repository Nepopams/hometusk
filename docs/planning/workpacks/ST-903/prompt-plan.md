# Codex PLAN Prompt: ST-903 — Streaks v0

## Mode
**PLAN ONLY** — read-only exploration, NO file modifications.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-903/workpack.md`
- Story: `docs/planning/epics/EP-009/stories/ST-903-streaks.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Objective
Plan the implementation of streak tracking:
1. Backend: StreakState entity + StreakService + integration with task completion
2. Grace day logic (1 day tolerance)
3. Best streak preservation
4. Add streak fields to progress response

---

## Discovery Commands (read-only)

### Backend Structure
```bash
# Check existing gamification domain
ls -la services/backend/src/main/java/com/hometusk/gamification/domain/
cat services/backend/src/main/java/com/hometusk/gamification/domain/GamificationSettings.java

# Check existing services
ls -la services/backend/src/main/java/com/hometusk/gamification/service/
cat services/backend/src/main/java/com/hometusk/gamification/service/PointsService.java

# Check GamificationProgressResponse
cat services/backend/src/main/java/com/hometusk/gamification/dto/GamificationProgressResponse.java

# Check controller
cat services/backend/src/main/java/com/hometusk/gamification/api/GamificationController.java

# Check where task completion triggers gamification
rg "awardForTaskCompleted" --type java

# Check migrations
ls -la services/backend/src/main/resources/db/migration/
```

### Web Structure
```bash
# Check Progress page current state
cat clients/web/src/routes/Progress.tsx

# Check gamification types
cat clients/web/src/types/api.ts | grep -A30 "GamificationProgress"
```

---

## Expected Deliverables

### Backend
1. **Entity**: `StreakState.java`
   - Fields: id, user (@ManyToOne), household (@ManyToOne), currentStreak, bestStreak, lastActivityDate, graceUsedToday, streakVisible, updatedAt
   - Unique constraint: (user_id, household_id)

2. **Repository**: `StreakStateRepository.java`
   - `findByUser_IdAndHousehold_Id(UUID, UUID)`

3. **Service**: `StreakService.java`
   - `updateStreak(User user, Household household, LocalDate activityDate)`
   - Grace day logic
   - Best streak preservation
   - Check gamificationEnabled before updating

4. **Migration**: V020__add_streak_states.sql

5. **DTO changes**: Add streak fields to `GamificationProgressResponse`

6. **Integration**: Call StreakService from task completion flow

### Web
1. **Type update**: Add streak fields to GamificationProgress
2. **UI**: Show streak on Progress page (PersonalProgressCard enhancement)

---

## Questions to Answer

1. What is the latest migration version number after V019?
2. Where exactly is task completion handled (which handler/service)?
3. How does GamificationProgressResponse currently look?
4. Does PointsService or a handler call awardForTaskCompleted?
5. How should graceUsedToday reset (daily cron vs on activity)?

---

## Allowed Commands (PLAN mode)
- `ls`, `find`
- `cat`, `head`, `tail`
- `rg`, `grep`
- `git status`, `git diff` (read-only)

## Forbidden (PLAN mode)
- File modifications
- `git commit/push`
- Package installs

---

## Output Expected
Detailed implementation plan with:
1. Exact file paths
2. Code structure for each component
3. Migration SQL
4. Integration points with task completion
5. Grace day reset strategy
6. Test cases needed

**STOP-THE-LINE**: If any required context is missing, stop and request it.
