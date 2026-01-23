# Codex APPLY Prompt: ST-602 + ST-603 — Web Notifications UI + Realtime Subscribe

## Mode
**APPLY** — Implementation mode. File modifications allowed.

## Allowed Operations
```
- Create/edit TypeScript/TSX files in clients/web/src/
- Edit CSS files in clients/web/src/styles/
- Run npm build/lint commands
```

## Forbidden
- Modifying files outside clients/web/
- Adding new npm dependencies without approval
- Changing unrelated functionality

---

## Task
Implement web notifications UI (bell icon, dropdown, list) and real-time SSE subscription.

## Sources of Truth (MUST READ)
1. `docs/planning/workpacks/ST-602-603/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-007/stories/ST-602-web-notifications-ui.md` — UI story spec
3. `docs/planning/epics/EP-007/stories/ST-603-web-realtime-subscribe.md` — Realtime story spec
4. `docs/contracts/http/commands.openapi.yaml` — Notification schema

## Approved Plan Summary
- Add notification functions to existing `api.ts` (not separate file)
- Use ASCII icons (no new icon dependencies)
- Mirror HouseholdDropdown patterns for click outside/ESC
- EventSource with `withCredentials: true`

---

## Critical Constraints

### 1. Notification Types
```typescript
// types/notification.ts
export type NotificationType =
  | 'invite_accepted'
  | 'task_assigned'
  | 'task_completed'
  | 'shopping_item_added'
  | 'shopping_item_purchased';

export interface NotificationPayload {
  actorId: string;
  actorName: string;
  entityId: string;
  entityType: string;
  summary: string;
}

export interface Notification {
  id: string;
  householdId: string;
  userId: string;
  type: NotificationType;
  payload: NotificationPayload;
  createdAt: string;
  readAt: string | null;
}
```

### 2. API Functions (add to api.ts)
```typescript
export interface NotificationFilters {
  since?: string;
  limit?: number;
}

export async function listNotifications(
  householdId: string,
  filters: NotificationFilters = {}
): Promise<Notification[]> {
  const params = new URLSearchParams();
  if (filters.since) params.set('since', filters.since);
  if (filters.limit) params.set('limit', String(filters.limit));
  const query = params.toString();
  return apiFetch<Notification[]>(
    `/households/${householdId}/notifications${query ? `?${query}` : ''}`
  );
}

export async function markNotificationRead(notificationId: string): Promise<Notification> {
  return apiFetch<Notification>(`/notifications/${notificationId}/read`, {
    method: 'POST',
  });
}
```

### 3. useNotifications Hook
```typescript
// hooks/useNotifications.ts
export function useNotifications(householdId: string | undefined) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [isMarkingAll, setIsMarkingAll] = useState(false);

  const unreadCount = useMemo(
    () => notifications.filter(n => !n.readAt).length,
    [notifications]
  );

  const fetchNotifications = useCallback(async () => {
    if (!householdId) {
      setNotifications([]);
      setIsLoading(false);
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
    setNotifications(prev =>
      prev.map(n => (n.id === id ? { ...n, readAt: updated.readAt } : n))
    );
  }, []);

  const markAllAsRead = useCallback(async () => {
    const unread = notifications.filter(n => !n.readAt);
    if (unread.length === 0) return;
    setIsMarkingAll(true);
    try {
      await Promise.all(unread.map(n => markNotificationRead(n.id)));
      setNotifications(prev =>
        prev.map(n => ({ ...n, readAt: n.readAt || new Date().toISOString() }))
      );
    } finally {
      setIsMarkingAll(false);
    }
  }, [notifications]);

  const addNotification = useCallback((notification: Notification) => {
    setNotifications(prev => {
      if (prev.some(n => n.id === notification.id)) return prev;
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
```

### 4. useNotificationStream Hook
```typescript
// hooks/useNotificationStream.ts
export function useNotificationStream(
  householdId: string | undefined,
  onNotification: (notification: Notification) => void,
  onAuthError: () => void
) {
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
  const eventSourceRef = useRef<EventSource | null>(null);
  const retryCountRef = useRef(0);
  const retryTimeoutRef = useRef<number | null>(null);

  const MAX_RETRIES = 10;
  const BASE_DELAY = 1000;
  const MAX_DELAY = 30000;

  useEffect(() => {
    if (!householdId) {
      setStatus('disconnected');
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
          console.warn('Failed to parse notification event', e);
        }
      });

      eventSource.addEventListener('heartbeat', () => {
        // Connection alive, no action needed
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
        retryCountRef.current++;
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
```

