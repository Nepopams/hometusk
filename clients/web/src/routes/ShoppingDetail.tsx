import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useShoppingItems } from '../hooks/useShoppingItems';
import { getShoppingList } from '../lib/api';
import { ApiError } from '../lib/errors';
import { Button } from '../components/ui';
import type { ShoppingItem, ShoppingList } from '../types/api';
import './ShoppingDetail.css';

/**
 * Shopping Detail page for viewing and managing items in a shopping list.
 *
 * States:
 * - Loading: skeleton placeholders
 * - Not Found (404): list doesn't exist or no access
 * - Error: general error with retry
 * - Empty: no items yet with add form
 * - Normal: items list with add form, checkboxes, delete buttons
 *
 * Features:
 * - Optimistic UI for add/toggle/delete operations
 * - Disabled state while saving to prevent double-clicks
 * - Purchased items shown with 60% opacity and strikethrough
 *
 * @see Pencil frames: dataShopping (shopping item interface)
 */
export default function ShoppingDetail() {
  const { householdId } = useAuth();
  const { listId } = useParams();

  const [list, setList] = useState<ShoppingList | null>(null);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState<Error | null>(null);

  const {
    items,
    isLoading: itemsLoading,
    error: itemsError,
    refetch,
    addItem,
    togglePurchased,
    removeItem,
    isSaving,
    savingItemIds,
  } = useShoppingItems({ householdId, listId });

  const [newItemName, setNewItemName] = useState('');
  const [addError, setAddError] = useState<string | null>(null);

  // Fetch list info
  useEffect(() => {
    if (!householdId || !listId) {
      setList(null);
      setListLoading(false);
      return;
    }

    setListLoading(true);
    setListError(null);

    getShoppingList(householdId, listId)
      .then((data) => setList(data))
      .catch((e) => setListError(e instanceof Error ? e : new Error('Failed to load list')))
      .finally(() => setListLoading(false));
  }, [householdId, listId]);

  const handleAddItem = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      const name = newItemName.trim();
      if (!name) return;

      setAddError(null);
      try {
        await addItem({ name });
        setNewItemName('');
      } catch (err) {
        setAddError(err instanceof Error ? err.message : 'Failed to add item');
      }
    },
    [newItemName, addItem]
  );

  const handleToggle = useCallback(
    async (itemId: string) => {
      try {
        await togglePurchased(itemId);
      } catch {
        // Error handled silently - item will rollback
      }
    },
    [togglePurchased]
  );

  const handleDelete = useCallback(
    async (itemId: string) => {
      try {
        await removeItem(itemId);
      } catch {
        // Error handled silently - item will rollback
      }
    },
    [removeItem]
  );

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  if (!householdId) {
    return (
      <div className="shopping-detail">
        <div className="shopping-detail__wrapper">
          <div className="shopping-detail__empty-page">
            <p>Please select a household to view shopping items.</p>
          </div>
        </div>
      </div>
    );
  }

  const isLoading = listLoading || itemsLoading;
  const error = listError || itemsError;

  // Loading state
  if (isLoading) {
    return (
      <div className="shopping-detail">
        <div className="shopping-detail__wrapper">
          <Link to={`/households/${householdId}/shopping`} className="shopping-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to shopping lists
          </Link>

          <div className="shopping-detail__skeleton-header">
            <div className="shopping-detail__skeleton-title" />
            <div className="shopping-detail__skeleton-desc" />
          </div>

          <div className="shopping-detail__card">
            <div className="shopping-detail__add-row">
              <div className="shopping-detail__skeleton-input" />
            </div>
            {[1, 2, 3, 4].map((i, idx) => (
              <div key={i}>
                {idx > 0 && <div className="shopping-detail__divider" />}
                <div className="shopping-detail__skeleton-item">
                  <div className="shopping-detail__skeleton-checkbox" />
                  <div className="shopping-detail__skeleton-name" />
                  <div className="shopping-detail__skeleton-action" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Not found state (404)
  if (error instanceof ApiError && (error.status === 404 || error.status === 403)) {
    return (
      <div className="shopping-detail">
        <div className="shopping-detail__wrapper">
          <Link to={`/households/${householdId}/shopping`} className="shopping-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to shopping lists
          </Link>

          <div className="shopping-detail__card">
            <div className="shopping-detail__not-found">
              <svg
                className="shopping-detail__not-found-icon"
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="12" cy="12" r="10" />
                <path d="M16 16s-1.5-2-4-2-4 2-4 2" />
                <line x1="9" y1="9" x2="9.01" y2="9" />
                <line x1="15" y1="9" x2="15.01" y2="9" />
              </svg>
              <h3 className="shopping-detail__not-found-title">Shopping list not found</h3>
              <p className="shopping-detail__not-found-desc">
                This list may have been deleted or you don&apos;t have access to it.
              </p>
              <Link to={`/households/${householdId}/shopping`}>
                <Button variant="primary" size="md">
                  Go to shopping lists
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="shopping-detail">
        <div className="shopping-detail__wrapper">
          <Link to={`/households/${householdId}/shopping`} className="shopping-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to shopping lists
          </Link>

          <div className="shopping-detail__card">
            <div className="shopping-detail__error">
              <svg
                className="shopping-detail__error-icon"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
              <div className="shopping-detail__error-content">
                <h3 className="shopping-detail__error-title">Unable to load shopping items</h3>
                <p className="shopping-detail__error-message">Check your connection and try again.</p>
              </div>
              <Button variant="primary" size="sm" onClick={handleRetry}>
                Retry
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const unpurchasedItems = items.filter((item) => !item.purchased);
  const purchasedItems = items.filter((item) => item.purchased);

  const renderItem = (item: ShoppingItem) => {
    const isItemSaving = savingItemIds.has(item.id);
    const isTemp = item.id.startsWith('temp-');

    return (
      <div
        key={item.id}
        className={`shopping-detail__item ${item.purchased ? 'shopping-detail__item--purchased' : ''} ${isItemSaving || isTemp ? 'shopping-detail__item--saving' : ''}`}
      >
        <button
          type="button"
          className={`shopping-detail__checkbox ${item.purchased ? 'shopping-detail__checkbox--checked' : ''}`}
          onClick={() => handleToggle(item.id)}
          disabled={isItemSaving || isTemp}
          aria-label={item.purchased ? 'Mark as not purchased' : 'Mark as purchased'}
        >
          {item.purchased && (
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          )}
        </button>

        <div className="shopping-detail__item-info">
          <span className={`shopping-detail__item-name ${item.purchased ? 'shopping-detail__item-name--purchased' : ''}`}>
            {item.name}
          </span>
          {(item.quantity || item.unit) && (
            <span className="shopping-detail__item-meta">
              {item.quantity && item.quantity > 1 ? `${item.quantity}` : ''}
              {item.quantity && item.unit ? ' ' : ''}
              {item.unit || ''}
            </span>
          )}
          {item.linkedTaskId && (
            <Link
              to={`/tasks/${item.linkedTaskId}`}
              className="shopping-detail__task-link"
              onClick={(e) => e.stopPropagation()}
            >
              For task
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </Link>
          )}
        </div>

        <button
          type="button"
          className="shopping-detail__delete-btn"
          onClick={() => handleDelete(item.id)}
          disabled={isItemSaving || isTemp}
          aria-label="Delete item"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
        </button>
      </div>
    );
  };

  return (
    <div className="shopping-detail">
      <div className="shopping-detail__wrapper">
        <Link to={`/households/${householdId}/shopping`} className="shopping-detail__back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Back to shopping lists
        </Link>

        {/* Header */}
        <div className="shopping-detail__header">
          <h1 className="shopping-detail__title">{list?.name || 'Shopping List'}</h1>
          <span className="shopping-detail__count">
            {unpurchasedItems.length === 0
              ? 'All done!'
              : unpurchasedItems.length === 1
                ? '1 item to buy'
                : `${unpurchasedItems.length} items to buy`}
          </span>
        </div>

        {/* Main Card */}
        <div className="shopping-detail__card">
          {/* Add Item Form */}
          <form className="shopping-detail__add-row" onSubmit={handleAddItem}>
            <input
              type="text"
              className="shopping-detail__add-input"
              placeholder="Add item..."
              value={newItemName}
              onChange={(e) => setNewItemName(e.target.value)}
              disabled={isSaving}
            />
            <button
              type="submit"
              className="shopping-detail__add-btn"
              disabled={!newItemName.trim() || isSaving}
              aria-label="Add item"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
          </form>

          {addError && (
            <div className="shopping-detail__add-error">
              {addError}
            </div>
          )}

          {/* Empty state */}
          {items.length === 0 && (
            <div className="shopping-detail__empty">
              <svg
                className="shopping-detail__empty-icon"
                width="40"
                height="40"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
              <h3 className="shopping-detail__empty-title">Your list is empty</h3>
              <p className="shopping-detail__empty-desc">Type an item name above and press Enter to add</p>
            </div>
          )}

          {/* Unpurchased items */}
          {unpurchasedItems.length > 0 && (
            <div className="shopping-detail__section">
              {unpurchasedItems.map((item, idx) => (
                <div key={item.id}>
                  {idx > 0 && <div className="shopping-detail__divider" />}
                  {renderItem(item)}
                </div>
              ))}
            </div>
          )}

          {/* Purchased items */}
          {purchasedItems.length > 0 && (
            <>
              {unpurchasedItems.length > 0 && (
                <div className="shopping-detail__section-divider">
                  <span className="shopping-detail__section-label">Purchased ({purchasedItems.length})</span>
                </div>
              )}
              <div className="shopping-detail__section shopping-detail__section--purchased">
                {purchasedItems.map((item, idx) => (
                  <div key={item.id}>
                    {idx > 0 && <div className="shopping-detail__divider" />}
                    {renderItem(item)}
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
