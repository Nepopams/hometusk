import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';
import { useTasks } from '../hooks/useTasks';
import { useZones } from '../hooks/useZones';
import { Button } from '../components/ui';
import { useI18n } from '../i18n';
import type { Task } from '../types/api';
import './Dashboard.css';

interface ShoppingItem {
  id: string;
  text: string;
  purchased: boolean;
}

const STORAGE_KEY = 'hometusk_shopping';

function getZoneBadgeClass(zoneName?: string): string {
  if (!zoneName) return 'dashboard__badge--zone-default';
  const lower = zoneName.toLowerCase();
  if (lower.includes('kitchen')) return 'dashboard__badge--zone-kitchen';
  if (lower.includes('bathroom')) return 'dashboard__badge--zone-bathroom';
  if (lower.includes('living')) return 'dashboard__badge--zone-living';
  if (lower.includes('bedroom')) return 'dashboard__badge--zone-bedroom';
  return 'dashboard__badge--zone-default';
}

/**
 * Dashboard page combining Tasks and Shopping lists.
 *
 * Layout:
 * - Desktop/Tablet: Two columns (tasks flex, shopping 360px fixed)
 * - Mobile: Vertical stack
 *
 * States: loading, empty, error, normal
 *
 * @see Pencil frames: PARvt, UxFDL, lCp6u, eTaes, phvdl, PydL5, ZgDKd
 */
