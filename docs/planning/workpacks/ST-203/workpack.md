# ST-203 — Tasks List & Filters

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q1-web-client.md`
- Epic: `docs/planning/epics/EP-003/epic.md`
- Story: `docs/planning/epics/EP-003/stories/ST-203-tasks-list.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (Task, listTasks, Zone, HouseholdMember)
- API Coverage: `docs/mvp/api-coverage.md` (GET /households/{id}/tasks, zones, members)
- ST-201 + ST-202 Baseline: `clients/web/` (completed)

## Baseline from ST-201 + ST-202 (Actual State)

**Available infrastructure:**
- `src/types/api.ts`: UserProfile, HouseholdSummary, AuthErrorResponse (to extend)
- `src/lib/api.ts`: apiFetch<T> with auth header, getMe() (to extend with getTasks, getZones, getMembers)
- `src/lib/errors.ts`: AuthError, ApiError
- `src/context/AuthContext.tsx`: useAuth() → { householdId, user, status }
- `src/hooks/useAuth.ts`: access auth context
- `src/routes/TasksList.tsx`: placeholder (to replace with full implementation)
- `src/components/ProtectedRoute.tsx`: route guard (already wrapping /households/:householdId/tasks)

**Dependency versions (actual):**
- react@18.2.0, react-router-dom@6.16.0
- typescript@5.4.5

**IMPORTANT:** Extend existing types/api.ts, do NOT rewrite from scratch.

## Outcome
User can view tasks in their household with filters:
- Filter by status (open, in_progress, done, cancelled)
- Filter by assignee (household members)
- Filter by zone (household zones)
- See task details: title, status, assignee, zone, deadline

## Acceptance Criteria
- [ ] AC1: Tasks list loads on /households/:householdId/tasks
- [ ] AC2: Task item shows title, status badge, assignee, zone, deadline
- [ ] AC3: Status filter works (All, Open, In Progress, Done, Cancelled)
- [ ] AC4: Assignee filter works (All + household members)
- [ ] AC5: Zone filter works (All + household zones)
- [ ] AC6: Combined filters work
- [ ] AC7: Empty state shown when no tasks match
- [ ] AC8: Loading state shown during fetch
- [ ] AC9: Error handling (error message + retry, 403 access denied)
- [ ] AC10: Task row click navigates to detail (placeholder OK)

## Non-goals (explicit)
- No task creation/editing (NEXT increment)
- No task detail view content (ST-204)
- No pagination (defer until needed)
- No sorting (defer until needed)

## Files to create/modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/types/api.ts` | Modify | Add Task, Zone, HouseholdMember types |
| `clients/web/src/lib/api.ts` | Modify | Add getTasks, getZones, getMembers |
| `clients/web/src/hooks/useTasks.ts` | Create | Fetch tasks with filters |
| `clients/web/src/hooks/useZones.ts` | Create | Fetch zones for filter |
| `clients/web/src/hooks/useMembers.ts` | Create | Fetch members for filter |
| `clients/web/src/routes/TasksList.tsx` | Modify | Full implementation |
| `clients/web/src/components/tasks/TasksTable.tsx` | Create | Table layout |
| `clients/web/src/components/tasks/TaskRow.tsx` | Create | Single task row |
| `clients/web/src/components/tasks/TaskFilters.tsx` | Create | Filter dropdowns |
| `clients/web/src/components/tasks/TaskStatusBadge.tsx` | Create | Status badge |
| `clients/web/src/components/tasks/EmptyTasks.tsx` | Create | Empty state |
| `clients/web/src/components/ui/Select.tsx` | Create | Filter dropdown component |
| `clients/web/src/components/ui/Spinner.tsx` | Create | Loading indicator |
| `clients/web/src/components/ui/ErrorMessage.tsx` | Create | Error display |

## Implementation Plan

