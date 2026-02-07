import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { closeShoppingRun, getShoppingRun, updateShoppingRunItem } from '../lib/api';
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
  const [snackbar, setSnackbar] = useState<{ message: string; variant: 'success' | 'error' } | null>(
    null
  );

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

  const handleToggleItem = useCallback(
    async (item: ShoppingRunItem) => {
      if (!householdId || !runId || !run || run.status !== 'ACTIVE') return;

      const newPurchased = !item.purchased;
      const previousItem = item;

      setRun((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          items: prev.items.map((i) =>
            i.id === item.id
              ? {
                  ...i,
                  purchased: newPurchased,
                  purchasedAt: newPurchased ? new Date().toISOString() : undefined,
                }
              : i
          ),
        };
      });

      setUpdatingItems((prev) => new Set(prev).add(item.id));

      try {
        const updatedItem = await updateShoppingRunItem(householdId, runId, item.id, newPurchased);
        setRun((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            items: prev.items.map((i) => (i.id === item.id ? { ...i, ...updatedItem } : i)),
          };
        });
      } catch {
        setRun((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            items: prev.items.map((i) => (i.id === item.id ? previousItem : i)),
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
  const purchasedCount = run.items.filter((item) => item.purchased).length;
  const totalCount = run.items.length;
  const skippedCount = totalCount - purchasedCount;
  const progressPercent = totalCount > 0 ? (purchasedCount / totalCount) * 100 : 0;

  return (
    <div className="shopping-run">
      <div className="shopping-run__wrapper">
        <Link to={`/households/${householdId}/shopping/${run.listId}`} className="shopping-run__back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Back to list
        </Link>

        <div className="shopping-run__header">
          <h1 className="shopping-run__title">Shopping Trip</h1>
          {!isActive && (
            <span className={`shopping-run__badge shopping-run__badge--${run.status.toLowerCase()}`}>
              {run.status === 'COMPLETED' ? 'Completed' : 'Cancelled'}
            </span>
          )}
        </div>

        <div className="shopping-run__progress-section">
          <div className="shopping-run__progress-text">
            <span className="shopping-run__progress-count">
              {purchasedCount} of {totalCount} purchased
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

        {!isActive && (
          <div className="shopping-run__summary">
            <div className="shopping-run__summary-stat">
              <span className="shopping-run__summary-value">{purchasedCount}</span>
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

        <Modal
          open={showCompleteModal}
          onClose={() => !isClosing && setShowCompleteModal(false)}
          title="Complete Shopping Trip"
          size="sm"
          closeOnBackdrop={!isClosing}
          closeOnEscape={!isClosing}
        >
          <div className="shopping-run__modal">
            <p className="shopping-run__modal-info">
              You purchased <strong>{purchasedCount}</strong> of{' '}
              <strong>{totalCount}</strong> items.
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

        <Modal
          open={showCancelModal}
          onClose={() => !isClosing && setShowCancelModal(false)}
          title="Cancel Shopping Trip"
          size="sm"
          closeOnBackdrop={!isClosing}
          closeOnEscape={!isClosing}
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

        {snackbar && (
          <Snackbar open onClose={() => setSnackbar(null)} variant={snackbar.variant}>
            {snackbar.message}
          </Snackbar>
        )}
      </div>
    </div>
  );
}
