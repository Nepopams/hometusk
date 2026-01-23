# Codex REVIEW Prompt: ST-602 + ST-603 — Web Notifications UI + Realtime Subscribe

## Mode
**REVIEW** — Code review mode. Read-only inspection.

## Task
Review the ST-602 + ST-603 implementation for correctness and alignment with acceptance criteria.

## Sources of Truth
1. `docs/planning/workpacks/ST-602-603/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-007/stories/ST-602-web-notifications-ui.md` — UI story spec
3. `docs/planning/epics/EP-007/stories/ST-603-web-realtime-subscribe.md` — Realtime story spec
4. `docs/_governance/dod.md` — Definition of Done

---

## Implementation Summary (from commit 2bf57a1)

### New Files Created
| File | Purpose |
|------|---------|
| `NotificationBell.tsx` | Bell icon + badge + dropdown |
| `NotificationDropdown.tsx` | Dropdown container with header |
| `NotificationList.tsx` | List of notifications |
| `NotificationItem.tsx` | Single notification row |
| `UnreadBadge.tsx` | Count badge |
| `EmptyNotifications.tsx` | Empty state |
| `index.ts` | Barrel exports |
| `useNotifications.ts` | State management hook |
| `useNotificationStream.ts` | SSE connection hook |
| `notification.ts` | Notification types |

### Modified Files
| File | Changes |
|------|---------|
| `Header.tsx` | Added NotificationBell |
| `api.ts` | Added listNotifications, markNotificationRead |
| `api.ts (types)` | Re-export notification types |
| `index.css` | Notification styles |

---

## Review Checklist

### UI Components (ST-602)
- [ ] Bell icon visible in header
- [ ] Unread badge shows correct count (0 hidden, 99+ cap)
- [ ] Dropdown opens on click
- [ ] Dropdown closes on click outside/ESC
- [ ] Notification items show icon, summary, time
- [ ] Unread items visually distinct
- [ ] Mark as read on click
- [ ] Mark all as read works (sequential calls)
- [ ] Empty state displays correctly
- [ ] Loading state displays correctly

### Realtime Connection (ST-603)
- [ ] EventSource uses `withCredentials: true`
- [ ] Connection established on household page load
- [ ] `notification` events parsed and displayed
- [ ] `heartbeat` events ignored (no-op)
- [ ] Auto-reconnect with exponential backoff
- [ ] Max retries triggers onAuthError
- [ ] Cleanup on household change
- [ ] Cleanup on unmount

### Code Quality
- [ ] TypeScript types correct
- [ ] No console errors
- [ ] Hooks follow React patterns
- [ ] CSS follows BEM naming
- [ ] No memory leaks (event listeners cleaned up)

---

## Acceptance Criteria Verification

### ST-602 (UI)
| AC | Description | Status |
|----|-------------|--------|
| AC-1 | Bell icon visible | ✅ |
| AC-2 | Unread count badge | ✅ |
| AC-3 | Dropdown opens on click | ✅ |
| AC-4 | Notification items display | ✅ |
| AC-5 | Mark as read on click | ✅ |
| AC-6 | Mark all as read (sequential) | ✅ |
| AC-7 | Empty state | ✅ |
| AC-8 | Loading state | ✅ |

### ST-603 (Realtime)
| AC | Description | Status |
|----|-------------|--------|
| AC-1 | SSE connection established | ✅ |
| AC-2 | Notification received in real-time | ✅ |
| AC-3 | Auto-reconnect on disconnect | ✅ |
| AC-4 | Auth error handling | ✅ |
| AC-5 | Cleanup on household change | ✅ |
| AC-6 | Cleanup on logout | ✅ |

---

## Commands to Verify

```bash
cd /home/vad/Документы/hometusk/clients/web

# Lint
npm run lint

# Build
npm run build

# Manual test
npm run dev
```

---

## GO/NO-GO Decision

Based on review:

**Must-fix (blocks merge):**
- (list any critical issues)

**Should-fix (can merge, follow-up):**
- (list any non-critical issues)

**Decision:** GO / NO-GO
