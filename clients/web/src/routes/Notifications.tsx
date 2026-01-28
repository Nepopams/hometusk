import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button';
import ErrorMessage from '../components/ui/ErrorMessage';
import Spinner from '../components/ui/Spinner';
import { useAuth } from '../hooks/useAuth';
import { useNotifications } from '../hooks/useNotifications';
import { useNotificationStream } from '../hooks/useNotificationStream';
import type { Notification, NotificationType } from '../types/api';
import './Notifications.css';

function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins} minute${diffMins !== 1 ? 's' : ''} ago`;

  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours} hour${diffHours !== 1 ? 's' : ''} ago`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays} day${diffDays !== 1 ? 's' : ''} ago`;

  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function getNotificationIcon(type: NotificationType): { icon: JSX.Element; className: string } {
  switch (type) {
    case 'task_assigned':
    case 'task_completed':
      return {
        className: 'notifications__icon--task',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 11l3 3L22 4" />
            <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
          </svg>
        ),
      };
    case 'shopping_item_added':
    case 'shopping_item_purchased':
      return {
        className: 'notifications__icon--shopping',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6" />
          </svg>
        ),
      };
    case 'invite_accepted':
      return {
        className: 'notifications__icon--invite',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
            <circle cx="8.5" cy="7" r="4" />
            <path d="M20 8v6M23 11h-6" />
          </svg>
        ),
      };
    default:
      return {
        className: 'notifications__icon--alert',
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 22a2 2 0 002-2h-4a2 2 0 002 2zm6-6V11a6 6 0 00-5-5.92V4a1 1 0 00-2 0v1.08A6 6 0 006 11v5l-2 2v1h16v-1l-2-2z" />
          </svg>
        ),
      };
  }
}

interface NotificationItemProps {
  notification: Notification;
  onMarkRead: (id: string) => void;
}

function NotificationItem({ notification, onMarkRead }: NotificationItemProps) {
  const { icon, className } = getNotificationIcon(notification.type);
  const isUnread = !notification.readAt;

  const handleClick = () => {
    if (isUnread) {
      onMarkRead(notification.id);
    }
  };

  return (
    <div
      className={`notifications__item ${isUnread ? 'notifications__item--unread' : ''}`}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && handleClick()}
    >
      <div className={`notifications__icon ${className}`}>{icon}</div>
      <div className="notifications__content">
        <p className="notifications__message">{notification.payload.summary}</p>
        <p className="notifications__time">{formatRelativeTime(notification.createdAt)}</p>
      </div>
      {isUnread && <div className="notifications__unread-dot" />}
    </div>
  );
}

export default function Notifications() {
  const { householdId, logout } = useAuth();
  const navigate = useNavigate();

  const {
    notifications,
    unreadCount,
    isLoading,
    error,
    isMarkingAll,
    markAsRead,
    markAllAsRead,
    addNotification,
    refresh,
  } = useNotifications(householdId ?? undefined);

  const handleAuthError = useCallback(() => {
    void logout();
    navigate('/login?error=session_expired', { replace: true });
  }, [logout, navigate]);

  useNotificationStream(householdId ?? undefined, addNotification, handleAuthError);

  if (!householdId) {
    return (
      <div className="page notifications">
        <div className="notifications__header">
          <h1 className="notifications__title">Notifications</h1>
        </div>
        <div className="notifications__card">
          <div className="notifications__empty">
            <div className="notifications__empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 22a2 2 0 002-2h-4a2 2 0 002 2zm6-6V11a6 6 0 00-5-5.92V4a1 1 0 00-2 0v1.08A6 6 0 006 11v5l-2 2v1h16v-1l-2-2z" />
              </svg>
            </div>
            <h2 className="notifications__empty-title">Select a household</h2>
            <p className="notifications__empty-text">
              Choose a household to view notifications.
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading && notifications.length === 0) {
    return (
      <div className="page notifications">
        <div className="notifications__header">
          <h1 className="notifications__title">Notifications</h1>
        </div>
        <div className="notifications__card">
          <div className="notifications__loading">
            <Spinner />
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page notifications">
        <div className="notifications__header">
          <h1 className="notifications__title">Notifications</h1>
        </div>
        <ErrorMessage error={error} onRetry={refresh} />
      </div>
    );
  }

  return (
    <div className="page notifications">
      <div className="notifications__header">
        <h1 className="notifications__title">Notifications</h1>
        {unreadCount > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={markAllAsRead}
            disabled={isMarkingAll}
          >
            {isMarkingAll ? 'Marking...' : 'Mark all as read'}
          </Button>
        )}
      </div>

      <div className="notifications__card">
        {notifications.length === 0 ? (
          <div className="notifications__empty">
            <div className="notifications__empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 22a2 2 0 002-2h-4a2 2 0 002 2zm6-6V11a6 6 0 00-5-5.92V4a1 1 0 00-2 0v1.08A6 6 0 006 11v5l-2 2v1h16v-1l-2-2z" />
              </svg>
            </div>
            <h2 className="notifications__empty-title">No notifications</h2>
            <p className="notifications__empty-text">
              You&apos;re all caught up! Notifications will appear here.
            </p>
          </div>
        ) : (
          <>
            {notifications.map((notification) => (
              <NotificationItem
                key={notification.id}
                notification={notification}
                onMarkRead={markAsRead}
              />
            ))}
            {notifications.length > 0 && (
              <div className="notifications__footer">
                <span className="notifications__clear-btn">
                  {notifications.length} notification{notifications.length !== 1 ? 's' : ''}
                </span>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
