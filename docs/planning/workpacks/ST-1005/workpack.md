# Workpack: ST-1005 — Routines Page (List + Create/Edit Form)

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1005-routines-page.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S12/sprint.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/routines.openapi.yaml`
- Web Client Patterns: `clients/web/src/routes/TasksList.tsx`, `clients/web/src/hooks/useTasks.ts`

---

## Status
**Ready** — DoR pass, backend CRUD available

---

## Outcome

Users can view, create, edit, and delete routines through the web interface with frequency presets and assignment policy selection.

---

## Acceptance Criteria Summary

1. **AC-1:** Routines page accessible at `/households/{id}/routines`
2. **AC-2:** List shows routines with title, zone, frequency, status, policy
3. **AC-3:** Create button opens form
4. **AC-4-7:** Create routine with DAILY/WEEKLY/MONTHLY/EVERY_N_DAYS
5. **AC-8:** Fixed policy requires user selection
6. **AC-9:** Edit form pre-populated
7. **AC-10:** Delete with confirmation
8. **AC-11:** Validation errors shown
9. **AC-12:** Empty state displayed

---

## Files to Create

| File | Description |
|------|-------------|
| `clients/web/src/routes/Routines.tsx` | Routines list page component |
| `clients/web/src/routes/Routines.css` | Styles for routines page |
| `clients/web/src/hooks/useRoutines.ts` | Hook for fetching routines list |
| `clients/web/src/components/routines/RoutineRow.tsx` | Single routine list item |
| `clients/web/src/components/routines/RoutineForm.tsx` | Create/edit form modal |
| `clients/web/src/components/routines/RoutineStatusBadge.tsx` | Status badge component |
| `clients/web/src/components/routines/FrequencyDisplay.tsx` | Human-readable frequency text |
| `clients/web/src/components/routines/index.ts` | Barrel exports |

---

## Files to Modify

| File | Changes |
|------|---------|
| `clients/web/src/routes/index.tsx` | Add `/routines` route |
| `clients/web/src/components/Layout/Sidebar.tsx` | Add "Routines" navigation link |
| `clients/web/src/lib/api.ts` | Add routine API functions |
| `clients/web/src/types/api.ts` | Add Routine types |

---

## Implementation Plan

### Step 1: Add TypeScript Types
**File:** `clients/web/src/types/api.ts`

Add types for Routine entity:
```typescript
export type RoutineStatus = 'ACTIVE' | 'PAUSED' | 'DELETED';
export type AssignmentPolicy = 'FIXED' | 'ROUND_ROBIN' | 'MANUAL';

