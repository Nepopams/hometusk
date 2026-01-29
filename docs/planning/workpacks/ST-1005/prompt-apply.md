# Codex APPLY Prompt: ST-1005 — Routines Page (List + Create/Edit Form)

## Mode: APPLY (Implementation)

**CRITICAL:** This is the implementation phase. You MAY edit files.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1005/workpack.md
docs/planning/epics/EP-010/stories/ST-1005-routines-page.md
docs/_governance/dod.md
```

---

## PLAN Phase Findings (incorporated)

### Verified State
- RoutineController: exists with CRUD + lifecycle endpoints in `routines/api/RoutineController.java`
- RoutineDto: includes `zone` (ZoneSummary) and `fixedAssignee` (UserSummary) as objects, not IDs
- OpenAPI: matches DTO structure
- Frontend patterns: established in TasksList, useZones, CreateZoneModal

### Clarifications Applied
1. **pausedAt display:** Badge only for ST-1005, detailed display deferred to ST-1006
2. **Empty state text:** "No routines yet. Create your first routine to automate recurring tasks."
3. **Delete confirmation:** Use Modal component (not window.confirm)
4. **Type adaptation:** Response uses `zone?: Zone`, `fixedAssignee?: UserSummary`; requests use `zoneId`, `fixedAssigneeId`

---

## Implementation Steps

### Step 1: Add TypeScript Types

**File:** `clients/web/src/types/api.ts`

Add at end of file (before any re-exports):

```typescript
// ============================================
// Routine Types (ST-1005)
// ============================================

export type RoutineStatus = 'ACTIVE' | 'PAUSED' | 'DELETED';
export type AssignmentPolicy = 'FIXED' | 'ROUND_ROBIN' | 'MANUAL';
export type RecurrenceType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'EVERY_N_DAYS';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface RecurrenceRule {
  type: RecurrenceType;
  daysOfWeek?: DayOfWeek[];
  dayOfMonth?: number;
  interval?: number;
}

export interface Routine {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  zone?: Zone;
  recurrenceRule: RecurrenceRule;
  assignmentPolicy: AssignmentPolicy;
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
```

### Step 2: Add API Functions

**File:** `clients/web/src/lib/api.ts`

Add imports at top:
```typescript
import type {
  // ... existing imports ...
  Routine,
  CreateRoutineRequest,
  UpdateRoutineRequest,
} from '../types/api';
```

Add functions at end:
```typescript
// ============================================
// Routines API (ST-1005)
// ============================================

export async function getRoutines(householdId: string): Promise<Routine[]> {
  return apiFetch<Routine[]>(`/households/${householdId}/routines`);
}

export async function getRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}`);
}

export async function createRoutine(
  householdId: string,
  data: CreateRoutineRequest
): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines`, {
    method: 'POST',
    body: data,
  });
}

export async function updateRoutine(
  householdId: string,
  routineId: string,
  data: UpdateRoutineRequest
): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}`, {
    method: 'PATCH',
    body: data,
  });
}

export async function deleteRoutine(householdId: string, routineId: string): Promise<void> {
  return apiFetch<void>(`/households/${householdId}/routines/${routineId}`, {
    method: 'DELETE',
  });
}
```

### Step 3: Create useRoutines Hook

**File:** `clients/web/src/hooks/useRoutines.ts`

```typescript
import { useEffect, useState, useCallback } from 'react';
import { getRoutines } from '../lib/api';
import type { Routine } from '../types/api';

export function useRoutines(householdId: string | null | undefined) {
  const [routines, setRoutines] = useState<Routine[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchRoutines = useCallback(() => {
    if (!householdId) {
      setRoutines([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    getRoutines(householdId)
      .then(setRoutines)
      .catch((e) => setError(e instanceof Error ? e : new Error('Failed to load routines')))
      .finally(() => setIsLoading(false));
  }, [householdId]);

  useEffect(() => {
    fetchRoutines();
  }, [fetchRoutines]);

  return { routines, isLoading, error, refetch: fetchRoutines };
}
```

### Step 4: Create Routine Components

