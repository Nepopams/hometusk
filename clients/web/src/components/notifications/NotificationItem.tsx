import type { Notification, NotificationType } from '../../types/api';

interface NotificationItemProps {
  notification: Notification;
  onMarkRead: (id: string) => void;
}

const NOTIFICATION_ICONS: Record<NotificationType, string> = {
  invite_accepted: 'INV',
  task_assigned: 'TASK',
  task_completed: 'DONE',
  shopping_item_added: 'ADD',
  shopping_item_purchased: 'BUY',
};

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

export default function NotificationItem({ notification, onMarkRead }: NotificationItemProps) {
  const isUnread = !notification.readAt;
  const summary = notification.payload?.summary || 'New notification';
  const relativeTime = formatRelativeTime(notification.createdAt);

  const handleClick = () => {
    if (isUnread) {
      onMarkRead(notification.id);
    }
  };

  return (
    <button
      type="button"
      className={`notification-item ${isUnread ? 'notification-item--unread' : ''}`}
      onClick={handleClick}
      title={new Date(notification.createdAt).toLocaleString()}
    >
      <span className="notification-item__icon">{NOTIFICATION_ICONS[notification.type]}</span>
      <span className="notification-item__content">
        <span className="notification-item__summary">{summary}</span>
        <span className="notification-item__time">{relativeTime}</span>
      </span>
      {isUnread && <span className="notification-item__dot" aria-hidden="true" />}
    </button>
  );
}
