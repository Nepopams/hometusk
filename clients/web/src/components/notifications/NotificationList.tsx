import type { Notification } from '../../types/api';
import NotificationItem from './NotificationItem';

interface NotificationListProps {
  notifications: Notification[];
  onMarkRead: (id: string) => void;
}

export default function NotificationList({ notifications, onMarkRead }: NotificationListProps) {
  return (
    <div className="notification-dropdown__list">
      {notifications.map((notification) => (
        <NotificationItem
          key={notification.id}
          notification={notification}
          onMarkRead={onMarkRead}
        />
      ))}
    </div>
  );
}
