# Sprint S12 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S12/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- OpenAPI: `docs/contracts/http/routines.openapi.yaml`
- ADR-013: `docs/adr/013-routine-scheduler-design.md`

---

## Committed Scope

### ST-1005: Routines Page (List + Create/Edit Form)
**Points:** 5
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-010/stories/ST-1005-routines-page.md`
**Workpack:** `docs/planning/workpacks/ST-1005/workpack.md`

**What's included:**
- Route: `/households/{householdId}/routines` -> `Routines.tsx`
- Sidebar navigation link "Routines" in `Sidebar.tsx`
- `useRoutines` hook for list fetch with filters
- `useRoutine` hook for single routine fetch
- Routines list component with:
  - Title, zone, frequency text, status badge, policy icon
  - Edit button, delete button
  - Empty state
  - Loading skeleton
  - Error state
- Create routine form modal/page:
  - Title input (required)
  - Description textarea (optional)
  - Zone selector dropdown
  - Frequency presets:
    - Daily (no extra config)
    - Weekly (day-of-week checkboxes)
    - Monthly (day-of-month selector 1-31)
    - Every N days (interval input)
  - Assignment policy:
    - Round-robin (default)
    - Fixed (user selector dropdown)
    - Manual (no assignment)
  - Cancel/Save buttons
- Edit routine form (same as create, pre-populated)
- Delete routine with confirmation dialog
- API functions in `api.ts`:
  - `getRoutines(householdId)`
  - `getRoutine(householdId, routineId)`
  - `createRoutine(householdId, data)`
  - `updateRoutine(householdId, routineId, data)`
  - `deleteRoutine(householdId, routineId)`
- TypeScript types in `api.ts`:
  - `Routine`, `RoutineStatus`, `RecurrenceRule`, `AssignmentPolicy`
  - `CreateRoutineRequest`, `UpdateRoutineRequest`
- Validation:
  - Title required
  - Weekly requires at least one day selected
  - Monthly day must be 1-31
  - Every N days interval >= 2
  - Fixed policy requires user selection

**What's NOT included:**
- Pause/resume buttons (ST-1006)
- Upcoming instances view (ST-1006)
- Custom RRULE input
- Drag-and-drop ordering
- Bulk operations

**Flags:**
- contract_impact: no (uses existing endpoints)
- security_sensitive: no

**DoR:** PASS
**Dependencies:** ST-1001 (Routine CRUD endpoints)

---

### ST-1006: Pause/Resume + Upcoming Instances View
**Points:** 3
**Priority:** P2
**Status:** Ready
**Story:** `docs/planning/epics/EP-010/stories/ST-1006-pause-resume-upcoming.md`
**Workpack:** `docs/planning/workpacks/ST-1006/workpack.md`

**What's included:**

**Backend:**
- `POST /households/{hid}/routines/{rid}/pause` endpoint
  - Changes status ACTIVE -> PAUSED
  - Sets pausedAt = now
  - Idempotent (already PAUSED -> 200 OK)
  - DELETED routine -> 400 error
- `POST /households/{hid}/routines/{rid}/resume` endpoint
  - Changes status PAUSED -> ACTIVE
  - Clears pausedAt
  - Idempotent (already ACTIVE -> 200 OK)
  - DELETED routine -> 400 error
- `GET /households/{hid}/routines/{rid}/upcoming?days=7` endpoint
  - Returns list of `UpcomingInstance`:
    - `scheduledDate`: date
    - `exists`: boolean (task already created)
    - `taskId`: UUID | null
    - `projectedAssignee`: UserSummary | null
  - Uses RecurrenceRuleParser to calculate dates
  - Checks existing tasks for each date
  - Projects round-robin assignment for future dates
  - PAUSED routine -> empty array
- OpenAPI contract update with new endpoints and schemas

**Frontend:**
- API functions in `api.ts`:
  - `pauseRoutine(householdId, routineId)`
  - `resumeRoutine(householdId, routineId)`
  - `getUpcomingInstances(householdId, routineId, days?)`
- TypeScript types:
  - `UpcomingInstance`
- Pause button in routine list row:
  - Visible for ACTIVE routines
  - Confirmation dialog: "Pause routine? No new tasks will be generated."
  - On confirm: call pauseRoutine, refresh list
- Resume button in routine list row:
  - Visible for PAUSED routines
  - Immediate action (no confirmation)
  - On click: call resumeRoutine, refresh list
- Routine detail view (or expandable row):
  - "Upcoming Tasks" section
  - Shows next 7 scheduled dates
  - For each date:
    - Date display
    - "Created" badge if task exists (link to task)
    - Projected assignee if not created
- Status badges:
  - ACTIVE: green/success
  - PAUSED: yellow/warning

**What's NOT included:**
- Delete pending instances on pause
- Vacation mode (bulk pause all)
- Calendar view
- Push notifications for reminders

**Flags:**
- contract_impact: yes (new endpoints: pause, resume, upcoming)
- security_sensitive: no

**DoR:** PASS
**Dependencies:** ST-1003 (Scheduler), ST-1005 (Routines UI base)

---

## Out of Scope (Explicit)

### Post-Epic (LATER)
- Calendar view of upcoming instances
- Vacation mode (bulk pause all routines)
- Custom RRULE input (advanced)
- Drag-and-drop ordering of routines
- Bulk operations (select multiple, pause all)
- Delete pending instances on pause
- Push notifications for routine reminders
- Per-user timezone

### Never in EP-010 Scope
- Sub-tasks/checklists in routine templates
- Auto-assign based on availability/calendar
- External calendar sync (Google/Apple)
- Complex RRULE (exceptions, BYSETPOS, end date)

---

## Acceptance Criteria Summary

**Sprint succeeds if:**
1. Routines page accessible at `/households/{id}/routines`
2. Can create routine with any frequency preset
3. Can edit existing routine
4. Can delete routine with confirmation
5. Sidebar shows "Routines" navigation link
6. Pause/resume buttons work in UI
7. Upcoming instances display for ACTIVE routines
8. Status badges show correctly (ACTIVE/PAUSED)
9. Form validation works (title required, policy constraints)
10. `./gradlew build` passes
11. `npm run build` passes
12. OpenAPI contract matches implementation

**Sprint fails if:**
- Cannot access routines page
- Cannot create routine via form
- Pause/resume doesn't change status
- Upcoming instances not displayed
- Sidebar link missing

---

## Readiness Notes

**All committed stories:**
- Have clear ACs with Given/When/Then format
- Have UI mockups in story spec
- Dependencies are sequential and achievable
- Web client patterns established in existing pages

**Key patterns to follow:**
- Route: `clients/web/src/routes/Routines.tsx`
- Hook: `clients/web/src/hooks/useRoutines.ts`
- List: similar to `TasksList.tsx`
- Form: similar to `CreateZoneModal.tsx` / `InviteModal.tsx`
- API: follow `api.ts` patterns

**Human gates:**
- Gate B: approve committed scope (this sprint)
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)

---

## Story Dependency Graph

```
ST-1005 (Routines Page - List + Create/Edit)
    |
    +-- ST-1006 (Pause/Resume + Upcoming Instances)
