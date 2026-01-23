# Codex APPLY Prompt: ST-601 — SSE Realtime Endpoint

## Mode
**APPLY** — Implementation mode. File modifications allowed.

## Allowed Operations
```
- Create/edit Java files in services/backend/src/
- Create/edit TypeScript files in clients/web/src/
- Edit OpenAPI contract in docs/contracts/
- Run gradle build/test commands
- Run npm/pnpm commands in clients/web/
```

## Forbidden
- Modifying files outside listed paths
- Changing unrelated functionality
- Adding features not in the plan
- Skipping tests
- Hardcoding secrets or credentials

---

## Task
Implement SSE (Server-Sent Events) endpoint for real-time notification delivery with JWT-in-HttpOnly-cookie authentication.

## Sources of Truth (MUST READ BEFORE IMPLEMENTATION)
1. `docs/planning/workpacks/ST-601/workpack.md` — Implementation plan (AUTHORITATIVE)
2. `docs/planning/epics/EP-007/stories/ST-601-sse-endpoint.md` — Story spec with ACs
3. `docs/_governance/dod.md` — Definition of Done checklist

## Approved Plan Summary
From PLAN phase verification:
- SecurityConfig uses `SessionCreationPolicy.STATELESS` + `oauth2ResourceServer().jwt()`
- CORS has `allowCredentials(false)` → must change
- No existing SSE/events/scheduling infrastructure
- Web OIDC flow does not call `/auth/session`

---

## Critical Constraints (MUST FOLLOW)

### 1. JWT in HttpOnly Cookie
- `POST /api/v1/auth/session` validates JWT from Authorization header, sets HttpOnly cookie
- Cookie name: `hometusk_token`
- Cookie attributes (configurable via `application.yml`):
  - `HttpOnly=true` (always)
  - `Secure=true` in prod, `false` in dev
  - `SameSite=Strict` in prod, `Lax` in dev
  - `Path=/`
  - `MaxAge` = token expiry (from JWT claims)

### 2. JwtCookieAuthFilter
- Extends `OncePerRequestFilter`
- Only activates if `Authorization` header is absent
- Extracts JWT from `hometusk_token` cookie
- Wraps request with `Authorization: Bearer <token>` header
- Registered BEFORE `BearerTokenAuthenticationFilter`

### 3. CORS with Credentials
- `allowCredentials(true)`
- Explicit origins from config property `hometusk.cors.allowed-origins`
- Cannot use `*` with credentials

### 4. AFTER_COMMIT Event Publishing
- `NotificationService.createNotification()` publishes `NotificationCreatedEvent`
- `SseNotificationService` handles with `@TransactionalEventListener(phase = AFTER_COMMIT)`
- Prevents phantom notifications on rollback

### 5. Heartbeat via @Scheduled
- Add `@EnableScheduling` in `SchedulingConfig`
- `@Scheduled(fixedRate = 30000)` in `SseNotificationService`
- Event type: `heartbeat`, data: `{"timestamp": "..."}`

### 6. Single Emitter per User+Household
- Key format: `{userId}:{householdId}`
- `ConcurrentHashMap<String, SseEmitter>`
- Cleanup on completion/error/timeout

---

## Implementation Steps

### Step 1: Create SchedulingConfig
```java
// services/backend/src/main/java/com/hometusk/config/SchedulingConfig.java
@Configuration
@EnableScheduling
public class SchedulingConfig {}
```

### Step 2: Create Cookie Properties
```yaml
# services/backend/src/main/resources/application.yml
hometusk:
  cookie:
    secure: false  # Override in prod profile
    same-site: Lax  # Override in prod profile
  cors:
    allowed-origins:
      - http://localhost:5173
```

### Step 3: Create AuthController
```java
// services/backend/src/main/java/com/hometusk/auth/api/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${hometusk.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${hometusk.cookie.same-site:Lax}")
    private String sameSite;

    @PostMapping("/session")
    public ResponseEntity<Void> createSession(HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtAuth.getToken().getTokenValue();
        Instant expiry = jwtAuth.getToken().getExpiresAt();
        int maxAge = (int) Duration.between(Instant.now(), expiry).getSeconds();

        ResponseCookie cookie = ResponseCookie.from("hometusk_token", token)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }
}
```

### Step 4: Create JwtCookieAuthFilter
```java
// services/backend/src/main/java/com/hometusk/auth/filter/JwtCookieAuthFilter.java
@Component
public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "hometusk_token";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip if Authorization header already present
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT from cookie
        String token = extractTokenFromCookie(request);
        if (token != null) {
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                        return "Bearer " + token;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                        return Collections.enumeration(List.of("Bearer " + token));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    if (!names.contains(HttpHeaders.AUTHORIZATION)) {
                        names.add(HttpHeaders.AUTHORIZATION);
                    }
                    return Collections.enumeration(names);
                }
            };
            filterChain.doFilter(wrapped, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
            .filter(c -> COOKIE_NAME.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
```

