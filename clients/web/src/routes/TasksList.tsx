import { useCallback, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useMembers } from '../hooks/useMembers';
import { useTasks } from '../hooks/useTasks';
import { useZones } from '../hooks/useZones';
import { Button } from '../components/ui';
import TaskFiltersPanel from '../components/tasks/TaskFilters';
import TasksCard from '../components/tasks/TasksTable';
import { ApiError } from '../lib/errors';
import { executeCommand, generateIdempotencyKey } from '../lib/api';
import { useI18n } from '../i18n';
import type { TaskFilters as TaskFiltersType, TaskStatus, CommandRequest } from '../types/api';
import './TasksList.css';

const validStatuses: TaskStatus[] = ['open', 'in_progress', 'done', 'cancelled'];

/**
 * Tasks List page for viewing and filtering household tasks.
 *
 * States:
 * - Loading: skeleton placeholders in card
 * - Empty: centered icon + title + desc + CTA
 * - Error: warning banner with retry
 * - Normal: tasks list card with filter bar
 *
 * Filters stored in URL query string: status, assigneeId, zoneId.
 * Pattern follows Invites/Members pages.
 *
 * @see Pencil frames: clickMapList (filters), dataTask (task row)
 */
export default function TasksList() {
  const { householdId } = useAuth();
  const { t } = useI18n();
  const [searchParams, setSearchParams] = useSearchParams();

  const statusParam = searchParams.get('status');
  const status = validStatuses.includes(statusParam as TaskStatus)
    ? (statusParam as TaskStatus)
    : undefined;

  const filters: TaskFiltersType = {
    status,
    assigneeId: searchParams.get('assigneeId') || undefined,
    zoneId: searchParams.get('zoneId') || undefined,
  };

  const hasActiveFilters = Boolean(filters.status || filters.assigneeId || filters.zoneId);

  const { tasks, isLoading: tasksLoading, error: tasksError, refetch } = useTasks(
    householdId,
    filters
  );
  const { zones, isLoading: zonesLoading } = useZones(householdId);
  const { members, isLoading: membersLoading } = useMembers(householdId);

  const [completingTaskIds, setCompletingTaskIds] = useState<Set<string>>(new Set());

  const setFilter = useCallback(
    (key: string, value: string) => {
      const newParams = new URLSearchParams(searchParams);
      if (value) {
        newParams.set(key, value);
      } else {
        newParams.delete(key);
      }
      setSearchParams(newParams);
    },
    [searchParams, setSearchParams]
  );

  const clearAllFilters = useCallback(() => {
    setSearchParams(new URLSearchParams());
  }, [setSearchParams]);

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  const handleCompleteTask = useCallback(
    async (taskId: string) => {
      if (!householdId) return;

      setCompletingTaskIds((prev) => new Set(prev).add(taskId));

      const request: CommandRequest = {
        householdId,
        type: 'complete_task',
        payload: { taskId },
        source: 'web',
      };

      try {
        await executeCommand(request, generateIdempotencyKey());
        refetch();
      } catch {
        // Error handling: just stop spinner, user can retry
      } finally {
        setCompletingTaskIds((prev) => {
          const next = new Set(prev);
          next.delete(taskId);
          return next;
        });
      }
    },
    [householdId, refetch]
  );

  if (!householdId) {
    return (
      <div className="tasks">
        <div className="tasks__wrapper">
          <div className="tasks__empty-page">
            <p>{t('tasks.noHousehold')}</p>
          </div>
        </div>
      </div>
    );
  }

  // Error state - 403
  if (tasksError instanceof ApiError && tasksError.status === 403) {
    return (
      <div className="tasks">
        <div className="tasks__wrapper">
          <section className="tasks__section">
            <div className="tasks__section-header">
              <h2 className="tasks__section-title">{t('tasks.title')}</h2>
            </div>
            <div className="tasks__card">
              <div className="tasks__error">
                <svg
                  className="tasks__error-icon"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <circle cx="12" cy="12" r="10" />
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
                </svg>
                <div className="tasks__error-content">
                  <h3 className="tasks__error-title">{t('common.accessDenied')}</h3>
                  <p className="tasks__error-message">
                    {t('tasks.noAccess')}
                  </p>
                </div>
                <Link to="/households">
                  <Button variant="primary" size="sm">
                    {t('common.backToHouseholds')}
                  </Button>
                </Link>
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Error state - general
  if (tasksError) {
    return (
      <div className="tasks">
        <div className="tasks__wrapper">
          <section className="tasks__section">
            <div className="tasks__section-header">
              <h2 className="tasks__section-title">{t('tasks.title')}</h2>
            </div>
            <div className="tasks__card">
              <div className="tasks__error">
                <svg
                  className="tasks__error-icon"
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
                <div className="tasks__error-content">
                  <h3 className="tasks__error-title">{t('tasks.unableLoad')}</h3>
                  <p className="tasks__error-message">
                    {t('common.checkConnection')}
                  </p>
                </div>
                <Button variant="primary" size="sm" onClick={handleRetry}>
                  {t('common.retry')}
                </Button>
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Loading state
  if (tasksLoading && tasks.length === 0) {
    return (
      <div className="tasks">
        <div className="tasks__wrapper">
          <section className="tasks__section">
            <div className="tasks__section-header">
              <h2 className="tasks__section-title">{t('tasks.title')}</h2>
            </div>

            <TaskFiltersPanel
              status={filters.status}
              assigneeId={filters.assigneeId}
              zoneId={filters.zoneId}
              onStatusChange={(value) => setFilter('status', value)}
              onAssigneeChange={(value) => setFilter('assigneeId', value)}
              onZoneChange={(value) => setFilter('zoneId', value)}
              zones={zones}
              members={members}
              isLoading={zonesLoading || membersLoading}
            />

            <div className="tasks__card">
              {[1, 2, 3, 4, 5].map((i, idx) => (
                <div key={i}>
                  {idx > 0 && <div className="tasks__divider" />}
                  <div className="tasks__skeleton">
                    <div className="tasks__skeleton-checkbox" />
                    <div className="tasks__skeleton-content">
                      <div className="tasks__skeleton-line" />
                      <div className="tasks__skeleton-line tasks__skeleton-line--short" />
                    </div>
                    <div className="tasks__skeleton-badge" />
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    );
  }

  return (
    <div className="tasks">
      <div className="tasks__wrapper">
        <section className="tasks__section">
          <div className="tasks__section-header">
            <h2 className="tasks__section-title">{t('tasks.title')}</h2>
            <div className="tasks__section-actions">
              <Link to="/commands">
                <Button variant="primary" size="sm">
                  {t('tasks.addViaCommand')}
                </Button>
              </Link>
            </div>
          </div>

          <TaskFiltersPanel
            status={filters.status}
            assigneeId={filters.assigneeId}
            zoneId={filters.zoneId}
            onStatusChange={(value) => setFilter('status', value)}
            onAssigneeChange={(value) => setFilter('assigneeId', value)}
            onZoneChange={(value) => setFilter('zoneId', value)}
            zones={zones}
            members={members}
            isLoading={zonesLoading || membersLoading}
          />

          {hasActiveFilters && (
            <div className="tasks__active-filters">
              <span>{t('common.filtersActive')}</span>
              <button
                type="button"
                className="tasks__clear-filters"
                onClick={clearAllFilters}
              >
                {t('common.clearAll')}
              </button>
            </div>
          )}

          <TasksCard
            tasks={tasks}
            householdId={householdId}
            hasActiveFilters={hasActiveFilters}
            onComplete={handleCompleteTask}
            completingTaskIds={completingTaskIds}
          />

          {tasks.length >= 10 && (
            <p className="tasks__hint">
              {hasActiveFilters
                ? t('tasks.countMatching', { count: tasks.length })
                : t('tasks.countTotal', { count: tasks.length })}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}
