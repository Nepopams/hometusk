# Story: ST-701 — Analytics Summary Endpoint

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Part of Sprint S07 committed scope

## User Value
> "Хочу через API получить разбивку по задачам: кто сколько сделал, какие зоны активны, где просрочки — чтобы UI мог это отобразить."

---

## Description
Create backend endpoint for analytics summary:
- Completed tasks by member for period
- Open/overdue tasks by member
- Completed/overdue tasks by zone
- Top overdue tasks list

All queries scoped to household with membership check.

---

## Acceptance Criteria

### AC-1: Endpoint exists and returns data
```
Given authenticated user who is household member
When GET /api/v1/households/{householdId}/analytics?period=7d
Then response 200 with AnalyticsSummary schema
And data reflects last 7 days
```

### AC-2: Period parameter works
```
Given period=30d
When requesting analytics
Then periodStart = now - 30 days
And periodEnd = now
And data reflects 30-day window
```

### AC-3: Default period is 7d
```
Given no period parameter
When requesting analytics
Then period=7d used by default
```

### AC-4: perMember stats correct
```
Given household with members A, B, C
And A completed 5 tasks, B completed 3 tasks, C completed 0 tasks in period
When requesting analytics
Then perMember contains:
  - A: completedCount=5
  - B: completedCount=3
  - C: completedCount=0
And each has overdueCount and openCount
```

### AC-5: perZone stats correct
```
Given zones Kitchen, Bathroom
And Kitchen has 8 completed, 2 overdue
And Bathroom has 3 completed, 0 overdue
When requesting analytics
Then perZone contains correct counts
```

### AC-6: overdueTop limited to 5
```
Given 10 overdue tasks
When requesting analytics
Then overdueTop contains max 5 items
And sorted by daysOverdue descending
```

### AC-7: Household boundary enforced
```
Given user NOT member of householdId
When requesting analytics
Then response 403 Forbidden
```

### AC-8: No cross-household data
```
Given two households H1, H2
When requesting analytics for H1
Then only H1 tasks included
```

---

## Data Sources

| Table | Fields Used | Index Needed |
|-------|-------------|--------------|
| `tasks` | household_id, assignee_id, zone_id, status, completed_at, deadline | idx_tasks_household_completed |
| `users` | id, display_name | — |
| `zones` | id, name | — |

---

## Contract Impact
**Yes** — New endpoint in OpenAPI:
- `GET /households/{householdId}/analytics`

See epic.md for full schema.

---

## Test Notes

### Unit Tests
- AnalyticsService.getAnalytics() with mocked repository
- Period calculation logic
- Edge cases: no tasks, single member, no zones

### Integration Tests
- AnalyticsControllerIntegrationTest:
  - `getAnalytics_asMember_returnsData`
  - `getAnalytics_notMember_returns403`
  - `getAnalytics_periodFilter_filtersCorrectly`
  - `getAnalytics_crossHousehold_noLeaks`
  - `getAnalytics_overdueTop_limitedTo5`

---

## Points
**3 points**

---

## Flags
- contract_impact: yes
- security_sensitive: yes