### Step 5: Update SecurityConfig
```java
// Add to SecurityConfig.java

@Autowired
private JwtCookieAuthFilter jwtCookieAuthFilter;

@Value("${hometusk.cors.allowed-origins}")
private List<String> allowedOrigins;

// In filterChain method:
http.addFilterBefore(jwtCookieAuthFilter, BearerTokenAuthenticationFilter.class);

// In corsConfigurationSource method:
config.setAllowCredentials(true);
config.setAllowedOriginPatterns(allowedOrigins);
```

### Step 6: Create NotificationCreatedEvent
```java
// services/backend/src/main/java/com/hometusk/notifications/event/NotificationCreatedEvent.java
public record NotificationCreatedEvent(NotificationDto notification) {}
```

### Step 7: Create SseNotificationService
```java
// services/backend/src/main/java/com/hometusk/notifications/service/SseNotificationService.java
@Service
@Slf4j
public class SseNotificationService {

    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(UUID userId, UUID householdId, SseEmitter emitter) {
        String key = buildKey(userId, householdId);
        emitters.put(key, emitter);
        log.debug("Registered SSE emitter: {}", key);
    }

    public void remove(UUID userId, UUID householdId) {
        String key = buildKey(userId, householdId);
        emitters.remove(key);
        log.debug("Removed SSE emitter: {}", key);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        NotificationDto notification = event.notification();
        String key = buildKey(notification.recipientId(), notification.householdId());
        SseEmitter emitter = emitters.get(key);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
                log.debug("Sent notification via SSE: {}", notification.id());
            } catch (IOException e) {
                log.warn("Failed to send SSE notification, removing emitter: {}", key);
                emitters.remove(key);
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        String heartbeatData = "{\"timestamp\":\"" + Instant.now() + "\"}";
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(heartbeatData));
            } catch (IOException e) {
                log.debug("Heartbeat failed, removing emitter: {}", key);
                emitters.remove(key);
            }
        });
    }

    private String buildKey(UUID userId, UUID householdId) {
        return userId + ":" + householdId;
    }
}
```

### Step 8: Update NotificationService
```java
// Add to NotificationService.java

private final ApplicationEventPublisher eventPublisher;

// In createNotification method, after save:
NotificationDto dto = toDto(notification);
eventPublisher.publishEvent(new NotificationCreatedEvent(dto));
```

### Step 9: Add SSE Endpoint to NotificationController
```java
// Add to NotificationController.java

private final SseNotificationService sseNotificationService;

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

### Step 10: Update Web Client

#### api.ts
```typescript
export async function createAuthSession(): Promise<void> {
  const token = await tokenProvider();
  if (!token) throw new Error('No token available');

  const response = await fetch(`${API_BASE_URL}/auth/session`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to create session: ${response.status}`);
  }
}
```

#### Callback.tsx
```typescript
// After signinCallback() success:
try {
  await createAuthSession();
} catch (e) {
  console.warn('Failed to create auth session:', e);
}
// Then redirect
```

#### AuthContext.tsx
```typescript
// When token refreshes (in Keycloak mode):
// Call createAuthSession() to sync cookie
```

### Step 11: Update OpenAPI Contract
Add to `docs/contracts/http/commands.openapi.yaml`:
- `POST /auth/session` — 200 OK, sets cookie
- `GET /households/{householdId}/notifications/stream` — text/event-stream

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

# Format check
./gradlew spotlessCheck

# Web build
cd /home/vad/Документы/hometusk/clients/web
pnpm build
pnpm lint
```

---

## Tests to Write

### Unit Tests (SseNotificationServiceTest)
- `register_shouldStoreEmitter`
- `remove_shouldRemoveEmitter`
- `handleNotificationCreated_shouldSendToRegisteredEmitter`
- `handleNotificationCreated_shouldNotFailIfNoEmitter`
- `handleNotificationCreated_shouldRemoveEmitterOnError`
- `sendHeartbeat_shouldSendToAllEmitters`

### Integration Tests (NotificationSseIntegrationTest)
- `streamNotifications_withValidSession_shouldConnect`
- `streamNotifications_withoutSession_shouldReturn401`
- `streamNotifications_notMember_shouldReturn403`
- `createSession_withValidJwt_shouldSetCookie`
- `createSession_withoutAuth_shouldReturn401`

---

## DoD Checklist
- [ ] Code follows project conventions (Java 21, Spring Boot)
- [ ] Spotless formatting applied
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] AFTER_COMMIT publishing verified
- [ ] OpenAPI contract updated
- [ ] No cross-household leaks (membership enforced)
- [ ] No hardcoded secrets (config via properties)
- [ ] Cookie-based auth working

---

## STOP-THE-LINE Rules
If any of these occur, STOP and report:
- Cannot add filter before BearerTokenAuthenticationFilter
- CORS with credentials breaks existing endpoints
- TransactionalEventListener not firing
- Tests fail unexpectedly
- Missing dependencies

Do NOT proceed with workarounds without approval.
