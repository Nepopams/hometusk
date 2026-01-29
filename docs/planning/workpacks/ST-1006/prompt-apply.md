# Codex APPLY Prompt: ST-1006 — Pause/Resume + Upcoming Instances View

## Mode: APPLY (Implementation)

**CRITICAL:** This is the implementation phase. You MAY edit files.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1006/workpack.md
docs/planning/epics/EP-010/stories/ST-1006-pause-resume-upcoming.md
docs/_governance/dod.md
```

---

## PLAN Phase Findings (incorporated)

### Backend Status: COMPLETE ✅
All endpoints already exist in RoutineController:
- `POST /households/{hid}/routines/{rid}/pause`
- `POST /households/{hid}/routines/{rid}/resume`
- `GET /households/{hid}/routines/{rid}/upcoming`

Service methods exist: `pauseRoutine()`, `resumeRoutine()`, `getUpcomingInstances()`

**NO BACKEND CODE CHANGES NEEDED** (except OpenAPI verification).

### Frontend Status: NEEDS IMPLEMENTATION
From ST-1005:
- ✅ Routine types exist
- ✅ CRUD API functions exist
- ✅ Routines.tsx page exists
- ✅ RoutineRow.tsx exists

Missing:
- ❌ API functions: pauseRoutine, resumeRoutine, getUpcomingInstances
- ❌ Types: UpcomingInstance, UpcomingInstancesResponse
- ❌ Components: PauseResumeButton, UpcomingInstances

### Clarifications Applied (Human Gate C)

1. **projectedAssignee for upcoming:**
   - FIXED policy → shows `fixedAssignee.displayName`
   - ROUND_ROBIN policy → shows "Rotating" (backend returns null)
   - MANUAL policy → shows "Unassigned" (backend returns null)

2. **alreadyGenerated flag:** SKIP for MVP
   - No `alreadyGenerated` field in response
   - No batch task lookup needed
   - Deferred to post-MVP

3. **Backend changes:** Only verify OpenAPI has pause/resume/upcoming endpoints

---

## Implementation Steps

### Step 1: Verify OpenAPI Contract

**File:** `docs/contracts/http/routines.openapi.yaml`

Check if pause/resume/upcoming endpoints are documented. If missing, add:

```yaml
  /households/{householdId}/routines/{routineId}/pause:
    post:
      operationId: pauseRoutine
      summary: Pause a routine
      tags: [Routines]
      parameters:
        - $ref: '#/components/parameters/householdId'
        - $ref: '#/components/parameters/routineId'
      responses:
        '200':
          description: Routine paused
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Routine'
        '400':
          description: Cannot pause deleted routine
        '403':
          description: Not a member
        '404':
          description: Routine not found

  /households/{householdId}/routines/{routineId}/resume:
    post:
      operationId: resumeRoutine
      summary: Resume a paused routine
      tags: [Routines]
      parameters:
        - $ref: '#/components/parameters/householdId'
        - $ref: '#/components/parameters/routineId'
      responses:
        '200':
          description: Routine resumed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Routine'
        '400':
          description: Cannot resume deleted routine
        '403':
          description: Not a member
        '404':
          description: Routine not found

  /households/{householdId}/routines/{routineId}/upcoming:
    get:
      operationId: getUpcomingInstances
      summary: Get upcoming scheduled task instances
      tags: [Routines]
      parameters:
        - $ref: '#/components/parameters/householdId'
        - $ref: '#/components/parameters/routineId'
        - name: days
          in: query
          schema:
            type: integer
            default: 7
            maximum: 30
      responses:
        '200':
          description: Upcoming instances
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UpcomingInstancesResponse'
```

Add schemas if missing:
```yaml
    UpcomingInstancesResponse:
      type: object
      properties:
        routineId:
          type: string
          format: uuid
        routineTitle:
          type: string
        instances:
          type: array
          items:
            $ref: '#/components/schemas/UpcomingInstance'

    UpcomingInstance:
      type: object
      properties:
        scheduledDate:
          type: string
          format: date
        projectedAssignee:
          $ref: '#/components/schemas/UserSummary'
          nullable: true
```

### Step 2: Add TypeScript Types

**File:** `clients/web/src/types/api.ts`

Add after existing Routine types:

```typescript
// ============================================
// Upcoming Instances Types (ST-1006)
// ============================================

export interface UpcomingInstance {
  scheduledDate: string;
  projectedAssignee?: UserSummary;
}

