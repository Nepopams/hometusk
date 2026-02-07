import { useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useTask } from '../hooks/useTask';
import { Button } from '../components/ui';
import { ApiError } from '../lib/errors';
import type { TaskStatus } from '../types/api';
import './TaskDetail.css';

const statusLabels: Record<TaskStatus, string> = {
  open: 'Open',
  in_progress: 'In Progress',
  done: 'Done',
  cancelled: 'Cancelled',
};

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
  const { taskId } = useParams();
  const { task, isLoading, error, refetch } = useTask(householdId, taskId);

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  if (!householdId) {
    return (
      <div className="task-detail">
        <div className="task-detail__wrapper">
          <div className="task-detail__empty-page">
            <p>Please select a household to view task details.</p>
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
          <Link to="/tasks" className="task-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to tasks
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
          <Link to="/tasks" className="task-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to tasks
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
              <h3 className="task-detail__not-found-title">Task not found</h3>
              <p className="task-detail__not-found-desc">
                This task may have been deleted or you don&apos;t have access to it.
              </p>
              <Link to="/tasks">
                <Button variant="primary" size="md">
                  Go to tasks
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
          <Link to="/tasks" className="task-detail__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to tasks
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
                <h3 className="task-detail__error-title">Unable to load task</h3>
                <p className="task-detail__error-message">
                  Check your connection and try again.
                </p>
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

  // No task (shouldn't happen if no error, but safety check)
  if (!task) {
    return null;
  }

  const isDone = task.status === 'done';
  const deadline = formatDeadline(task.deadline);

  return (
    <div className="task-detail">
      <div className="task-detail__wrapper">
        <Link to="/tasks" className="task-detail__back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Back to tasks
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
              <span className="task-detail__row-label">Assignee</span>
              <span className={`task-detail__row-value ${!task.assignee ? 'task-detail__row-value--muted' : ''}`}>
                {task.assignee?.displayName || 'Unassigned'}
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
              <span className="task-detail__row-label">Zone</span>
              <span className={`task-detail__row-value ${!task.zone ? 'task-detail__row-value--muted' : ''}`}>
                {task.zone?.name || 'No zone'}
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
              <span className="task-detail__row-label">Deadline</span>
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
                  <span className="task-detail__row-label">Completed</span>
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
              <span>Created {formatRelativeTime(task.createdAt)}</span>
              {task.createdBy && <span>by {task.createdBy.displayName}</span>}
            </div>
            <div className="task-detail__meta-row">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 20h9" />
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
              </svg>
              <span>Updated {formatRelativeTime(task.updatedAt)}</span>
            </div>
            <div className="task-detail__meta-row">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>Created via {task.createdVia}</span>
            </div>
          </div>
        </div>

        {/* Shopping Items Section */}
        {task.linkedShoppingItems && task.linkedShoppingItems.length > 0 && (
          <>
            <div className="task-detail__section-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
              <span>Shopping Items ({task.linkedShoppingItems.length})</span>
            </div>
            <div className="task-detail__card">
              {task.linkedShoppingItems.map((item, idx) => (
                <div key={item.id}>
                  {idx > 0 && <div className="task-detail__divider" />}
                  <Link
                    to={`/households/${householdId}/shopping/${item.listId}`}
                    className="task-detail__shopping-item"
                  >
                    <span
                      className={`task-detail__shopping-name ${item.purchased ? 'task-detail__shopping-name--purchased' : ''}`}
                    >
                      {item.name}
                    </span>
                    {(item.quantity || item.unit) && (
                      <span className="task-detail__shopping-meta">
                        {item.quantity && item.quantity > 1 ? `${item.quantity}` : ''}
                        {item.quantity && item.quantity > 1 && item.unit ? ' ' : ''}
                        {item.unit || ''}
                      </span>
                    )}
                    {item.purchased && (
                      <svg
                        className="task-detail__shopping-check"
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    )}
                  </Link>
                </div>
              ))}
            </div>
          </>
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

function formatDeadline(value?: string): { text: string; isOverdue: boolean } {
  if (!value) return { text: 'No deadline', isOverdue: false };

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return { text: 'No deadline', isOverdue: false };

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const taskDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

  const isOverdue = taskDate < today;

  if (taskDate.getTime() === today.getTime()) {
    return { text: 'Today', isOverdue: false };
  }
  if (taskDate.getTime() === tomorrow.getTime()) {
    return { text: 'Tomorrow', isOverdue: false };
  }

  const diffDays = Math.ceil((taskDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays > 0 && diffDays <= 7) {
    return { text: `In ${diffDays} days`, isOverdue: false };
  }

  if (isOverdue) {
    const overdueDays = Math.ceil((today.getTime() - taskDate.getTime()) / (1000 * 60 * 60 * 24));
    return { text: `${overdueDays}d overdue`, isOverdue: true };
  }

  return { text: date.toLocaleDateString(), isOverdue: false };
}

function formatRelativeTime(timestamp: string): string {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diffMs = now - then;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${diffDays}d ago`;
}

function formatDateTime(timestamp: string): string {
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return 'Unknown';
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}
