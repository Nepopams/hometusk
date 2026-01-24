# Story: ST-601 — SSE Realtime Endpoint

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing API: `docs/contracts/http/commands.openapi.yaml`

---

## Status
**Done** — Implementation complete (commit f68f679)

## Priority
P1 (Core)

## Points
3

---

## Description
Add a Server-Sent Events (SSE) endpoint that streams new notifications to connected web clients in real-time.

### User Value
As a household member, I want to receive notifications instantly when something happens (task assigned, item purchased) without refreshing the page, so I stay informed about household activity.

### Technical Approach
- New controller method with `SseEmitter` return type
- **Authentication via session cookie** (same-origin, Spring Security session)
- Register emitter per user/household combination
- **Publish AFTER_COMMIT only** — use `@TransactionalEventListener(phase = AFTER_COMMIT)` or equivalent to prevent phantom notifications
- Heartbeat thread to keep connections alive
- Cleanup on disconnect/timeout

---

## Acceptance Criteria

### AC-1: SSE Connection Established
```gherkin
Given user has valid session cookie (authenticated)
And user is member of household H1
When client connects to GET /api/v1/households/{H1}/notifications/stream
  with credentials: 'include' (or withCredentials: true)
Then HTTP 200 with Content-Type: text/event-stream
And connection stays open
```

### AC-2: Notification Delivered via SSE (AFTER_COMMIT)
```gherkin
Given user U1 has active SSE connection for household H1
When another user creates task assigned to U1
And the transaction commits successfully
Then U1 receives SSE event within 2 seconds:
  event: notification
  data: {"id":"...", "type":"task_assigned", ...}

Given the transaction rolls back
Then NO SSE event is sent (no phantom notifications)
```

### AC-3: Heartbeat Sent
```gherkin
Given active SSE connection
When 30 seconds pass without notification
Then server sends:
  event: heartbeat
  data: {"timestamp":"2026-01-23T12:00:00Z"}
```

### AC-4: Auth Required (Session Cookie)
```gherkin
Given no valid session cookie
When client connects to SSE endpoint
Then HTTP 401 Unauthorized
```

### AC-5: Household Boundary Enforced
```gherkin
Given user U1 is NOT member of household H2
When U1 connects to GET /api/v1/households/{H2}/notifications/stream
Then HTTP 403 Forbidden
```

### AC-6: Graceful Disconnect
```gherkin
Given active SSE connection
When client disconnects
Then server cleans up emitter
And no resource leak
```

---

## Test Strategy

### Unit Tests
- `SseNotificationServiceTest`: Mock emitter registration/publishing
- `NotificationSseControllerTest`: Auth/boundary checks

### Integration Tests
- `NotificationSseIntegrationTest`:
  - Connect with session → receive notification → verify event format
  - Connect without session → 401
  - Connect to wrong household → 403
  - Disconnect → cleanup verified
  - Verify AFTER_COMMIT: rollback → no event sent

### Test Data
- Household H1 with members U1, U2
- U1 connected to SSE
- U2 creates task assigned to U1
- Verify U1 receives notification

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | New SSE endpoint |
| adr_needed | no | SSE + cookie auth is standard |
| diagrams_needed | no | |
| security_sensitive | yes | Auth + boundary |
| traceability_critical | no | |

---

## Dependencies
- NotificationService.createNotification() hook point
- Spring Security session management
- `@TransactionalEventListener` support

## Blocks
- ST-603 (Web Realtime Subscribe) depends on this

---

## Implementation Notes

### Cookie-based Authentication
SSE endpoint uses standard Spring Security session authentication:
- Client connects with `withCredentials: true` (EventSource limitation: no custom headers)
- Server validates session cookie via SecurityContext
- No token in URL (security: logs, history, referrer)

```java
@GetMapping(value = "/households/{householdId}/notifications/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamNotifications(@PathVariable UUID householdId) {
    // Spring Security already validated session via filter chain
    CurrentUser user = userResolver.resolveCurrentUser();
    membershipService.requireMembership(user.id(), householdId);

    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    sseNotificationService.register(user.id(), householdId, emitter);

    emitter.onCompletion(() -> sseNotificationService.remove(user.id(), householdId));
    emitter.onTimeout(() -> sseNotificationService.remove(user.id(), householdId));
    emitter.onError(e -> sseNotificationService.remove(user.id(), householdId));

    return emitter;
}
```

### CORS + Credentials Assumptions

**Deployment model:** Same-origin (web and API share same origin or are behind single reverse proxy).

| Scenario | CORS config | Notes |
|----------|-------------|-------|
| Same-origin | Not required | Cookie sent automatically |
| Single reverse proxy | Not required | Proxy presents unified origin |
| Cross-origin (future) | `@CrossOrigin(origins = "...", allowCredentials = "true")` | Requires explicit config |

Current implementation assumes **same-origin**. If cross-origin deployment is needed, add:
```java
@CrossOrigin(origins = "${hometusk.web.origin}", allowCredentials = "true")
```

### Response Headers / Deployment Notes (Proxy Buffering)

SSE requires **unbuffered streaming**. Add response headers to prevent proxy buffering:

```java
@GetMapping(value = "/households/{householdId}/notifications/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamNotifications(@PathVariable UUID householdId, HttpServletResponse response) {
    // Disable proxy buffering (nginx, AWS ALB, etc.)
    response.setHeader("X-Accel-Buffering", "no");       // nginx
    response.setHeader("Cache-Control", "no-cache");    // general
    response.setHeader("Connection", "keep-alive");     // explicit

    // ... rest of implementation
}
```

**Reverse proxy checklist:**
| Proxy | Setting |
|-------|---------|
| nginx | `proxy_buffering off;` or respect `X-Accel-Buffering: no` |
| AWS ALB | SSE works by default; ensure idle timeout > heartbeat interval |
| Cloudflare | Disable response buffering for SSE path |

### AFTER_COMMIT Publishing (Critical)
Notifications must be published to SSE **only after the transaction commits** to avoid phantom notifications on rollback.

**Option A: TransactionalEventListener**
```java
// In NotificationService.createNotification()
Notification notification = notificationRepository.save(...);
applicationEventPublisher.publishEvent(new NotificationCreatedEvent(notification));

// In SseNotificationService
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleNotificationCreated(NotificationCreatedEvent event) {
    publish(event.getNotification());
}
```

**Option B: TransactionSynchronization**
```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            sseNotificationService.publish(notification);
        }
    }
);
```

### New Service: SseNotificationService
- `register(userId, householdId, emitter)`
- `remove(userId, householdId)`
- `publish(notification)` — called AFTER_COMMIT
- `@Scheduled` heartbeat method (every 30s)

### Event Format
```
event: notification
data: {"id":"abc-123","type":"task_assigned","payload":{"actorId":"...","entityId":"...","entityType":"task","summary":"Task assigned: Clean kitchen"},"createdAt":"2026-01-23T10:30:00Z","readAt":null}

event: heartbeat
data: {"timestamp":"2026-01-23T10:30:30Z"}
```
