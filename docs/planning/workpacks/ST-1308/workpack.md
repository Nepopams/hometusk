# Workpack: ST-1308 — ShoppingRun Checklist UI

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1308-shopping-run-checklist-ui.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- ADR-014: `docs/adr/014-shopping-run-entity-design.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — ST-1302 endpoints + ST-1307 creation UI done

---

## Goal
Create ShoppingRun page with checklist UI for marking items purchased during a shopping trip. Support complete/cancel flows with confirmation and read-only view for closed runs.

---

## Scope

### In Scope
- New route: `/households/{hid}/shopping-runs/{runId}`
- Item checklist with checkboxes
- Progress indicator (X of Y purchased)
- Optimistic UI for checkbox updates with rollback
- "Complete Trip" button with confirmation modal
- "Cancel Trip" button with confirmation
- Summary view after run closure
- Read-only mode for COMPLETED/CANCELLED runs
- Status badge for closed runs
- Back navigation to shopping list
- Error handling with Snackbar

### Out of Scope
- Item reordering
- Adding items during run
- Photos/receipts
- Run history comparison

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/api.ts` | MODIFY | Add getShoppingRun, updateRunItem, closeShoppingRun |
| `clients/web/src/routes/ShoppingRun.tsx` | CREATE | Run checklist page |
| `clients/web/src/routes/ShoppingRun.css` | CREATE | Page styles |
| `clients/web/src/routes/index.tsx` | MODIFY | Add route |

---

## Implementation Plan

### Step 1: Add API functions

**File:** `clients/web/src/lib/api.ts`

```typescript
export async function getShoppingRun(
  householdId: string,
  runId: string
): Promise<ShoppingRun> {
  return apiFetch<ShoppingRun>(`/households/${householdId}/shopping-runs/${runId}`);
}

export async function updateShoppingRunItem(
  householdId: string,
  runId: string,
  itemId: string,
  purchased: boolean
): Promise<ShoppingRunItem> {
  return apiFetch<ShoppingRunItem>(
    `/households/${householdId}/shopping-runs/${runId}/items/${itemId}`,
    {
      method: 'PATCH',
      body: { purchased },
    }
  );
}

export async function closeShoppingRun(
  householdId: string,
  runId: string,
  status: 'COMPLETED' | 'CANCELLED'
): Promise<ShoppingRun> {
  return apiFetch<ShoppingRun>(
    `/households/${householdId}/shopping-runs/${runId}/close`,
    {
      method: 'POST',
      body: { status },
    }
  );
}
```

### Step 2: Create ShoppingRun page

**File:** `clients/web/src/routes/ShoppingRun.tsx`

Key components:
1. **Header**: Run title, back link, status badge (if closed)
2. **Progress**: "X of Y purchased" with progress bar
3. **Checklist**: Items with checkboxes (optimistic updates)
4. **Actions**: Complete Trip / Cancel Trip buttons (ACTIVE only)
5. **Modals**: Confirmation for complete/cancel
6. **Summary**: Shown after closure (or for closed runs)

State:
```typescript
const [run, setRun] = useState<ShoppingRun | null>(null);
const [isLoading, setIsLoading] = useState(true);
const [error, setError] = useState<Error | null>(null);
const [updatingItems, setUpdatingItems] = useState<Set<string>>(new Set());
const [showCompleteModal, setShowCompleteModal] = useState(false);
const [showCancelModal, setShowCancelModal] = useState(false);
const [isClosing, setIsClosing] = useState(false);
const [snackbar, setSnackbar] = useState<{...} | null>(null);
```

Optimistic update pattern:
```typescript
const handleToggleItem = async (itemId: string, currentPurchased: boolean) => {
  if (!run || run.status !== 'ACTIVE') return;

  // Optimistic update
  setRun(prev => ({
    ...prev!,
    items: prev!.items.map(item =>
      item.id === itemId ? { ...item, purchased: !currentPurchased } : item
    ),
    purchasedCount: currentPurchased ? prev!.purchasedCount - 1 : prev!.purchasedCount + 1,
  }));

  setUpdatingItems(prev => new Set(prev).add(itemId));

  try {
    await updateShoppingRunItem(householdId, runId, itemId, !currentPurchased);
  } catch {
    // Rollback
    setRun(prev => ({
      ...prev!,
      items: prev!.items.map(item =>
        item.id === itemId ? { ...item, purchased: currentPurchased } : item
      ),
      purchasedCount: currentPurchased ? prev!.purchasedCount : prev!.purchasedCount - 1,
    }));
    setSnackbar({ message: 'Failed to update item', variant: 'error' });
  } finally {
    setUpdatingItems(prev => {
      const next = new Set(prev);
      next.delete(itemId);
      return next;
    });
  }
};
```

### Step 3: Add route

**File:** `clients/web/src/routes/index.tsx`

Add import:
```typescript
import ShoppingRun from './ShoppingRun';
```

Add route (after shopping/:listId):
```typescript
{ path: 'shopping-runs/:runId', element: <ShoppingRun /> },
```

### Step 4: Create CSS

**File:** `clients/web/src/routes/ShoppingRun.css`

Key classes:
- `.shopping-run` - container
- `.shopping-run__header` - title + back + badge
- `.shopping-run__progress` - progress bar + text
- `.shopping-run__checklist` - items list
- `.shopping-run__item` - item row
- `.shopping-run__checkbox` - checkbox (similar to ShoppingDetail)
- `.shopping-run__actions` - complete/cancel buttons
- `.shopping-run__summary` - closure summary
- `.shopping-run__status-badge` - COMPLETED/CANCELLED badge

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web

npm run build
npm run lint
```

Manual test:
1. Create shopping run (from ShoppingDetail)
2. Verify redirect to run page
3. Check progress shows "0 of N purchased"
4. Click checkbox → optimistic update + progress changes
5. Click again → unchecks
6. Simulate network error → verify rollback
7. Click "Complete Trip" → confirm → summary shown
8. Refresh page → read-only view, badge visible
9. Start new run, click "Cancel" → confirm → redirect to list

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Checklist display + progress | Visual check |
| AC-2 | Mark purchased (optimistic) | Click checkbox, verify instant update |
| AC-3 | Unmark purchased | Click again, verify toggle |
| AC-4 | Complete Trip flow | Modal → confirm → summary |
| AC-5 | Cancel Trip flow | Modal → confirm → redirect |
| AC-6 | Closed run read-only | Refresh COMPLETED run → no checkboxes |
| AC-7 | Error recovery | Simulate error → rollback + snackbar |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Race conditions on rapid clicks | Low | Disable checkbox during update |
| Stale run data | Low | Refetch after close |

---

## Rollback

- Remove ShoppingRun route from index.tsx
- Delete ShoppingRun.tsx and ShoppingRun.css
- Remove API functions from api.ts

---

## References

- Endpoints:
  - `GET /households/{hid}/shopping-runs/{runId}`
  - `PATCH /households/{hid}/shopping-runs/{runId}/items/{itemId}`
  - `POST /households/{hid}/shopping-runs/{runId}/close`
- Types: `ShoppingRun`, `ShoppingRunItem` (already in api.ts from ST-1307)
