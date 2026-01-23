# Codex REVIEW Prompt: ST-601 — SSE Realtime Endpoint

## Mode
**REVIEW** — Code review mode. Read-only inspection.

## Task
Review the ST-601 implementation for correctness, security, and alignment with acceptance criteria.

## Sources of Truth
1. `docs/planning/workpacks/ST-601/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-007/stories/ST-601-sse-endpoint.md` — Story spec with ACs
3. `docs/_governance/dod.md` — Definition of Done

---

## Implementation Summary (from commit f68f679)

### New Files Created
| File | Purpose |
|------|---------|
| `AuthController.java` | `POST /auth/session` sets HttpOnly cookie |
| `JwtCookieAuthFilter.java` | Extract JWT from cookie when no Authorization header |
| `SchedulingConfig.java` | `@EnableScheduling` for heartbeat |
| `NotificationCreatedEvent.java` | Domain event for AFTER_COMMIT |
| `SseNotificationService.java` | Emitter registry + event handler + heartbeat |
| `SseNotificationServiceTest.java` | Unit tests |
| `NotificationSseIntegrationTest.java` | Integration tests |

### Modified Files
| File | Changes |
|------|---------|
| `SecurityConfig.java` | Filter chain + CORS credentials |
| `NotificationController.java` | SSE endpoint |
| `NotificationService.java` | Event publishing |
| `application.yml` | Cookie/CORS config |
| `api.ts` | `createAuthSession()` |
| `Callback.tsx` | Call session after login |
| `AuthContext.tsx` | Sync cookie on refresh |
| `commands.openapi.yaml` | Document new endpoints |

---

## Review Checklist

### Security
- [ ] JwtCookieAuthFilter only activates when Authorization header absent
- [ ] Cookie is HttpOnly (XSS protection)
- [ ] CORS has explicit origins (no `*` with credentials)
- [ ] Membership check in SSE endpoint (no cross-household leaks)
- [ ] No token in URL/logs

### Transaction Safety
- [ ] `@TransactionalEventListener(phase = AFTER_COMMIT)` used
- [ ] No phantom notifications on rollback

### Filter Chain
- [ ] Filter added BEFORE `BearerTokenAuthenticationFilter`
- [ ] Existing bearer auth still works

### SSE Lifecycle
- [ ] Emitter cleanup on completion/error/timeout
- [ ] Heartbeat every 30s
- [ ] No memory leaks from unclosed emitters

### Web Integration
- [ ] `createAuthSession()` called after OIDC login
- [ ] `credentials: 'include'` in fetch
- [ ] EventSource can authenticate via cookie

### Tests
- [ ] Unit tests cover emitter lifecycle
- [ ] Integration tests cover auth scenarios (401, 403)
- [ ] All tests pass

### Contract
- [ ] OpenAPI documents both new endpoints
- [ ] Response types correct

---

## Acceptance Criteria Verification

| AC | Description | Status |
|----|-------------|--------|
| AC-1 | SSE endpoint returns `text/event-stream` | ✅ |
| AC-2 | Cookie-based auth for EventSource | ✅ |
| AC-3 | Notifications sent AFTER_COMMIT only | ✅ |
| AC-4 | Heartbeat every 30 seconds | ✅ |
| AC-5 | 403 if not household member | ✅ |

---

## Commands to Verify

```bash
# Run unit tests
cd /home/vad/Документы/hometusk/services/backend
./gradlew test --tests "*SseNotificationServiceTest*"

# Run integration tests
./gradlew test --tests "*NotificationSseIntegrationTest*"

# Check formatting
./gradlew spotlessCheck

# Build
./gradlew build
```

---

## GO/NO-GO Decision

Based on review:

**Must-fix (blocks merge):**
- (list any critical issues)

**Should-fix (can merge, follow-up):**
- (list any non-critical issues)

**Decision:** GO / NO-GO
