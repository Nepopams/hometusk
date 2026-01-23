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
**Ready** — Pending sprint commitment

---

## Outcome
Backend SSE endpoint that streams new notifications to connected clients in real-time, with authentication, household boundary checks, and heartbeat keepalive.

---

## Acceptance Criteria Summary
1. SSE endpoint `GET /api/v1/households/{householdId}/notifications/stream` returns `text/event-stream`
2. New notifications delivered via SSE within 2 seconds
3. Heartbeat sent every 30 seconds
4. 401 if no valid JWT
5. 403 if not household member
6. Graceful disconnect with resource cleanup

---

## Files to Change/Create

### New Files
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/notifications/service/SseNotificationService.java` | Manage SSE emitters, publish events |
| `services/backend/src/main/java/com/hometusk/notifications/dto/SseNotificationEventDto.java` | SSE event payload DTO |
| `services/backend/src/test/java/com/hometusk/notifications/service/SseNotificationServiceTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/notifications/api/NotificationSseIntegrationTest.java` | Integration tests |

### Modified Files
| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/notifications/api/NotificationController.java` | Add `streamNotifications()` SSE endpoint |
| `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` | Inject SseNotificationService, call `publish()` after save |
| `docs/contracts/http/commands.openapi.yaml` | Add SSE endpoint specification |

---

## Implementation Plan

### Step 1: Create SseNotificationService
- Create service to manage SSE emitters
- Methods: `register()`, `remove()`, `publish(notification)`
- Use `ConcurrentHashMap<String, SseEmitter>` for emitter storage
- Key format: `{userId}:{householdId}`
- Add `@Scheduled` heartbeat method (every 30s)

### Step 2: Create SSE Event DTO
- `SseNotificationEventDto` with notification fields
- Matches existing `NotificationDto` structure

### Step 3: Add SSE Endpoint to Controller
- `@GetMapping(produces = "text/event-stream")`
- Validate JWT and membership
- Create `SseEmitter` with timeout
- Register with SseNotificationService
- Set up `onCompletion`/`onTimeout` cleanup

### Step 4: Hook NotificationService to SSE
- Inject `SseNotificationService`
- After `notificationRepository.save()`, call `sseNotificationService.publish(notification)`

### Step 5: Write Tests
- Unit test: mock emitter registration/publishing
- Integration test: full flow with real SSE connection

### Step 6: Update OpenAPI Contract
- Add `/households/{householdId}/notifications/stream` endpoint
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
  - `publish_shouldSendToRegisteredEmitters`
  - `publish_shouldSkipDisconnectedEmitters`
  - `heartbeat_shouldSendToAllEmitters`

### Integration Tests
- `NotificationSseIntegrationTest`:
  - `streamNotifications_authenticated_shouldEstablishConnection`
  - `streamNotifications_noAuth_shouldReturn401`
  - `streamNotifications_notMember_shouldReturn403`
  - `streamNotifications_newNotification_shouldReceiveEvent`
  - `streamNotifications_disconnect_shouldCleanup`

---

## DoD Checklist
- [ ] Code follows project conventions
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] OpenAPI contract updated
- [ ] No cross-household leaks (membership enforced)
- [ ] No hardcoded secrets

---

## Risks
| Risk | Mitigation |
|------|------------|
| SseEmitter timeout handling | Set reasonable timeout, cleanup on error |
| Memory leak from unclosed emitters | Explicit cleanup in onCompletion/onTimeout |
| Token in query param security | Document limitation, consider cookie approach for v2 |

---

## Rollback
- Remove SSE endpoint from controller
- Remove SseNotificationService
- Revert NotificationService changes
- Web client falls back to polling (ST-604)

---

## Prompt Pack
- `prompt-plan.md`: Read-only exploration of notification infrastructure
- `prompt-apply.md`: Implementation after plan approval
- `prompt-review.md`: Review after apply completion
