# Codex REVIEW Prompt: ST-906 — Privacy Settings + Opt-out

## Mode
**REVIEW** — verify implementation against acceptance criteria.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-906/workpack.md`
- Story: `docs/planning/epics/EP-009/stories/ST-906-privacy-settings.md`
- DoD: `docs/_governance/dod.md`

---

## Review Checklist

### Backend Entity & Repository
- [ ] `GamificationSettings.java` exists with @ManyToOne for User and Household
- [ ] `GamificationSettingsRepository.java` exists
- [ ] `findByUser_IdAndHousehold_Id` method present
- [ ] Unique constraint on (user_id, household_id)
- [ ] @PreUpdate for updatedAt

### Backend Service
- [ ] `GamificationSettingsService.java` exists
- [ ] `getOrCreate(User, Household)` method with race condition handling
- [ ] `update(User, Household, GamificationSettingsDto)` method
- [ ] `isGamificationEnabled(User, Household)` method

### Backend Migration
- [ ] V019 migration file exists
- [ ] Table `gamification_settings` created
- [ ] Index on (user_id, household_id) created
- [ ] Defaults are TRUE for both toggles

### Backend Endpoints
- [ ] `GET /gamification/settings` returns settings (lazy-create)
- [ ] `PUT /gamification/settings` updates settings
- [ ] Both endpoints check membership (403 for non-members)

### Backend Enforcement
- [ ] PointsService.awardForTaskCompleted checks `gamificationEnabled`
- [ ] PointsService.reverseForTaskUncompleted does NOT check (always runs)
- [ ] BadgeService.checkAndAwardBadges checks `gamificationEnabled`
- [ ] Disabled user does NOT earn points
- [ ] Disabled user does NOT earn badges

### Backend Tests
- [ ] `GamificationSettingsIntegrationTest.java` exists
- [ ] Test: GET returns defaults
- [ ] Test: PUT updates and persists
- [ ] Test: GET 403 for non-member
- [ ] Test: PUT 403 for non-member

### Web Types & API
- [ ] `GamificationSettings` interface exists
- [ ] `getGamificationSettings` function works
- [ ] `updateGamificationSettings` function works

### Web Hook
- [ ] `useGamification` returns `settings`
- [ ] `useGamification` returns `updateSettings`
- [ ] `useGamification` returns `isUpdating`
- [ ] Parallel fetch includes settings

### Web UI
- [ ] `PrivacySettingsCard` renders
- [ ] "Show my progress to household members" toggle present
- [ ] "Enable gamification" toggle present
- [ ] Warning shown when gamification disabled
- [ ] Toggles update on change
- [ ] "Saving..." indicator during update

### Security
- [ ] GET settings: 403 for non-members
- [ ] PUT settings: 403 for non-members
- [ ] No cross-household data access

---

## Verification Commands

```bash
# Backend tests
cd services/backend
./gradlew test --tests "*GamificationSettings*" --info
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

### 1. Settings Defaults
- New user has no settings → GET returns defaults (both true)

### 2. Settings Update
- PUT with `gamificationEnabled: false` → saved
- Subsequent GET returns false

### 3. Enforcement
- User disables gamification
- User completes task
- NO points awarded
- NO badge check

### 4. Reverse Still Works
- User with points disables gamification
- Task gets uncompleted
- Points ARE reversed (reverseForTaskUncompleted runs)

### 5. Security
- User A requests User B's household settings → 403

---

## Expected Files

### Created
- `services/backend/src/main/java/com/hometusk/gamification/domain/GamificationSettings.java`
- `services/backend/src/main/java/com/hometusk/gamification/repository/GamificationSettingsRepository.java`
- `services/backend/src/main/java/com/hometusk/gamification/dto/GamificationSettingsDto.java`
- `services/backend/src/main/java/com/hometusk/gamification/service/GamificationSettingsService.java`
- `services/backend/src/main/resources/db/migration/V019__add_gamification_settings.sql`
- `services/backend/src/test/java/com/hometusk/integration/GamificationSettingsIntegrationTest.java`
- `clients/web/src/components/gamification/PrivacySettingsCard.tsx`

### Modified
- `GamificationController.java` — new GET/PUT /settings endpoints
- `PointsService.java` — gamificationEnabled check in awardForTaskCompleted
- `BadgeService.java` — gamificationEnabled check in checkAndAwardBadges
- `clients/web/src/types/api.ts` — GamificationSettings type
- `clients/web/src/lib/api.ts` — settings functions
- `clients/web/src/hooks/useGamification.ts` — include settings
- `clients/web/src/components/gamification/index.ts` — export PrivacySettingsCard
- `clients/web/src/routes/Progress.tsx` — render settings card
- `clients/web/src/routes/Progress.css` — settings styles

---

## GO/NO-GO Decision

### Must-Fix (blocks merge)
- Settings not persisting
- Enforcement not working (disabled user still earns points)
- reverseForTaskUncompleted broken (should always run)
- 403 not returned for non-members
- Build fails

### Should-Fix (follow-up ticket)
- CSS polish
- Loading states
- Optimistic UI updates

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
- Settings persist: [VERIFIED/FAILED]
- Enforcement works: [VERIFIED/FAILED]
- Reverse still works: [VERIFIED/FAILED]

### Recommendation
[Approve for merge / Block with required fixes]
```