```

**Critical path:** ST-1005 -> ST-1006

---

## Technical Notes

### New Web Client Files (ST-1005)

```
clients/web/src/
├── routes/
│   ├── index.tsx          # Add routines route
│   ├── Routines.tsx       # NEW: Routines list page
│   └── RoutineDetail.tsx  # OPTIONAL: detail view (or inline)
├── hooks/
│   ├── useRoutines.ts     # NEW: list fetch
│   └── useRoutine.ts      # NEW: single routine fetch
├── components/
│   └── routines/
│       ├── RoutineRow.tsx       # NEW: list row
│       ├── RoutineForm.tsx      # NEW: create/edit form
│       ├── RoutineStatusBadge.tsx # NEW: status badge
│       ├── FrequencyPicker.tsx  # NEW: frequency selector
│       ├── PolicyPicker.tsx     # NEW: assignment policy selector
│       └── index.ts             # NEW: exports
├── types/
│   └── api.ts             # Add Routine types
└── lib/
    └── api.ts             # Add routine API functions
```

### Backend Endpoints (ST-1006)

```yaml
POST /api/v1/households/{householdId}/routines/{routineId}/pause
  Request: empty body
  Response: 200 Routine (updated with status=PAUSED)
  Errors: 400 (DELETED routine), 403 (not member), 404 (not found)

POST /api/v1/households/{householdId}/routines/{routineId}/resume
  Request: empty body
  Response: 200 Routine (updated with status=ACTIVE)
  Errors: 400 (DELETED routine), 403 (not member), 404 (not found)

GET /api/v1/households/{householdId}/routines/{routineId}/upcoming
  Query: days (int, default 7, max 30)
  Response: 200 UpcomingInstance[]
  Errors: 403 (not member), 404 (not found)
```

### Type Definitions (to add to api.ts)

```typescript
export type RoutineStatus = 'ACTIVE' | 'PAUSED' | 'DELETED';
export type AssignmentPolicy = 'FIXED' | 'ROUND_ROBIN' | 'MANUAL';

export interface RecurrenceRule {
  type: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'EVERY_N_DAYS';
  daysOfWeek?: string[];  // for WEEKLY: ['MONDAY', 'WEDNESDAY', 'FRIDAY']
  dayOfMonth?: number;    // for MONTHLY: 1-31
  interval?: number;      // for EVERY_N_DAYS: >= 2
}

export interface Routine {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  zoneId?: string;
  zone?: Zone;
  recurrenceRule: RecurrenceRule;
  assignmentPolicy: AssignmentPolicy;
  fixedAssigneeId?: string;
  fixedAssignee?: UserSummary;
  status: RoutineStatus;
  generationWindowDays: number;
  createdBy: UserSummary;
  createdAt: string;
  updatedAt: string;
  pausedAt?: string;
}

export interface CreateRoutineRequest {
  title: string;
  description?: string;
  zoneId?: string;
  recurrenceRule: RecurrenceRule;
  assignmentPolicy: AssignmentPolicy;
  fixedAssigneeId?: string;
}

export interface UpdateRoutineRequest {
  title?: string;
  description?: string;
  zoneId?: string;
  recurrenceRule?: RecurrenceRule;
  assignmentPolicy?: AssignmentPolicy;
  fixedAssigneeId?: string;
}

export interface UpcomingInstance {
  scheduledDate: string;
  exists: boolean;
  taskId?: string;
  projectedAssignee?: UserSummary;
}
```
