# Sprint S06: Notifications & Realtime v0

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- Epic: `docs/planning/epics/EP-007/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Sprint Goal
Deliver **real-time notification experience** in web client: users see notifications appear instantly via SSE, with a dedicated UI (bell icon + dropdown), creating a "living" household collaboration experience.

---

## Thin Slice Definition
**"Task assigned → notification appears in UI within 2 seconds, no refresh needed"**

This slice validates:
1. SSE transport works (backend → web)
2. Notification UI is usable
3. Integration is seamless

---

## Key Technical Decisions

### SSE Authentication: Cookie-based
- EventSource connects with `withCredentials: true`
- Server validates session cookie (Spring Security)
- No token in URL (security: logs, history, referrer)

### Mark All as Read: Sequential Calls
- No new bulk endpoint
- UI calls `POST /notifications/{id}/read` for each visible unread
- Acceptable UX for typical notification counts (< 20)

### AFTER_COMMIT Publishing
- SSE events sent only after transaction commits
- Prevents phantom notifications on rollback

---

## Scope

### Committed (Must Deliver)

| ID | Story | Points | Workpack |
|----|-------|--------|----------|
| ST-601 | SSE Realtime Endpoint | 3 | `workpacks/ST-601/` |
| ST-602 | Web Notifications UI | 5 | `workpacks/ST-602-603/` |
| ST-603 | Web Realtime Subscribe | 3 | `workpacks/ST-602-603/` |

**Total Committed:** 11 points

### Stretch (If Capacity Allows)

| ID | Story | Points | Workpack |
|----|-------|--------|----------|
| ST-604 | Degraded Fallback | 2 | `workpacks/ST-604-605/` |

**Total with Stretch:** 13 points

### Out of Scope (Explicit)

| ID | Story | Reason |
|----|-------|--------|
| ST-605 | Notification Deduplication | Defer to S07, not critical for thin slice |
| — | Rate limiting | Risk of losing notifications; requires product decision |
| — | Push notifications | Out of initiative scope |
| — | Email/SMS | Out of initiative scope |
| — | Notification preferences | Future initiative |

---

## Dependencies

| Dependency | Status | Risk |
|------------|--------|------|
| Backend notification infrastructure | ✅ Done | None |
| Web patterns (EP-006) | ✅ Done | None |
| Household context (EP-005) | ✅ Done | None |
| Session cookie auth (EP-004) | ✅ Done | None |

---

## Delivery Sequence

```
Day 1-2: ST-601 (Backend SSE)
         - SseNotificationService
         - SSE endpoint with cookie auth
         - AFTER_COMMIT event publishing
         ↓
Day 3-4: ST-602 + ST-603 (Web UI + Realtime)
         - NotificationBell component
         - useNotifications hook
         - useNotificationStream hook
         - EventSource with withCredentials
         ↓
Day 5:   Integration testing + polish
         ↓
(Stretch) ST-604 (Degraded fallback)
```

**Note:** ST-602 and ST-603 can start UI work in parallel with ST-601 using mock SSE, but integration requires ST-601 complete.

---

## Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| SSE complexity higher than expected | Delay | Medium | Start ST-601 first, have clear contract |
| Safari EventSource issues | UX degradation | Low | Test early, use polyfill if needed |
| Session expiry during SSE | Connection drops | Medium | Client auto-reconnect, redirect on auth failure |
| CORS with credentials | Connection fails | Low | Verify CORS config early |

---

## Acceptance Criteria (Sprint-level)

### Core Flow
- [ ] User opens household page → SSE connection established (cookie auth)
- [ ] Another user assigns task → notification appears in < 2s
- [ ] Bell icon shows unread count
- [ ] Click notification → marked as read
- [ ] "Mark all as read" works (sequential calls)

### Technical
- [ ] SSE endpoint returns `text/event-stream`
- [ ] SSE uses session cookie auth (no token in URL)
- [ ] SSE events sent AFTER_COMMIT only
- [ ] Heartbeat sent every 30s
- [ ] Auto-reconnect on disconnect
- [ ] No cross-household leaks (403 enforced)
- [ ] Build passes (backend + web)

### Stretch
- [ ] Polling fallback when SSE unavailable

---

## Definition of Done (Sprint)

Sprint is **Done** when:
1. All committed stories pass DoD
2. Integration testing complete
3. Manual QA playbook executed
4. Code review approved
5. Merged to main branch
6. Demo-ready

---

## Capacity Notes
- Backend: ~6 points (ST-601)
- Web: ~8 points (ST-602, ST-603, ST-604)
- Buffer: 2 points for unknowns

---

## Human Gate
**Gate B:** Approve sprint goal + committed scope before implementation begins.

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md` |
| Epic | `docs/planning/epics/EP-007/epic.md` |
| Workpack ST-601 | `docs/planning/workpacks/ST-601/` |
| Workpack ST-602-603 | `docs/planning/workpacks/ST-602-603/` |
| Workpack ST-604-605 | `docs/planning/workpacks/ST-604-605/` |