export interface UpcomingInstancesResponse {
  routineId: string;
  routineTitle: string;
  instances: UpcomingInstance[];
}
```

### Step 3: Add API Functions

**File:** `clients/web/src/lib/api.ts`

Add import for new type:
```typescript
import type {
  // ... existing imports ...
  UpcomingInstancesResponse,
} from '../types/api';
```

Add functions after existing routine functions:
```typescript
// ============================================
// Routine Lifecycle API (ST-1006)
// ============================================

export async function pauseRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}/pause`, {
    method: 'POST',
  });
}

export async function resumeRoutine(householdId: string, routineId: string): Promise<Routine> {
  return apiFetch<Routine>(`/households/${householdId}/routines/${routineId}/resume`, {
    method: 'POST',
  });
}

export async function getUpcomingInstances(
  householdId: string,
  routineId: string,
  days: number = 7
): Promise<UpcomingInstancesResponse> {
  return apiFetch<UpcomingInstancesResponse>(
    `/households/${householdId}/routines/${routineId}/upcoming?days=${days}`
  );
}
```

### Step 4: Create PauseResumeButton Component

**File:** `clients/web/src/components/routines/PauseResumeButton.tsx`

```typescript
import { useState } from 'react';
import type { Routine } from '../../types/api';
import { Button } from '../ui';
import Modal from '../ui/Modal';
import './PauseResumeButton.css';

interface Props {
  routine: Routine;
  isPausing: boolean;
  isResuming: boolean;
  onPause: () => void;
  onResume: () => void;
}

export default function PauseResumeButton({
  routine,
  isPausing,
  isResuming,
  onPause,
  onResume,
}: Props) {
  const [showConfirm, setShowConfirm] = useState(false);

  if (routine.status === 'DELETED') {
    return null;
  }

  const handlePauseClick = () => {
    setShowConfirm(true);
  };

  const handleConfirmPause = () => {
    setShowConfirm(false);
    onPause();
  };

  if (routine.status === 'PAUSED') {
    return (
      <Button
        variant="secondary"
        size="sm"
        onClick={onResume}
        loading={isResuming}
        disabled={isPausing}
      >
        Resume
      </Button>
    );
  }

  return (
    <>
      <Button
        variant="secondary"
        size="sm"
        onClick={handlePauseClick}
        loading={isPausing}
        disabled={isResuming}
      >
        Pause
      </Button>

      <Modal
        open={showConfirm}
        onClose={() => setShowConfirm(false)}
        title="Pause routine"
        size="sm"
      >
        <div className="pause-confirm">
          <p className="pause-confirm__message">
            Pause routine? No new tasks will be generated while paused.
          </p>
          <div className="pause-confirm__actions">
            <Button variant="ghost" size="md" onClick={() => setShowConfirm(false)}>
              Cancel
            </Button>
            <Button variant="primary" size="md" onClick={handleConfirmPause}>
              Pause routine
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
```

**File:** `clients/web/src/components/routines/PauseResumeButton.css`

```css
.pause-confirm {
  padding: 8px 0;
}

.pause-confirm__message {
  margin: 0 0 24px;
  color: #475569;
  line-height: 1.5;
}

.pause-confirm__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
```

### Step 5: Create UpcomingInstances Component

**File:** `clients/web/src/components/routines/UpcomingInstances.tsx`

```typescript
import { useEffect, useState } from 'react';
import { getUpcomingInstances } from '../../lib/api';
import type { UpcomingInstancesResponse } from '../../types/api';
import './UpcomingInstances.css';

interface Props {
  householdId: string;
  routineId: string;
  routineStatus: string;
  assignmentPolicy: string;
}

export default function UpcomingInstances({
  householdId,
  routineId,
  routineStatus,
  assignmentPolicy,
}: Props) {
  const [data, setData] = useState<UpcomingInstancesResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (routineStatus === 'PAUSED' || routineStatus === 'DELETED') {
      setData(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    getUpcomingInstances(householdId, routineId, 7)
      .then(setData)
      .catch(() => setError('Failed to load upcoming instances'))
      .finally(() => setIsLoading(false));
  }, [householdId, routineId, routineStatus]);

  // Helper to display assignee based on policy
  const getAssigneeDisplay = (assigneeName?: string): string => {
    if (assigneeName) return assigneeName;
    if (assignmentPolicy === 'ROUND_ROBIN') return 'Rotating';
    if (assignmentPolicy === 'MANUAL') return 'Unassigned';
    return 'Unassigned';
  };

  if (routineStatus === 'PAUSED') {
    return (
      <div className="upcoming-instances upcoming-instances--paused">
        <p className="upcoming-instances__empty">
          Routine is paused. Resume to see upcoming tasks.
        </p>
      </div>
    );
  }

  if (routineStatus === 'DELETED') {
    return null;
  }

  if (isLoading) {
    return (
      <div className="upcoming-instances upcoming-instances--loading">
        <div className="upcoming-instances__skeleton" />
        <div className="upcoming-instances__skeleton" />
        <div className="upcoming-instances__skeleton" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="upcoming-instances upcoming-instances--error">
        <p>{error}</p>
      </div>
    );
  }

  if (!data || data.instances.length === 0) {
    return (
      <div className="upcoming-instances upcoming-instances--empty">
        <p className="upcoming-instances__empty">No upcoming tasks scheduled.</p>
      </div>
    );
  }

  return (
    <div className="upcoming-instances">
      <h4 className="upcoming-instances__title">Upcoming tasks</h4>
      <ul className="upcoming-instances__list">
        {data.instances.map((instance) => (
          <li key={instance.scheduledDate} className="upcoming-instances__item">
            <span className="upcoming-instances__date">
              {formatDate(instance.scheduledDate)}
            </span>
            <span className="upcoming-instances__assignee">
              {getAssigneeDisplay(instance.projectedAssignee?.displayName)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);

  const dateOnly = date.toDateString();
  if (dateOnly === today.toDateString()) return 'Today';
  if (dateOnly === tomorrow.toDateString()) return 'Tomorrow';

  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
}
```