### Commit 1 — Types and API methods
Steps:
1. Add Task, Zone, HouseholdMember, UserSummary types
2. Add getTasks(householdId, filters), getZones(householdId), getMembers(householdId) to api.ts
3. Define TaskFilters type

Files:
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/api.ts`

Verification:
- TypeScript compiles

### Commit 2 — Custom hooks
Steps:
1. Create useTasks hook with filter state and refetch
2. Create useZones hook (cached fetch)
3. Create useMembers hook (cached fetch)

Files:
- `clients/web/src/hooks/useTasks.ts`
- `clients/web/src/hooks/useZones.ts`
- `clients/web/src/hooks/useMembers.ts`

Verification:
- Hooks compile without errors

### Commit 3 — UI components
Steps:
1. Create Select dropdown component
2. Create Spinner component
3. Create ErrorMessage component
4. Create TaskStatusBadge component
5. Create EmptyTasks component

Files:
- `clients/web/src/components/ui/Select.tsx`
- `clients/web/src/components/ui/Spinner.tsx`
- `clients/web/src/components/ui/ErrorMessage.tsx`
- `clients/web/src/components/tasks/TaskStatusBadge.tsx`
- `clients/web/src/components/tasks/EmptyTasks.tsx`

Verification:
- Components compile

### Commit 4 — Tasks table and filters
Steps:
1. Create TaskFilters component with status/assignee/zone dropdowns
2. Create TaskRow component
3. Create TasksTable component
4. Wire filters to URL search params

Files:
- `clients/web/src/components/tasks/TaskFilters.tsx`
- `clients/web/src/components/tasks/TaskRow.tsx`
- `clients/web/src/components/tasks/TasksTable.tsx`

Verification:
- Components compile and render

### Commit 5 — TasksList page integration
Steps:
1. Update TasksList.tsx to use all components
2. Handle loading/error/empty states
3. Navigate to task detail on row click
4. Sync filters with URL params

Files:
- `clients/web/src/routes/TasksList.tsx`

Verification:
- `npm run build` passes
- `npm run lint` passes
- Manual test: filters work, tasks display

## Contract Reference (OpenAPI)

### GET /households/{id}/tasks
Query params: `status`, `assigneeId`, `zoneId`

### Task type
```typescript
interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  status: 'open' | 'in_progress' | 'done' | 'cancelled';
  assignee?: UserSummary;
  zone?: Zone;
  deadline?: string;
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

interface HouseholdMember {
  userId: string;
  displayName: string;
  email: string;
  role: 'admin' | 'member';
  joinedAt: string;
}
```

## Contract Impact
None — consuming existing API endpoints.

## Docs Updates
None required.

## Tests
- [ ] Unit: TasksTable renders tasks
- [ ] Unit: TaskFilters updates params
- [ ] Unit: EmptyTasks renders when no tasks
- [ ] Unit: ErrorMessage renders on error
- [ ] Integration: Mock API → verify list renders

## Verification Commands
```bash
cd clients/web
npm run lint              # → passes
npm run build             # → passes
npm run dev               # → manual testing

# Manual test flow:
# 1. Login → select household → navigate to tasks
# 2. Tasks load from API
# 3. Select status filter → list updates
# 4. Select assignee filter → list updates
# 5. Select zone filter → list updates
# 6. Combine filters → list updates
# 7. Clear filters → all tasks shown
# 8. No matching tasks → empty state
# 9. Click task row → navigate to detail (placeholder)
```

## DoD Checklist
- [ ] All 5 commits complete
- [ ] Tasks list loads from API
- [ ] All 3 filters work (status, assignee, zone)
- [ ] Combined filters work
- [ ] Empty state shown
- [ ] Loading state shown
- [ ] Error handling works
- [ ] npm run lint/build pass

## Risks
| Risk | Mitigation |
|------|------------|
| API returns many tasks | Defer pagination |
| Filter UX confusion | Keep simple dropdowns |

## Rollback
- Revert commits in reverse order
- No backend changes
