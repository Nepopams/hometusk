import { useCallback, useEffect, useRef, useState } from 'react';
import { listNotifications } from '../lib/api';
import type { Notification } from '../types/api';

type ConnectionMode = 'sse' | 'polling';
type StreamStatus = 'connecting' | 'connected' | 'disconnected' | 'polling' | 'error';

export function useNotificationStream(
  householdId: string | undefined,
  onNotification: (notification: Notification) => void,
  onAuthError: () => void
) {
  const [mode, setMode] = useState<ConnectionMode>('sse');
  const [status, setStatus] = useState<StreamStatus>('connecting');

  const eventSourceRef = useRef<EventSource | null>(null);
  const retryCountRef = useRef(0);
  const retryTimeoutRef = useRef<number | null>(null);
  const pollIntervalRef = useRef<number | null>(null);
  const sseRetryIntervalRef = useRef<number | null>(null);
  const lastFetchRef = useRef<string>(new Date().toISOString());

  const MAX_SSE_RETRIES = 5;
  const MAX_TOTAL_RETRIES = 10;
  const BASE_DELAY = 1000;
  const MAX_DELAY = 30000;
  const POLL_INTERVAL = 30000;
  const SSE_RETRY_INTERVAL = 60000;

  const clearAllTimers = useCallback(() => {
    if (retryTimeoutRef.current) {
      clearTimeout(retryTimeoutRef.current);
      retryTimeoutRef.current = null;
    }
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
    if (sseRetryIntervalRef.current) {
      clearInterval(sseRetryIntervalRef.current);
      sseRetryIntervalRef.current = null;
    }
  }, []);

  const fetchNewNotifications = useCallback(async () => {
    if (!householdId) return;

    try {
      const since = lastFetchRef.current;
      const notifications = await listNotifications(householdId, { since });

      lastFetchRef.current = new Date().toISOString();
      notifications.forEach(onNotification);
    } catch (error) {
      console.debug('[Notifications] Polling failed, will retry', error);
    }
  }, [householdId, onNotification]);

  const startPolling = useCallback(() => {
    setMode('polling');
    setStatus('polling');
    fetchNewNotifications();
    pollIntervalRef.current = window.setInterval(fetchNewNotifications, POLL_INTERVAL);
  }, [fetchNewNotifications]);

  const attemptSseReconnect = useCallback(() => {
    if (!householdId) return;

    const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
    const url = `${baseUrl}/households/${householdId}/notifications/stream`;

    const testSource = new EventSource(url, { withCredentials: true });

    testSource.onopen = () => {
      testSource.close();
      clearAllTimers();
      retryCountRef.current = 0;
      setMode('sse');
    };

    testSource.onerror = () => {
      testSource.close();
    };

    setTimeout(() => {
      if (testSource.readyState !== EventSource.CLOSED) {
        testSource.close();
      }
    }, 5000);
  }, [householdId, clearAllTimers]);

  useEffect(() => {
    if (!householdId) {
      setStatus('disconnected');
      setMode('sse');
      eventSourceRef.current?.close();
      clearAllTimers();
      return;
    }

    if (mode === 'polling') {
      startPolling();
      sseRetryIntervalRef.current = window.setInterval(attemptSseReconnect, SSE_RETRY_INTERVAL);

      return () => {
        clearAllTimers();
      };
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

        retryCountRef.current += 1;

        if (retryCountRef.current >= MAX_SSE_RETRIES) {
          console.debug('[Notifications] SSE unavailable, switching to polling');
          setMode('polling');
          return;
        }

        if (retryCountRef.current >= MAX_TOTAL_RETRIES) {
          setStatus('error');
          onAuthError();
          return;
        }

        setStatus('disconnected');
        const delay = Math.min(BASE_DELAY * Math.pow(2, retryCountRef.current), MAX_DELAY);
        retryTimeoutRef.current = window.setTimeout(connect, delay);
      };
    };

    connect();

    return () => {
      isMounted = false;
      eventSourceRef.current?.close();
      clearAllTimers();
    };
  }, [
    householdId,
    mode,
    onNotification,
    onAuthError,
    startPolling,
    attemptSseReconnect,
    clearAllTimers,
  ]);

  return { mode, status };
}
