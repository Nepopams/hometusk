import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useMarketplaceTemplates } from '../hooks/useMarketplaceTemplates';
import { useShoppingItems } from '../hooks/useShoppingItems';
import { useTasks } from '../hooks/useTasks';
import { useI18n } from '../i18n';
import {
  SHOPPING_ITEM_CATEGORIES,
  buildAddShoppingItemPayload,
  buildShoppingItemDetailsUpdate,
  groupShoppingItems,
  normalizeShoppingSource,
  type ShoppingGroupMode,
  type ShoppingItemGroup,
} from '../lib/shoppingMetadata';
import { createShoppingRun, exportShoppingList, getShoppingList } from '../lib/api';
import { ApiError } from '../lib/errors';
import { buildMarketplaceUrl } from '../lib/marketplaceUrl';
import { Button, Modal, Snackbar } from '../components/ui';
import type { ShoppingItem, ShoppingItemCategory, ShoppingList } from '../types/api';
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
  const { t } = useI18n();
  const { householdId } = useAuth();
  const { listId } = useParams();
  const navigate = useNavigate();

  const [list, setList] = useState<ShoppingList | null>(null);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState<Error | null>(null);

  const [newItemName, setNewItemName] = useState('');
  const [newItemQuantity, setNewItemQuantity] = useState('');
  const [newItemUnit, setNewItemUnit] = useState('');
  const [newItemCategory, setNewItemCategory] = useState<ShoppingItemCategory | ''>('');
  const [newItemSource, setNewItemSource] = useState('');
  const [newItemLinkedTaskId, setNewItemLinkedTaskId] = useState('');
  const [showAddDetails, setShowAddDetails] = useState(false);
  const [groupMode, setGroupMode] = useState<ShoppingGroupMode>('none');
  const [filterCategory, setFilterCategory] = useState<ShoppingItemCategory | ''>('');
  const [filterSource, setFilterSource] = useState('');
  const [editingItem, setEditingItem] = useState<ShoppingItem | null>(null);
  const [editCategory, setEditCategory] = useState<ShoppingItemCategory | ''>('');
  const [editSource, setEditSource] = useState('');
  const [editLinkedTaskId, setEditLinkedTaskId] = useState('');
  const [editError, setEditError] = useState<string | null>(null);
  const [addError, setAddError] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{ message: string; variant: 'success' | 'error' } | null>(
    null
  );
  const [showStartModal, setShowStartModal] = useState(false);
  const [isCreatingRun, setIsCreatingRun] = useState(false);
  const [createRunError, setCreateRunError] = useState<string | null>(null);

  const normalizedFilterSource = normalizeShoppingSource(filterSource) ?? undefined;

  const {
    items,
    isLoading: itemsLoading,
    error: itemsError,
    refetch,
    addItem,
    togglePurchased,
    updateMetadata,
    removeItem,
    isSaving,
    savingItemIds,
  } = useShoppingItems({
    householdId,
    listId,
    filters: {
      category: filterCategory || undefined,
      source: normalizedFilterSource,
    },
  });

  const { templates: marketplaceTemplates } = useMarketplaceTemplates();
  const { tasks } = useTasks(householdId, {});

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
      .catch((e) => setListError(e instanceof Error ? e : new Error(t('shopping.failedLoadList'))))
      .finally(() => setListLoading(false));
  }, [householdId, listId, t]);

  const handleAddItem = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      const name = newItemName.trim();
      if (!name) return;

      setAddError(null);
      try {
        await addItem(
          buildAddShoppingItemPayload(
            name,
            parseQuantity(newItemQuantity),
            newItemUnit,
            newItemCategory,
            newItemSource,
            newItemLinkedTaskId || null
          )
        );
        setNewItemName('');
        setNewItemQuantity('');
        setNewItemUnit('');
        setNewItemCategory('');
        setNewItemSource('');
        setNewItemLinkedTaskId('');
      } catch (err) {
        setAddError(err instanceof Error ? err.message : t('shopping.failedAddItem'));
      }
    },
    [
      newItemName,
      newItemQuantity,
      newItemUnit,
      newItemCategory,
      newItemSource,
      newItemLinkedTaskId,
      addItem,
      t,
    ]
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

  const handleOpenMetadataModal = useCallback((item: ShoppingItem) => {
    setEditingItem(item);
    setEditCategory(item.category ?? '');
    setEditSource(item.source ?? '');
    setEditLinkedTaskId(item.linkedTaskId ?? '');
    setEditError(null);
  }, []);

  const isEditingSaving = Boolean(editingItem && savingItemIds.has(editingItem.id));

  const handleCloseMetadataModal = useCallback(() => {
    if (!isEditingSaving) {
      setEditingItem(null);
      setEditError(null);
    }
  }, [isEditingSaving]);

  const handleSaveMetadata = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      if (!editingItem) return;

      setEditError(null);
      try {
        const updated = await updateMetadata(
          editingItem.id,
          buildShoppingItemDetailsUpdate(editCategory, editSource, editLinkedTaskId || null)
        );
        if (!updated) {
          setEditError(t('shopping.failedUpdateItem'));
          return;
        }
        setEditingItem(null);
        setSnackbar({ message: t('shopping.itemUpdated'), variant: 'success' });
      } catch (err) {
        setEditError(err instanceof Error ? err.message : t('shopping.failedUpdateItem'));
        setSnackbar({ message: t('shopping.failedUpdateItem'), variant: 'error' });
      }
    },
    [editCategory, editSource, editLinkedTaskId, editingItem, t, updateMetadata]
  );

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  const handleShare = useCallback(async () => {
    if (!householdId || !listId) return;
    try {
      const text = await exportShoppingList(householdId, listId, 'text');
      if (!navigator.clipboard) {
        throw new Error(t('invite.copyUnsupported'));
      }
      await navigator.clipboard.writeText(text);
      setSnackbar({ message: t('shopping.listCopied'), variant: 'success' });
    } catch {
      setSnackbar({ message: t('shopping.failedCopyList'), variant: 'error' });
    }
  }, [householdId, listId, t]);

  const handleExportCsv = useCallback(async () => {
    if (!householdId || !listId) return;
    try {
      const csv = await exportShoppingList(householdId, listId, 'csv');
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `shopping-list-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch {
      setSnackbar({ message: t('shopping.failedExport'), variant: 'error' });
    }
  }, [householdId, listId, t]);

  const handleStartTrip = useCallback(async () => {
    if (!householdId || !listId) return;

    setIsCreatingRun(true);
    setCreateRunError(null);

    try {
      const run = await createShoppingRun(householdId, listId);
      setShowStartModal(false);
      navigate(`/households/${householdId}/shopping-runs/${run.id}`);
    } catch (err) {
      setCreateRunError(err instanceof Error ? err.message : t('shopping.failedStartTrip'));
    } finally {
      setIsCreatingRun(false);
    }
  }, [householdId, listId, navigate, t]);

  const handleOpenStartModal = useCallback(() => {
    setCreateRunError(null);
    setShowStartModal(true);
  }, []);

  const handleCloseStartModal = useCallback(() => {
    if (!isCreatingRun) {
      setShowStartModal(false);
    }
  }, [isCreatingRun]);

  const handleClearFilters = useCallback(() => {
    setFilterCategory('');
    setFilterSource('');
  }, []);

  if (!householdId) {
    return (
      <div className="shopping-detail">
        <div className="shopping-detail__wrapper">
          <div className="shopping-detail__empty-page">
            <p>{t('shopping.noHouseholdItems')}</p>
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
            {t('shopping.backToLists')}
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
            {t('shopping.backToLists')}
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
              <h3 className="shopping-detail__not-found-title">{t('shopping.listNotFound')}</h3>
              <p className="shopping-detail__not-found-desc">
                {t('shopping.listNotFoundDesc')}
              </p>
              <Link to={`/households/${householdId}/shopping`}>
                <Button variant="primary" size="md">
                  {t('shopping.goToLists')}
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
            {t('shopping.backToLists')}
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
                <h3 className="shopping-detail__error-title">{t('shopping.unableLoadItems')}</h3>
                <p className="shopping-detail__error-message">{t('common.checkConnection')}</p>
              </div>
              <Button variant="primary" size="sm" onClick={handleRetry}>
                {t('common.retry')}
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const unpurchasedItems = items.filter((item) => !item.purchased);
  const purchasedItems = items.filter((item) => item.purchased);
  const hasActiveFilters = Boolean(filterCategory || normalizedFilterSource);
  const unpurchasedCount = hasActiveFilters
    ? Math.max(list?.unpurchasedCount ?? 0, unpurchasedItems.length)
    : unpurchasedItems.length;
  const addDetailsActive =
    showAddDetails || Boolean(newItemCategory || newItemSource.trim() || newItemLinkedTaskId);
  const taskTitleById = new Map(tasks.map((task) => [task.id, task.title]));

  const renderItem = (item: ShoppingItem) => {
    const isItemSaving = savingItemIds.has(item.id);
    const isTemp = item.id.startsWith('temp-');
    const quantityMeta = getQuantityMeta(item);

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
          aria-label={item.purchased ? t('shopping.markNotPurchased') : t('shopping.markPurchased')}
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
          {quantityMeta && <span className="shopping-detail__item-meta">{quantityMeta}</span>}
          {(item.category || item.source) && (
            <div className="shopping-detail__item-badges">
              {item.category && (
                <span className="shopping-detail__badge shopping-detail__badge--category">
                  {getCategoryLabel(item.category, t)}
                </span>
              )}
              {item.source && (
                <span className="shopping-detail__badge shopping-detail__badge--source">
                  {item.source}
                </span>
              )}
            </div>
          )}
          <div className="shopping-detail__item-links">
            {item.linkedTaskId && (
              <Link
                to={`/households/${householdId}/tasks/${item.linkedTaskId}`}
                className="shopping-detail__task-link"
                onClick={(e) => e.stopPropagation()}
              >
                {taskTitleById.get(item.linkedTaskId) ?? t('shopping.forTask')}
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
              </Link>
            )}
            {marketplaceTemplates.length > 0 && (
              <div className="shopping-detail__item-marketplaces">
                {marketplaceTemplates.map((mp) => (
                  <a
                    key={mp.id}
                    href={buildMarketplaceUrl(mp.urlTemplate, item.name)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="shopping-detail__marketplace-link"
                    aria-label={t('shopping.searchOn', { item: item.name, market: mp.name })}
                    title={mp.name}
                    onClick={(e) => e.stopPropagation()}
                  >
                    {mp.iconUrl ? (
                      <img src={mp.iconUrl} alt="" width="14" height="14" />
                    ) : (
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                        <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                        <polyline points="15 3 21 3 21 9" />
                        <line x1="10" y1="14" x2="21" y2="3" />
                      </svg>
                    )}
                  </a>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="shopping-detail__item-actions">
          <button
            type="button"
            className="shopping-detail__metadata-btn"
            onClick={() => handleOpenMetadataModal(item)}
            disabled={isItemSaving || isTemp}
            aria-label={t('shopping.editMetadataFor', { item: item.name })}
            title={t('shopping.editMetadata')}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20.59 13.41 11 3.83a2 2 0 0 0-1.41-.59H4a2 2 0 0 0-2 2v5.59a2 2 0 0 0 .59 1.41l9.59 9.59a2 2 0 0 0 2.83 0l5.59-5.59a2 2 0 0 0 0-2.83Z" />
              <circle cx="7.5" cy="7.5" r="1.5" />
            </svg>
          </button>
          <button
            type="button"
            className="shopping-detail__delete-btn"
            onClick={() => handleDelete(item.id)}
            disabled={isItemSaving || isTemp}
            aria-label={t('shopping.deleteItem')}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
      </div>
    );
  };

  const renderGroupedItems = (sectionItems: ShoppingItem[]) =>
    groupShoppingItems(sectionItems, groupMode).map((group) => (
      <div key={group.key} className="shopping-detail__group">
        {groupMode !== 'none' && (
          <div className="shopping-detail__group-header">
            <span className="shopping-detail__group-title">
              {getGroupLabel(group, groupMode, t)}
            </span>
            <span className="shopping-detail__group-count">
              {group.items.length}
            </span>
          </div>
        )}
        {group.items.map((item, idx) => (
          <div key={item.id}>
            {idx > 0 && <div className="shopping-detail__divider" />}
            {renderItem(item)}
          </div>
        ))}
      </div>
    ));

  return (
    <div className="shopping-detail">
      <div className="shopping-detail__wrapper">
        <Link to={`/households/${householdId}/shopping`} className="shopping-detail__back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          {t('shopping.backToLists')}
        </Link>

        {/* Header */}
        <div className="shopping-detail__header">
          <h1 className="shopping-detail__title">{list?.name || t('shopping.listFallback')}</h1>
          <div className="shopping-detail__header-actions">
            <span className="shopping-detail__count">
              {getItemsToBuyLabel(unpurchasedCount, t)}
            </span>
            <button
              type="button"
              className="ghost-button shopping-detail__header-btn"
              onClick={handleShare}
              aria-label={t('shopping.copyClipboard')}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
                <polyline points="16 6 12 2 8 6" />
                <line x1="12" y1="2" x2="12" y2="15" />
              </svg>
              {t('shopping.share')}
            </button>
            <button
              type="button"
              className="ghost-button shopping-detail__header-btn"
              onClick={handleExportCsv}
              aria-label={t('shopping.exportCsv')}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              {t('common.export')}
            </button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleOpenStartModal}
              disabled={unpurchasedCount === 0}
              aria-label={t('shopping.startTrip')}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
              {t('shopping.startTrip')}
            </Button>
          </div>
        </div>

        {/* Main Card */}
        <div className="shopping-detail__card">
          {/* Add Item Form */}
          <form className="shopping-detail__add-form" onSubmit={handleAddItem}>
            <div className="shopping-detail__add-row">
              <input
                type="text"
                className="shopping-detail__add-input"
                placeholder={t('shopping.addPlaceholder')}
                value={newItemName}
                onChange={(e) => setNewItemName(e.target.value)}
                disabled={isSaving}
              />
              <input
                type="number"
                className="shopping-detail__add-quantity"
                min={1}
                max={999}
                inputMode="numeric"
                placeholder={t('shopping.quantityShort')}
                value={newItemQuantity}
                onChange={(e) => setNewItemQuantity(e.target.value)}
                disabled={isSaving}
                aria-label={t('shopping.quantity')}
              />
              <input
                type="text"
                className="shopping-detail__add-unit"
                maxLength={50}
                placeholder={t('shopping.unitShort')}
                value={newItemUnit}
                onChange={(e) => setNewItemUnit(e.target.value)}
                disabled={isSaving}
                aria-label={t('shopping.unit')}
              />
              <button
                type="button"
                className={`shopping-detail__details-btn ${addDetailsActive ? 'shopping-detail__details-btn--active' : ''}`}
                onClick={() => setShowAddDetails((value) => !value)}
                disabled={isSaving}
                aria-label={t('shopping.addDetails')}
                title={t('shopping.addDetails')}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20.59 13.41 11 3.83a2 2 0 0 0-1.41-.59H4a2 2 0 0 0-2 2v5.59a2 2 0 0 0 .59 1.41l9.59 9.59a2 2 0 0 0 2.83 0l5.59-5.59a2 2 0 0 0 0-2.83Z" />
                  <circle cx="7.5" cy="7.5" r="1.5" />
                </svg>
              </button>
              <button
                type="submit"
                className="shopping-detail__add-btn"
                disabled={!newItemName.trim() || isSaving}
                aria-label={t('common.addItem')}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              </button>
            </div>

            {addDetailsActive && (
              <div className="shopping-detail__add-details">
                <label className="shopping-detail__field">
                  <span className="shopping-detail__field-label">{t('shopping.category')}</span>
                  <select
                    className="shopping-detail__select"
                    value={newItemCategory}
                    onChange={(e) => setNewItemCategory(e.target.value as ShoppingItemCategory | '')}
                    disabled={isSaving}
                  >
                    <option value="">{t('shopping.noCategory')}</option>
                    {SHOPPING_ITEM_CATEGORIES.map((category) => (
                      <option key={category} value={category}>
                        {getCategoryLabel(category, t)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="shopping-detail__field">
                  <span className="shopping-detail__field-label">{t('shopping.source')}</span>
                  <input
                    className="shopping-detail__text-input"
                    value={newItemSource}
                    maxLength={120}
                    onChange={(e) => setNewItemSource(e.target.value)}
                    placeholder={t('shopping.sourcePlaceholder')}
                    disabled={isSaving}
                  />
                </label>
                <label className="shopping-detail__field shopping-detail__field--task">
                  <span className="shopping-detail__field-label">{t('shopping.linkedTask')}</span>
                  <select
                    className="shopping-detail__select"
                    value={newItemLinkedTaskId}
                    onChange={(e) => setNewItemLinkedTaskId(e.target.value)}
                    disabled={isSaving}
                  >
                    <option value="">{t('shopping.noTask')}</option>
                    {tasks.map((task) => (
                      <option key={task.id} value={task.id}>
                        {task.title}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            )}
          </form>

          {addError && (
            <div className="shopping-detail__add-error">
              {addError}
            </div>
          )}

          <div className="shopping-detail__controls">
            <label className="shopping-detail__field">
              <span className="shopping-detail__field-label">{t('shopping.groupBy')}</span>
              <select
                className="shopping-detail__select"
                value={groupMode}
                onChange={(e) => setGroupMode(e.target.value as ShoppingGroupMode)}
              >
                <option value="none">{t('shopping.noGrouping')}</option>
                <option value="category">{t('shopping.groupByCategory')}</option>
                <option value="source">{t('shopping.groupBySource')}</option>
              </select>
            </label>
            <label className="shopping-detail__field">
              <span className="shopping-detail__field-label">{t('shopping.filterCategory')}</span>
              <select
                className="shopping-detail__select"
                value={filterCategory}
                onChange={(e) => setFilterCategory(e.target.value as ShoppingItemCategory | '')}
              >
                <option value="">{t('shopping.allCategories')}</option>
                {SHOPPING_ITEM_CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {getCategoryLabel(category, t)}
                  </option>
                ))}
              </select>
            </label>
            <label className="shopping-detail__field shopping-detail__field--source-filter">
              <span className="shopping-detail__field-label">{t('shopping.filterSource')}</span>
              <input
                className="shopping-detail__text-input"
                value={filterSource}
                maxLength={120}
                onChange={(e) => setFilterSource(e.target.value)}
                placeholder={t('shopping.sourcePlaceholder')}
              />
            </label>
            {hasActiveFilters && (
              <button
                type="button"
                className="shopping-detail__clear-filters"
                onClick={handleClearFilters}
              >
                {t('common.clear')}
              </button>
            )}
          </div>

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
              <h3 className="shopping-detail__empty-title">
                {hasActiveFilters ? t('shopping.noFilteredItems') : t('shopping.emptyList')}
              </h3>
              <p className="shopping-detail__empty-desc">
                {hasActiveFilters ? t('shopping.noFilteredItemsDesc') : t('shopping.emptyListDesc')}
              </p>
            </div>
          )}

          {/* Unpurchased items */}
          {unpurchasedItems.length > 0 && (
            <div className="shopping-detail__section">
              {renderGroupedItems(unpurchasedItems)}
            </div>
          )}

          {/* Purchased items */}
          {purchasedItems.length > 0 && (
            <>
              {unpurchasedItems.length > 0 && (
                <div className="shopping-detail__section-divider">
                  <span className="shopping-detail__section-label">
                    {t('shopping.purchasedCount', { count: purchasedItems.length })}
                  </span>
                </div>
              )}
              <div className="shopping-detail__section shopping-detail__section--purchased">
                {renderGroupedItems(purchasedItems)}
              </div>
            </>
          )}
        </div>
        <Modal
          open={Boolean(editingItem)}
          onClose={handleCloseMetadataModal}
          title={t('shopping.editMetadata')}
          size="sm"
          closeOnBackdrop={!isEditingSaving}
        >
          <form className="shopping-detail__metadata-form" onSubmit={handleSaveMetadata}>
            <label className="shopping-detail__field">
              <span className="shopping-detail__field-label">{t('shopping.category')}</span>
              <select
                className="shopping-detail__select"
                value={editCategory}
                onChange={(e) => setEditCategory(e.target.value as ShoppingItemCategory | '')}
                disabled={isEditingSaving}
              >
                <option value="">{t('shopping.noCategory')}</option>
                {SHOPPING_ITEM_CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {getCategoryLabel(category, t)}
                  </option>
                ))}
              </select>
            </label>
            <label className="shopping-detail__field">
              <span className="shopping-detail__field-label">{t('shopping.source')}</span>
              <input
                className="shopping-detail__text-input"
                value={editSource}
                maxLength={120}
                onChange={(e) => setEditSource(e.target.value)}
                placeholder={t('shopping.sourcePlaceholder')}
                disabled={isEditingSaving}
              />
            </label>
            <label className="shopping-detail__field">
              <span className="shopping-detail__field-label">{t('shopping.linkedTask')}</span>
              <select
                className="shopping-detail__select"
                value={editLinkedTaskId}
                onChange={(e) => setEditLinkedTaskId(e.target.value)}
                disabled={isEditingSaving}
              >
                <option value="">{t('shopping.noTask')}</option>
                {tasks.map((task) => (
                  <option key={task.id} value={task.id}>
                    {task.title}
                  </option>
                ))}
              </select>
            </label>
            {editError && (
              <p className="shopping-detail__start-modal-error">{editError}</p>
            )}
            <div className="shopping-detail__start-modal-actions">
              <Button
                type="button"
                variant="secondary"
                size="md"
                onClick={handleCloseMetadataModal}
                disabled={isEditingSaving}
              >
                {t('common.cancel')}
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="md"
                loading={isEditingSaving}
              >
                {t('common.saveChanges')}
              </Button>
            </div>
          </form>
        </Modal>
        <Modal
          open={showStartModal}
          onClose={handleCloseStartModal}
          title={t('shopping.startTripTitle')}
          size="sm"
          closeOnBackdrop={!isCreatingRun}
        >
          <div className="shopping-detail__start-modal">
            <p className="shopping-detail__start-modal-info">
              {t('shopping.startTripInfo', {
                name: list?.name ?? t('shopping.listFallback'),
                count: unpurchasedCount,
                itemLabel: unpurchasedCount === 1 ? t('shopping.item') : t('shopping.items'),
              })}
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
                {t('common.cancel')}
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={handleStartTrip}
                disabled={isCreatingRun}
              >
                {isCreatingRun ? t('shopping.starting') : t('shopping.start')}
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

function getItemsToBuyLabel(count: number, t: ReturnType<typeof useI18n>['t']): string {
  if (count === 0) return t('common.allDone');
  if (count === 1) return t('shopping.oneToBuy');
  return t('shopping.manyToBuy', { count });
}

function getQuantityMeta(item: ShoppingItem): string {
  const quantity = item.quantity && item.quantity > 1 ? `${item.quantity}` : '';
  const unit = item.unit ?? '';
  return [quantity, unit].filter(Boolean).join(' ');
}

function parseQuantity(value: string): number | null {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

function getCategoryLabel(
  category: ShoppingItemCategory,
  t: ReturnType<typeof useI18n>['t']
): string {
  switch (category) {
    case 'groceries':
      return t('shopping.category.groceries');
    case 'cleaning':
      return t('shopping.category.cleaning');
    case 'personal_care':
      return t('shopping.category.personalCare');
    case 'diy':
      return t('shopping.category.diy');
    case 'electronics':
      return t('shopping.category.electronics');
    case 'other':
      return t('shopping.category.other');
  }
}

function getGroupLabel(
  group: ShoppingItemGroup,
  mode: ShoppingGroupMode,
  t: ReturnType<typeof useI18n>['t']
): string {
  if (mode === 'category') {
    return group.category ? getCategoryLabel(group.category, t) : t('shopping.uncategorised');
  }
  if (mode === 'source') {
    return group.source ?? t('shopping.noSource');
  }
  return '';
}