export default function Dashboard() {
  const { householdId } = useAuth();
  const { t } = useI18n();
  const { householdId: householdIdParam } = useParams();
  const activeHouseholdId = householdIdParam ?? householdId ?? undefined;

  const { tasks, isLoading: tasksLoading, error: tasksError, refetch } = useTasks(activeHouseholdId, {});
  const { zones, isLoading: zonesLoading } = useZones(activeHouseholdId);

  // Shopping list (localStorage for MVP)
  const [shoppingItems, setShoppingItems] = useState<ShoppingItem[]>([]);
  const [newItemText, setNewItemText] = useState('');

  // Load shopping items from localStorage
  useEffect(() => {
    if (!activeHouseholdId) return;
    const key = `${STORAGE_KEY}_${activeHouseholdId}`;
    const stored = localStorage.getItem(key);
    if (stored) {
      try {
        setShoppingItems(JSON.parse(stored));
      } catch {
        setShoppingItems([]);
      }
    } else {
      setShoppingItems([]);
    }
  }, [activeHouseholdId]);

  // Save shopping items to localStorage
  useEffect(() => {
    if (!activeHouseholdId) return;
    const key = `${STORAGE_KEY}_${activeHouseholdId}`;
    localStorage.setItem(key, JSON.stringify(shoppingItems));
  }, [shoppingItems, activeHouseholdId]);

  const handleAddShoppingItem = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = newItemText.trim();
    if (!trimmed) return;

    setShoppingItems((prev) => [
      ...prev,
      { id: crypto.randomUUID(), text: trimmed, purchased: false },
    ]);
    setNewItemText('');
  };

  const handleToggleShoppingItem = useCallback((id: string) => {
    setShoppingItems((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, purchased: !item.purchased } : item
      )
    );
  }, []);

  const getZoneName = useCallback(
    (zoneId?: string) => {
      if (!zoneId) return undefined;
      return zones.find((z) => z.id === zoneId)?.name;
    },
    [zones]
  );

  const doneTasksCount = tasks.filter((t) => t.status === 'done').length;
  const activeTasksCount = tasks.length - doneTasksCount;

  // Access denied
  if (tasksError instanceof ApiError && tasksError.status === 403) {
    return (
      <div className="page">
        <h1>{t('common.accessDenied')}</h1>
        <p>{t('tasks.noAccess')}</p>
        <Link className="button" to="/households">
          {t('common.backToHouseholdSelector')}
        </Link>
      </div>
    );
  }

  // Loading state
  if ((tasksLoading || zonesLoading) && tasks.length === 0) {
    return (
      <div className="dashboard">
        <div className="dashboard__tasks-col">
          <div className="dashboard__header">
            <h2 className="dashboard__title">{t('tasks.title')}</h2>
          </div>
          <div className="dashboard__card">
            {[1, 2, 3].map((i) => (
              <div key={i} className="dashboard__skeleton-row">
                <div className="dashboard__skeleton-checkbox" />
                <div className="dashboard__skeleton-content">
                  <div className="dashboard__skeleton-title" style={{ width: i === 1 ? '70%' : i === 2 ? '50%' : '60%' }} />
                  <div className="dashboard__skeleton-badges">
                    <div className="dashboard__skeleton-badge" />
                    <div className="dashboard__skeleton-badge" style={{ width: 50 }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="dashboard__shopping-col">
          <div className="dashboard__header">
            <h2 className="dashboard__title">{t('dashboard.shopping')}</h2>
          </div>
          <div className="dashboard__card">
            {[1, 2, 3].map((i) => (
              <div key={i} className="dashboard__skeleton-row">
                <div className="dashboard__skeleton-checkbox" />
                <div className="dashboard__skeleton-content">
                  <div className="dashboard__skeleton-title" style={{ width: i === 1 ? '40%' : i === 2 ? '30%' : '50%' }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Error state for tasks
  const renderTasksError = () => (
    <div className="dashboard__card">
      <div className="dashboard__error">
        <div className="dashboard__error-content">
          <svg className="dashboard__error-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <div className="dashboard__error-text">
            <h3 className="dashboard__error-title">{t('tasks.couldntLoad')}</h3>
            <p className="dashboard__error-desc">{t('common.checkConnection')}</p>
          </div>
        </div>
        <button type="button" className="dashboard__retry-btn" onClick={() => void refetch()}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          {t('common.tryAgain')}
        </button>
      </div>
    </div>
  );

  // Empty tasks state
  const renderTasksEmpty = () => (
    <div className="dashboard__card">
      <div className="dashboard__empty">
        <svg className="dashboard__empty-icon" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M9 11l3 3L22 4" />
          <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
        </svg>
        <h3 className="dashboard__empty-title">{t('tasks.noTasksYet')}</h3>
        <p className="dashboard__empty-desc">{t('tasks.emptyDashboardDesc')}</p>
        <div className="dashboard__empty-actions">
          <Button variant="primary" size="md" fullWidth>
            {t('tasks.addTask')}
          </Button>
        </div>
      </div>
    </div>
  );

  // Render task row
  const renderTaskRow = (task: Task) => {
    const isDone = task.status === 'done';
    const zoneName = task.zone?.name ?? getZoneName(task.zone?.id);

    return (
      <div key={task.id} className="dashboard__task-row">
        <div className={`dashboard__task-checkbox ${isDone ? 'dashboard__task-checkbox--done' : ''}`}>
          {isDone && (
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          )}
        </div>
        <div className="dashboard__task-content">
          <Link to={`tasks/${task.id}`}>
            <h4 className={`dashboard__task-title ${isDone ? 'dashboard__task-title--done' : ''}`}>
              {task.title}
            </h4>
          </Link>
          <div className="dashboard__task-meta">
            {zoneName && (
              <span className={`dashboard__badge ${getZoneBadgeClass(zoneName)}`}>
                {zoneName}
              </span>
            )}
            {task.assignee && (
              <span className="dashboard__badge dashboard__badge--assignee">
                {task.assignee.displayName}
              </span>
            )}
            {task.deadline && (
              <span className="dashboard__badge dashboard__badge--due">
                {formatDeadline(task.deadline, t)}
              </span>
            )}
          </div>
        </div>
      </div>
    );
  };

  // Empty shopping state
  const renderShoppingEmpty = () => (
    <div className="dashboard__empty" style={{ padding: 20 }}>
      <svg className="dashboard__empty-icon" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="9" cy="21" r="1" />
        <circle cx="20" cy="21" r="1" />
        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
      </svg>
      <h3 className="dashboard__empty-title">{t('dashboard.emptyShopping')}</h3>
      <p className="dashboard__empty-desc">{t('dashboard.emptyShoppingDesc')}</p>
    </div>
  );

  // Render shopping item
  const renderShoppingItem = (item: ShoppingItem) => (
    <div key={item.id} className="dashboard__shop-row">
      <div
        className={`dashboard__shop-checkbox ${item.purchased ? 'dashboard__shop-checkbox--done' : ''}`}
        onClick={() => handleToggleShoppingItem(item.id)}
        role="checkbox"
        aria-checked={item.purchased}
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && handleToggleShoppingItem(item.id)}
      >
        {item.purchased && (
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        )}
      </div>
      <span className={`dashboard__shop-text ${item.purchased ? 'dashboard__shop-text--done' : ''}`}>
        {item.text}
      </span>
    </div>
  );

  return (
    <div className="dashboard">
      {/* Tasks Column */}
      <div className="dashboard__tasks-col">
        <div className="dashboard__header">
          <h2 className="dashboard__title">{t('tasks.title')}</h2>
          <span className="dashboard__count">
            {activeTasksCount > 0
              ? t('tasks.countActive', { count: activeTasksCount })
              : doneTasksCount > 0
                ? t('tasks.countDone', { count: doneTasksCount })
                : t('tasks.countZero')}
          </span>
        </div>
        {tasksError ? (
          renderTasksError()
        ) : tasks.length === 0 ? (
          renderTasksEmpty()
        ) : (
          <div className="dashboard__card">
            {tasks.map(renderTaskRow)}
          </div>
        )}
      </div>

      {/* Shopping Column */}
      <div className="dashboard__shopping-col">
        <div className="dashboard__header">
          <h2 className="dashboard__title">{t('dashboard.shopping')}</h2>
        </div>
        <div className="dashboard__card">
          <form className="dashboard__add-input-row" onSubmit={handleAddShoppingItem}>
            <input
              type="text"
              className="dashboard__add-input"
              placeholder={t('dashboard.addItemPlaceholder')}
              value={newItemText}
              onChange={(e) => setNewItemText(e.target.value)}
            />
            <button type="submit" className="dashboard__add-btn" aria-label={t('common.addItem')}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
          </form>
          {shoppingItems.length === 0 ? (
            renderShoppingEmpty()
          ) : (
            shoppingItems.map(renderShoppingItem)
          )}
        </div>
      </div>
    </div>
  );
}

function formatDeadline(deadline: string, t: ReturnType<typeof useI18n>['t']): string {
  const date = new Date(deadline);
  const now = new Date();
  const diffDays = Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return t('common.today');
  if (diffDays === 1) return t('common.tomorrow');
  if (diffDays < 0) return t('tasks.overdueDays', { count: Math.abs(diffDays) });
  if (diffDays <= 7) return t('tasks.inDays', { count: diffDays });
  return date.toLocaleDateString();
}
