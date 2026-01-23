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
Plan the implementation of SSE (Server-Sent Events) endpoint for real-time notification delivery.

## Sources of Truth (MUST READ)
1. `docs/planning/epics/EP-007/stories/ST-601-sse-endpoint.md` — Story spec with ACs
2. `docs/planning/workpacks/ST-601/workpack.md` — Implementation plan
3. `services/backend/src/main/java/com/hometusk/notifications/service/NotificationService.java` — Existing notification service
4. `services/backend/src/main/java/com/hometusk/notifications/api/NotificationController.java` — Existing controller
5. `docs/contracts/http/commands.openapi.yaml` — Existing notification contract

## Critical Constraints (MUST FOLLOW)

### 1. Cookie-based Authentication
- SSE endpoint uses session cookie auth (Spring Security)
- NO token in URL query params
- Client connects with `withCredentials: true`
- Verify existing CORS configuration allows credentials

### 2. AFTER_COMMIT Publishing
- SSE events MUST be sent only AFTER transaction commits
- Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`
- Prevents phantom notifications on rollback
- Check if Spring event infrastructure is already configured

### 3. Household Boundary Enforcement
- Validate user is member of requested household (403 if not)
- Use existing `membershipService.requireMembership()`

---

## Exploration Tasks

### Task 1: Understand Existing Infrastructure
- Read `NotificationService.java` — how are notifications created?
- Read `NotificationController.java` — existing endpoints
- Check for existing Spring event publishing in the codebase
- Check CORS configuration for credentials support

### Task 2: Identify Hook Points
- Where in `NotificationService.createNotification()` should we publish events?
- Is `ApplicationEventPublisher` already injected? If not, plan to add it.
- Are there existing domain events (e.g., `NotificationCreatedEvent`)?

### Task 3: Plan New Components
List files to create:
- `SseNotificationService.java` — emitter registry + publish logic
- `NotificationCreatedEvent.java` — domain event (if not exists)
- Test files

List files to modify:
- `NotificationController.java` — add SSE endpoint
- `NotificationService.java` — publish event after save

### Task 4: Verify Security Configuration
- Check `SecurityConfig.java` for SSE endpoint permissions
- Check CORS config for `allowCredentials`
- Verify session-based auth is working for other endpoints

### Task 5: Check for Existing Patterns
- Are there other SSE endpoints in the codebase?
- How are scheduled tasks configured? (for heartbeat)
- What's the pattern for `@TransactionalEventListener`?

---

## Output Format

After exploration, provide:

1. **Files to Create** (with purpose)
2. **Files to Modify** (with specific changes)
3. **Key Implementation Details**:
   - How to wire event publishing
   - How to implement AFTER_COMMIT
   - How to handle emitter lifecycle
4. **Risks/Blockers** (if any)
5. **Questions** (if clarification needed)

---

## Stop Conditions
- If missing dependencies or unclear requirements → STOP and list questions
- If existing patterns conflict with plan → STOP and describe conflict
- Do NOT guess or invent solutions without evidence from codebase
