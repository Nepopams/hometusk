# Codex PLAN Prompt: ST-906 — Privacy Settings + Opt-out

## Mode
**PLAN ONLY** — read-only exploration, NO file modifications.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-906/workpack.md`
- Story: `docs/planning/epics/EP-009/stories/ST-906-privacy-settings.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Objective
Plan the implementation of gamification privacy settings:
1. Backend: GamificationSettings entity + PUT/GET endpoints
2. Enforcement: gamificationEnabled check in PointsService/BadgeService
3. Web: Settings toggle UI on Progress page

---

## Discovery Commands (read-only)

### Backend Structure
```bash
# Check existing gamification domain
ls -la services/backend/src/main/java/com/hometusk/gamification/domain/
cat services/backend/src/main/java/com/hometusk/gamification/domain/PointsLedger.java

# Check existing gamification services
ls -la services/backend/src/main/java/com/hometusk/gamification/service/
cat services/backend/src/main/java/com/hometusk/gamification/service/PointsService.java
cat services/backend/src/main/java/com/hometusk/gamification/service/BadgeService.java

# Check controller
cat services/backend/src/main/java/com/hometusk/gamification/api/GamificationController.java

# Check existing DTOs
ls -la services/backend/src/main/java/com/hometusk/gamification/dto/

# Check migrations
ls -la services/backend/src/main/resources/db/migration/
```

### Web Structure
```bash
# Check current Progress page
cat clients/web/src/routes/Progress.tsx

# Check gamification hook
cat clients/web/src/hooks/useGamification.ts

# Check API functions
cat clients/web/src/lib/api.ts | grep -A5 "gamification"

# Check types
cat clients/web/src/types/api.ts | grep -A20 "Gamification"
```

---

## Expected Deliverables

### Backend
1. **Entity**: `GamificationSettings.java`
   - Fields: id, userId, householdId, showProgressToOthers, gamificationEnabled, createdAt, updatedAt
   - Unique constraint: (userId, householdId)

2. **Repository**: `GamificationSettingsRepository.java`
   - `findByUserIdAndHouseholdId(UUID, UUID)`
   - Lazy-create pattern with defaults

3. **DTO**: `GamificationSettingsDto.java`
   - Request/response record

4. **Controller changes**:
   - `PUT /gamification/settings`
   - `GET /gamification/settings` (or include in progress)

5. **Service changes**:
   - PointsService: check gamificationEnabled
   - BadgeService: check gamificationEnabled

6. **Migration**: V019__add_gamification_settings.sql

### Web
1. **Type**: GamificationSettings interface
2. **API**: getSettings, updateSettings functions
3. **Hook**: include settings in useGamification
4. **Component**: PrivacySettingsCard with toggles

---

## Questions to Answer

1. What is the latest migration version number?
2. How does PointsService currently award points (method signature)?
3. How does BadgeService currently check badges (method signature)?
4. What pattern does the project use for lazy-create (or should we use explicit create)?
5. Does GamificationController already return settings in progress response?

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
4. Test cases needed
5. Integration points

**STOP-THE-LINE**: If any required context is missing, stop and request it.