**File:** `clients/web/src/components/routines/RoutineStatusBadge.tsx`

```typescript
import type { RoutineStatus } from '../../types/api';
import './RoutineStatusBadge.css';

interface Props {
  status: RoutineStatus;
}

export default function RoutineStatusBadge({ status }: Props) {
  const className = `routine-status-badge routine-status-badge--${status.toLowerCase()}`;
  const label = status === 'ACTIVE' ? 'Active' : status === 'PAUSED' ? 'Paused' : 'Deleted';
  return <span className={className}>{label}</span>;
}
```

**File:** `clients/web/src/components/routines/RoutineStatusBadge.css`

```css
.routine-status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
}

.routine-status-badge--active {
  background-color: #dcfce7;
  color: #166534;
}

.routine-status-badge--paused {
  background-color: #fef9c3;
  color: #854d0e;
}

.routine-status-badge--deleted {
  background-color: #fee2e2;
  color: #991b1b;
}
```

**File:** `clients/web/src/components/routines/FrequencyDisplay.tsx`

```typescript
import type { RecurrenceRule } from '../../types/api';

const DAYS_SHORT: Record<string, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
};

function getOrdinalSuffix(n: number): string {
  const s = ['th', 'st', 'nd', 'rd'];
  const v = n % 100;
  return s[(v - 20) % 10] || s[v] || s[0];
}

export function formatFrequency(rule: RecurrenceRule): string {
  switch (rule.type) {
    case 'DAILY':
      return 'Daily';
    case 'WEEKLY':
      if (!rule.daysOfWeek || rule.daysOfWeek.length === 0) return 'Weekly';
      if (rule.daysOfWeek.length === 7) return 'Every day';
      return rule.daysOfWeek.map((d) => DAYS_SHORT[d] || d).join(', ');
    case 'MONTHLY': {
      const day = rule.dayOfMonth || 1;
      return `${day}${getOrdinalSuffix(day)} of month`;
    }
    case 'EVERY_N_DAYS':
      return `Every ${rule.interval || 2} days`;
    default:
      return 'Custom';
  }
}

interface Props {
  rule: RecurrenceRule;
}

export default function FrequencyDisplay({ rule }: Props) {
  return <span>{formatFrequency(rule)}</span>;
}
```

**File:** `clients/web/src/components/routines/RoutineRow.tsx`

```typescript
import { Link } from 'react-router-dom';
import type { Routine } from '../../types/api';
import RoutineStatusBadge from './RoutineStatusBadge';
import { formatFrequency } from './FrequencyDisplay';
import './RoutineRow.css';

interface Props {
  routine: Routine;
  householdId: string;
  onEdit: (routine: Routine) => void;
  onDelete: (routine: Routine) => void;
}

const POLICY_LABELS: Record<string, string> = {
  ROUND_ROBIN: 'Round-robin',
  FIXED: 'Fixed',
  MANUAL: 'Manual',
};

export default function RoutineRow({ routine, householdId, onEdit, onDelete }: Props) {
  const policyLabel = routine.assignmentPolicy === 'FIXED' && routine.fixedAssignee
    ? `Fixed: ${routine.fixedAssignee.displayName}`
    : POLICY_LABELS[routine.assignmentPolicy] || routine.assignmentPolicy;

  return (
    <div className="routine-row">
      <div className="routine-row__main">
        <span className="routine-row__title">{routine.title}</span>
        <span className="routine-row__zone">{routine.zone?.name || '—'}</span>
        <span className="routine-row__frequency">{formatFrequency(routine.recurrenceRule)}</span>
        <span className="routine-row__policy">{policyLabel}</span>
        <RoutineStatusBadge status={routine.status} />
      </div>
      <div className="routine-row__actions">
        <button
          type="button"
          className="routine-row__btn routine-row__btn--edit"
          onClick={() => onEdit(routine)}
          aria-label="Edit routine"
        >
          Edit
        </button>
        <button
          type="button"
          className="routine-row__btn routine-row__btn--delete"
          onClick={() => onDelete(routine)}
          aria-label="Delete routine"
        >
          Delete
        </button>
      </div>
    </div>
  );
}
```

