# Story: ST-604 — Degraded Fallback

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — DoR complete, pending sprint commitment

## Priority
P2 (Enhancement)

## Points
2

---

## Description
When SSE realtime connection is unavailable (network issues, server down), fall back to periodic polling to ensure users still receive notifications.

### User Value
As a household member, I want notifications to still work even when realtime is unavailable, so I don't miss important updates.

### Technical Approach
- Detect SSE connection failure after max retries
- Switch to polling mode (every 30s)
- Show subtle indicator of degraded mode
- Auto-switch back to SSE when available
- Silent degradation (no error modals)

---

## Acceptance Criteria

### AC-1: Fallback After Max Retries
```gherkin
Given SSE connection fails
And reconnect attempted 5 times (max retries)
When all retries fail
Then switch to polling mode
And poll every 30 seconds
```

### AC-2: Polling Fetches New Notifications
```gherkin
Given app is in polling mode
When 30 seconds pass
Then GET /notifications is called with since={lastFetch}
And new notifications added to list
And unread count updated
```

### AC-3: Degraded Mode Indicator
```gherkin
Given app is in polling mode
Then subtle indicator shows in header
  (e.g., small icon or tooltip "Updates may be delayed")
And no intrusive error message
```

### AC-4: Auto-recovery to SSE
```gherkin
Given app is in polling mode
When SSE endpoint becomes available
Then switch back to SSE mode
And degraded indicator disappears
```

### AC-5: No Duplicate Notifications
```gherkin
Given polling returns notifications
When same notification received multiple times
Then only one instance shown in list
```

### AC-6: Silent Degradation
```gherkin
Given SSE fails
Then no error modal shown
And no console errors visible to user
And app continues to function
```

---

## Test Strategy

### Manual Tests
- Block SSE endpoint (DevTools) → verify polling starts
- Verify notifications arrive via polling
- Unblock SSE → verify auto-recovery
- Verify no duplicate notifications

### Unit Tests
- Test retry logic and max retry threshold
- Test polling interval
- Test deduplication logic

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Using existing API |
| adr_needed | no | Standard fallback pattern |
| diagrams_needed | no | |
| security_sensitive | no | |
| traceability_critical | no | |

---

## Dependencies
- ST-602 (Notifications UI)
- ST-603 (Realtime Subscribe)

## Blocked By
- ST-603

---

## Implementation Notes

### Updated useNotificationStream Hook
```typescript
export function useNotificationStream(
  householdId: string,
  onNotification: (notification: Notification) => void
) {
  const [mode, setMode] = useState<'sse' | 'polling'>('sse');
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'polling'>('connecting');
  const MAX_RETRIES = 5;
  const POLL_INTERVAL = 30000;

  // ... SSE logic with retry count ...

  const startPolling = () => {
    setMode('polling');
    setStatus('polling');

    // Poll immediately
    fetchNewNotifications();

    // Set up interval
    const interval = setInterval(fetchNewNotifications, POLL_INTERVAL);

    // Periodically try SSE again
    const sseRetry = setInterval(() => {
      attemptSseReconnect();
    }, 60000); // Try SSE every minute

    return () => {
      clearInterval(interval);
      clearInterval(sseRetry);
    };
  };

  const fetchNewNotifications = async () => {
    const since = lastFetchRef.current;
    const notifications = await api.listNotifications(householdId, { since });
    notifications.forEach(onNotification);
    lastFetchRef.current = new Date().toISOString();
  };

  return { mode, status };
}
```

### Degraded Indicator Component
```tsx
function ConnectionStatus({ mode, status }: { mode: 'sse' | 'polling', status: string }) {
  if (mode === 'polling') {
    return (
      <span
        className="connection-degraded"
        title="Real-time updates unavailable. Checking for updates every 30 seconds."
      >
        <CloudOffIcon size={14} />
      </span>
    );
  }
  return null;
}
```

### Deduplication
```typescript
const addNotification = (notification: Notification) => {
  setNotifications(prev => {
    // Check if already exists
    if (prev.some(n => n.id === notification.id)) {
      return prev;
    }
    // Add to beginning
    return [notification, ...prev];
  });
};
```

### CSS
```css
.connection-degraded {
  color: var(--color-warning);
  opacity: 0.7;
  margin-left: 8px;
}

.connection-degraded:hover {
  opacity: 1;
}
```
