# Codex APPLY: ST-1308 — ShoppingRun Checklist UI

## Context
Create ShoppingRun checklist page based on PLAN findings:
- Route placement: after `shopping/:listId`, add `shopping-runs/:runId`
- Page structure: follow TaskDetail pattern (skeleton loading, error card with retry)
- Checkbox styles: reuse `.shopping-detail__checkbox` pattern
- Optimistic update: follow useShoppingItems pattern (optimistic flip → rollback on error)
- Modal confirmation: follow DeleteRoutineModal pattern
- BEM prefix: `shopping-run__*`
- Status badge: create new, inspired by TaskDetail badges

## Files to Create/Modify

| File | Action |
|------|--------|
| `clients/web/src/lib/api.ts` | MODIFY — add getShoppingRun, updateRunItem, closeShoppingRun |
| `clients/web/src/routes/ShoppingRun.tsx` | CREATE — checklist page |
| `clients/web/src/routes/ShoppingRun.css` | CREATE — styles |
| `clients/web/src/routes/index.tsx` | MODIFY — add route |

---

## Step 1: Add API functions

**File:** `clients/web/src/lib/api.ts`

### 1a: Add ShoppingRunItem import (if not already imported)

Ensure `ShoppingRun` and `ShoppingRunItem` are imported from `../types/api`.

### 1b: Add functions after createShoppingRun (around line 213)

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

---

## Step 2: Create ShoppingRun page

**File:** `clients/web/src/routes/ShoppingRun.tsx`