**File:** `clients/web/src/components/routines/UpcomingInstances.css`

```css
.upcoming-instances {
  padding: 12px 16px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.upcoming-instances__title {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.upcoming-instances__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upcoming-instances__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.upcoming-instances__date {
  font-weight: 500;
  color: #1e293b;
}

.upcoming-instances__assignee {
  font-size: 14px;
  color: #64748b;
}

.upcoming-instances__empty {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
  font-style: italic;
}

.upcoming-instances--loading .upcoming-instances__skeleton {
  height: 40px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
  margin-bottom: 8px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.upcoming-instances--error {
  color: #dc2626;
}

.upcoming-instances--paused {
  background: #fefce8;
}
```

### Step 6: Update Barrel Export

**File:** `clients/web/src/components/routines/index.ts`

Add new exports:
```typescript
export { default as PauseResumeButton } from './PauseResumeButton';
export { default as UpcomingInstances } from './UpcomingInstances';
```

### Step 7: Update RoutineRow Component

**File:** `clients/web/src/components/routines/RoutineRow.tsx`

Update interface and add pause/resume + expand buttons:

```typescript
import { Link } from 'react-router-dom';
import type { Routine } from '../../types/api';
import RoutineStatusBadge from './RoutineStatusBadge';
import PauseResumeButton from './PauseResumeButton';
import { formatFrequency } from './FrequencyDisplay';
import './RoutineRow.css';

interface Props {
  routine: Routine;
  isPausing: boolean;
  isResuming: boolean;
  isExpanded: boolean;
  onEdit: (routine: Routine) => void;
  onDelete: (routine: Routine) => void;
  onPause: (routine: Routine) => void;
  onResume: (routine: Routine) => void;
  onToggleExpand: (routine: Routine) => void;
}

const POLICY_LABELS: Record<string, string> = {
  ROUND_ROBIN: 'Round-robin',
  FIXED: 'Fixed',
  MANUAL: 'Manual',
};

export default function RoutineRow({
  routine,
  isPausing,
  isResuming,
  isExpanded,
  onEdit,
  onDelete,
  onPause,
  onResume,
  onToggleExpand,
}: Props) {
  const policyLabel =
    routine.assignmentPolicy === 'FIXED' && routine.fixedAssignee
      ? `Fixed: ${routine.fixedAssignee.displayName}`
      : POLICY_LABELS[routine.assignmentPolicy] || routine.assignmentPolicy;

  return (
    <div className="routine-row">
      <div className="routine-row__main">
        <button
          type="button"
          className="routine-row__expand-btn"
          onClick={() => onToggleExpand(routine)}
          aria-label={isExpanded ? 'Collapse' : 'Expand'}
          aria-expanded={isExpanded}
        >
          {isExpanded ? '▼' : '▶'}
        </button>
        <span className="routine-row__title">{routine.title}</span>
        <span className="routine-row__zone">{routine.zone?.name || '—'}</span>
        <span className="routine-row__frequency">{formatFrequency(routine.recurrenceRule)}</span>
        <span className="routine-row__policy">{policyLabel}</span>
        <RoutineStatusBadge status={routine.status} />
      </div>
      <div className="routine-row__actions">
        <PauseResumeButton
          routine={routine}
          isPausing={isPausing}
          isResuming={isResuming}
          onPause={() => onPause(routine)}
          onResume={() => onResume(routine)}
        />
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

Add CSS for expand button in `RoutineRow.css`:
```css
.routine-row__expand-btn {
  background: none;
  border: none;
  padding: 4px 8px;
  cursor: pointer;
  color: #64748b;
  font-size: 12px;
  flex-shrink: 0;
}

