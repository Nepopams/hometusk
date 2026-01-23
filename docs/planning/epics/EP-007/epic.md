# Epic: EP-007 — Notifications & Realtime v0

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (notifications section)
- Existing Backend: `services/backend/src/main/java/com/hometusk/notifications/`

---

## Status
**Planning** — Stories defined, pending human gate approval

## Initiative Alignment
This epic implements the **NOW** increment of INIT-2026Q2-notifications-realtime:
- Realtime notification delivery via SSE
- Web notifications UI (bell + list)
- Realtime subscription in web client
- Degraded fallback (polling when SSE unavailable)

---

## Epic Goal
Enable users to:
1. See new notifications appear in real-time without page refresh
2. View notifications list via bell icon in header
3. Mark notifications as read
4. Continue working when realtime channel is unavailable (degraded mode)

**Contract note:** Backend notification publishing is already complete. This epic focuses on:
- Adding SSE realtime delivery endpoint
- Building web notifications UI
- Connecting web to realtime channel

---

## Pre-existing Infrastructure (Already Done)

### Backend (Complete)
| Component | Location | Status |
|-----------|----------|--------|
| NotificationService | `notifications/service/NotificationService.java` | ✅ Done |
| Notification entity | `notifications/domain/Notification.java` | ✅ Done |
| NotificationRepository | `notifications/repository/NotificationRepository.java` | ✅ Done |
| NotificationController | `notifications/api/NotificationController.java` | ✅ Done |
| Event publishing | Called from ActionExecutor, ShoppingService, InviteService | ✅ Done |

### API Endpoints (Complete)
| Endpoint | Method | Status |
|----------|--------|--------|
| `/api/v1/households/{householdId}/notifications` | GET | ✅ Done |
| `/api/v1/notifications/{notificationId}/read` | POST | ✅ Done |

---

## In Scope (This Epic)

### SSE Realtime Endpoint (ST-601)
- New endpoint: `GET /api/v1/households/{householdId}/notifications/stream`
- SSE (Server-Sent Events) transport
- Authentication via JWT (query param or header)
- Household boundary check (403 if not member)
- Heartbeat every 30s to keep connection alive
- Graceful disconnect handling

### Web Notifications UI (ST-602)
- Bell icon in header with unread count badge
- Notifications dropdown/panel
- Individual notification items with:
  - Type icon (task/shopping/invite)
  - Summary text
  - Relative timestamp
  - Read/unread visual state
- Mark as read on click
- "Mark all as read" action
- Empty state when no notifications

### Web Realtime Subscribe (ST-603)
- EventSource connection to SSE endpoint
- Auto-reconnect on disconnect (exponential backoff)
- Parse incoming notification events
- Update notifications list in real-time
- Update unread count badge
- Connection status indicator (optional)

### Degraded Fallback (ST-604)
- Detect when SSE is unavailable (connection fails)
- Fall back to polling (every 30s)
- Visual indicator of degraded mode
- Auto-switch back to SSE when available
- No user-facing errors (silent degradation)

### Notification Deduplication (ST-605)
- Idempotency key on notification creation
- Prevent duplicate notifications for same event
- Rate limiting consideration (max N per minute per user)

---

## Out of Scope (Explicit)

### Deferred to NEXT
- **Push notifications** (FCM/APNs/Web Push)
- **Email/SMS notifications**
- **Notification preferences** (user can mute types)
- **Batch notifications grouping** ("3 tasks assigned to you")
- **Notification sounds**

### Never in Scope
- Complex notification rules engine
- ML-based notification prioritization
- Cross-household notifications

---

## Security & Data Boundaries

### Authentication
- SSE endpoint requires valid JWT
- Token validation on connection establishment
- Connection closed on token expiry (client must reconnect)

### Household Scoping
- SSE stream scoped to single household
- Server publishes only user's own notifications
- 403 if user not member of requested household

### No Cross-Household Leaks
- Each SSE connection is single-household
- Backend filters by householdId + userId
- Integration tests verify boundary enforcement

---

## API Contract Additions

### New Endpoint: SSE Stream
```yaml
/households/{householdId}/notifications/stream:
  get:
    operationId: streamNotifications
    summary: Stream notifications via SSE
    description: |
      Server-Sent Events stream for real-time notification delivery.
      Connection stays open until client disconnects.
      Heartbeat sent every 30 seconds.
    tags:
      - Notifications
    parameters:
      - name: householdId
        in: path
        required: true
        schema:
          type: string
          format: uuid
    responses:
      '200':
        description: SSE stream established
        content:
          text/event-stream:
            schema:
              type: string
      '401':
        description: Authentication required
      '403':
        description: Not a member of this household
```

### SSE Event Format
```
event: notification
data: {"id":"...", "type":"task_assigned", "payload":{...}, "createdAt":"..."}

event: heartbeat
data: {"timestamp":"..."}
```

---

## Stories

| ID | Title | Status | Priority | Points |
|----|-------|--------|----------|--------|
| ST-601 | SSE Realtime Endpoint | Ready | P1 | 3 |
| ST-602 | Web Notifications UI | Ready | P1 | 5 |
| ST-603 | Web Realtime Subscribe | Ready | P1 | 3 |
| ST-604 | Degraded Fallback | Ready | P2 | 2 |
| ST-605 | Notification Deduplication | Ready | P3 | 2 |

**Total:** 15 points

### Sprint Mapping (Proposed)
- **Sprint S06 (committed):** ST-601, ST-602, ST-603 (core flow, 11 points)
- **Sprint S06 (stretch):** ST-604 (degraded, 2 points)
- **Sprint S07 (candidate):** ST-605 (dedup, 2 points)

---

## Dependencies

| Dependency | Type | Status | Notes |
|------------|------|--------|-------|
| EP-003 (Web Foundation) | Internal | Done | React app, routing |
| EP-004 (Auth/Session) | Internal | Done | Token handling |
| EP-005 (Household Lifecycle) | Internal | Done | Household context |
| EP-006 (Command UX) | Internal | Done | Proves web patterns |
| Backend Notifications | Internal | Done | Service + API ready |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| SSE connection instability | Users miss notifications | ST-604 degraded fallback |
| Token expiry mid-stream | Connection drops | Client auto-reconnect with fresh token |
| High connection count | Server resource pressure | Heartbeat timeout + cleanup |
| Notification spam | UX degradation | ST-605 deduplication + rate limits |
| Cross-browser SSE issues | Safari edge cases | Test on Safari, use polyfill if needed |

---

## Exit Criteria (NOW Delivered)

From initiative INIT-2026Q2-notifications-realtime:

1. ✅ In-app notifications appear for task/shopping/invite events (backend done)
2. [ ] Notifications delivered in real-time via SSE (ST-601, ST-603)
3. [ ] Web UI shows notifications with bell icon (ST-602)
4. [ ] Mark as read works (ST-602, backend done)
5. [ ] Degraded mode works when SSE unavailable (ST-604)
6. [ ] 95% notifications visible ≤ 2s after event (ST-603)
7. [ ] 0 cross-household leaks (ST-601 boundary checks)

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | New SSE endpoint |
| adr_needed | no | SSE is standard pattern |
| diagrams_needed | no | No structural changes |
| security_sensitive | yes | Auth on SSE, household scoping |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
| Backend Service | `services/backend/src/main/java/com/hometusk/notifications/` |