```typescript
import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import {
  closeShoppingRun,
  getShoppingRun,
  updateShoppingRunItem,
} from '../lib/api';
import { Button, Modal, Snackbar } from '../components/ui';
import type { ShoppingRun as ShoppingRunType, ShoppingRunItem } from '../types/api';
import './ShoppingRun.css';

export default function ShoppingRun() {
  const { householdId } = useAuth();
  const { runId } = useParams();
  const navigate = useNavigate();

  const [run, setRun] = useState<ShoppingRunType | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [updatingItems, setUpdatingItems] = useState<Set<string>>(new Set());
  const [showCompleteModal, setShowCompleteModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [snackbar, setSnackbar] = useState<{ message: string; variant: 'success' | 'error' } | null>(null);

  // Fetch run data
  useEffect(() => {
    if (!householdId || !runId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    getShoppingRun(householdId, runId)
      .then((data) => setRun(data))
      .catch((err) => setError(err instanceof Error ? err : new Error('Failed to load run')))
      .finally(() => setIsLoading(false));
  }, [householdId, runId]);

  // Toggle item purchased status (optimistic)
  const handleToggleItem = useCallback(
    async (item: ShoppingRunItem) => {
      if (!householdId || !runId || !run || run.status !== 'ACTIVE') return;

      const newPurchased = !item.purchased;

      // Optimistic update
      setRun((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          items: prev.items.map((i) =>
            i.id === item.id ? { ...i, purchased: newPurchased } : i
          ),
          purchasedCount: newPurchased
            ? prev.purchasedCount + 1
            : prev.purchasedCount - 1,
        };
      });

      setUpdatingItems((prev) => new Set(prev).add(item.id));

      try {
        await updateShoppingRunItem(householdId, runId, item.id, newPurchased);
      } catch {
        // Rollback
        setRun((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            items: prev.items.map((i) =>
              i.id === item.id ? { ...i, purchased: item.purchased } : i
            ),
            purchasedCount: item.purchased
              ? prev.purchasedCount + 1
              : prev.purchasedCount - 1,
          };
        });
        setSnackbar({ message: 'Failed to update item', variant: 'error' });
      } finally {
        setUpdatingItems((prev) => {
          const next = new Set(prev);
          next.delete(item.id);
          return next;
        });
      }
    },
    [householdId, runId, run]
  );

  // Close run (complete or cancel)
  const handleCloseRun = useCallback(
    async (status: 'COMPLETED' | 'CANCELLED') => {
      if (!householdId || !runId) return;

      setIsClosing(true);

      try {
        const closedRun = await closeShoppingRun(householdId, runId, status);
        setRun(closedRun);
        setShowCompleteModal(false);
        setShowCancelModal(false);

        if (status === 'CANCELLED') {
          // Redirect to shopping list
          navigate(`/households/${householdId}/shopping/${closedRun.listId}`);
        }
      } catch {
        setSnackbar({ message: 'Failed to close trip', variant: 'error' });
      } finally {
        setIsClosing(false);
      }
    },
    [householdId, runId, navigate]
  );

  const handleRetry = useCallback(() => {
    if (!householdId || !runId) return;
    setIsLoading(true);
    setError(null);
    getShoppingRun(householdId, runId)
      .then((data) => setRun(data))
      .catch((err) => setError(err instanceof Error ? err : new Error('Failed to load run')))
      .finally(() => setIsLoading(false));
  }, [householdId, runId]);

  if (!householdId) {
    return (
      <div className="shopping-run">
        <div className="shopping-run__wrapper">
          <p>Please select a household.</p>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="shopping-run">
        <div className="shopping-run__wrapper">
          <div className="shopping-run__skeleton-back" />
          <div className="shopping-run__skeleton-header" />
          <div className="shopping-run__card">
            <div className="shopping-run__skeleton-progress" />
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="shopping-run__skeleton-item" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="shopping-run">
        <div className="shopping-run__wrapper">
          <Link to={`/households/${householdId}/shopping`} className="shopping-run__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to shopping
          </Link>
          <div className="shopping-run__card">
            <div className="shopping-run__error">
              <p>Unable to load shopping trip.</p>
              <Button variant="primary" size="sm" onClick={handleRetry}>
                Retry
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!run) {
    return (
      <div className="shopping-run">
        <div className="shopping-run__wrapper">
          <p>Shopping trip not found.</p>
        </div>
      </div>
    );
  }

  const isActive = run.status === 'ACTIVE';
  const skippedCount = run.totalCount - run.purchasedCount;
  const progressPercent = run.totalCount > 0 ? (run.purchasedCount / run.totalCount) * 100 : 0;

  return (
    <div className="shopping-run">
      <div className="shopping-run__wrapper">
        <Link to={`/households/${householdId}/shopping/${run.listId}`} className="shopping-run__back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Back to list
        </Link>

        {/* Header */}
        <div className="shopping-run__header">
          <h1 className="shopping-run__title">Shopping Trip</h1>
          {!isActive && (
            <span className={`shopping-run__badge shopping-run__badge--${run.status.toLowerCase()}`}>
              {run.status === 'COMPLETED' ? 'Completed' : 'Cancelled'}
            </span>
          )}
        </div>

        {/* Progress */}
        <div className="shopping-run__progress-section">
          <div className="shopping-run__progress-text">
            <span className="shopping-run__progress-count">
              {run.purchasedCount} of {run.totalCount} purchased
            </span>
            {!isActive && skippedCount > 0 && (
              <span className="shopping-run__progress-skipped">
                ({skippedCount} skipped)
              </span>
            )}
          </div>
          <div className="shopping-run__progress-bar">
            <div
              className="shopping-run__progress-fill"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>

        {/* Checklist */}
        <div className="shopping-run__card">
          {run.items.length === 0 ? (
            <div className="shopping-run__empty">No items in this trip.</div>
          ) : (
            <div className="shopping-run__checklist">
              {run.items.map((item, idx) => {
                const isUpdating = updatingItems.has(item.id);
                return (
                  <div key={item.id}>
                    {idx > 0 && <div className="shopping-run__divider" />}
                    <div
                      className={`shopping-run__item ${item.purchased ? 'shopping-run__item--purchased' : ''} ${isUpdating ? 'shopping-run__item--updating' : ''}`}
                    >
                      <button
                        type="button"
                        className={`shopping-run__checkbox ${item.purchased ? 'shopping-run__checkbox--checked' : ''}`}
                        onClick={() => handleToggleItem(item)}
                        disabled={!isActive || isUpdating}
                        aria-label={item.purchased ? 'Mark as not purchased' : 'Mark as purchased'}
                      >
                        {item.purchased && (
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                            <polyline points="20 6 9 17 4 12" />
                          </svg>
                        )}
                      </button>
                      <div className="shopping-run__item-info">
                        <span className={`shopping-run__item-name ${item.purchased ? 'shopping-run__item-name--purchased' : ''}`}>
                          {item.name}
                        </span>
                        {(item.quantity || item.unit) && (
                          <span className="shopping-run__item-meta">
                            {item.quantity && item.quantity > 1 ? `${item.quantity}` : ''}
                            {item.quantity && item.unit ? ' ' : ''}
                            {item.unit || ''}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Actions (only for active runs) */}
        {isActive && (
          <div className="shopping-run__actions">
            <Button
              variant="secondary"
              size="md"
              onClick={() => setShowCancelModal(true)}
            >
              Cancel Trip
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={() => setShowCompleteModal(true)}
            >
              Complete Trip
            </Button>
          </div>
        )}

        {/* Summary (for closed runs) */}
        {!isActive && (
          <div className="shopping-run__summary">
            <div className="shopping-run__summary-stat">
              <span className="shopping-run__summary-value">{run.purchasedCount}</span>
              <span className="shopping-run__summary-label">Purchased</span>
            </div>
            {skippedCount > 0 && (
              <div className="shopping-run__summary-stat">
                <span className="shopping-run__summary-value">{skippedCount}</span>
                <span className="shopping-run__summary-label">Skipped</span>
              </div>
            )}
          </div>
        )}

        {/* Complete Modal */}
        <Modal
          open={showCompleteModal}
          onClose={() => !isClosing && setShowCompleteModal(false)}
          title="Complete Shopping Trip"
          size="sm"
          closeOnBackdrop={!isClosing}
        >
          <div className="shopping-run__modal">
            <p className="shopping-run__modal-info">
              You purchased <strong>{run.purchasedCount}</strong> of{' '}
              <strong>{run.totalCount}</strong> items.
              {skippedCount > 0 && (
                <> <strong>{skippedCount}</strong> {skippedCount === 1 ? 'item' : 'items'} will be skipped.</>
              )}
            </p>
            <div className="shopping-run__modal-actions">
              <Button
                variant="secondary"
                size="md"
                onClick={() => setShowCompleteModal(false)}
                disabled={isClosing}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={() => handleCloseRun('COMPLETED')}
                disabled={isClosing}
              >
                {isClosing ? 'Completing...' : 'Complete'}
              </Button>
            </div>
          </div>
        </Modal>

        {/* Cancel Modal */}
        <Modal
          open={showCancelModal}
          onClose={() => !isClosing && setShowCancelModal(false)}
          title="Cancel Shopping Trip"
          size="sm"
          closeOnBackdrop={!isClosing}
        >
          <div className="shopping-run__modal">
            <p className="shopping-run__modal-info">
              Are you sure you want to cancel this trip? Your progress will be saved but the trip will be marked as cancelled.
            </p>
            <div className="shopping-run__modal-actions">
              <Button
                variant="secondary"
                size="md"
                onClick={() => setShowCancelModal(false)}
                disabled={isClosing}
              >
                Keep Shopping
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={() => handleCloseRun('CANCELLED')}
                disabled={isClosing}
              >
                {isClosing ? 'Cancelling...' : 'Cancel Trip'}
              </Button>
            </div>
          </div>
        </Modal>

        {/* Snackbar */}
        {snackbar && (
          <Snackbar open onClose={() => setSnackbar(null)} variant={snackbar.variant}>
            {snackbar.message}
          </Snackbar>
        )}
      </div>
    </div>
  );
}
```

