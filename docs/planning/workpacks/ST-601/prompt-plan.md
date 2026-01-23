# Codex PLAN Prompt: ST-601 — SSE Realtime Endpoint

## Mode
**PLAN ONLY** — Read-only exploration. NO file modifications allowed.

## Allowed Commands (Whitelist)
```
ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
```

## Forbidden
- Any file edits/writes/moves/deletes
- Network access
- Package install
- git commit/push
- Database operations

---

## Task
Plan the implementation of SSE (Server-Sent Events) endpoint for real-time notification delivery with JWT-in-HttpOnly-cookie authentication.

## Sources of Truth (MUST READ)
1. `docs/planning/epics/EP-007/stories/ST-601-sse-endpoint.md` — Story spec with ACs
2. `docs/planning/workpacks/ST-601/workpack.md` — Implementation plan (UPDATED)
3. `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java` — Current security config
4. `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` — Existing notification service
5. `services/backend/src/main/java/com/hometusk/notifications/api/NotificationController.java` — Existing controller
6. `clients/web/src/lib/auth/oidc.ts` — Web OIDC flow
7. `clients/web/src/context/AuthContext.tsx` — Web auth context

## Architecture Context (Already Known)
- Backend is **stateless JWT** with `oauth2ResourceServer().jwt()`
- CORS has `allowCredentials(false)` - needs change
- No `@EnableScheduling` - needs to be added
- Keycloak issues JWT, backend validates

---

## Critical Constraints (MUST FOLLOW)

### 1. JWT in HttpOnly Cookie (Auth Flow)
EventSource can't send Authorization header. Solution:
1. New endpoint: `POST /api/v1/auth/session`
   - Validates JWT from Authorization header
   - Sets JWT as HttpOnly cookie `hometusk_token`
2. New filter: `JwtCookieAuthFilter`
   - If no Authorization header, extract JWT from cookie
   - Wrap request with Authorization header for downstream processing
3. Update CORS: `allowCredentials(true)` with explicit origins
4. Web client: call `/auth/session` after OIDC login

### 2. AFTER_COMMIT Publishing
- SSE events MUST be sent only AFTER transaction commits
- Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`
- Prevents phantom notifications on rollback

### 3. Heartbeat via @Scheduled
- Add `@EnableScheduling` config
- `@Scheduled(fixedRate = 30000)` in SseNotificationService

### 4. Household Boundary Enforcement
- Validate user is member of requested household (403 if not)
- Use existing `membershipService.requireMembership()`

---

## Exploration Tasks

### Task 1: Understand Current Security Setup
- Read `SecurityConfig.java` — current filter chain order
- Where does `oauth2ResourceServer()` fit in filter chain?
- What class is `BearerTokenAuthenticationFilter`?
- How to add filter BEFORE it?

### Task 2: Understand JWT Token Extraction
- How does oauth2ResourceServer extract JWT from header?
- Can we reuse `JwtDecoder` bean?
- How to extract token from SecurityContext?

### Task 3: Understand Notification Infrastructure
- Read `NotificationService.java` — where is `createNotification()` called?
- Is `ApplicationEventPublisher` already injected?
- What transaction boundaries exist?

### Task 4: Check for Existing Patterns
- Are there other filters in the codebase?
- How are cookies set in other controllers?
- Are there existing Spring events?

### Task 5: Understand Web Auth Flow
- Read `oidc.ts` — when is token available?
- Read `AuthContext.tsx` — when to call `/auth/session`?
- Where to add EventSource with credentials?

---

## Output Format

After exploration, provide:

1. **Verification of Assumptions**
   - Confirm SecurityConfig structure
   - Confirm filter chain order
   - Confirm NotificationService hook points

2. **Files to Create** (with purpose and key code snippets)
   - AuthController
   - JwtCookieAuthFilter
   - SchedulingConfig
   - SseNotificationService
   - NotificationCreatedEvent

3. **Files to Modify** (with specific changes)
   - SecurityConfig — filter addition, CORS changes
   - NotificationService — event publishing
   - NotificationController — SSE endpoint
   - Web: oidc.ts, Callback.tsx, useNotificationStream.ts

4. **Key Implementation Details**
   - Filter chain order
   - Cookie attributes (HttpOnly, Secure, SameSite, Path, MaxAge)
   - How to extract JWT from SecurityContext
   - Event publishing wiring

5. **Risks/Blockers** (if any)

6. **Questions** (if clarification needed)

---

## Stop Conditions
- If security architecture differs from expected → STOP and describe
- If missing dependencies → STOP and list
- Do NOT guess without evidence from codebase
