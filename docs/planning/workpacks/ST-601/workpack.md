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

### Authentication: JWT in HttpOnly Cookie
The backend is stateless JWT (oauth2ResourceServer). To enable SSE with cookie auth:

1. **New endpoint `POST /api/v1/auth/session`**:
   - Validates JWT from Authorization header
   - Sets JWT as HttpOnly cookie (`hometusk_token`)
   - Returns 200 OK

2. **Security filter modification**:
   - Check Authorization header first (existing behavior)
   - Fallback: extract JWT from `hometusk_token` cookie
   - Validate JWT same way for both

3. **CORS update**:
   - `allowCredentials(true)`
   - Explicit origins (not `*`)

4. **Web client flow**:
   - After OIDC callback: `POST /auth/session` with JWT
   - Before SSE: browser sends cookie automatically
   - On token refresh: call `/auth/session` again

**Why this approach:**
- HttpOnly cookie (XSS protection)
- No token in URL (logs/history safe)
- Works with existing stateless JWT architecture
- Keycloak still issues tokens, backend just validates

### Transaction Safety: AFTER_COMMIT
- SSE events published only AFTER transaction commits
- Use `@TransactionalEventListener(phase = AFTER_COMMIT)`
- Prevents phantom notifications on rollback

### Heartbeat: @EnableScheduling
- Add `@EnableScheduling` to application config
- SseNotificationService uses `@Scheduled(fixedRate = 30000)` for heartbeat

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
| `services/backend/src/main/java/com/hometusk/auth/api/AuthController.java` | `POST /auth/session` endpoint for cookie setup |
| `services/backend/src/main/java/com/hometusk/auth/filter/JwtCookieAuthFilter.java` | Extract JWT from cookie as fallback |
| `services/backend/src/main/java/com/hometusk/notifications/service/SseNotificationService.java` | Manage SSE emitters, publish events, heartbeat |
| `services/backend/src/main/java/com/hometusk/notifications/event/NotificationCreatedEvent.java` | Domain event for AFTER_COMMIT publishing |
| `services/backend/src/main/java/com/hometusk/config/SchedulingConfig.java` | Enable @Scheduled support |
| `services/backend/src/test/java/com/hometusk/notifications/service/SseNotificationServiceTest.java` | Unit tests |
| `services/backend/src/test/java/com/hometusk/notifications/api/NotificationSseIntegrationTest.java` | Integration tests |

### Modified Files
| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java` | Add JwtCookieAuthFilter, CORS allowCredentials(true) |
| `services/backend/src/main/java/com/hometusk/notifications/api/NotificationController.java` | Add `streamNotifications()` SSE endpoint |
| `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` | Publish `NotificationCreatedEvent` after save |
| `docs/contracts/http/commands.openapi.yaml` | Add SSE + auth/session endpoints |
| `clients/web/src/lib/auth/oidc.ts` | Call /auth/session after login and token refresh |

---

## Implementation Plan

### Step 1: Add @EnableScheduling Config
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {}
```

### Step 2: Create AuthController with /auth/session Endpoint
```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/session")
    public ResponseEntity<Void> createSession(HttpServletResponse response) {
        // JWT already validated by Spring Security filter chain
        CurrentUser user = userResolver.resolveCurrentUser();
        String token = extractTokenFromSecurityContext();

        Cookie cookie = new Cookie("hometusk_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // In production
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour, matches token expiry
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }
}
```

### Step 3: Create JwtCookieAuthFilter
```java
public class JwtCookieAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        // If Authorization header present, skip (let oauth2ResourceServer handle)
        if (request.getHeader("Authorization") != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT from cookie
        Cookie[] cookies = request.getCookies();
        String token = findCookie(cookies, "hometusk_token");

        if (token != null) {
            // Set as Authorization header for downstream processing
            HttpServletRequest wrapped = new HeaderAddingRequestWrapper(request, "Authorization", "Bearer " + token);
            filterChain.doFilter(wrapped, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
```

### Step 4: Update SecurityConfig
```java
// Add filter before oauth2ResourceServer
http.addFilterBefore(jwtCookieAuthFilter(), BearerTokenAuthenticationFilter.class);

// Update CORS
config.setAllowCredentials(true);
config.setAllowedOriginPatterns(List.of("http://localhost:5173")); // Explicit origin
```

### Step 5: Create NotificationCreatedEvent
```java
public record NotificationCreatedEvent(Notification notification) {}
```

### Step 6: Create SseNotificationService
- `ConcurrentHashMap<String, SseEmitter>` for emitter storage
- Key format: `{userId}:{householdId}`
- Methods: `register()`, `remove()`, AFTER_COMMIT handler
- `@Scheduled(fixedRate = 30000)` heartbeat

### Step 7: Add SSE Endpoint to NotificationController
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
    emitter.onError(e -> sseNotificationService.remove(user.id(), householdId));

    return emitter;
}
```

### Step 8: Hook NotificationService to Event Publishing
```java
// In NotificationService.createNotification()
Notification notification = notificationRepository.save(...);
applicationEventPublisher.publishEvent(new NotificationCreatedEvent(notification));
```

### Step 9: Update Web Client (OIDC flow)
```typescript
// After signinCallback() in Callback.tsx
await api.createAuthSession(); // POST /auth/session with JWT

// In useNotificationStream - EventSource with credentials
new EventSource(url, { withCredentials: true });
```

### Step 10: Write Tests
- Unit test: SseNotificationService emitter management
- Integration test: SSE with cookie auth
- Integration test: AFTER_COMMIT (no event on rollback)

### Step 11: Update OpenAPI Contract
- Add `POST /auth/session`
- Add `GET /notifications/stream`
- Document cookie auth for SSE

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
