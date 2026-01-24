# Story: ST-904 — Gamification UI (Progress Page)

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — SAFE S08 Scope (no streaks, no individual breakdown)

## User Value
> "Открываю Progress — вижу свои очки, бейджи, серию. Вижу household прогресс (кто сколько сделал, без стыда)."

---

## Description
Implement Progress page in web app (SAFE S08 scope):
- Personal progress card: points, badges (no streak in S08)
- Household progress: **aggregate only** (no individual breakdown until ST-906 privacy exists)
- Non-toxic wording throughout
- Navigation link in sidebar

**S08 Constraints:**
- No streak display (ST-903 deferred)
- No individual member breakdown (privacy toggle required first)
- No "hide my progress" toggle (ST-906 deferred)

---

## Acceptance Criteria

### AC-1: Progress page accessible
```
Given authenticated user in household
When clicking "Progress" in sidebar
Then navigates to /households/{id}/progress
And page loads with gamification data
```

### AC-2: Personal progress card (S08)
```
When page loads
Then shows:
  - Total points (large number)
  - Points this week
  - Earned badges (icons + names)
Note: Streak display deferred to S09 (ST-903)
```

### AC-3: Household progress section (S08 — aggregate only)
```
When page loads
Then shows:
  - Household total tasks this week
  - Household total points
  - NO individual member breakdown (deferred until ST-906 privacy exists)
  - Non-toxic wording: "Household Team Progress"
```

### AC-4: No individual breakdown in S08
```
Given S08 scope
When household progress section loads
Then shows ONLY:
  - Total tasks completed by household
  - Total points earned by household
And does NOT show:
  - Individual member names
  - Individual member points
  - Progress bars per member
Note: Individual breakdown requires ST-906 (privacy toggle) — deferred to S09
```

### AC-5: Non-toxic wording
```
Then UI uses:
  - "Keep it up!" not "Don't lose your streak!"
  - "Team progress" not "Leaderboard"
  - Progress bars, not rank numbers
  - Encouraging language only
```

### AC-6: Empty state
```
Given new user with 0 points
When page loads
Then shows "Start completing tasks to earn points!"
And empty badges section shows "Badges you can earn"
```

### AC-7: Loading and error states
```
Given slow network
When loading Progress page
Then shows spinner
Given API error
Then shows error message with retry
```

---

## UI Components

### New Components
| Component | Purpose |
|-----------|---------|
| `ProgressPage.tsx` | Main page route |
| `PersonalProgressCard.tsx` | Points, streak, badges |
| `HouseholdProgressCard.tsx` | Aggregate + breakdown |
| `BadgeGrid.tsx` | Display earned badges |
| `StreakDisplay.tsx` | Current/best streak |
| `useGamification.ts` | Data fetching hook |

### Sidebar Addition
- Add "Progress" nav link after "Analytics"

---

## Non-Toxic Copy Guidelines

| Element | Text |
|---------|------|
| Page title | "Progress" |
| Points label | "Your Points" |
| Streak label | "Current Streak" |
| Best streak | "Personal Best" |
| Household section | "Household Team Progress" |
| Member breakdown title | "Team Contributions" |
| No activity message | "Complete tasks to start earning!" |
| Badge not earned | (grayed out, no "locked" or "failed") |

---

## Mockup (Text-based) — S08 SAFE Scope

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
| |  (Individual breakdown in future     | |
| |   update with privacy controls)      | |
| +--------------------------------------+ |
+------------------------------------------+
```

**S08 vs S09:**
- S08: "Your Progress" + "Household Total" (aggregate)
- S09: + Streaks + Individual breakdown + Privacy toggle

---

## Contract Impact
**No** — Uses existing endpoints from ST-901, ST-902, ST-903

---

## Test Notes

### Manual Testing
- Navigate to Progress
- Verify points display
- Verify badge display
- Verify streak (if enabled)
- Verify household view
- Test privacy toggle effect

---

## Points
**5 points**

---

## Flags
- contract_impact: no (uses existing APIs)
