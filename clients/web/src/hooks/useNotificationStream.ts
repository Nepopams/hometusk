import { useEffect, useRef, useState } from 'react';
import type { Notification } from '../types/api';

type StreamStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

export function useNotificationStream(
  householdId: string | undefined,
  onNotification: (notification: Notification) => void,
  onAuthError: () => void
) {
  const [status, setStatus] = useState<StreamStatus>('connecting');
  const eventSourceRef = useRef<EventSource | null>(null);
  const retryCountRef = useRef(0);
  const retryTimeoutRef = useRef<number | null>(null);

  const MAX_RETRIES = 10;
  const BASE_DELAY = 1000;
  const MAX_DELAY = 30000;

  useEffect(() => {
    if (!householdId) {
      setStatus('disconnected');
      eventSourceRef.current?.close();
      return;
    }

    let isMounted = true;

    const connect = () => {
      if (!isMounted) return;

      const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
      const url = `${baseUrl}/households/${householdId}/notifications/stream`;

      const eventSource = new EventSource(url, { withCredentials: true });
      eventSourceRef.current = eventSource;
      setStatus('connecting');

      eventSource.onopen = () => {
        if (!isMounted) return;
        setStatus('connected');
        retryCountRef.current = 0;
      };

      eventSource.addEventListener('notification', (event) => {
        if (!isMounted) return;
        try {
          const notification = JSON.parse(event.data) as Notification;
          onNotification(notification);
        } catch (e) {
          console.warn('[Notifications] Failed to parse event', e);
        }
      });

      eventSource.addEventListener('heartbeat', () => {
        // No-op heartbeat
      });

      eventSource.onerror = () => {
        if (!isMounted) return;
        eventSource.close();

        if (retryCountRef.current >= MAX_RETRIES) {
          setStatus('error');
          onAuthError();
          return;
        }

        setStatus('disconnected');
        const delay = Math.min(BASE_DELAY * Math.pow(2, retryCountRef.current), MAX_DELAY);
        retryCountRef.current += 1;
        retryTimeoutRef.current = window.setTimeout(connect, delay);
      };
    };

    connect();

    return () => {
      isMounted = false;
      eventSourceRef.current?.close();
      if (retryTimeoutRef.current) {
        clearTimeout(retryTimeoutRef.current);
      }
    };
  }, [householdId, onNotification, onAuthError]);

  return { status };
}
