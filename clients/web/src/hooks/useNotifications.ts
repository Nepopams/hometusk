import { useCallback, useEffect, useMemo, useState } from 'react';
import { listNotifications, markNotificationRead } from '../lib/api';
import type { Notification } from '../types/api';

export function useNotifications(householdId: string | undefined) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [isMarkingAll, setIsMarkingAll] = useState(false);

  const unreadCount = useMemo(
    () => notifications.filter((n) => !n.readAt).length,
    [notifications]
  );

  const fetchNotifications = useCallback(async () => {
    if (!householdId) {
      setNotifications([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const data = await listNotifications(householdId);
      setNotifications(data);
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to load notifications'));
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const markAsRead = useCallback(async (id: string) => {
    const updated = await markNotificationRead(id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, readAt: updated.readAt } : n))
    );
  }, []);

  const markAllAsRead = useCallback(async () => {
    const unread = notifications.filter((n) => !n.readAt);
    if (unread.length === 0) return;

    setIsMarkingAll(true);
    try {
      await Promise.all(unread.map((n) => markNotificationRead(n.id)));
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, readAt: n.readAt || new Date().toISOString() }))
      );
    } finally {
      setIsMarkingAll(false);
    }
  }, [notifications]);

  const addNotification = useCallback((notification: Notification) => {
    setNotifications((prev) => {
      if (prev.some((n) => n.id === notification.id)) return prev;
      return [notification, ...prev];
    });
  }, []);

  return {
    notifications,
    unreadCount,
    isLoading,
    error,
    isMarkingAll,
    markAsRead,
    markAllAsRead,
    addNotification,
    refresh: fetchNotifications,
  };
}
