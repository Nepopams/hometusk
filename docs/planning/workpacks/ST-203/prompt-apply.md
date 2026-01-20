# ST-203 APPLY Prompt

**Mode:** IMPLEMENTATION — Execute approved plan

---

## Context

You are implementing ST-203: Tasks List & Filters for the HomeTusk web client.

**Read these files (mandatory):**
- `docs/planning/workpacks/ST-203/workpack.md` — implementation plan
- `docs/planning/epics/EP-003/stories/ST-203-tasks-list.md` — story spec
- `docs/contracts/http/commands.openapi.yaml` — API contract
- Your approved PLAN output (if available)

**Prerequisite files (verify exist):**
- `clients/web/src/types/api.ts` (from ST-202)
- `clients/web/src/lib/api.ts` (from ST-202)
- `clients/web/src/hooks/useAuth.ts` (from ST-202)
- `clients/web/src/routes/TasksList.tsx` (placeholder from ST-201)

---

## Your Task

Implement tasks list with filters according to the workpack.

**Deliverables:**
1. Types for Task, Zone, HouseholdMember
2. API methods for tasks, zones, members
3. Custom hooks
4. UI components (table, filters, badges)
5. Integrated TasksList page

---

## Allowed Operations

### Files to create
- `clients/web/src/hooks/useTasks.ts`
- `clients/web/src/hooks/useZones.ts`
- `clients/web/src/hooks/useMembers.ts`
- `clients/web/src/components/tasks/TasksTable.tsx`
- `clients/web/src/components/tasks/TaskRow.tsx`
- `clients/web/src/components/tasks/TaskFilters.tsx`
- `clients/web/src/components/tasks/TaskStatusBadge.tsx`
- `clients/web/src/components/tasks/EmptyTasks.tsx`
- `clients/web/src/components/ui/Select.tsx`
- `clients/web/src/components/ui/Spinner.tsx`
- `clients/web/src/components/ui/ErrorMessage.tsx`

### Files to modify
- `clients/web/src/types/api.ts`
- `clients/web/src/lib/api.ts`
- `clients/web/src/routes/TasksList.tsx`

### Commands allowed
- `npm run lint`
- `npm run build`
- `npm run dev`

### Forbidden
- **DO NOT** add pagination (defer)
- **DO NOT** add sorting (defer)
- **DO NOT** modify backend code
- **DO NOT** implement task detail (ST-204)

---

## Key Implementations

### 1. Types (add to src/types/api.ts)
```typescript
export interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  status: TaskStatus;
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

export type TaskStatus = 'open' | 'in_progress' | 'done' | 'cancelled';

export interface UserSummary {
  id: string;
  displayName: string;
}

export interface Zone {
  id: string;
  name: string;
  householdId: string;
  createdAt: string;
}

export interface HouseholdMember {
  userId: string;
  displayName: string;
  email: string;
  role: 'admin' | 'member';
  joinedAt: string;
}

export interface TaskFilters {
  status?: TaskStatus;
  assigneeId?: string;
  zoneId?: string;
}
```

### 2. API Methods (add to src/lib/api.ts)
```typescript
import type { Task, Zone, HouseholdMember, TaskFilters } from '../types/api';

export const api = {
  // ... existing methods

  getTasks: (householdId: string, filters?: TaskFilters) => {
    const params = new URLSearchParams();
    if (filters?.status) params.set('status', filters.status);
    if (filters?.assigneeId) params.set('assigneeId', filters.assigneeId);
    if (filters?.zoneId) params.set('zoneId', filters.zoneId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return apiFetch<Task[]>(`/households/${householdId}/tasks${query}`);
  },

  getZones: (householdId: string) =>
    apiFetch<Zone[]>(`/households/${householdId}/zones`),

  getMembers: (householdId: string) =>
    apiFetch<HouseholdMember[]>(`/households/${householdId}/members`),
};
```

### 3. useTasks Hook (src/hooks/useTasks.ts)
```typescript
import { useState, useEffect, useCallback } from 'react';
import { api } from '../lib/api';
import type { Task, TaskFilters } from '../types/api';

export function useTasks(householdId: string, filters: TaskFilters) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetch = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.getTasks(householdId, filters);
      setTasks(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load tasks'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId, filters.status, filters.assigneeId, filters.zoneId]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { tasks, isLoading, error, refetch: fetch };
}
```

### 4. Filter State from URL
```typescript
// In TasksList.tsx
import { useSearchParams } from 'react-router-dom';

function TasksList() {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters: TaskFilters = {
    status: searchParams.get('status') as TaskStatus | undefined,
    assigneeId: searchParams.get('assigneeId') || undefined,
    zoneId: searchParams.get('zoneId') || undefined,
  };

  const setFilter = (key: string, value: string | undefined) => {
    const newParams = new URLSearchParams(searchParams);
    if (value) {
      newParams.set(key, value);
    } else {
      newParams.delete(key);
    }
    setSearchParams(newParams);
  };

  // ...
}
```

### 5. TaskStatusBadge Component
```typescript
import type { TaskStatus } from '../../types/api';

const statusColors: Record<TaskStatus, string> = {
  open: 'bg-blue-100 text-blue-800',
  in_progress: 'bg-yellow-100 text-yellow-800',
  done: 'bg-green-100 text-green-800',
  cancelled: 'bg-gray-100 text-gray-800',
};

const statusLabels: Record<TaskStatus, string> = {
  open: 'Open',
  in_progress: 'In Progress',
  done: 'Done',
  cancelled: 'Cancelled',
};

export function TaskStatusBadge({ status }: { status: TaskStatus }) {
  return (
    <span className={`px-2 py-1 rounded text-sm ${statusColors[status]}`}>
      {statusLabels[status]}
    </span>
  );
}
```

---

## Acceptance Criteria Verification

After implementation, verify:
- [ ] AC1: Tasks load on /households/:householdId/tasks
- [ ] AC2: Task shows all fields
- [ ] AC3: Status filter works
- [ ] AC4: Assignee filter works
- [ ] AC5: Zone filter works
- [ ] AC6: Combined filters work
- [ ] AC7: Empty state displays
- [ ] AC8: Loading state shows
- [ ] AC9: Errors handled
- [ ] AC10: Row click navigates

---

## STOP-THE-LINE

If you encounter:
- API returning unexpected format
- Missing prerequisite files
- Type mismatches

**STOP and report** — do not proceed with workarounds.

---

## Report Format

After completion:
```markdown
# ST-203 Implementation Report

## Files Created
- [list]

## Files Modified
- [list]

## Verification
- npm run lint: PASS/FAIL
- npm run build: PASS/FAIL
- Manual testing: PASS/FAIL

## AC Status
- AC1: PASS/FAIL
- AC2: PASS/FAIL
- ...
- AC10: PASS/FAIL

## Issues
- [any issues]
```