---

## Step 3: Create CSS file

**File:** `clients/web/src/routes/ShoppingRun.css`

```css
.shopping-run {
  min-height: 100vh;
  background: var(--color-bg-secondary);
  padding: var(--spacing-4);
}

.shopping-run__wrapper {
  max-width: 600px;
  margin: 0 auto;
}

/* Back Link */
.shopping-run__back {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-4);
  transition: color var(--transition-fast);
}

.shopping-run__back:hover {
  color: var(--color-text-primary);
}

/* Header */
.shopping-run__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.shopping-run__title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.shopping-run__badge {
  display: inline-flex;
  align-items: center;
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
}

.shopping-run__badge--completed {
  background-color: var(--color-success-bg, rgba(34, 197, 94, 0.1));
  color: var(--color-success, #22c55e);
}

.shopping-run__badge--cancelled {
  background-color: var(--color-warning-bg, rgba(234, 179, 8, 0.1));
  color: var(--color-warning, #eab308);
}

/* Progress */
.shopping-run__progress-section {
  margin-bottom: var(--spacing-4);
}

.shopping-run__progress-text {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

.shopping-run__progress-count {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.shopping-run__progress-skipped {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.shopping-run__progress-bar {
  height: 8px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.shopping-run__progress-fill {
  height: 100%;
  background: var(--color-success, #22c55e);
  border-radius: var(--radius-full);
  transition: width var(--transition-normal);
}

/* Card */
.shopping-run__card {
  background: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* Checklist */
.shopping-run__checklist {
  padding: 0;
}

.shopping-run__divider {
  height: 1px;
  background: var(--color-border);
  margin: 0 var(--spacing-4);
}

.shopping-run__item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3) var(--spacing-4);
  transition: opacity var(--transition-fast);
}

.shopping-run__item--purchased {
  opacity: 0.6;
}

.shopping-run__item--updating {
  opacity: 0.5;
  pointer-events: none;
}

/* Checkbox */
.shopping-run__checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.shopping-run__checkbox:hover:not(:disabled) {
  border-color: var(--color-primary);
}

.shopping-run__checkbox--checked {
  background: var(--color-success, #22c55e);
  border-color: var(--color-success, #22c55e);
  color: white;
}

.shopping-run__checkbox:disabled {
  cursor: default;
  opacity: 0.5;
}

/* Item Info */
.shopping-run__item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--spacing-2);
}

.shopping-run__item-name {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.shopping-run__item-name--purchased {
  text-decoration: line-through;
  color: var(--color-text-muted);
}

.shopping-run__item-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* Empty State */
.shopping-run__empty {
  padding: var(--spacing-8);
  text-align: center;
  color: var(--color-text-muted);
}

/* Error State */
.shopping-run__error {
  padding: var(--spacing-8);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-4);
  color: var(--color-text-secondary);
}

/* Actions */
.shopping-run__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-3);
  margin-top: var(--spacing-4);
}

/* Summary */
.shopping-run__summary {
  display: flex;
  justify-content: center;
  gap: var(--spacing-8);
  margin-top: var(--spacing-4);
  padding: var(--spacing-4);
  background: var(--color-bg-primary);
  border-radius: var(--radius-lg);
}

.shopping-run__summary-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-1);
}

.shopping-run__summary-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.shopping-run__summary-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

/* Modal */
.shopping-run__modal {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.shopping-run__modal-info {
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.shopping-run__modal-info strong {
  color: var(--color-text-primary);
}

.shopping-run__modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-2);
}

/* Skeleton Loading */
.shopping-run__skeleton-back {
  width: 120px;
  height: 20px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-4);
}

.shopping-run__skeleton-header {
  width: 200px;
  height: 32px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-4);
}

.shopping-run__skeleton-progress {
  height: 40px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
  margin: var(--spacing-4);
}

.shopping-run__skeleton-item {
  height: 48px;
  background: var(--color-bg-tertiary);
  margin: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-sm);
}
```

---

## Step 4: Add route

**File:** `clients/web/src/routes/index.tsx`

### 4a: Add import

```typescript
import ShoppingRun from './ShoppingRun';
```

### 4b: Add route (after `shopping/:listId`, around line 55)

```typescript
{ path: 'shopping-runs/:runId', element: <ShoppingRun /> },
```

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web

npm run build
npm run lint
```

Manual test:
1. Start shopping trip (from ShoppingDetail) → redirected to run page
2. Verify progress bar shows "0 of N purchased"
3. Click checkbox → optimistic update → progress increments
4. Click again → toggle back
5. Click "Complete Trip" → modal → confirm → summary view + badge
6. Refresh → read-only view, checkboxes disabled
7. Start new trip → "Cancel Trip" → confirm → redirect to list
8. Simulate error (network off) → rollback + snackbar

---

## Constraints
- Use existing `Button`, `Modal`, `Snackbar` components
- Follow BEM naming: `shopping-run__*`
- Checkbox styles similar to ShoppingDetail
- Optimistic updates with rollback on error
- Read-only mode when `status !== 'ACTIVE'`