export interface RecurrenceRule {
  type: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'EVERY_N_DAYS';
  daysOfWeek?: string[];
  dayOfMonth?: number;
  interval?: number;
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
```

### Step 2: Add API Functions
**File:** `clients/web/src/lib/api.ts`

Add routine API functions:
```typescript
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

Follow pattern from `useZones.ts`:
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

### Step 4: Add Route
**File:** `clients/web/src/routes/index.tsx`

Add route inside HouseholdLayout children:
```typescript
import Routines from './Routines';
// ...
{ path: 'routines', element: <Routines /> },
```

### Step 5: Add Sidebar Link
**File:** `clients/web/src/components/Layout/Sidebar.tsx`

Add navigation link after "Tasks":
```tsx
<NavLink className={getLinkClass} to={`${basePath}/routines`}>
  Routines
</NavLink>
```

### Step 6: Create RoutineStatusBadge Component
**File:** `clients/web/src/components/routines/RoutineStatusBadge.tsx`

Simple badge for ACTIVE/PAUSED status:
```tsx
interface Props {
  status: 'ACTIVE' | 'PAUSED' | 'DELETED';
}

export default function RoutineStatusBadge({ status }: Props) {
  const className = `routine-status-badge routine-status-badge--${status.toLowerCase()}`;
  return <span className={className}>{status}</span>;
}
```

### Step 7: Create FrequencyDisplay Component
**File:** `clients/web/src/components/routines/FrequencyDisplay.tsx`

Convert recurrence rule to human-readable text:
```tsx
import type { RecurrenceRule } from '../../types/api';

const DAYS_MAP: Record<string, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
};

export function formatFrequency(rule: RecurrenceRule): string {
  switch (rule.type) {
    case 'DAILY':
      return 'Daily';
    case 'WEEKLY':
      return rule.daysOfWeek?.map(d => DAYS_MAP[d] || d).join(', ') || 'Weekly';
    case 'MONTHLY':
      return `${rule.dayOfMonth}${getOrdinalSuffix(rule.dayOfMonth || 1)} of month`;
    case 'EVERY_N_DAYS':
      return `Every ${rule.interval} days`;
    default:
      return 'Unknown';
  }
}
```

### Step 8: Create RoutineRow Component
**File:** `clients/web/src/components/routines/RoutineRow.tsx`

List item showing routine info with edit/delete buttons.

### Step 9: Create RoutineForm Component
**File:** `clients/web/src/components/routines/RoutineForm.tsx`

Modal form with:
- Title input (required)
- Description textarea
- Zone selector
- Frequency preset radio buttons:
  - Daily
  - Weekly (show day checkboxes when selected)
  - Monthly (show day selector when selected)
  - Every N days (show interval input when selected)
- Assignment policy radio:
  - Round-robin
  - Fixed (show member selector when selected)
  - Manual

### Step 10: Create Routines Page
**File:** `clients/web/src/routes/Routines.tsx`

Follow TasksList.tsx pattern:
- Loading state with skeletons
- Error state with retry button
- Empty state with CTA
- List of RoutineRow components
- Create button opening RoutineForm modal

### Step 11: Create barrel export
**File:** `clients/web/src/components/routines/index.ts`

```typescript
export { default as RoutineRow } from './RoutineRow';
export { default as RoutineForm } from './RoutineForm';
export { default as RoutineStatusBadge } from './RoutineStatusBadge';
export { formatFrequency } from './FrequencyDisplay';
```

---

## Verification Commands

```bash
# Build web client
cd clients/web && npm run build

# Type check
cd clients/web && npm run typecheck

# Lint
cd clients/web && npm run lint

# Run dev server and test manually
cd clients/web && npm run dev
```

---

## Tests

### Unit Tests (React)
- `RoutineForm.test.tsx`:
  - `renders_emptyForm_forCreate`
  - `renders_prePopulated_forEdit`
  - `validation_titleRequired`
  - `weeklyFrequency_showsDayPicker`
  - `monthlyFrequency_showsDaySelector`
  - `everyNDays_showsIntervalInput`
  - `fixedPolicy_showsUserSelector`

### E2E Tests (if applicable)
- Navigate to routines page
- Create routine with DAILY frequency
- Edit routine title
- Delete routine

---

## DoD Checklist

- [ ] TypeScript types added for Routine
- [ ] API functions added for CRUD
- [ ] useRoutines hook created
- [ ] Route added to index.tsx
- [ ] Sidebar link added
- [ ] Routines.tsx page component created
- [ ] RoutineForm with all frequency presets
- [ ] RoutineStatusBadge component
- [ ] Validation errors displayed
- [ ] Empty state displayed
- [ ] `npm run build` passes
- [ ] `npm run typecheck` passes
- [ ] `npm run lint` passes
- [ ] Manual testing completed

---

## Risks

| Risk | Mitigation |
|------|------------|
| Form complexity with conditional fields | Use controlled components, test each preset |
| API contract mismatch | Verify against routines.openapi.yaml |
| CSS inconsistency | Follow existing page patterns |

---

## Rollback

If issues discovered:
1. Revert route addition in index.tsx
2. Revert sidebar link
3. Remove new files (components/hooks/routes)
4. Keep API functions/types (no harm if unused)
