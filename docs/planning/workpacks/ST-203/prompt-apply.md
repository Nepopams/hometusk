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
- `clients/web/src/types/api.ts` (from ST-202 - to extend)
- `clients/web/src/lib/api.ts` (from ST-202 - to extend)
- `clients/web/src/hooks/useAuth.ts` (from ST-202)
- `clients/web/src/routes/TasksList.tsx` (placeholder from ST-201 - to replace)

**ST-202 Baseline (Actual State):**
```typescript
// src/types/api.ts (existing):
- UserProfile, HouseholdSummary, AuthErrorResponse
- HouseholdRole, AuthErrorCode

// src/lib/api.ts (existing):
- apiFetch<T>(path, options) with auth header
- getMe(): Promise<UserProfile>

// src/hooks/useAuth.ts (existing):
- useAuth() → { householdId, user, status, ... }
```

**IMPORTANT:** EXTEND existing files, do NOT rewrite from scratch.

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

### 1. Types (EXTEND src/types/api.ts)

**Add these types to existing file (do NOT remove UserProfile, HouseholdSummary, etc.):**

```typescript
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

export interface TaskFilters {
  status?: TaskStatus;
  assigneeId?: string;
  zoneId?: string;
}
```

---

### 2. API Methods (EXTEND src/lib/api.ts)

**Add these functions to existing file (keep apiFetch, getMe, etc.):**

**Query building helper:**
```typescript
function buildQueryString(filters: TaskFilters): string {
  const params = new URLSearchParams();

  if (filters.status) params.set('status', filters.status);
  if (filters.assigneeId) params.set('assigneeId', filters.assigneeId);
  if (filters.zoneId) params.set('zoneId', filters.zoneId);

  const query = params.toString();
  return query ? `?${query}` : '';
}
```

**API methods (add after existing getMe function):**
```typescript
import type { Task, Zone, HouseholdMember, TaskFilters } from '../types/api';

export async function getTasks(
  householdId: string,
  filters: TaskFilters = {}
): Promise<Task[]> {
  const query = buildQueryString(filters);
  return apiFetch<Task[]>(`/households/${householdId}/tasks${query}`);
}

export async function getZones(householdId: string): Promise<Zone[]> {
  return apiFetch<Zone[]>(`/households/${householdId}/zones`);
}

export async function getMembers(householdId: string): Promise<HouseholdMember[]> {
  return apiFetch<HouseholdMember[]>(`/households/${householdId}/members`);
}
```

**Result:** api.ts will export apiFetch, getMe, getTasks, getZones, getMembers.

---

### 3. Custom Hooks

#### useTasks (src/hooks/useTasks.ts)
```typescript
import { useState, useEffect, useCallback } from 'react';
import { getTasks } from '../lib/api';
import type { Task, TaskFilters } from '../types/api';

export function useTasks(
  householdId: string | undefined,
  filters: TaskFilters
) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchTasks = useCallback(async () => {
    if (!householdId) return;

    setIsLoading(true);
    setError(null);
    try {
      const data = await getTasks(householdId, filters);
      setTasks(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load tasks'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId, filters.status, filters.assigneeId, filters.zoneId]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  return {
    tasks,
    isLoading,
    error,
    refetch: fetchTasks,
  };
}
```

#### useZones (src/hooks/useZones.ts)
```typescript
import { useState, useEffect } from 'react';
import { getZones } from '../lib/api';
import type { Zone } from '../types/api';

export function useZones(householdId: string | undefined) {
  const [zones, setZones] = useState<Zone[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!householdId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    getZones(householdId)
      .then(setZones)
      .catch((e) => setError(e instanceof Error ? e : new Error('Failed to load zones')))
      .finally(() => setIsLoading(false));
  }, [householdId]);

  return { zones, isLoading, error };
}
```

#### useMembers (src/hooks/useMembers.ts)
```typescript
import { useState, useEffect } from 'react';
import { getMembers } from '../lib/api';
import type { HouseholdMember } from '../types/api';

export function useMembers(householdId: string | undefined) {
  const [members, setMembers] = useState<HouseholdMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!householdId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    getMembers(householdId)
      .then(setMembers)
      .catch((e) => setError(e instanceof Error ? e : new Error('Failed to load members')))
      .finally(() => setIsLoading(false));
  }, [householdId]);

  return { members, isLoading, error };
}
```

