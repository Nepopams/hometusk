# Workpack: ST-904 + ST-905 — Gamification UI + Security (SAFE S08)

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- Stories:
  - `docs/planning/epics/EP-009/stories/ST-904-gamification-ui.md`
  - `docs/planning/epics/EP-009/stories/ST-905-security-boundaries.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-gamification-motivation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

> Note: ST-906 (Privacy Settings) deferred to S09. Original workpack renamed from ST-904-906.

---

## Status
**Draft** — SAFE S08 Scope (no streaks, no privacy toggle, aggregate-only household view)

---

## Outcome (SAFE S08)
Web gamification experience with security:
1. Progress page showing points, badges (**no streaks** in S08)
2. Household **aggregate only** (no individual breakdown until privacy exists)
3. ~~Privacy controls~~ DEFERRED to S09
4. Security tests (403, no leaks, user sees only own progress)

---

## Key Decisions (SAFE S08 — Approved)
- Non-toxic wording only
- ~~Privacy toggle per-user~~ DEFERRED to S09
- ~~Full opt-out option~~ DEFERRED to S09
- Household aggregate only (no individual breakdown)
- User sees only own progress details

---

## Scope

### In Scope (S08)
- ST-904: Progress page with "My Progress" + "Household Aggregate"
- ST-905: Security integration tests (5 tests, no privacy toggle tests)
- Navigation link in sidebar
- Empty states for new users
- Error/loading states

### Out of Scope (S08)
- Individual member breakdown (requires privacy toggle)
- Privacy settings UI toggle (ST-906 deferred)
- Streak display (ST-903 deferred)
- Mobile layout optimization
- Charts/graphs (simple cards only)

---

## Files to Create

### ST-904: Web UI Components (S08)
| Path | Purpose |
|------|---------|
| `services/web/src/pages/households/[householdId]/progress/index.tsx` | Progress page |
| `services/web/src/components/gamification/PersonalProgressCard.tsx` | User's points + badges (no streak) |
| `services/web/src/components/gamification/HouseholdAggregateCard.tsx` | Team total (no breakdown) |
| `services/web/src/components/gamification/BadgeGrid.tsx` | Earned badges display |
| `services/web/src/hooks/useGamification.ts` | API fetching hook |
| `services/web/src/lib/api/gamification.ts` | API client functions |

### ST-905: Security Tests (S08)
| Path | Purpose |
|------|---------|
| `services/backend/src/test/java/com/hometusk/gamification/api/GamificationSecurityIntegrationTest.java` | Security test suite (5 tests) |

### DEFERRED (ST-906 — S09)
~~| `services/backend/src/main/java/com/hometusk/gamification/domain/GamificationSettings.java` | Settings entity |~~
~~| `services/web/src/components/gamification/PrivacySettingsCard.tsx` | UI toggle card |~~

### Files to Modify
| Path | Changes |
|------|---------|
| `services/web/src/components/Layout/Sidebar.tsx` | Add Progress nav link |

---

## Implementation Plan

### Step 1: ST-905 — Security Tests First (S08 scope)
Write security tests that initially fail (TDD approach):

```java
@DisplayName("Gamification Security Tests — S08")
class GamificationSecurityIntegrationTest {

    @Test
    void getProgress_notMember_returns403() {
        // User A requests progress for Household B
        // Assert 403 Forbidden
    }

    @Test
    void getProgress_differentHousehold_noDataLeak() {
        // User in H1, H2 has 500 points
        // Request H1 progress
        // Assert no H2 data present
    }

    @Test
    void getProgress_idorAttempt_returns403() {
        // Use guessed UUID for householdId
        // Assert 403 (not 404)
    }

    @Test
    void getProgress_userSeesOnlyOwnProgress() {
        // User requests progress
        // Assert only own userId in response details
    }

    @Test
    void getProgress_householdAggregateIncludesAll() {
        // Household has 3 members with points
        // Assert aggregate = sum of all
    }

