import { FormEvent, useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useShoppingLists } from '../hooks/useShoppingLists';
import { useTask } from '../hooks/useTask';
import { Button, Modal, Snackbar } from '../components/ui';
import { addShoppingItem, updateShoppingItem } from '../lib/api';
import { ApiError } from '../lib/errors';
import { SHOPPING_ITEM_CATEGORIES, buildAddShoppingItemPayload } from '../lib/shoppingMetadata';
import { useI18n } from '../i18n';
import type { ShoppingItemCategory, TaskStatus } from '../types/api';
import './TaskDetail.css';

/**
 * Task Detail page for viewing a single task.
 *
 * States:
 * - Loading: skeleton placeholders
 * - Not Found (404): task doesn't exist or no access
 * - Error: general error with retry
 * - Normal: task details card
 *
 * Read-only view. Pattern follows Members/Zones detail style.
 *
 * @see Pencil frames: dataTask (task data interface)
 */
export default function TaskDetail() {
  const { householdId } = useAuth();
  const { t, formatDateTime, formatRelativeTime } = useI18n();
  const { taskId } = useParams();
  const { task, isLoading, error, refetch } = useTask(householdId, taskId);
  const {
    lists: shoppingLists,
    isCreating: isCreatingList,
    createList,
  } = useShoppingLists(householdId);
  const tasksPath = householdId ? `/households/${householdId}/tasks` : '/households';
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [selectedListId, setSelectedListId] = useState('');
  const [newListName, setNewListName] = useState('');
  const [newItemName, setNewItemName] = useState('');
  const [newItemQuantity, setNewItemQuantity] = useState('');
  const [newItemUnit, setNewItemUnit] = useState('');
  const [newItemCategory, setNewItemCategory] = useState<ShoppingItemCategory | ''>('');
  const [newItemSource, setNewItemSource] = useState('');
  const [addError, setAddError] = useState<string | null>(null);
  const [isAddingItem, setIsAddingItem] = useState(false);
  const [savingItemIds, setSavingItemIds] = useState<Set<string>>(new Set());
  const [snackbar, setSnackbar] = useState<{ message: string; variant: 'success' | 'error' } | null>(
    null
  );

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  const handleOpenAddItem = useCallback(() => {
    setSelectedListId(shoppingLists[0]?.id ?? 'new');
    setNewListName(shoppingLists.length === 0 ? t('shopping.defaultListName') : '');
    setNewItemName('');
    setNewItemQuantity('');
    setNewItemUnit('');
    setNewItemCategory('');
    setNewItemSource('');
    setAddError(null);
    setAddModalOpen(true);
  }, [shoppingLists, t]);

  const handleCloseAddItem = useCallback(() => {
    if (!isAddingItem && !isCreatingList) {
      setAddModalOpen(false);
      setAddError(null);
    }
  }, [isAddingItem, isCreatingList]);

  const handleAddLinkedItem = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      if (!householdId || !task) return;

      const itemName = newItemName.trim();
      if (!itemName) return;

      setIsAddingItem(true);
      setAddError(null);

      try {
        let listId = selectedListId;
        if (listId === 'new' || shoppingLists.length === 0) {
          const created = await createList(newListName.trim() || t('shopping.defaultListName'));
          if (!created) return;
          listId = created.id;
        }

        await addShoppingItem(
          householdId,
          listId,
          buildAddShoppingItemPayload(
            itemName,
            parseQuantity(newItemQuantity),
            newItemUnit,
            newItemCategory,
            newItemSource,
            task.id
          )
        );

        setAddModalOpen(false);
        setSnackbar({ message: t('shopping.itemAddedToTask'), variant: 'success' });
        refetch();
      } catch (err) {
        setAddError(err instanceof Error ? err.message : t('shopping.failedAddItem'));
      } finally {
        setIsAddingItem(false);
      }
    },
    [
      createList,
      householdId,
      newItemCategory,
      newItemName,
      newItemQuantity,
      newItemSource,
      newItemUnit,
      newListName,
      refetch,
      selectedListId,
      shoppingLists.length,
      t,
      task,
    ]
  );

  const handleMarkShoppingPurchased = useCallback(
    async (itemId: string) => {
      if (!householdId) return;
      setSavingItemIds((prev) => new Set(prev).add(itemId));
      try {
        await updateShoppingItem(householdId, itemId, { purchased: true });
        refetch();
      } catch {
        setSnackbar({ message: t('shopping.failedUpdateItem'), variant: 'error' });
      } finally {
        setSavingItemIds((prev) => {
          const next = new Set(prev);
          next.delete(itemId);
          return next;
        });
      }
    },
    [householdId, refetch, t]
  );

  if (!householdId) {
    return (
      <div className="task-detail">
        <div className="task-detail__wrapper">
          <div className="task-detail__empty-page">
            <p>{t('tasks.noHouseholdDetails')}</p>
          </div>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="task-detail">
        <div className="task-detail__wrapper">
          <Link to={tasksPath} className="task-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            {t('tasks.backToTasks')}
          </Link>

          <div className="task-detail__skeleton-header">
            <div className="task-detail__skeleton-title" />
            <div className="task-detail__skeleton-desc" />
          </div>

          <div className="task-detail__card">
            {[1, 2, 3, 4].map((i, idx) => (
              <div key={i}>
                {idx > 0 && <div className="task-detail__divider" />}
                <div className="task-detail__skeleton-row">
                  <div className="task-detail__skeleton-icon" />
                  <div className="task-detail__skeleton-content">
                    <div className="task-detail__skeleton-label" />
                    <div className="task-detail__skeleton-value" />
                  </div>
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
      <div className="task-detail">
        <div className="task-detail__wrapper">
          <Link to={tasksPath} className="task-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            {t('tasks.backToTasks')}
          </Link>

          <div className="task-detail__card">
            <div className="task-detail__not-found">
              <svg
                className="task-detail__not-found-icon"
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
              <h3 className="task-detail__not-found-title">{t('tasks.taskNotFound')}</h3>
              <p className="task-detail__not-found-desc">
                {t('tasks.taskNotFoundDesc')}
              </p>
              <Link to={tasksPath}>
                <Button variant="primary" size="md">
                  {t('tasks.goToTasks')}
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
      <div className="task-detail">
        <div className="task-detail__wrapper">
          <Link to={tasksPath} className="task-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            {t('tasks.backToTasks')}
          </Link>

          <div className="task-detail__card">
            <div className="task-detail__error">
              <svg
                className="task-detail__error-icon"
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
              <div className="task-detail__error-content">
                <h3 className="task-detail__error-title">{t('tasks.unableLoadTask')}</h3>
                <p className="task-detail__error-message">
                  {t('common.checkConnection')}
                </p>
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

  // No task (shouldn't happen if no error, but safety check)
  if (!task) {
    return null;
  }

  const isDone = task.status === 'done';
  const deadline = formatDeadline(task.deadline, t);
  const statusLabels: Record<TaskStatus, string> = {
    open: t('common.open'),
    in_progress: t('common.inProgress'),
    done: t('common.done'),
    cancelled: t('common.cancelled'),
  };
  const linkedShoppingItems = task.linkedShoppingItems ?? [];
  const isSavingAdd = isAddingItem || isCreatingList;

  return (
    <div className="task-detail">
      <div className="task-detail__wrapper">
        <Link to={tasksPath} className="task-detail__back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          {t('tasks.backToTasks')}
        </Link>

        {/* Header */}
        <div className="task-detail__header">
          <div className="task-detail__header-row">
            <h1 className={`task-detail__title ${isDone ? 'task-detail__title--done' : ''}`}>
              {task.title}
            </h1>
            <div className="task-detail__status">
              <span className={`task-detail__badge task-detail__badge--${task.status}`}>
                {statusLabels[task.status]}
              </span>
            </div>
          </div>
          {task.description && (
            <p className="task-detail__description">{task.description}</p>
          )}
        </div>

        {/* Details Card */}
        <div className="task-detail__card">
          {/* Assignee */}
          <div className="task-detail__row">
            {task.assignee ? (
              <div className="task-detail__avatar">
                {getInitials(task.assignee.displayName)}
              </div>
            ) : (
              <div className="task-detail__row-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </div>
            )}
            <div className="task-detail__row-content">
              <span className="task-detail__row-label">{t('common.assignee')}</span>
              <span className={`task-detail__row-value ${!task.assignee ? 'task-detail__row-value--muted' : ''}`}>
                {task.assignee?.displayName || t('common.unassigned')}
              </span>
            </div>
          </div>

          <div className="task-detail__divider" />

          {/* Zone */}
          <div className="task-detail__row">
            <div className="task-detail__row-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="14" y="14" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
              </svg>
            </div>
            <div className="task-detail__row-content">
              <span className="task-detail__row-label">{t('common.zone')}</span>
              <span className={`task-detail__row-value ${!task.zone ? 'task-detail__row-value--muted' : ''}`}>
                {task.zone?.name || t('common.noZone')}
              </span>
            </div>
          </div>

          <div className="task-detail__divider" />

          {/* Deadline */}
          <div className="task-detail__row">
            <div className="task-detail__row-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
            </div>
            <div className="task-detail__row-content">
              <span className="task-detail__row-label">{t('common.deadline')}</span>
              <span className={`task-detail__row-value ${deadline.isOverdue ? 'task-detail__row-value--overdue' : ''} ${!task.deadline ? 'task-detail__row-value--muted' : ''}`}>
                {deadline.text}
              </span>
            </div>
          </div>

          {/* Completed At (if done) */}
          {task.completedAt && (
            <>
              <div className="task-detail__divider" />
              <div className="task-detail__row">
                <div className="task-detail__row-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                </div>
                <div className="task-detail__row-content">
                  <span className="task-detail__row-label">{t('tasks.completed')}</span>
                  <span className="task-detail__row-value">
                    {formatDateTime(task.completedAt)}
                  </span>
                </div>
              </div>
            </>
          )}

          {/* Metadata */}
          <div className="task-detail__divider" />
          <div className="task-detail__meta">
            <div className="task-detail__meta-row">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <span>{t('common.created')} {formatRelativeTime(task.createdAt)}</span>
              {task.createdBy && <span>{t('tasks.createdBy', { name: task.createdBy.displayName })}</span>}
            </div>
            <div className="task-detail__meta-row">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 20h9" />
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
              </svg>
              <span>{t('common.updated')} {formatRelativeTime(task.updatedAt)}</span>
            </div>
            <div className="task-detail__meta-row">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>{t('tasks.createdVia', { value: task.createdVia })}</span>
            </div>
          </div>
        </div>

        {/* Shopping Items Section */}
        <div className="task-detail__section-header">
          <div className="task-detail__section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="9" cy="21" r="1" />
              <circle cx="20" cy="21" r="1" />
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
            </svg>
            <span>{t('tasks.shoppingItems', { count: linkedShoppingItems.length })}</span>
          </div>
          <Button variant="secondary" size="sm" onClick={handleOpenAddItem}>
            {t('shopping.addLinkedItem')}
          </Button>
        </div>
        <div className="task-detail__card">
          {linkedShoppingItems.length === 0 ? (
            <div className="task-detail__shopping-empty">
              <p>{t('shopping.noLinkedItems')}</p>
              <Button variant="primary" size="sm" onClick={handleOpenAddItem}>
                {t('shopping.addLinkedItem')}
              </Button>
            </div>
          ) : (
            linkedShoppingItems.map((item, idx) => (
                <div key={item.id}>
                  {idx > 0 && <div className="task-detail__divider" />}
                  <div className="task-detail__shopping-item">
                    <Link
                      to={`/households/${householdId}/shopping/${item.listId}`}
                      className="task-detail__shopping-link"
                    >
                      <span
                        className={`task-detail__shopping-name ${item.purchased ? 'task-detail__shopping-name--purchased' : ''}`}
                      >
                        {item.name}
                      </span>
                      {(item.quantity || item.unit) && (
                        <span className="task-detail__shopping-meta">
                          {getQuantityMeta(item)}
                        </span>
                      )}
                      {(item.category || item.source) && (
                        <span className="task-detail__shopping-badges">
                          {item.category && (
                            <span className="task-detail__shopping-badge">
                              {getCategoryLabel(item.category, t)}
                            </span>
                          )}
                          {item.source && (
                            <span className="task-detail__shopping-badge task-detail__shopping-badge--source">
                              {item.source}
                            </span>
                          )}
                        </span>
                      )}
                    </Link>
                    {item.purchased ? (
                      <svg className="task-detail__shopping-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    ) : (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleMarkShoppingPurchased(item.id)}
                        loading={savingItemIds.has(item.id)}
                      >
                        {t('shopping.markPurchased')}
                      </Button>
                    )}
                  </div>
                </div>
            ))
          )}
        </div>

        <Modal
          open={addModalOpen}
          onClose={handleCloseAddItem}
          title={t('shopping.addLinkedItem')}
          size="md"
          closeOnBackdrop={!isSavingAdd}
        >
          <form className="task-detail__shopping-form" onSubmit={handleAddLinkedItem}>
            <label className="task-detail__field">
              <span className="task-detail__field-label">{t('shopping.targetList')}</span>
              <select
                className="task-detail__select"
                value={selectedListId || 'new'}
                onChange={(e) => setSelectedListId(e.target.value)}
                disabled={isSavingAdd}
              >
                {shoppingLists.map((list) => (
                  <option key={list.id} value={list.id}>
                    {list.name}
                  </option>
                ))}
                <option value="new">{t('shopping.createNewList')}</option>
              </select>
            </label>
            {(selectedListId === 'new' || shoppingLists.length === 0) && (
              <label className="task-detail__field">
                <span className="task-detail__field-label">{t('shopping.listName')}</span>
                <input
                  className="task-detail__text-input"
                  value={newListName}
                  maxLength={80}
                  onChange={(e) => setNewListName(e.target.value)}
                  placeholder={t('shopping.defaultListName')}
                  disabled={isSavingAdd}
                />
              </label>
            )}
            <label className="task-detail__field">
              <span className="task-detail__field-label">{t('common.name')}</span>
              <input
                className="task-detail__text-input"
                value={newItemName}
                maxLength={255}
                onChange={(e) => setNewItemName(e.target.value)}
                placeholder={t('shopping.addPlaceholder')}
                disabled={isSavingAdd}
                autoFocus
              />
            </label>
            <div className="task-detail__field-row">
              <label className="task-detail__field">
                <span className="task-detail__field-label">{t('shopping.quantity')}</span>
                <input
                  className="task-detail__text-input"
                  type="number"
                  min={1}
                  max={999}
                  inputMode="numeric"
                  value={newItemQuantity}
                  onChange={(e) => setNewItemQuantity(e.target.value)}
                  disabled={isSavingAdd}
                />
              </label>
              <label className="task-detail__field">
                <span className="task-detail__field-label">{t('shopping.unit')}</span>
                <input
                  className="task-detail__text-input"
                  value={newItemUnit}
                  maxLength={50}
                  onChange={(e) => setNewItemUnit(e.target.value)}
                  disabled={isSavingAdd}
                />
              </label>
            </div>
            <div className="task-detail__field-row">
              <label className="task-detail__field">
                <span className="task-detail__field-label">{t('shopping.category')}</span>
                <select
                  className="task-detail__select"
                  value={newItemCategory}
                  onChange={(e) => setNewItemCategory(e.target.value as ShoppingItemCategory | '')}
                  disabled={isSavingAdd}
                >
                  <option value="">{t('shopping.noCategory')}</option>
                  {SHOPPING_ITEM_CATEGORIES.map((category) => (
                    <option key={category} value={category}>
                      {getCategoryLabel(category, t)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="task-detail__field">
                <span className="task-detail__field-label">{t('shopping.source')}</span>
                <input
                  className="task-detail__text-input"
                  value={newItemSource}
                  maxLength={120}
                  onChange={(e) => setNewItemSource(e.target.value)}
                  placeholder={t('shopping.sourcePlaceholder')}
                  disabled={isSavingAdd}
                />
              </label>
            </div>
            {addError && <p className="task-detail__form-error">{addError}</p>}
            <div className="task-detail__modal-actions">
              <Button type="button" variant="secondary" size="md" onClick={handleCloseAddItem} disabled={isSavingAdd}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" variant="primary" size="md" loading={isSavingAdd} disabled={!newItemName.trim()}>
                {t('common.add')}
              </Button>
            </div>
          </form>
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

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

function formatDeadline(
  value: string | undefined,
  t: ReturnType<typeof useI18n>['t']
): { text: string; isOverdue: boolean } {
  if (!value) return { text: t('common.noDeadline'), isOverdue: false };

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return { text: t('common.noDeadline'), isOverdue: false };

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const taskDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

  const isOverdue = taskDate < today;

  if (taskDate.getTime() === today.getTime()) {
    return { text: t('common.today'), isOverdue: false };
  }
  if (taskDate.getTime() === tomorrow.getTime()) {
    return { text: t('common.tomorrow'), isOverdue: false };
  }

  const diffDays = Math.ceil((taskDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays > 0 && diffDays <= 7) {
    return { text: t('tasks.inDays', { count: diffDays }), isOverdue: false };
  }

  if (isOverdue) {
    const overdueDays = Math.ceil((today.getTime() - taskDate.getTime()) / (1000 * 60 * 60 * 24));
    return { text: t('tasks.overdueDays', { count: overdueDays }), isOverdue: true };
  }

  return { text: date.toLocaleDateString(), isOverdue: false };
}

function getQuantityMeta(item: { quantity?: number; unit?: string }): string {
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
