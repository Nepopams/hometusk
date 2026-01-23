import type { Notification } from '../../types/api';
import NotificationList from './NotificationList';
import EmptyNotifications from './EmptyNotifications';

interface NotificationDropdownProps {
  notifications: Notification[];
  isLoading: boolean;
  error: Error | null;
  isMarkingAll: boolean;
  onMarkAllRead: () => void;
  onMarkRead: (id: string) => void;
}

export default function NotificationDropdown({
  notifications,
  isLoading,
  error,
  isMarkingAll,
  onMarkAllRead,
  onMarkRead,
}: NotificationDropdownProps) {
  const hasUnread = notifications.some((n) => !n.readAt);

  return (
    <div className="notification-dropdown" role="menu">
      <div className="notification-dropdown__header">
        <span className="notification-dropdown__title">Notifications</span>
        <button
          type="button"
          className="notification-dropdown__mark-all"
          onClick={onMarkAllRead}
          disabled={!hasUnread || isMarkingAll}
        >
          {isMarkingAll ? 'Marking...' : 'Mark all as read'}
        </button>
      </div>

      {isLoading && <div className="notification-loading">Loading...</div>}

      {!isLoading && error && (
        <div className="notification-error">Failed to load notifications.</div>
      )}

      {!isLoading && !error && notifications.length === 0 && <EmptyNotifications />}

      {!isLoading && !error && notifications.length > 0 && (
        <NotificationList notifications={notifications} onMarkRead={onMarkRead} />
      )}
    </div>
  );
}
