# Codex PLAN Prompt: ST-904 + ST-905 — Gamification UI + Security

## Mode
**PLAN ONLY** — read-only exploration, NO file modifications.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-904-905/workpack.md`
- Stories:
  - `docs/planning/epics/EP-009/stories/ST-904-gamification-ui.md`
  - `docs/planning/epics/EP-009/stories/ST-905-security-boundaries.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Objective
Plan the implementation of:
1. **ST-905**: Security integration tests for gamification endpoints (5 tests)
2. **ST-904**: Web Progress page (points + badges, household aggregate)

---

## Constraints (S08 SAFE Scope)
- **NO streak display** (ST-903 deferred)
- **NO individual member breakdown** (requires privacy toggle, ST-906 deferred)
- **NO privacy settings UI** (ST-906 deferred)
- Household progress shows **aggregate only** (totalTasks, totalPoints)
- User sees **only own progress** details (points, badges)

---

## Task 1: ST-905 — Security Tests

### Acceptance Criteria
1. `getProgress_notMember_returns403` — 403 for non-members
2. `getProgress_differentHousehold_noDataLeak` — no cross-household leak
3. `getProgress_idorAttempt_returns403` — IDOR returns 403 (not 404)
4. `getProgress_userSeesOnlyOwnProgress` — user sees only own data
5. `getProgress_householdAggregateIncludesAllMembers` — aggregate = sum of all

### Discovery Commands (read-only)
```bash
# Check existing gamification controller and tests
cat services/backend/src/main/java/com/hometusk/gamification/api/GamificationController.java
cat services/backend/src/test/java/com/hometusk/integration/GamificationControllerIntegrationTest.java
cat services/backend/src/test/java/com/hometusk/integration/IntegrationTestBase.java

# Check DTO structure
cat services/backend/src/main/java/com/hometusk/gamification/dto/GamificationProgressResponse.java
```

### Target File
`services/backend/src/test/java/com/hometusk/integration/GamificationSecurityIntegrationTest.java`

### Implementation Notes
- Extend `IntegrationTestBase`
- Use `jwtForUser(user)` for auth
- Use `testUser` (member) and `testUser2` (non-member by default)
- Create second household for cross-household tests
- Verify membership enforcement via `membershipService.requireMembership`

---

## Task 2: ST-904 — Web Progress Page

### Acceptance Criteria
1. Progress page accessible at `/households/{id}/progress`
2. Personal progress card: totalPoints, pointsThisWeek, earnedBadges
3. Household aggregate card: totalTasks, totalPoints (NO individual breakdown)
4. Empty state for new users
5. Loading/error states
6. Navigation link in Sidebar after "Analytics"
7. Non-toxic wording ("Keep it up!", "Team progress", etc.)

### Discovery Commands (read-only)
```bash
# Check existing API endpoint
cat services/backend/src/main/java/com/hometusk/gamification/api/GamificationController.java
cat services/backend/src/main/java/com/hometusk/gamification/dto/GamificationProgressResponse.java

# Check web structure
ls -la clients/web/src/routes/
ls -la clients/web/src/hooks/
ls -la clients/web/src/components/
ls -la clients/web/src/lib/

# Check existing patterns
cat clients/web/src/routes/Analytics.tsx
cat clients/web/src/hooks/useAnalytics.ts
cat clients/web/src/lib/api.ts
cat clients/web/src/types/api.ts
cat clients/web/src/components/Layout/Sidebar.tsx
cat clients/web/src/routes/index.tsx
```

### Files to Create
| Path | Purpose |
|------|---------|
| `clients/web/src/types/api.ts` | Add GamificationProgress, Badge, PointsEntry types |
| `clients/web/src/lib/api.ts` | Add getGamificationProgress, getBadgeCatalog |
| `clients/web/src/hooks/useGamification.ts` | Data fetching hook |
| `clients/web/src/components/gamification/PersonalProgressCard.tsx` | Points + badges |
| `clients/web/src/components/gamification/HouseholdAggregateCard.tsx` | Team total |
| `clients/web/src/components/gamification/BadgeGrid.tsx` | Badge display |
| `clients/web/src/components/gamification/index.ts` | Exports |
| `clients/web/src/routes/Progress.tsx` | Page component |
| `clients/web/src/routes/Progress.css` | Styles |

### Files to Modify
| Path | Changes |
|------|---------|
| `clients/web/src/routes/index.tsx` | Add Progress route |
| `clients/web/src/components/Layout/Sidebar.tsx` | Add Progress nav link |

### Backend API (already exists)
- `GET /api/v1/households/{householdId}/gamification/progress`
  - Returns: `{ userId, totalPoints, pointsThisWeek, earnedBadges[], recentActivity[], householdTotalTasks, householdTotalPoints }`
- `GET /api/v1/households/{householdId}/gamification/badges`
  - Returns: `{ badges[] }`

---

## Verification Commands
```bash
# Backend tests
cd services/backend && ./gradlew test --tests "*GamificationSecurity*"
cd services/backend && ./gradlew spotlessCheck

# Web
cd clients/web && npm run build
cd clients/web && npm run lint
```

---

## Allowed Commands (PLAN mode)
- `ls`, `find`
- `cat`, `head`, `tail`
- `rg`, `grep`
- `git status`, `git diff` (read-only)

## Forbidden (PLAN mode)
- File modifications (edit/write/move/delete)
- `git commit/push`
- Package installs
- Network access

---

## Output Expected
Produce a detailed implementation plan with:
1. Exact file paths and changes
2. Code structure for each component
3. Test cases for security tests
4. Verification steps

**STOP-THE-LINE**: If any required context is missing, stop and request it.
