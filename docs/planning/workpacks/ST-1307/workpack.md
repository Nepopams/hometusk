# Workpack: ST-1307 — ShoppingRun Creation UI

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1307-shopping-run-create-ui.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- ADR-014: `docs/adr/014-shopping-run-entity-design.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — ST-1302 backend endpoints delivered

---

## Goal
Add "Start Shopping Trip" button to ShoppingDetail page with confirmation modal. On confirm, create shopping run via API and redirect to run page.

---

## Scope

### In Scope
- "Start Shopping Trip" button in ShoppingDetail header
- Button disabled when no unpurchased items
- Confirmation modal with list name + item count
- API call to create run: `POST /households/{hid}/shopping-lists/{lid}/runs`
- Loading state during creation
- Error handling with retry in modal
- Redirect to `/households/{hid}/shopping-runs/{runId}` after success

### Out of Scope
- ShoppingRunPage (ST-1308)
- Multi-list runs
- Scheduled runs
- Run from empty list (just disable button)

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/api.ts` | MODIFY | Add `createShoppingRun()` function |
| `clients/web/src/types/api.ts` | MODIFY | Add `ShoppingRun`, `ShoppingRunItem` types |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY | Add button + modal + handler |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY | Add button styles (if needed) |

---

## Implementation Plan

### Step 1: Add types for ShoppingRun

**File:** `clients/web/src/types/api.ts`

Add interfaces matching contract:
```typescript
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

### Step 2: Add API function

**File:** `clients/web/src/lib/api.ts`

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

### Step 3: Add Start Trip UI to ShoppingDetail

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

3a. Add imports:
```typescript
import { createShoppingRun } from '../lib/api';
import { Modal } from '../components/ui';
import { useNavigate } from 'react-router-dom';
```

3b. Add state:
```typescript
const navigate = useNavigate();
const [showStartModal, setShowStartModal] = useState(false);
const [isCreatingRun, setIsCreatingRun] = useState(false);
const [createRunError, setCreateRunError] = useState<string | null>(null);
```

3c. Add handler:
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
    setCreateRunError(err instanceof Error ? err.message : 'Failed to start trip');
  } finally {
    setIsCreatingRun(false);
  }
}, [householdId, listId, navigate]);
```

3d. Add button to header (after Export button):
```tsx
<button
  type="button"
  className="primary-button shopping-detail__header-btn"
  onClick={() => setShowStartModal(true)}
  disabled={unpurchasedItems.length === 0}
  aria-label="Start shopping trip"
>
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
    <circle cx="9" cy="21" r="1" />
    <circle cx="20" cy="21" r="1" />
    <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
  </svg>
  Start Trip
</button>
```

3e. Add modal before snackbar:
```tsx
<Modal
  open={showStartModal}
  onClose={() => !isCreatingRun && setShowStartModal(false)}
  title="Start Shopping Trip"
  size="sm"
  closeOnBackdrop={!isCreatingRun}
>
  <div className="shopping-detail__start-modal">
    <p className="shopping-detail__start-modal-info">
      Starting a shopping trip for <strong>{list?.name}</strong> with{' '}
      <strong>{unpurchasedItems.length}</strong> {unpurchasedItems.length === 1 ? 'item' : 'items'}.
    </p>
    {createRunError && (
      <p className="shopping-detail__start-modal-error">{createRunError}</p>
    )}
    <div className="shopping-detail__start-modal-actions">
      <Button
        variant="secondary"
        size="md"
        onClick={() => setShowStartModal(false)}
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

### Step 4: Add CSS (if needed)

**File:** `clients/web/src/routes/ShoppingDetail.css`

```css
/* Start Trip Button (primary style in header) */
.shopping-detail__header-btn.primary-button {
  background-color: var(--color-primary);
  color: white;
}

.shopping-detail__header-btn.primary-button:hover {
  background-color: var(--color-primary-hover);
}

.shopping-detail__header-btn.primary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

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

.shopping-detail__start-modal-error {
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

.shopping-detail__start-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-2);
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
2. Verify "Start Trip" button visible and enabled
3. Open list with all purchased items → button disabled
4. Click "Start Trip" → modal opens with item count
5. Click "Start" → loading state → redirect to run page (will 404 for now - ST-1308)
6. Test error handling: disconnect network, try again

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Button visible when unpurchased items exist | Visual check |
| AC-2 | Modal shows list name + item count | Click button, check modal |
| AC-3 | Start creates run + redirects | Click Start, check URL |
| AC-4 | Error shown in modal | Simulate API error |
| AC-5 | Empty list shows disabled button | Check with empty list |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Run page 404 | Low | Expected until ST-1308 implemented |
| Modal closes during loading | Low | Disable close during loading |

---

## Rollback

- Remove Start Trip button and modal from ShoppingDetail
- Remove createShoppingRun from api.ts
- Remove ShoppingRun types

---

## References

- Endpoint: `POST /households/{hid}/shopping-lists/{lid}/runs`
- Response: `ShoppingRunDto` from contract
- Modal: Uses existing `Modal` component from `components/ui`
