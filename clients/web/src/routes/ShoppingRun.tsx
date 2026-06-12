import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n';
import { closeShoppingRun, getShoppingRun, updateShoppingRunItem } from '../lib/api';
import { Button, Modal, Snackbar } from '../components/ui';
import type { ShoppingRun as ShoppingRunType, ShoppingRunItem } from '../types/api';
import './ShoppingRun.css';

export default function ShoppingRun() {
  const { t } = useI18n();
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
      .catch((err) => setError(err instanceof Error ? err : new Error(t('shopping.failedLoadRun'))))
      .finally(() => setIsLoading(false));
  }, [householdId, runId, t]);

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
        setSnackbar({ message: t('shopping.failedUpdateItem'), variant: 'error' });
      } finally {
        setUpdatingItems((prev) => {
          const next = new Set(prev);
          next.delete(item.id);
          return next;
        });
      }
    },
    [householdId, runId, run, t]
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
        setSnackbar({ message: t('shopping.failedCloseTrip'), variant: 'error' });
      } finally {
        setIsClosing(false);
      }
    },
    [householdId, runId, navigate, t]
  );

  const handleRetry = useCallback(() => {
    if (!householdId || !runId) return;
    setIsLoading(true);
    setError(null);
    getShoppingRun(householdId, runId)
      .then((data) => setRun(data))
      .catch((err) => setError(err instanceof Error ? err : new Error(t('shopping.failedLoadRun'))))
      .finally(() => setIsLoading(false));
  }, [householdId, runId, t]);

  if (!householdId) {
    return (
      <div className="shopping-run">
        <div className="shopping-run__wrapper">
          <p>{t('shopping.noHousehold')}</p>
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
            {t('shopping.backToShopping')}
          </Link>
          <div className="shopping-run__card">
            <div className="shopping-run__error">
              <p>{t('shopping.unableLoadTrip')}</p>
              <Button variant="primary" size="sm" onClick={handleRetry}>
                {t('common.retry')}
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
          <p>{t('shopping.tripNotFound')}</p>
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
          {t('shopping.backToList')}
        </Link>

        <div className="shopping-run__header">
          <h1 className="shopping-run__title">{t('shopping.trip')}</h1>
          {!isActive && (
            <span className={`shopping-run__badge shopping-run__badge--${run.status.toLowerCase()}`}>
              {run.status === 'COMPLETED' ? t('common.completed') : t('common.cancelled')}
            </span>
          )}
        </div>

        <div className="shopping-run__progress-section">
          <div className="shopping-run__progress-text">
            <span className="shopping-run__progress-count">
              {t('shopping.purchasedOf', { purchased: purchasedCount, total: totalCount })}
            </span>
            {!isActive && skippedCount > 0 && (
              <span className="shopping-run__progress-skipped">
                {t('shopping.skipped', { count: skippedCount })}
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
            <div className="shopping-run__empty">{t('shopping.noItemsTrip')}</div>
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
                        aria-label={item.purchased ? t('shopping.markNotPurchased') : t('shopping.markPurchased')}
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
              {t('shopping.cancelTrip')}
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={() => setShowCompleteModal(true)}
            >
              {t('shopping.completeTrip')}
            </Button>
          </div>
        )}

        {!isActive && (
          <div className="shopping-run__summary">
            <div className="shopping-run__summary-stat">
              <span className="shopping-run__summary-value">{purchasedCount}</span>
              <span className="shopping-run__summary-label">{t('shopping.purchased')}</span>
            </div>
            {skippedCount > 0 && (
              <div className="shopping-run__summary-stat">
                <span className="shopping-run__summary-value">{skippedCount}</span>
                <span className="shopping-run__summary-label">{t('shopping.skippedLabel')}</span>
              </div>
            )}
          </div>
        )}

        <Modal
          open={showCompleteModal}
          onClose={() => !isClosing && setShowCompleteModal(false)}
          title={t('shopping.completeTripTitle')}
          size="sm"
          closeOnBackdrop={!isClosing}
          closeOnEscape={!isClosing}
        >
          <div className="shopping-run__modal">
            <p className="shopping-run__modal-info">
              {t('shopping.completeTripInfo', { purchased: purchasedCount, total: totalCount })}
              {skippedCount > 0 && (
                <>
                  {' '}
                  {t('shopping.skippedWillBe', {
                    count: skippedCount,
                    itemLabel: skippedCount === 1 ? t('shopping.item') : t('shopping.items'),
                  })}
                </>
              )}
            </p>
            <div className="shopping-run__modal-actions">
              <Button
                variant="secondary"
                size="md"
                onClick={() => setShowCompleteModal(false)}
                disabled={isClosing}
              >
                {t('common.cancel')}
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={() => handleCloseRun('COMPLETED')}
                disabled={isClosing}
              >
                {isClosing ? t('shopping.completing') : t('shopping.complete')}
              </Button>
            </div>
          </div>
        </Modal>

        <Modal
          open={showCancelModal}
          onClose={() => !isClosing && setShowCancelModal(false)}
          title={t('shopping.cancelTripTitle')}
          size="sm"
          closeOnBackdrop={!isClosing}
          closeOnEscape={!isClosing}
        >
          <div className="shopping-run__modal">
            <p className="shopping-run__modal-info">
              {t('shopping.cancelTripInfo')}
            </p>
            <div className="shopping-run__modal-actions">
              <Button
                variant="secondary"
                size="md"
                onClick={() => setShowCancelModal(false)}
                disabled={isClosing}
              >
                {t('shopping.keepShopping')}
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={() => handleCloseRun('CANCELLED')}
                disabled={isClosing}
              >
                {isClosing ? t('shopping.cancelling') : t('shopping.cancelTrip')}
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
