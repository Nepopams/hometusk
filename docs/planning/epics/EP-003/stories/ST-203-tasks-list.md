# Story: Tasks List & Filters

**ID:** ST-203
**Epic:** EP-003 (Web Foundation)
**Points:** 3
**Status:** Ready
**Priority:** P1
**Depends on:** ST-202

---

## Title

Display tasks list with status/assignee/zone filters

---

## Description

As a household member, I want to see tasks in my household with filters, so that I can understand what needs to be done.

**Context:**
This story implements the core "tasks list" view — the primary value delivery for the NOW increment. Users will see tasks filtered by status, assignee, and zone.

---

## Acceptance Criteria

### AC1: Tasks list loads
```
Given I am authenticated and household is selected
When I navigate to /households/:householdId/tasks
Then GET /api/v1/households/{id}/tasks is called
And tasks are displayed in a list/table
```

### AC2: Task item display
```
Given tasks are loaded
When I view the list
Then each task shows:
  - Title
  - Status (badge: open/in_progress/done/cancelled)
  - Assignee (display name or "Unassigned")
  - Zone (name or "No zone")
  - Deadline (formatted date or "No deadline")
  - Created date
```

### AC3: Status filter
```
Given tasks are displayed
When I select status filter dropdown
Then options are: All, Open, In Progress, Done, Cancelled

When I select "Open"
Then GET /api/v1/households/{id}/tasks?status=open is called
And only open tasks are displayed
```

### AC4: Assignee filter
```
Given tasks are displayed
When I select assignee filter dropdown
Then options are: All + list of household members

When I select a member
Then GET /api/v1/households/{id}/tasks?assigneeId={id} is called
And only tasks assigned to that member are displayed
```

### AC5: Zone filter
```
Given tasks are displayed
When I select zone filter dropdown
Then options are: All + list of household zones

When I select a zone
Then GET /api/v1/households/{id}/tasks?zoneId={id} is called
And only tasks in that zone are displayed
```

### AC6: Combined filters
```
Given filters are available
When I select status=open AND zone={kitchenId}
Then GET /api/v1/households/{id}/tasks?status=open&zoneId={kitchenId} is called
And only matching tasks are displayed
```

### AC7: Empty state
```
Given no tasks match filters
When I view the list
Then I see "No tasks found" message
And suggestion to adjust filters or create a task
```

### AC8: Loading state
```
Given tasks are loading
When I view the list
Then I see a loading indicator (spinner/skeleton)
```

### AC9: Error handling
```
Given API returns error
When I view the list
Then I see error message with retry button

Given API returns 403 (not a member)
Then I see "Access denied" message
And option to go back to household selector
```

### AC10: Task row click
```
Given tasks are displayed
When I click a task row
Then I navigate to /households/:householdId/tasks/:taskId
(Task detail view — implemented in ST-204, placeholder OK for now)
```

---

## Test Strategy

**Manual verification:**
- Login → select household → see tasks list
- Apply each filter → verify API call and display
- Combine filters → verify correct results
- Empty state → verify message
- Error state → verify error display

**Unit tests:**
- TasksList component renders tasks
- Filter dropdowns update query params
- Empty state renders when tasks=[]
- Error state renders on API error

**Integration tests:**
- Mock tasks API → verify list renders
- Mock filters → verify query params

---

## Technical Notes

**API endpoints used:**
- `GET /api/v1/households/{id}/tasks` — list tasks
- `GET /api/v1/households/{id}/tasks?status={status}` — filter by status
- `GET /api/v1/households/{id}/tasks?assigneeId={id}` — filter by assignee
- `GET /api/v1/households/{id}/tasks?zoneId={id}` — filter by zone
- `GET /api/v1/households/{id}/members` — for assignee filter options
- `GET /api/v1/households/{id}/zones` — for zone filter options

**Task type (from OpenAPI):**
```typescript
interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  status: 'open' | 'in_progress' | 'done' | 'cancelled';
  assignee?: UserSummary;
  zone?: Zone;
  deadline?: string; // ISO date-time
  createdBy: UserSummary;
  commandId?: string;
  createdVia: 'command' | 'fallback' | 'direct';
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

interface UserSummary {
  id: string;
  displayName: string;
}

interface Zone {
  id: string;
  name: string;
  householdId: string;
  createdAt: string;
}
```

**State management:**
```typescript
// URL-based filters (react-router)
const [searchParams, setSearchParams] = useSearchParams();
const status = searchParams.get('status');
const assigneeId = searchParams.get('assigneeId');
const zoneId = searchParams.get('zoneId');

// Or local state + sync to URL
```

**Files to create/modify:**
```
src/
├── routes/
│   └── TasksList.tsx (update from placeholder)
├── components/
│   ├── tasks/
│   │   ├── TasksTable.tsx
│   │   ├── TaskRow.tsx
│   │   ├── TaskFilters.tsx
│   │   ├── TaskStatusBadge.tsx
│   │   └── EmptyTasks.tsx
│   └── ui/
│       ├── Select.tsx (filter dropdown)
│       ├── Spinner.tsx
│       └── ErrorMessage.tsx
├── hooks/
│   ├── useTasks.ts (fetch tasks with filters)
│   ├── useMembers.ts (fetch household members)
│   └── useZones.ts (fetch household zones)
└── lib/
    └── api.ts (add getTasks, getMembers, getZones)
```

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Epic | `docs/planning/epics/EP-003/epic.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` (Task, listTasks) |
| API Coverage | `docs/mvp/api-coverage.md` (tasks endpoints) |

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consuming existing API |
| adr_needed | no | Standard list/filter pattern |
| diagrams_needed | no | — |

---

## Definition of Ready Checklist

- [x] Title clear
- [x] AC testable
- [x] Deliverables defined
- [x] Test strategy defined
- [x] Dependencies identified (ST-202)
- [x] No blockers
