# Codex APPLY: ST-1307 — ShoppingRun Creation UI

## Context
Add "Start Shopping Trip" button to ShoppingDetail header with confirmation modal. Based on PLAN findings:
- Header is at lines 391–421 with `.shopping-detail__header-actions`
- Use `Button` component with `variant="primary"` (not CSS class)
- Add `useNavigate` import (not currently imported)
- Types go in `types/api.ts` near Shopping Types (~line 240)
- Follow `CreateZoneModal` pattern for modal

## Files to Create/Modify

| File | Action |
|------|--------|
| `clients/web/src/types/api.ts` | MODIFY — add ShoppingRun types near Shopping Types |
| `clients/web/src/lib/api.ts` | MODIFY — add createShoppingRun function |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY — add button, modal, handler |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY — add modal styles |

---

## Step 1: Add ShoppingRun types

**File:** `clients/web/src/types/api.ts`

Add near other Shopping types (around line 240):

```typescript
// Shopping Run Types
export type ShoppingRunStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface ShoppingRunItem {
  id: string;
  name: string;
  quantity?: number;
  unit?: string;
  purchased: boolean;
  purchasedAt?: string;
}

export interface ShoppingRun {
  id: string;
  listId: string;
  householdId: string;
  status: ShoppingRunStatus;
  items: ShoppingRunItem[];
  purchasedCount: number;
  totalCount: number;
  createdAt: string;
  closedAt?: string;
}
```

---

## Step 2: Add API function

**File:** `clients/web/src/lib/api.ts`

### 2a: Add import for ShoppingRun type

Add `ShoppingRun` to the imports from `../types/api`:

```typescript
import type {
  // ... existing imports ...
  ShoppingRun,
} from '../types/api';
```

### 2b: Add createShoppingRun function

Add after other shopping functions (after `deleteShoppingItem`):

```typescript
export async function createShoppingRun(
  householdId: string,
  listId: string
): Promise<ShoppingRun> {
  return apiFetch<ShoppingRun>(
    `/households/${householdId}/shopping-lists/${listId}/runs`,
    { method: 'POST' }
  );
}
```

---

## Step 3: Modify ShoppingDetail.tsx

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

### 3a: Add imports

Add `useNavigate` to react-router-dom import:

```typescript
import { Link, useNavigate, useParams } from 'react-router-dom';
```

Add `createShoppingRun` to api imports:

```typescript
import { exportShoppingList, getShoppingList, createShoppingRun } from '../lib/api';
```

Add `Modal` to ui imports:

```typescript
import { Button, Modal, Snackbar } from '../components/ui';
```

### 3b: Add state and hooks (after existing useState declarations, around line 56)

```typescript
const navigate = useNavigate();
const [showStartModal, setShowStartModal] = useState(false);
const [isCreatingRun, setIsCreatingRun] = useState(false);
const [createRunError, setCreateRunError] = useState<string | null>(null);
```

### 3c: Add handler (after handleExportCsv, around line 148)

```typescript
const handleStartTrip = useCallback(async () => {
  if (!householdId || !listId) return;

  setIsCreatingRun(true);
  setCreateRunError(null);

  try {
    const run = await createShoppingRun(householdId, listId);
    setShowStartModal(false);
    navigate(`/households/${householdId}/shopping-runs/${run.id}`);
  } catch (err) {
    setCreateRunError(err instanceof Error ? err.message : 'Failed to start shopping trip');
  } finally {
    setIsCreatingRun(false);
  }
}, [householdId, listId, navigate]);

const handleOpenStartModal = useCallback(() => {
  setCreateRunError(null);
  setShowStartModal(true);
}, []);

const handleCloseStartModal = useCallback(() => {
  if (!isCreatingRun) {
    setShowStartModal(false);
  }
}, [isCreatingRun]);
```

### 3d: Add Start Trip button to header (inside `.shopping-detail__header-actions`, after Export button, around line 426)

```tsx
<Button
  variant="primary"
  size="sm"
  onClick={handleOpenStartModal}
  disabled={unpurchasedItems.length === 0}
  aria-label="Start shopping trip"
>
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <circle cx="9" cy="21" r="1" />
    <circle cx="20" cy="21" r="1" />
    <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
  </svg>
  Start Trip
</Button>
```

### 3e: Add Modal (before Snackbar, around line 513)

```tsx
<Modal
  open={showStartModal}
  onClose={handleCloseStartModal}
  title="Start Shopping Trip"
  size="sm"
  closeOnBackdrop={!isCreatingRun}
>
  <div className="shopping-detail__start-modal">
    <p className="shopping-detail__start-modal-info">
      You're about to start a shopping trip for <strong>{list?.name}</strong> with{' '}
      <strong>{unpurchasedItems.length}</strong> {unpurchasedItems.length === 1 ? 'item' : 'items'} to buy.
    </p>
    {createRunError && (
      <p className="shopping-detail__start-modal-error">{createRunError}</p>
    )}
    <div className="shopping-detail__start-modal-actions">
      <Button
        variant="secondary"
        size="md"
        onClick={handleCloseStartModal}
        disabled={isCreatingRun}
      >
        Cancel
      </Button>
      <Button
        variant="primary"
        size="md"
        onClick={handleStartTrip}
        disabled={isCreatingRun}
      >
        {isCreatingRun ? 'Starting...' : 'Start'}
      </Button>
    </div>
  </div>
</Modal>
```

---

## Step 4: Add CSS styles

**File:** `clients/web/src/routes/ShoppingDetail.css`

Add at the end of file:

```css
/* Start Trip Modal */
.shopping-detail__start-modal {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.shopping-detail__start-modal-info {
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.shopping-detail__start-modal-info strong {
  color: var(--color-text-primary);
}

.shopping-detail__start-modal-error {
  color: var(--color-error);
  font-size: var(--font-size-sm);
  padding: var(--spacing-2);
  background-color: var(--color-error-bg, rgba(239, 68, 68, 0.1));
  border-radius: var(--radius-sm);
}

.shopping-detail__start-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-2);
  padding-top: var(--spacing-2);
}
```

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web

npm run build
npm run lint
```

Manual test:
1. Open shopping list with unpurchased items
2. Verify "Start Trip" button (blue/primary) appears in header
3. Click → modal opens showing list name + item count
4. Click "Cancel" → modal closes
5. Click "Start" → loading state → redirects (will 404 until ST-1308)
6. Open list with all items purchased → button disabled
7. Open empty list → button disabled

---

## Constraints
- Use existing `Button` component with `variant="primary"`, not custom CSS class
- Use existing `Modal` component
- Follow `createRoutine` pattern for API function
- Place types near other Shopping types in api.ts
- `stopPropagation` not needed for modal (handled internally)
