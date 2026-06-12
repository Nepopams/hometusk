import type { Notification } from '../../types/api';
import { useI18n } from '../../i18n';
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
  const { t } = useI18n();
  const hasUnread = notifications.some((n) => !n.readAt);

  return (
    <div className="notification-dropdown" role="menu">
      <div className="notification-dropdown__header">
        <span className="notification-dropdown__title">{t('notifications.title')}</span>
        <button
          type="button"
          className="notification-dropdown__mark-all"
          onClick={onMarkAllRead}
          disabled={!hasUnread || isMarkingAll}
        >
          {isMarkingAll ? t('notifications.marking') : t('notifications.markAllRead')}
        </button>
      </div>

      {isLoading && <div className="notification-loading">{t('common.loading')}</div>}

      {!isLoading && error && (
        <div className="notification-error">{t('notifications.failedLoad')}</div>
      )}

      {!isLoading && !error && notifications.length === 0 && <EmptyNotifications />}

      {!isLoading && !error && notifications.length > 0 && (
        <NotificationList notifications={notifications} onMarkRead={onMarkRead} />
      )}
    </div>
  );
}
