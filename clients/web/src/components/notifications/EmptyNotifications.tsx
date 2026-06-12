import { useI18n } from '../../i18n';

export default function EmptyNotifications() {
  const { t } = useI18n();

  return (
    <div className="notification-empty">
      <p>{t('notifications.noNotificationsYet')}</p>
    </div>
  );
}