    // DEFERRED to S09:
    // void getHouseholdProgress_privacyToggleRespected() {}
    // void manualAdjustment_auditLogCreated() {}
}
```

### Step 2: ST-904 — Web UI

#### Page Structure (S08)
```tsx
// /progress/index.tsx
export default function ProgressPage() {
  const { householdId } = useParams();
  const { data, isLoading, error } = useGamification(householdId);

  if (isLoading) return <Spinner />;
  if (error) return <ErrorState />;

  return (
    <div className="space-y-6">
      <h1>Progress</h1>
      <PersonalProgressCard
        points={data.totalPoints}
        pointsThisWeek={data.pointsThisWeek}
        badges={data.earnedBadges}
        // streak={} — deferred to S09
      />
      <HouseholdAggregateCard
        totalTasks={data.householdTotalTasks}
        totalPoints={data.householdTotalPoints}
        // members={} — deferred to S09 (requires privacy)
      />
      {/* PrivacySettingsCard — deferred to S09 */}
    </div>
  );
}
```

#### Non-Toxic Copy
| Element | Text |
|---------|------|
| Page title | "Progress" |
| Points label | "Your Points" |
| Streak label | "Current Streak" |
| Best streak | "Personal Best" |
| Household section | "Household Team Progress" |
| Member breakdown title | "Team Contributions" |
| Empty state | "Complete tasks to start earning!" |
| Badge not earned | (grayed out, no "locked" or "failed") |

### Step 4: Sidebar Integration

```tsx
// Add to Sidebar.tsx navigation items
{
  name: 'Progress',
  href: `/households/${householdId}/progress`,
  icon: TrophyIcon,
}
```

---

## DB Migration Addition

**S08:** No additional migration needed for ST-904/ST-905.
Points/badges tables created in ST-901-902 workpack.

```sql
-- DEFERRED to S09 (ST-906):
-- CREATE TABLE gamification_settings (...)
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk

# Backend security tests
./services/backend/gradlew test --tests "*GamificationSecurity*"

# All gamification tests
./services/backend/gradlew test --tests "*Gamification*"

# Web build
cd services/web && npm run build

# Web lint
cd services/web && npm run lint

# Full test suite
./scripts/test.sh
```

---

## Component Mockup (SAFE S08)

```
+------------------------------------------+
| PROGRESS                                 |
+------------------------------------------+
| YOUR PROGRESS                            |
| +--------------------------------------+ |
| |  ⭐ 145 points                       | |
| |  This week: +35                      | |
| +--------------------------------------+ |
| | BADGES: [🏆] [⭐] [🔥] [○] [○]       | |
| +--------------------------------------+ |
|                                          |
| HOUSEHOLD TEAM PROGRESS                  |
| +--------------------------------------+ |
| |  Tasks completed this week: 23       | |
| |  Total household points: 420         | |
| +--------------------------------------+ |
+------------------------------------------+
```

**S08 vs S09:**
- S08: "Your Progress" (points + badges) + "Household Total" (aggregate only)
- S09: + Streaks + Individual breakdown + Privacy settings toggle

---

## Risks

| Risk | Mitigation |
|------|------------|
| Privacy toggle ignored | Security tests enforce |
| Cross-household leak | IDOR tests + membership checks |
| Non-toxic wording drift | Copy guidelines in code comments |
| Empty state confusion | Clear CTA messaging |

---

## Rollback

- Remove Progress route from sidebar
- Delete gamification_settings table
- Web shows "Coming soon" placeholder
- Backend returns 404 for settings endpoint

---

## DoD Checklist (S08)

- [ ] ST-904: Progress page renders
- [ ] ST-904: Personal card shows points + badges (no streak)
- [ ] ST-904: Household card shows **aggregate only** (no individual breakdown)
- [ ] ST-904: Empty state works
- [ ] ST-905: 5 security tests pass
- [ ] ST-905: 403 for non-members verified
- [ ] ST-905: No cross-household data leak
- [ ] ST-905: User sees only own progress details
- [ ] All tests pass
- [ ] Spotless applied (backend)
- [ ] Lint passes (web)

**Deferred to S09:**
- ST-906: Privacy toggle saves
- ST-906: Toggle hides from household view
- ST-906: Full opt-out disables gamification
- ST-904: Individual member breakdown