---

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

### 5. UI Components

#### Spinner (src/components/ui/Spinner.tsx)
```typescript
export default function Spinner() {
  return <div className="spinner">Loading...</div>;
}
```

#### ErrorMessage (src/components/ui/ErrorMessage.tsx)
```typescript
interface ErrorMessageProps {
  error: Error;
  onRetry?: () => void;
}

export default function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
  return (
    <div className="error-message">
      <p>Error: {error.message}</p>
      {onRetry && (
        <button onClick={onRetry} type="button">
          Retry
        </button>
      )}
    </div>
  );
}
```

#### Select (src/components/ui/Select.tsx)
```typescript
interface SelectProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: { value: string; label: string }[];
}

export default function Select({ label, value, onChange, options }: SelectProps) {
  return (
    <div className="select-container">
      <label>
        {label}
        <select value={value} onChange={(e) => onChange(e.target.value)}>
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
```

---

### 6. Task Components

#### TaskStatusBadge (src/components/tasks/TaskStatusBadge.tsx)
```typescript
import type { TaskStatus } from '../../types/api';

const statusColors: Record<TaskStatus, string> = {
  open: 'chip chip-blue',
  in_progress: 'chip chip-yellow',
  done: 'chip chip-green',
  cancelled: 'chip chip-gray',
};

const statusLabels: Record<TaskStatus, string> = {
  open: 'Open',
  in_progress: 'In Progress',
  done: 'Done',
  cancelled: 'Cancelled',
};

interface TaskStatusBadgeProps {
  status: TaskStatus;
}

export default function TaskStatusBadge({ status }: TaskStatusBadgeProps) {
  return <span className={statusColors[status]}>{statusLabels[status]}</span>;
}
```

#### EmptyTasks (src/components/tasks/EmptyTasks.tsx)
```typescript
export default function EmptyTasks() {
  return (
    <div className="empty-state">
      <p>No tasks found.</p>
      <p>Try adjusting your filters or create a new task.</p>
    </div>
  );
}
```

#### TaskRow (src/components/tasks/TaskRow.tsx)
```typescript
import { useNavigate } from 'react-router-dom';
import type { Task } from '../../types/api';
import TaskStatusBadge from './TaskStatusBadge';

interface TaskRowProps {
  task: Task;
  householdId: string;
}

export default function TaskRow({ task, householdId }: TaskRowProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/households/${householdId}/tasks/${task.id}`);
  };

  return (
    <tr onClick={handleClick} className="task-row">
      <td>{task.title}</td>
      <td>
        <TaskStatusBadge status={task.status} />
      </td>
      <td>{task.assignee?.displayName || 'Unassigned'}</td>
      <td>{task.zone?.name || '—'}</td>
      <td>{task.deadline ? new Date(task.deadline).toLocaleDateString() : '—'}</td>
    </tr>
  );
}
```

#### TasksTable (src/components/tasks/TasksTable.tsx)
```typescript
import type { Task } from '../../types/api';
import TaskRow from './TaskRow';

interface TasksTableProps {
  tasks: Task[];
  householdId: string;
}

export default function TasksTable({ tasks, householdId }: TasksTableProps) {
  return (
    <table className="tasks-table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Status</th>
          <th>Assignee</th>
          <th>Zone</th>
          <th>Deadline</th>
        </tr>
      </thead>
      <tbody>
        {tasks.map((task) => (
          <TaskRow key={task.id} task={task} householdId={householdId} />
        ))}
      </tbody>
    </table>
  );
}
```

#### TaskFilters (src/components/tasks/TaskFilters.tsx)
```typescript
import type { TaskStatus, Zone, HouseholdMember } from '../../types/api';
import Select from '../ui/Select';

interface TaskFiltersProps {
  status: TaskStatus | undefined;
  assigneeId: string | undefined;
  zoneId: string | undefined;
  onStatusChange: (value: string) => void;
  onAssigneeChange: (value: string) => void;
  onZoneChange: (value: string) => void;
  zones: Zone[];
  members: HouseholdMember[];
  isLoading: boolean;
}

