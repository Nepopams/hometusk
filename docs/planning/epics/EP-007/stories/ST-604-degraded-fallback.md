# Story: ST-604 — Degraded Fallback

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (notifications endpoints)

---

## Status
**Done** — Implementation complete (commit e3251dc)

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
- **Use existing `since` parameter** (already supported in OpenAPI)
- Show subtle indicator of degraded mode
- Auto-switch back to SSE when available
- Silent degradation (no error modals)

---

## Contract Verification
The existing `GET /households/{householdId}/notifications` endpoint already supports the `since` query parameter:
- Type: RFC3339 timestamp string
- Filters notifications created after the given timestamp
- Location: `NotificationController.java:57-59`

**No contract changes required for polling.**

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
Then GET /notifications?since={lastFetchTimestamp} is called
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
When SSE endpoint becomes available (periodic check every 60s)
Then switch back to SSE mode
And degraded indicator disappears
```

### AC-5: No Duplicate Notifications (Client-side Dedup)
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
- Block SSE endpoint (DevTools Network → Block request URL) → verify polling starts
- Verify notifications arrive via polling (30s interval)
- Unblock SSE → verify auto-recovery (up to 60s)
- Verify no duplicate notifications

### Unit Tests
- Test retry logic and max retry threshold
- Test polling interval
- Test client-side deduplication logic

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Using existing `since` parameter |
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
  onNotification: (notification: Notification) => void,
  onAuthError: () => void
) {
  const [mode, setMode] = useState<'sse' | 'polling'>('sse');
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'polling'>('connecting');
  const lastFetchRef = useRef<string>(new Date().toISOString());
  const MAX_SSE_RETRIES = 5;
  const POLL_INTERVAL = 30000;
  const SSE_RETRY_INTERVAL = 60000;

  // ... SSE connection logic (from ST-603) ...

  const switchToPolling = () => {
    setMode('polling');
    setStatus('polling');
    startPolling();
  };

  const startPolling = () => {
    // Poll immediately
    fetchNewNotifications();

    // Set up polling interval
    const pollInterval = setInterval(fetchNewNotifications, POLL_INTERVAL);

    // Periodically try SSE again
    const sseRetryInterval = setInterval(() => {
      attemptSseReconnect();
    }, SSE_RETRY_INTERVAL);

    return () => {
      clearInterval(pollInterval);
      clearInterval(sseRetryInterval);
    };
  };

  const fetchNewNotifications = async () => {
    try {
      const since = lastFetchRef.current;
      const notifications = await api.listNotifications(householdId, { since });

      // Update last fetch timestamp
      lastFetchRef.current = new Date().toISOString();

      // Add each (dedup happens in onNotification)
      notifications.forEach(onNotification);
    } catch (error) {
      // Silent fail, will retry on next interval
      console.debug('Polling failed, will retry', error);
    }
  };

  const attemptSseReconnect = () => {
    // Try to establish SSE connection
    // If successful, switch back to SSE mode
    // If fails, stay in polling mode
  };

  return { mode, status };
}
```

### Degraded Indicator Component
```tsx
function ConnectionStatus({ mode }: { mode: 'sse' | 'polling' }) {
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

### Client-side Deduplication
Already implemented in `useNotifications.addNotification()`:
```typescript
const addNotification = (notification: Notification) => {
  setNotifications(prev => {
    // Check if already exists by ID
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
  cursor: help;
}

.connection-degraded:hover {
  opacity: 1;
}
```
