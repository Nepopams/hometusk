# Story: ST-601 — SSE Realtime Endpoint

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing API: `docs/contracts/http/commands.openapi.yaml`

---

## Status
**Ready** — DoR complete, pending sprint commitment

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
- Register emitter per user/household combination
- Publish to emitters when `NotificationService.createNotification()` saves
- Heartbeat thread to keep connections alive
- Cleanup on disconnect/timeout

---

## Acceptance Criteria

### AC-1: SSE Connection Established
```gherkin
Given user is authenticated with valid JWT
And user is member of household H1
When client connects to GET /api/v1/households/{H1}/notifications/stream
Then HTTP 200 with Content-Type: text/event-stream
And connection stays open
```

### AC-2: Notification Delivered via SSE
```gherkin
Given user U1 has active SSE connection for household H1
When another user creates task assigned to U1
Then U1 receives SSE event within 2 seconds:
  event: notification
  data: {"id":"...", "type":"task_assigned", ...}
```

### AC-3: Heartbeat Sent
```gherkin
Given active SSE connection
When 30 seconds pass without notification
Then server sends:
  event: heartbeat
  data: {"timestamp":"2026-01-23T12:00:00Z"}
```

### AC-4: Auth Required
```gherkin
Given no JWT provided
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
  - Connect → receive notification → verify event format
  - Connect without auth → 401
  - Connect to wrong household → 403
  - Disconnect → cleanup verified

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
| adr_needed | no | SSE is standard |
| diagrams_needed | no | |
| security_sensitive | yes | Auth + boundary |
| traceability_critical | no | |

---

## Dependencies
- NotificationService.createNotification() hook point
- JWT validation infrastructure

## Blocks
- ST-603 (Web Realtime Subscribe) depends on this

---

## Implementation Notes

### Recommended Approach
```java
@GetMapping(value = "/households/{householdId}/notifications/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamNotifications(@PathVariable UUID householdId) {
    CurrentUser user = userResolver.resolveCurrentUser();
    membershipService.requireMembership(user.id(), householdId);

    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    sseNotificationService.register(user.id(), householdId, emitter);

    emitter.onCompletion(() -> sseNotificationService.remove(user.id(), householdId));
    emitter.onTimeout(() -> sseNotificationService.remove(user.id(), householdId));

    return emitter;
}
```

### New Service: SseNotificationService
- `register(userId, householdId, emitter)`
- `remove(userId, householdId)`
- `publish(notification)` — called from NotificationService after save
- `startHeartbeatScheduler()` — every 30s

### Event Format
```
event: notification
data: {"id":"abc-123","type":"task_assigned","payload":{"actorId":"...","entityId":"...","entityType":"task","summary":"Task assigned: Clean kitchen"},"createdAt":"2026-01-23T10:30:00Z","readAt":null}

event: heartbeat
data: {"timestamp":"2026-01-23T10:30:30Z"}
```
