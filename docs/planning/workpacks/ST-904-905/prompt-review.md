# Codex REVIEW Prompt: ST-904 + ST-905 — Gamification UI + Security

## Mode
**REVIEW** — verify implementation against acceptance criteria.

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-904-905/workpack.md`
- Stories:
  - `docs/planning/epics/EP-009/stories/ST-904-gamification-ui.md`
  - `docs/planning/epics/EP-009/stories/ST-905-security-boundaries.md`
- DoD: `docs/_governance/dod.md`

---

## Review Checklist

### ST-905: Security Tests

#### Test Coverage
- [ ] `getProgress_notMember_returns403` exists and passes
- [ ] `getProgress_differentHousehold_noDataLeak` exists and passes
- [ ] `getProgress_idorAttempt_returns403` exists and passes
- [ ] `getProgress_userSeesOnlyOwnProgress` exists and passes
- [ ] `getProgress_householdAggregateIncludesAllMembers` exists and passes

#### Security Verification
- [ ] Non-member gets 403 (not 404) for any household
- [ ] IDOR with random UUID returns 403 (not 404)
- [ ] Cross-household data never exposed
- [ ] User cannot see other members' individual points/badges

### ST-904: Web Progress Page

#### Functional
- [ ] Page accessible at `/households/{id}/progress`
- [ ] Personal card shows: totalPoints, pointsThisWeek, earnedBadges
- [ ] Household card shows: totalTasks, totalPoints
- [ ] Empty state displayed for 0 points users
- [ ] Loading spinner during fetch
- [ ] Error state with retry button
- [ ] 403 redirects to access denied

#### S08 Scope Compliance
- [ ] **NO streak display** (verify absent)
- [ ] **NO individual member breakdown** (verify absent)
- [ ] **NO privacy toggle** (verify absent)

#### Non-Toxic Wording
- [ ] "Progress" as page title
- [ ] "Your Progress" or "Your Points"
- [ ] "Household Team Progress"
- [ ] "Keep it up!" style encouragement
- [ ] No "Leaderboard", no rank numbers

#### Navigation
- [ ] "Progress" link in Sidebar after "Analytics"
- [ ] Route `/households/:householdId/progress` works

### Code Quality

#### Backend
- [ ] Tests extend `IntegrationTestBase`
- [ ] Spotless formatting applied
- [ ] No compiler warnings
- [ ] Test isolation (no shared state between tests)

#### Web
- [ ] TypeScript compiles without errors
- [ ] ESLint passes (new files only)
- [ ] Follows existing patterns (useAnalytics, Analytics.tsx)
- [ ] No console.log statements
- [ ] Proper error handling

---

## Verification Commands

```bash
# Run security tests
cd services/backend
./gradlew test --tests "*GamificationSecurity*" --info

# Run all gamification tests
./gradlew test --tests "*Gamification*"

# Check formatting
./gradlew spotlessCheck

# Web build
cd clients/web
npm run build

# Web lint (check new files)
npm run lint -- --quiet
```

---

## Expected Files Changed/Created

### Created
- `services/backend/src/test/java/com/hometusk/integration/GamificationSecurityIntegrationTest.java`
- `clients/web/src/hooks/useGamification.ts`
- `clients/web/src/components/gamification/PersonalProgressCard.tsx`
- `clients/web/src/components/gamification/HouseholdAggregateCard.tsx`
- `clients/web/src/components/gamification/BadgeGrid.tsx`
- `clients/web/src/components/gamification/index.ts`
- `clients/web/src/routes/Progress.tsx`
- `clients/web/src/routes/Progress.css`

### Modified
- `clients/web/src/types/api.ts` — added gamification types
- `clients/web/src/lib/api.ts` — added API functions
- `clients/web/src/hooks/index.ts` — export useGamification
- `clients/web/src/routes/index.tsx` — Progress route
- `clients/web/src/components/Layout/Sidebar.tsx` — Progress link

---

## GO/NO-GO Decision

### Must-Fix (blocks merge)
- Security tests fail
- Cross-household data leak
- Individual breakdown shown (violates S08)
- Streak shown (violates S08)
- Build fails

### Should-Fix (can merge, follow-up ticket)
- Minor lint warnings (pre-existing)
- CSS polish
- Accessibility improvements

---

## Output Format

```
## Review Result: [GO / NO-GO]

### Must-Fix Issues
- [list or "None"]

### Should-Fix Issues
- [list or "None"]

### Evidence
- Security tests: [PASS/FAIL]
- Web build: [PASS/FAIL]
- S08 compliance: [VERIFIED/VIOLATED]

### Recommendation
[Approve for merge / Block with required fixes]
```