**File:** `clients/web/src/components/routines/RoutineRow.css`

```css
.routine-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  gap: 12px;
}

.routine-row__main {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.routine-row__title {
  font-weight: 500;
  min-width: 150px;
  flex-shrink: 0;
}

.routine-row__zone,
.routine-row__frequency,
.routine-row__policy {
  color: #64748b;
  font-size: 14px;
  min-width: 100px;
}

.routine-row__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.routine-row__btn {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  background: white;
  transition: background-color 0.15s;
}

.routine-row__btn:hover {
  background-color: #f1f5f9;
}

.routine-row__btn--delete {
  color: #dc2626;
  border-color: #fecaca;
}

.routine-row__btn--delete:hover {
  background-color: #fef2f2;
}
```

**File:** `clients/web/src/components/routines/RoutineForm.tsx`

Create full form component with:
- Title input (required)
- Description textarea
- Zone selector (from useZones)
- Frequency radio buttons with conditional fields
- Assignment policy radio with conditional user selector
- Validation
- Submit handling

(This is a larger component - implement following CreateZoneModal pattern with controlled inputs)

**File:** `clients/web/src/components/routines/DeleteRoutineModal.tsx`

Simple confirmation modal following InviteModal pattern.

**File:** `clients/web/src/components/routines/index.ts`

```typescript
export { default as RoutineRow } from './RoutineRow';
export { default as RoutineForm } from './RoutineForm';
export { default as RoutineStatusBadge } from './RoutineStatusBadge';
export { default as FrequencyDisplay, formatFrequency } from './FrequencyDisplay';
export { default as DeleteRoutineModal } from './DeleteRoutineModal';
```

### Step 5: Create Routines Page

**File:** `clients/web/src/routes/Routines.tsx`

Follow TasksList.tsx pattern:
- useAuth for householdId
- useRoutines hook
- useZones for form
- useMembers for form
- Loading skeleton state
- Error state (403 separate, general retry)
- Empty state with CTA
- List of RoutineRow
- Modal state for create/edit/delete
- Refetch after mutations

**File:** `clients/web/src/routes/Routines.css`

Style following TasksList.css pattern.

### Step 6: Add Route

**File:** `clients/web/src/routes/index.tsx`

Add import:
```typescript
import Routines from './Routines';
```

Add route inside HouseholdLayout children (after 'members'):
```typescript
{ path: 'routines', element: <Routines /> },
```

### Step 7: Add Sidebar Link

**File:** `clients/web/src/components/Layout/Sidebar.tsx`

Add after "Tasks" NavLink:
```tsx
<NavLink className={getLinkClass} to={`${basePath}/routines`}>
  Routines
</NavLink>
```

---

## Verification Commands

```bash
# Type check
cd clients/web && npm run typecheck

# Lint
cd clients/web && npm run lint

# Build
cd clients/web && npm run build

# Dev server (manual testing)
cd clients/web && npm run dev
```

---

## Manual Testing Checklist

1. Navigate to `/households/{id}/routines`
2. Verify sidebar shows "Routines" link
3. Verify empty state if no routines
4. Create routine with Daily frequency
5. Create routine with Weekly (select multiple days)
6. Create routine with Monthly (select day)
7. Create routine with Every N days
8. Create routine with Fixed policy (select user)
9. Edit existing routine
10. Delete routine with confirmation

---

## STOP-THE-LINE Rules

Stop and report if:
- TypeScript errors that can't be resolved
- API calls return unexpected structure
- Missing dependencies (useZones, useMembers not available)
- Build fails

---

## DoD Checklist (verify at end)

- [ ] TypeScript types added
- [ ] API functions added
- [ ] useRoutines hook created
- [ ] Route added to index.tsx
- [ ] Sidebar link added
- [ ] Routines.tsx page works
- [ ] RoutineForm with all presets
- [ ] Delete confirmation modal
- [ ] `npm run typecheck` passes
- [ ] `npm run lint` passes
- [ ] `npm run build` passes
- [ ] Manual testing completed
