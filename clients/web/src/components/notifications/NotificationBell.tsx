import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useI18n } from '../../i18n';
import { useNotifications } from '../../hooks/useNotifications';
import { useNotificationStream } from '../../hooks/useNotificationStream';
import NotificationDropdown from './NotificationDropdown';
import UnreadBadge from './UnreadBadge';

export default function NotificationBell() {
  const { t } = useI18n();
  const { householdId, logout } = useAuth();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const {
    notifications,
    unreadCount,
    isLoading,
    error,
    isMarkingAll,
    markAsRead,
    markAllAsRead,
    addNotification,
  } = useNotifications(householdId ?? undefined);

  const handleAuthError = useCallback(() => {
    void logout();
    navigate('/login?error=session_expired', { replace: true });
  }, [logout, navigate]);

  const { mode } = useNotificationStream(
    householdId ?? undefined,
    addNotification,
    handleAuthError
  );

  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  useEffect(() => {
    setIsOpen(false);
  }, [householdId]);

  if (!householdId) {
    return null;
  }

  return (
    <div className="notification-bell" ref={containerRef}>
      <button
        type="button"
        className="notification-bell__button"
        aria-label={t('notifications.title')}
        aria-expanded={isOpen}
        aria-haspopup="menu"
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <svg className="notification-bell__icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6V11a6 6 0 0 0-5-5.92V4a1 1 0 0 0-2 0v1.08A6 6 0 0 0 6 11v5l-2 2v1h16v-1l-2-2z"
            fill="currentColor"
          />
        </svg>
        <UnreadBadge count={unreadCount} />
      </button>
      {mode === 'polling' && (
        <span
          className="notification-bell__degraded"
          title={t('notifications.realtimeUnavailable')}
        >
          !
        </span>
      )}

      {isOpen && (
        <NotificationDropdown
          notifications={notifications}
          isLoading={isLoading}
          error={error}
          isMarkingAll={isMarkingAll}
          onMarkAllRead={markAllAsRead}
          onMarkRead={markAsRead}
        />
      )}
    </div>
  );
}