.routine-row__expand-btn:hover {
  color: #1e293b;
}
```

### Step 8: Update Routines Page

**File:** `clients/web/src/routes/Routines.tsx`

Add imports:
```typescript
import { pauseRoutine, resumeRoutine } from '../lib/api';
import { UpcomingInstances } from '../components/routines';
```

Add state variables:
```typescript
const [pausingId, setPausingId] = useState<string | null>(null);
const [resumingId, setResumingId] = useState<string | null>(null);
const [expandedId, setExpandedId] = useState<string | null>(null);
```

Add handlers:
```typescript
const handlePause = useCallback(async (routine: Routine) => {
  if (!householdId) return;
  setPausingId(routine.id);
  try {
    await pauseRoutine(householdId, routine.id);
    refetch();
  } catch (err) {
    console.error('Failed to pause routine:', err);
  } finally {
    setPausingId(null);
  }
}, [householdId, refetch]);

const handleResume = useCallback(async (routine: Routine) => {
  if (!householdId) return;
  setResumingId(routine.id);
  try {
    await resumeRoutine(householdId, routine.id);
    refetch();
  } catch (err) {
    console.error('Failed to resume routine:', err);
  } finally {
    setResumingId(null);
  }
}, [householdId, refetch]);

const handleToggleExpand = useCallback((routine: Routine) => {
  setExpandedId((prev) => (prev === routine.id ? null : routine.id));
}, []);
```

Update the routines list rendering to include new props and UpcomingInstances:
```tsx
{routines.map((routine, idx) => (
  <div key={routine.id}>
    {idx > 0 && <div className="routines__divider" />}
    <RoutineRow
      routine={routine}
      isPausing={pausingId === routine.id}
      isResuming={resumingId === routine.id}
      isExpanded={expandedId === routine.id}
      onEdit={openEdit}
      onDelete={handleDeleteOpen}
      onPause={handlePause}
      onResume={handleResume}
      onToggleExpand={handleToggleExpand}
    />
    {expandedId === routine.id && (
      <UpcomingInstances
        householdId={householdId}
        routineId={routine.id}
        routineStatus={routine.status}
        assignmentPolicy={routine.assignmentPolicy}
      />
    )}
  </div>
))}
```

---

## Verification Commands

```bash
# Verify OpenAPI (if changed)
cat docs/contracts/http/routines.openapi.yaml | grep -A 20 "pause\|resume\|upcoming"

# Build web client
cd clients/web && npm run build

# Dev server for manual testing
cd clients/web && npm run dev
```

---

## Manual Testing Checklist

1. **Pause active routine:**
   - Click Pause button on ACTIVE routine
   - Verify confirmation dialog appears
   - Click "Pause routine" to confirm
   - Verify status changes to PAUSED
   - Verify badge updates

2. **Resume paused routine:**
   - Click Resume button on PAUSED routine
   - Verify immediate action (no confirmation)
   - Verify status changes to ACTIVE

3. **Upcoming instances view:**
   - Click expand arrow on ACTIVE routine
   - Verify upcoming dates displayed
   - Verify assignee display:
     - FIXED: shows user name
     - ROUND_ROBIN: shows "Rotating"
     - MANUAL: shows "Unassigned"
   - Click again to collapse

4. **Paused routine upcoming:**
   - Expand PAUSED routine
   - Verify "Routine is paused" message

---

## STOP-THE-LINE Rules

Stop and report if:
- TypeScript errors that can't be resolved
- Backend API returns different structure than expected
- Build fails

---

## DoD Checklist (verify at end)

- [ ] OpenAPI verified/updated for pause/resume/upcoming
- [ ] `UpcomingInstance` and `UpcomingInstancesResponse` types added
- [ ] `pauseRoutine()`, `resumeRoutine()`, `getUpcomingInstances()` API functions added
- [ ] `PauseResumeButton` component created with confirmation modal
- [ ] `UpcomingInstances` component created with assignee display logic
- [ ] `RoutineRow` updated with pause/resume + expand button
- [ ] `Routines.tsx` updated with handlers and upcoming view
- [ ] Barrel exports updated
- [ ] `npm run build` passes
- [ ] Manual testing completed
