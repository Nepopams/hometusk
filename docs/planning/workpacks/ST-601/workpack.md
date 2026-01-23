# Workpack: ST-601 — SSE Realtime Endpoint

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Story: `docs/planning/epics/EP-007/stories/ST-601-sse-endpoint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing Service: `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java`

---

## Status
**In Progress** — Implementation started

---

## Outcome
Backend SSE endpoint that streams new notifications to connected clients in real-time, with cookie-based authentication, household boundary checks, AFTER_COMMIT event publishing, and heartbeat keepalive.

---

## Key Technical Decisions

### Authentication: Cookie-based
- SSE endpoint authenticates via session cookie (Spring Security)
- Client connects with `withCredentials: true`
- NO token in URL (security: logs, history, referrer)

### Transaction Safety: AFTER_COMMIT
- SSE events published only AFTER transaction commits
- Use `@TransactionalEventListener(phase = AFTER_COMMIT)`
- Prevents phantom notifications on rollback

---

## Acceptance Criteria Summary
1. SSE endpoint `GET /api/v1/households/{householdId}/notifications/stream` returns `text/event-stream`
2. New notifications delivered via SSE within 2 seconds (AFTER_COMMIT)
3. Heartbeat sent every 30 seconds
4. 401 if no valid session cookie
5. 403 if not household member
6. Graceful disconnect with resource cleanup
7. No phantom notifications on transaction rollback

---

## Files to Change/Create

### New Files
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/notifications/service/SseNotificationService.java` | Manage SSE emitters, publish events, heartbeat |
| `services/backend/src/main/java/com/hometusk/notifications/event/NotificationCreatedEvent.java` | Domain event for AFTER_COMMIT publishing |
| `services/backend/src/test/java/com/hometusk/notifications/service/SseNotificationServiceTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/notifications/api/NotificationSseIntegrationTest.java` | Integration tests |

### Modified Files
| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/notifications/api/NotificationController.java` | Add `streamNotifications()` SSE endpoint |
| `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` | Publish `NotificationCreatedEvent` after save |
| `docs/contracts/http/commands.openapi.yaml` | Add SSE endpoint specification |

---

## Implementation Plan

### Step 1: Create NotificationCreatedEvent
- Domain event class with notification data
- Published after `notificationRepository.save()`

### Step 2: Create SseNotificationService
- `ConcurrentHashMap<String, SseEmitter>` for emitter storage
- Key format: `{userId}:{householdId}`
- Methods:
  - `register(userId, householdId, emitter)`
  - `remove(userId, householdId)`
  - `@TransactionalEventListener(phase = AFTER_COMMIT)` handler
- `@Scheduled` heartbeat method (every 30s)

### Step 3: Add SSE Endpoint to Controller
```java
@GetMapping(value = "/households/{householdId}/notifications/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamNotifications(@PathVariable UUID householdId) {
    // Spring Security validates session cookie via filter chain
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

### Step 4: Hook NotificationService to Event Publishing
```java
// In NotificationService.createNotification()
Notification notification = notificationRepository.save(...);
applicationEventPublisher.publishEvent(new NotificationCreatedEvent(notification));
```

### Step 5: Implement AFTER_COMMIT Handler
```java
// In SseNotificationService
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleNotificationCreated(NotificationCreatedEvent event) {
    publish(event.getNotification());
}
```

### Step 6: Write Tests
- Unit test: mock emitter registration/publishing
- Integration test: full flow with real SSE connection
- Integration test: verify no event on rollback

### Step 7: Update OpenAPI Contract
- Add `/households/{householdId}/notifications/stream` endpoint
- Document cookie-based auth requirement
- Document SSE event format

---

## Verification Commands

```bash
# Build
cd /home/vad/Документы/hometusk/services/backend
./gradlew build

# Run unit tests
./gradlew test --tests "*SseNotificationServiceTest*"

# Run integration tests
./gradlew test --tests "*NotificationSseIntegrationTest*"

# Run all notification tests
./gradlew test --tests "*Notification*"

# Format check
./gradlew spotlessCheck
```

---

## Tests

### Unit Tests
- `SseNotificationServiceTest`:
  - `register_shouldStoreEmitter`
  - `remove_shouldCleanupEmitter`
  - `handleNotificationCreated_shouldSendToRegisteredEmitters`
  - `handleNotificationCreated_shouldSkipDisconnectedEmitters`
  - `heartbeat_shouldSendToAllEmitters`

### Integration Tests
- `NotificationSseIntegrationTest`:
  - `streamNotifications_withSession_shouldEstablishConnection`
  - `streamNotifications_noSession_shouldReturn401`
  - `streamNotifications_notMember_shouldReturn403`
  - `streamNotifications_newNotification_shouldReceiveEventAfterCommit`
  - `streamNotifications_rollback_shouldNotReceiveEvent`
  - `streamNotifications_disconnect_shouldCleanup`

---

## DoD Checklist
- [ ] Code follows project conventions
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] AFTER_COMMIT publishing verified (no phantom notifications)
- [ ] OpenAPI contract updated
- [ ] No cross-household leaks (membership enforced)
- [ ] No hardcoded secrets
- [ ] Cookie-based auth working (no token in URL)

---

## Risks
| Risk | Mitigation |
|------|------------|
| SseEmitter timeout handling | Set reasonable timeout, cleanup on error |
| Memory leak from unclosed emitters | Explicit cleanup in onCompletion/onTimeout/onError |
| CORS with credentials | Verify CORS config allows credentials |
| Heartbeat thread blocking | Use non-blocking async send |

---

## Rollback
- Remove SSE endpoint from controller
- Remove SseNotificationService
- Remove NotificationCreatedEvent
- Revert NotificationService changes
- Web client falls back to polling (ST-604)

---

## Prompt Pack
- `prompt-plan.md`: Read-only exploration of notification infrastructure
- `prompt-apply.md`: Implementation after plan approval
- `prompt-review.md`: Review after apply completion
