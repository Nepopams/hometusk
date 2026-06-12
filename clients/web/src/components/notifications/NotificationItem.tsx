import type { Notification, NotificationType } from '../../types/api';
import { useI18n } from '../../i18n';

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

export default function NotificationItem({ notification, onMarkRead }: NotificationItemProps) {
  const { t, formatRelativeTime, formatDateTime } = useI18n();
  const isUnread = !notification.readAt;
  const summary = notification.payload?.summary || t('notifications.newNotification');
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
      title={formatDateTime(notification.createdAt)}
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