export default function TaskFilters({
  status,
  assigneeId,
  zoneId,
  onStatusChange,
  onAssigneeChange,
  onZoneChange,
  zones,
  members,
  isLoading,
}: TaskFiltersProps) {
  const statusOptions = [
    { value: '', label: 'All' },
    { value: 'open', label: 'Open' },
    { value: 'in_progress', label: 'In Progress' },
    { value: 'done', label: 'Done' },
    { value: 'cancelled', label: 'Cancelled' },
  ];

  const assigneeOptions = [
    { value: '', label: 'All' },
    ...members.map((m) => ({ value: m.userId, label: m.displayName })),
  ];

  const zoneOptions = [
    { value: '', label: 'All' },
    ...zones.map((z) => ({ value: z.id, label: z.name })),
  ];

  if (isLoading) {
    return <div>Loading filters...</div>;
  }

  return (
    <div className="task-filters">
      <Select
        label="Status"
        value={status || ''}
        onChange={onStatusChange}
        options={statusOptions}
      />
      <Select
        label="Assignee"
        value={assigneeId || ''}
        onChange={onAssigneeChange}
        options={assigneeOptions}
      />
      <Select
        label="Zone"
        value={zoneId || ''}
        onChange={onZoneChange}
        options={zoneOptions}
      />
    </div>
  );
}
```

---

### 7. TasksList Page (src/routes/TasksList.tsx)

**Replace existing placeholder with:**
```typescript
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useTasks } from '../hooks/useTasks';
import { useZones } from '../hooks/useZones';
import { useMembers } from '../hooks/useMembers';
import type { TaskFilters, TaskStatus } from '../types/api';
import TaskFilters from '../components/tasks/TaskFilters';
import TasksTable from '../components/tasks/TasksTable';
import EmptyTasks from '../components/tasks/EmptyTasks';
import Spinner from '../components/ui/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage';
import { ApiError } from '../lib/errors';

export default function TasksList() {
  const { householdId } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse filters from URL
  const filters: TaskFilters = {
    status: (searchParams.get('status') as TaskStatus) || undefined,
    assigneeId: searchParams.get('assigneeId') || undefined,
    zoneId: searchParams.get('zoneId') || undefined,
  };

  // Fetch data
  const { tasks, isLoading: tasksLoading, error: tasksError, refetch } = useTasks(
    householdId,
    filters
  );
  const { zones, isLoading: zonesLoading } = useZones(householdId);
  const { members, isLoading: membersLoading } = useMembers(householdId);

  // Filter handlers
  const setFilter = (key: string, value: string) => {
    const newParams = new URLSearchParams(searchParams);
    if (value) {
      newParams.set(key, value);
    } else {
      newParams.delete(key);
    }
    setSearchParams(newParams);
  };

  // Handle 403
  if (tasksError && tasksError instanceof ApiError && tasksError.status === 403) {
    return (
      <div className="page">
        <h1>Access Denied</h1>
        <p>You do not have access to this household.</p>
        <a href="/households">Back to Household Selector</a>
      </div>
    );
  }

  // Error state
  if (tasksError) {
    return (
      <div className="page">
        <h1>Tasks</h1>
        <ErrorMessage error={tasksError} onRetry={refetch} />
      </div>
    );
  }

  // Loading state
  if (tasksLoading && tasks.length === 0) {
    return (
      <div className="page">
        <h1>Tasks</h1>
        <Spinner />
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Tasks</h1>

      <TaskFilters
        status={filters.status}
        assigneeId={filters.assigneeId}
        zoneId={filters.zoneId}
        onStatusChange={(value) => setFilter('status', value)}
        onAssigneeChange={(value) => setFilter('assigneeId', value)}
        onZoneChange={(value) => setFilter('zoneId', value)}
        zones={zones}
        members={members}
        isLoading={zonesLoading || membersLoading}
      />

      {tasks.length === 0 ? (
        <EmptyTasks />
      ) : (
        <TasksTable tasks={tasks} householdId={householdId || 'demo'} />
      )}
    </div>
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
