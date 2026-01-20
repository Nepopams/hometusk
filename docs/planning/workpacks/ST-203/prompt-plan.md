# ST-203 PLAN Prompt

**Mode:** PLAN ONLY — NO EDITS, NO COMMANDS

---

## Context

You are implementing ST-203: Tasks List & Filters for the HomeTusk web client.

**Read these files first (mandatory):**
- `docs/planning/workpacks/ST-203/workpack.md` — implementation plan
- `docs/planning/epics/EP-003/stories/ST-203-tasks-list.md` — story spec
- `docs/contracts/http/commands.openapi.yaml` — API contract (Task, Zone, HouseholdMember, listTasks)

**Prerequisite:**
- ST-201 completed (web foundation)
- ST-202 completed (auth integration)

**ST-202 Baseline (Actual State):**
```
clients/web/src/
├── types/api.ts          (UserProfile, HouseholdSummary, AuthErrorResponse)
├── lib/
│   ├── api.ts            (apiFetch with auth, getMe)
│   └── errors.ts         (AuthError, ApiError)
├── context/AuthContext.tsx (status, user, token, householdId, login/logout/selectHousehold)
├── hooks/useAuth.ts      (hook to access auth context)
├── routes/
│   ├── index.tsx         (router with /households/:householdId/*)
│   ├── Login.tsx         (dev token paste)
│   ├── HouseholdSelector.tsx (selector page)
│   ├── TasksList.tsx     (placeholder - to implement)
│   └── ...
└── components/
    ├── ProtectedRoute.tsx (requireHousehold prop)
    └── HouseholdCard.tsx
```

**Available for ST-203:**
- `useAuth()` → `{ householdId, user }` (for current household context)
- `apiFetch<T>(path, options)` → Promise<T> (with auth header)
- `ProtectedRoute` → already wrapping /households/:householdId routes

---

## Your Task

Create a detailed implementation plan for tasks list with filters.

**Output format:** Markdown plan with:
1. Types to add (from OpenAPI)
2. API methods to add
3. Custom hooks design
4. Component hierarchy
5. Filter state management
6. URL param sync
7. Verification steps

---

## Constraints (CRITICAL)

1. **NO FILE EDITS** — This is a plan-only prompt
2. **NO COMMAND EXECUTION** — Do not run npm, etc.
3. **Work within:** `clients/web/` (exists from ST-201/ST-202)
4. **API endpoints:**
   - GET /households/{id}/tasks
   - GET /households/{id}/zones
   - GET /households/{id}/members

---

## Acceptance Criteria to Plan For

- AC1: Tasks list loads on /households/:householdId/tasks
- AC2: Task shows title, status, assignee, zone, deadline
- AC3: Status filter (All/Open/In Progress/Done/Cancelled)
- AC4: Assignee filter (All + members)
- AC5: Zone filter (All + zones)
- AC6: Combined filters work
- AC7: Empty state
- AC8: Loading state
- AC9: Error handling
- AC10: Row click navigates to detail

---

## Expected Plan Structure

```markdown
# ST-203 Implementation Plan

## 1. Types (from OpenAPI)
interface Task { ... }
interface Zone { ... }
interface HouseholdMember { ... }
interface TaskFilters { ... }

## 2. API Methods
api.getTasks(householdId, filters)
api.getZones(householdId)
api.getMembers(householdId)

## 3. Custom Hooks
useTasks(householdId, filters)
useZones(householdId)
useMembers(householdId)

## 4. Component Hierarchy
TasksList
├── TaskFilters
│   ├── Select (status)
│   ├── Select (assignee)
│   └── Select (zone)
├── TasksTable / Spinner / EmptyTasks / ErrorMessage
│   └── TaskRow (multiple)
│       └── TaskStatusBadge

## 5. Filter State Management
[URL params vs local state]

## 6. Loading/Error States
[how handled]

## 7. Verification
[manual test steps]
```

---

## STOP-THE-LINE

If you encounter:
- Missing types in OpenAPI
- Unclear filter behavior
- Need for pagination

**STOP and ask** — do not assume.