### 5. Component Structure
```
components/notifications/
├── NotificationBell.tsx      # Bell + badge + dropdown trigger
├── NotificationDropdown.tsx  # Container with header + list
├── NotificationList.tsx      # Map notifications to items
├── NotificationItem.tsx      # Single notification row
├── UnreadBadge.tsx          # Count badge (0 hidden, 99+ cap)
├── EmptyNotifications.tsx   # Empty state
└── index.ts                 # Barrel exports
```

### 6. Icon Mapping (ASCII, no deps)
```typescript
const NOTIFICATION_ICONS: Record<NotificationType, string> = {
  invite_accepted: '👤',
  task_assigned: '📋',
  task_completed: '✅',
  shopping_item_added: '🛒',
  shopping_item_purchased: '🛍️',
};
```

### 7. CSS Classes (BEM pattern)
```css
/* Notification Bell */
.notification-bell { position: relative; }
.notification-bell__button { /* bell button styles */ }
.notification-bell__badge { /* red badge, absolute positioned */ }

/* Dropdown */
.notification-dropdown { /* absolute, shadow, z-index */ }
.notification-dropdown__header { /* flex, justify-between */ }
.notification-dropdown__title { /* bold */ }
.notification-dropdown__mark-all { /* button styles */ }
.notification-dropdown__list { /* max-height, overflow-y */ }

/* Item */
.notification-item { /* flex, padding, hover */ }
.notification-item--unread { /* bold, dot indicator */ }
.notification-item__icon { /* emoji size */ }
.notification-item__content { /* flex-1 */ }
.notification-item__summary { /* text */ }
.notification-item__time { /* small, muted */ }

/* States */
.notification-empty { /* centered text */ }
.notification-loading { /* spinner */ }
```

---

## Implementation Steps

### Step 1: Create Types
Create `clients/web/src/types/notification.ts` with Notification types.

### Step 2: Add API Functions
Add `listNotifications` and `markNotificationRead` to `clients/web/src/lib/api.ts`.

### Step 3: Create useNotifications Hook
Create `clients/web/src/hooks/useNotifications.ts`.

### Step 4: Create useNotificationStream Hook
Create `clients/web/src/hooks/useNotificationStream.ts`.

### Step 5: Create Notification Components
Create components in `clients/web/src/components/notifications/`:
- `UnreadBadge.tsx`
- `EmptyNotifications.tsx`
- `NotificationItem.tsx`
- `NotificationList.tsx`
- `NotificationDropdown.tsx`
- `NotificationBell.tsx`
- `index.ts`

### Step 6: Update Header
Modify `clients/web/src/components/Layout/Header.tsx` to include NotificationBell.

### Step 7: Add CSS
Add notification styles to `clients/web/src/styles/index.css`.

### Step 8: Export Types
Update `clients/web/src/types/api.ts` to re-export notification types.

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Type check
npm run typecheck

# Lint
npm run lint

# Build
npm run build

# Dev server for manual testing
npm run dev
```

---

## Manual Test Cases

1. Open household page → verify bell icon visible in header
2. Verify unread badge shows correct count
3. Click bell → dropdown opens
4. Click outside → dropdown closes
5. Click notification → marked as read, count decreases
6. Click "Mark all as read" → all marked, count becomes 0
7. Open DevTools Network → verify SSE connection (EventStream)
8. Create task in another tab → notification appears without refresh
9. Disconnect network → verify reconnect attempts with backoff

---

## DoD Checklist
- [ ] npm run lint passes
- [ ] npm run build passes
- [ ] Bell icon visible in header
- [ ] Unread badge shows correct count
- [ ] Dropdown opens/closes correctly
- [ ] Notifications display with icons
- [ ] Mark as read works
- [ ] Mark all as read works (sequential calls)
- [ ] SSE connection established
- [ ] Real-time notifications work
- [ ] Auto-reconnect on disconnect
- [ ] No console errors

---

## STOP-THE-LINE Rules
If any of these occur, STOP and report:
- EventSource fails to connect (CORS/cookie issue)
- API endpoints return unexpected format
- TypeScript errors that can't be resolved
- Build fails

Do NOT proceed with workarounds without approval.
