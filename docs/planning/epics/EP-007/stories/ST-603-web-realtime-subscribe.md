# Story: ST-603 — Web Realtime Subscribe

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Done** — Implementation complete (commit 2bf57a1)

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
- **Cookie-based auth** — connect with `withCredentials: true`
- Parse incoming notification events
- Update notifications state via hook
- Auto-reconnect on disconnect (exponential backoff)
- Connection lifecycle management

---

## Acceptance Criteria

### AC-1: SSE Connection Established
```gherkin
Given user is authenticated (session cookie valid)
And viewing household page
When household page loads
Then EventSource connects to /notifications/stream with withCredentials: true
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

### AC-4: Session Expiry Handling
```gherkin
Given SSE connection
When session expires (server returns 401 on reconnect)
Then client stops reconnecting
And redirects to login (or shows re-auth prompt)
```

**Note:** Unlike token-based auth, we cannot "refresh" a session cookie from JS. On 401, the user must re-authenticate.

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
- Open app → verify SSE connected (DevTools Network tab, look for EventStream)
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
| security_sensitive | yes | Session cookie in SSE |
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
  onNotification: (notification: Notification) => void,
  onAuthError: () => void
) {
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
  const eventSourceRef = useRef<EventSource | null>(null);
  const retryCountRef = useRef(0);
  const MAX_RETRIES = 10;

  useEffect(() => {
    let isMounted = true;

    const connect = () => {
      if (!isMounted) return;

      const url = `${API_BASE}/households/${householdId}/notifications/stream`;

      // withCredentials for cookie-based auth
      const eventSource = new EventSource(url, { withCredentials: true });
      eventSourceRef.current = eventSource;

      eventSource.onopen = () => {
        if (!isMounted) return;
        setStatus('connected');
        retryCountRef.current = 0;
      };

      eventSource.addEventListener('notification', (event) => {
        if (!isMounted) return;
        const notification = JSON.parse(event.data);
        onNotification(notification);
      });

      eventSource.addEventListener('heartbeat', () => {
        // Connection alive, no action needed
      });

      eventSource.onerror = (event) => {
        if (!isMounted) return;
        eventSource.close();

        // Check if it's an auth error (EventSource doesn't expose status directly)
        // We rely on retry count — if server keeps returning 401, retries will exhaust
        if (retryCountRef.current >= MAX_RETRIES) {
          setStatus('error');
          onAuthError();
          return;
        }

        setStatus('disconnected');
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
      isMounted = false;
      eventSourceRef.current?.close();
    };
  }, [householdId, onNotification, onAuthError]);

  return { status };
}
```

### Integration with useNotifications
```typescript
// In HouseholdLayout or NotificationProvider
const navigate = useNavigate();
const { addNotification, refresh } = useNotifications(householdId);

const handleAuthError = useCallback(() => {
  // Session expired, redirect to login
  navigate('/login?reason=session_expired');
}, [navigate]);

const { status } = useNotificationStream(
  householdId,
  addNotification,
  handleAuthError
);

// Optional: show connection status indicator
```

### EventSource with Credentials
Native EventSource supports `withCredentials: true`:
```typescript
new EventSource(url, { withCredentials: true });
```

This sends cookies with the request. No token in URL needed.

### Connection Status UI (Optional)
```tsx
{status === 'disconnected' && (
  <span className="connection-status connection-status--reconnecting">
    Reconnecting...
  </span>
)}

{status === 'error' && (
  <span className="connection-status connection-status--error">
    Connection lost
  </span>
)}
```

### Browser Support
EventSource with `withCredentials` is supported in all modern browsers:
- Chrome 26+
- Firefox 6+
- Safari 7+
- Edge 79+

For older browsers, consider a polyfill like `eventsource-polyfill`.
