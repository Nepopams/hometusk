# Codex APPLY Prompt: ST-604 — Degraded Fallback (Polling)

## Mode
**APPLY** — Implementation mode. File modifications allowed.

## Allowed Operations
```
- Edit TypeScript/TSX files in clients/web/src/
- Edit CSS files in clients/web/src/styles/
- Run npm build/lint commands
```

## Forbidden
- Modifying backend files
- Adding new npm dependencies

---

## Task
Add polling fallback when SSE connection fails after max retries.

## Sources of Truth
1. `docs/planning/epics/EP-007/stories/ST-604-degraded-fallback.md` — Story spec
2. `clients/web/src/hooks/useNotificationStream.ts` — Current hook to modify
3. `clients/web/src/lib/api.ts` — `listNotifications` with `since` param

---

## Implementation

### Step 1: Update useNotificationStream.ts

Replace the current hook with polling fallback support:

```typescript
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

  const MAX_SSE_RETRIES = 5;  // Switch to polling after this
  const MAX_TOTAL_RETRIES = 10;  // Give up completely
  const BASE_DELAY = 1000;
  const MAX_DELAY = 30000;
  const POLL_INTERVAL = 30000;  // 30 seconds
  const SSE_RETRY_INTERVAL = 60000;  // Try SSE again every 60s while polling

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

      // Update timestamp before processing to avoid gaps
      lastFetchRef.current = new Date().toISOString();

      // Add each notification (dedup happens in onNotification)
      notifications.forEach(onNotification);
    } catch (error) {
      // Silent fail, will retry on next interval
      console.debug('[Notifications] Polling failed, will retry', error);
    }
  }, [householdId, onNotification]);

  const startPolling = useCallback(() => {
    setMode('polling');
    setStatus('polling');

    // Fetch immediately
    fetchNewNotifications();

    // Set up polling interval
    pollIntervalRef.current = window.setInterval(fetchNewNotifications, POLL_INTERVAL);
  }, [fetchNewNotifications]);

  const attemptSseReconnect = useCallback(() => {
    if (!householdId) return;

    const baseUrl = import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '');
    const url = `${baseUrl}/households/${householdId}/notifications/stream`;

    // Try to connect
    const testSource = new EventSource(url, { withCredentials: true });

    testSource.onopen = () => {
      // SSE is back! Switch modes
      testSource.close();
      clearAllTimers();
      retryCountRef.current = 0;
      setMode('sse');
      // The main effect will re-establish connection
    };

    testSource.onerror = () => {
      // Still unavailable, stay in polling mode
      testSource.close();
    };

    // Clean up after 5 seconds if no response
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

    // If in polling mode, start polling and SSE retry
    if (mode === 'polling') {
      startPolling();
      sseRetryIntervalRef.current = window.setInterval(attemptSseReconnect, SSE_RETRY_INTERVAL);

      return () => {
        clearAllTimers();
      };
    }

    // SSE mode
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

        // After MAX_SSE_RETRIES, switch to polling
        if (retryCountRef.current >= MAX_SSE_RETRIES) {
          console.debug('[Notifications] SSE unavailable, switching to polling');
          setMode('polling');
          return;
        }

        // After MAX_TOTAL_RETRIES, give up completely
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
  }, [householdId, mode, onNotification, onAuthError, startPolling, attemptSseReconnect, clearAllTimers]);

  return { mode, status };
}
```

### Step 2: Update NotificationBell.tsx

Add degraded indicator:

```typescript
// Add to imports
// (no new imports needed)

// Get mode from hook
const { mode } = useNotificationStream(householdId ?? undefined, addNotification, handleAuthError);

// Add degraded indicator after the button, before dropdown
{mode === 'polling' && (
  <span
    className="notification-bell__degraded"
    title="Real-time updates unavailable. Checking every 30 seconds."
  >
    ⚠
  </span>
)}
```

### Step 3: Add CSS to index.css

```css
/* Degraded mode indicator */
.notification-bell__degraded {
  position: absolute;
  top: -2px;
  left: -2px;
  font-size: 10px;
  color: var(--color-warning, #f59e0b);
  cursor: help;
  opacity: 0.8;
}

.notification-bell__degraded:hover {
  opacity: 1;
}
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Lint
npm run lint

# Build
npm run build
```

---

## Manual Test Cases

1. **Normal SSE**: Open app → verify bell works, no warning icon
2. **Block SSE**: DevTools → Network → Block `/notifications/stream`
   - Wait for 5 retries (~30s with backoff)
   - Verify ⚠ indicator appears
   - Verify notifications still arrive (every 30s)
3. **Unblock SSE**:
   - Wait up to 60s for auto-recovery
   - Verify ⚠ indicator disappears
   - Verify real-time notifications work again
4. **No duplicates**: Create notifications while in polling mode, verify no duplicates in list

---

## DoD Checklist
- [ ] npm run lint passes
- [ ] npm run build passes
- [ ] Polling fallback works after 5 SSE failures
- [ ] Degraded indicator shown in polling mode
- [ ] Auto-recovery to SSE works
- [ ] No duplicate notifications
- [ ] Silent degradation (no error modals)

---

## STOP-THE-LINE Rules
- If `listNotifications` doesn't support `since` param → STOP
- If polling causes duplicate notifications → STOP
- Do NOT proceed with workarounds without approval
