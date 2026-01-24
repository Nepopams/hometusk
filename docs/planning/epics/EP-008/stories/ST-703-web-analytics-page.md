# Story: ST-703 — Web Analytics Page

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Part of Sprint S07 committed scope

## User Value
> "Хочу открыть страницу Analytics и сразу увидеть: кто сколько сделал, где проблемы, насколько сбалансировано — без экспорта в Excel."

---

## Description
Create web analytics page with:
- Member contribution breakdown (list view)
- Zone breakdown
- Overdue tasks highlight
- Balance score with "how calculated" expandable
- Link from household navigation
- Non-toxic wording throughout

---

## Acceptance Criteria

### AC-1: Page accessible
```
Given authenticated user in household
When navigating to /households/{id}/analytics
Then analytics page loads
And shows loading state while fetching
```

### AC-2: Member breakdown displayed
```
Given perMember data loaded
Then each member shown with:
  - Name
  - Completed count
  - Open count
  - Overdue count (highlighted if > 0)
```

### AC-3: Zone breakdown displayed
```
Given perZone data loaded
Then each zone shown with:
  - Zone name
  - Completed count
  - Overdue count
```

### AC-4: Overdue section
```
Given overdueTop data
Then section shows top overdue tasks:
  - Task title
  - Assignee name
  - Days overdue
```

### AC-5: Balance score displayed
```
Given fairness data loaded
Then shows:
  - Balance value (e.g., "75")
  - Visual indicator (color based on score)
  - Interpretation text
  - "How is this calculated?" expandable with formula
```

### AC-6: Balance null handled
```
Given balance = null (no tasks)
Then shows "N/A"
And interpretation explains "no tasks in period"
```

### AC-7: Navigation link
```
Given household context
When in household view
Then "Analytics" link visible in navigation
```

### AC-8: Non-toxic wording
```
When displaying any text
Then no "winner/loser" language
And no blame-focused phrasing
And uses "balance" not "score" for emphasis
```

### AC-9: Error handling
```
Given API error
Then shows user-friendly error message
And retry option
```

---

## UI Components

```
clients/web/src/pages/AnalyticsPage.tsx
clients/web/src/components/analytics/
  ├── MemberStatsList.tsx
  ├── ZoneStatsList.tsx
  ├── OverdueTasksList.tsx
  ├── BalanceScoreCard.tsx
  └── PeriodToggle.tsx (for ST-704)
clients/web/src/hooks/
  └── useAnalytics.ts
clients/web/src/api/
  └── analytics.ts
```

---

## Wire Layout (Simple)

```
┌─────────────────────────────────────────┐
│ Analytics          [7 days ▾]           │
├─────────────────────────────────────────┤
│ ┌─────────────────┐ ┌─────────────────┐ │
│ │ Balance Score   │ │ Overdue Tasks   │ │
│ │     75          │ │ • Task A (3d)   │ │
│ │ Good balance    │ │ • Task B (1d)   │ │
│ │ [How calculated]│ │                 │ │
│ └─────────────────┘ └─────────────────┘ │
├─────────────────────────────────────────┤
│ Members                                 │
│ ┌─────────────────────────────────────┐ │
│ │ Alice    ✓8  ○2  ⚠1                │ │
│ │ Bob      ✓6  ○1  ⚠0                │ │
│ │ Carol    ✓2  ○3  ⚠2                │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ Zones                                   │
│ ┌─────────────────────────────────────┐ │
│ │ Kitchen   ✓10  ⚠1                  │ │
│ │ Bathroom  ✓4   ⚠2                  │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘

Legend: ✓=completed, ○=open, ⚠=overdue
```

---

## Contract Dependency
- `GET /households/{id}/analytics?period=7d`
- Response: AnalyticsSummary

---

## Test Notes

### Component Tests
- AnalyticsPage renders with mock data
- MemberStatsList displays all members
- BalanceScoreCard shows score and expandable
- BalanceScoreCard handles null (N/A)
- Error state renders correctly

### E2E Tests (optional)
- Navigate to analytics → data displayed

---

## Points
**5 points**

---

## Flags
- contract_impact: no (consumes existing)
- diagrams_needed: no
