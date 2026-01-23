# Story: ST-603 — Web Realtime Subscribe

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — DoR complete, pending sprint commitment

## Priority
P1 (Core)

## Points
3

---

## Description
Connect web client to SSE realtime endpoint and update notifications UI when new events arrive.

### User Value
As a household member, I want notifications to appear instantly without refreshing, so I can react quickly to household activity.

### Technical Approach
- EventSource API to connect to SSE endpoint
- Parse incoming notification events
- Update notifications state via hook
- Auto-reconnect on disconnect (exponential backoff)
- Connection lifecycle management

---

## Acceptance Criteria

### AC-1: SSE Connection Established
```gherkin
Given user is authenticated and viewing household
When household page loads
Then EventSource connects to /notifications/stream
And connection status is "connected"
```

### AC-2: Notification Received in Real-time
```gherkin
Given active SSE connection
When server sends notification event
Then notification appears in bell dropdown instantly
And unread count increases
And no page refresh needed
```

### AC-3: Auto-reconnect on Disconnect
```gherkin
Given active SSE connection
When connection drops (network issue)
Then client waits 1 second
And attempts to reconnect
And uses exponential backoff (1s, 2s, 4s, 8s, max 30s)
```

### AC-4: Token Refresh on 401
```gherkin
Given SSE connection fails with 401
When token is expired
Then client refreshes token
And reconnects with new token
```

### AC-5: Cleanup on Household Change
```gherkin
Given active SSE connection for household H1
When user switches to household H2
Then H1 connection is closed
And new connection opened for H2
```

### AC-6: Cleanup on Logout
```gherkin
Given active SSE connection
When user logs out
Then connection is closed
And no reconnect attempts
```

---

## Test Strategy

### Manual Tests
- Open app → verify SSE connected (DevTools Network tab)
- Create task in another tab → verify notification appears
- Disable network → verify reconnect attempts
- Switch households → verify new connection

### Integration Tests
- Mock SSE server, verify event handling
- Verify reconnect logic
- Verify cleanup on unmount

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Using ST-601 endpoint |
| adr_needed | no | EventSource is standard |
| diagrams_needed | no | |
| security_sensitive | yes | Token in SSE connection |
| traceability_critical | no | |

---

## Dependencies
- ST-601 (SSE Endpoint) must be complete

## Blocked By
- ST-601

---

## Implementation Notes

### Hook: useNotificationStream
```typescript
export function useNotificationStream(
  householdId: string,
  onNotification: (notification: Notification) => void
) {
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
  const eventSourceRef = useRef<EventSource | null>(null);
  const retryCountRef = useRef(0);

  useEffect(() => {
    const connect = () => {
      const token = getAccessToken();
      const url = `${API_BASE}/households/${householdId}/notifications/stream?token=${token}`;

      const eventSource = new EventSource(url);
      eventSourceRef.current = eventSource;

      eventSource.onopen = () => {
        setStatus('connected');
        retryCountRef.current = 0;
      };

      eventSource.addEventListener('notification', (event) => {
        const notification = JSON.parse(event.data);
        onNotification(notification);
      });

      eventSource.addEventListener('heartbeat', () => {
        // Connection alive, no action needed
      });

      eventSource.onerror = () => {
        setStatus('disconnected');
        eventSource.close();
        scheduleReconnect();
      };
    };

    const scheduleReconnect = () => {
      const delay = Math.min(1000 * Math.pow(2, retryCountRef.current), 30000);
      retryCountRef.current++;
      setTimeout(connect, delay);
    };

    connect();

    return () => {
      eventSourceRef.current?.close();
    };
  }, [householdId, onNotification]);

  return { status };
}
```

### Integration with useNotifications
```typescript
// In HouseholdLayout or NotificationProvider
const { addNotification, refresh } = useNotifications(householdId);

const { status } = useNotificationStream(householdId, (notification) => {
  addNotification(notification);
});

// Optional: show connection status indicator
```

### Token Handling Options
1. **Query param**: `?token=...` (simpler, but token in URL)
2. **Cookie**: Set HttpOnly cookie before connection (more secure)
3. **Custom header**: Not supported by EventSource natively

Recommendation: Use query param for MVP, document security consideration.

### Connection Status UI (Optional)
```tsx
{status === 'disconnected' && (
  <span className="connection-status connection-status--offline">
    Reconnecting...
  </span>
)}
```
