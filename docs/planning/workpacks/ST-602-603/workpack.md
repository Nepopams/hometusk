# Workpack: ST-602 + ST-603 — Web Notifications UI + Realtime Subscribe

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Stories:
  - `docs/planning/epics/EP-007/stories/ST-602-web-notifications-ui.md`
  - `docs/planning/epics/EP-007/stories/ST-603-web-realtime-subscribe.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (notifications endpoints)
- Existing Web Patterns: `clients/web/src/components/commands/` (EP-006 patterns)

---

## Status
**Done** — Implementation complete (commit 2bf57a1)

---

## Outcome
Web client with notifications bell icon, dropdown panel, and real-time updates via SSE connection.

---

## Acceptance Criteria Summary

### ST-602 (UI)
1. Bell icon visible in header
2. Unread count badge shows correct number
3. Dropdown opens on click, shows notification list
4. Notification items show type icon, summary, time, read state
5. Mark as read on click
6. "Mark all as read" action works
7. Empty state when no notifications

### ST-603 (Realtime)
1. SSE connection established on household page load
2. New notifications appear instantly without refresh
3. Auto-reconnect on disconnect with exponential backoff
4. Cleanup on household change and logout

---

## Files to Change/Create

### New Files
| Path | Purpose |
|------|---------|
| `clients/web/src/components/notifications/NotificationBell.tsx` | Bell icon with badge |
| `clients/web/src/components/notifications/NotificationDropdown.tsx` | Dropdown container |
| `clients/web/src/components/notifications/NotificationList.tsx` | List of notifications |
| `clients/web/src/components/notifications/NotificationItem.tsx` | Single notification |
| `clients/web/src/components/notifications/UnreadBadge.tsx` | Count badge |
| `clients/web/src/components/notifications/EmptyNotifications.tsx` | Empty state |
| `clients/web/src/components/notifications/index.ts` | Barrel exports |
| `clients/web/src/hooks/useNotifications.ts` | Notification state management |
| `clients/web/src/hooks/useNotificationStream.ts` | SSE connection hook |
| `clients/web/src/lib/notificationApi.ts` | API calls for notifications |
| `clients/web/src/types/notification.ts` | Notification types |

### Modified Files
| Path | Changes |
|------|---------|
| `clients/web/src/routes/HouseholdLayout.tsx` | Add NotificationBell to header |
| `clients/web/src/styles/index.css` | Add notification styles |
| `clients/web/src/types/api.ts` | Add notification types |
| `clients/web/src/lib/api.ts` | Add notification API functions |

---

## Implementation Plan

### Step 1: Add Notification Types
- Create `types/notification.ts` with `Notification`, `NotificationType` types
- Add to `types/api.ts` exports

### Step 2: Create API Functions
- `listNotifications(householdId, { since?, limit? })`
- `markNotificationRead(notificationId)`

### Step 3: Create useNotifications Hook
- State: notifications, loading, error
- Computed: unreadCount
- Actions: markAsRead, markAllAsRead, refresh, addNotification

### Step 4: Create Notification UI Components
- `NotificationBell`: Bell icon + badge + dropdown trigger
- `NotificationDropdown`: Dropdown container with header/footer
- `NotificationList`: Maps notifications to items
- `NotificationItem`: Type icon + summary + time + read state
- `UnreadBadge`: Red circle with count
- `EmptyNotifications`: "No notifications yet"

### Step 5: Create useNotificationStream Hook
- EventSource connection to SSE endpoint
- Parse `notification` events, call `addNotification`
- Handle `heartbeat` events (no-op, just alive)
- Auto-reconnect with exponential backoff
- Cleanup on unmount/household change

### Step 6: Integrate into HouseholdLayout
- Add NotificationBell to header
- Connect useNotifications + useNotificationStream
- Pass addNotification callback to stream hook

### Step 7: Add CSS Styles
- Bell icon styles
- Badge styles (red, positioned)
- Dropdown styles (shadow, z-index)
- Notification item styles (hover, unread state)

---

## Verification Commands

```bash
# Build
cd /home/vad/Документы/hometusk/clients/web
npm run build

# Lint
npm run lint

# Type check
npm run typecheck

# Dev server for manual testing
npm run dev
```

---

## Tests

### Manual Test Cases
1. Open household page → verify bell icon visible
2. Check unread count matches actual unread
3. Click bell → verify dropdown opens
4. Verify notification items show correctly
5. Click notification → verify marked as read
6. "Mark all as read" → verify all marked
7. Open DevTools Network → verify SSE connection
8. Create task in another tab → verify notification appears
9. Close network → verify reconnect attempts

### Component Tests (if time permits)
- NotificationItem renders correctly
- UnreadBadge shows correct count
- useNotifications state management

---

## DoD Checklist
- [ ] Code follows project conventions
- [ ] npm run lint passes
- [ ] npm run build passes
- [ ] Bell icon visible in header
- [ ] Notifications display correctly
- [ ] Mark as read works
- [ ] SSE connection established
- [ ] Real-time updates work
- [ ] Auto-reconnect works
- [ ] No console errors

---

## Risks
| Risk | Mitigation |
|------|------------|
| SSE not supported in older browsers | Use EventSource polyfill if needed |
| Token in URL security | Document, use polling fallback (ST-604) |
| Safari EventSource issues | Test on Safari, handle edge cases |

---

## Rollback
- Remove NotificationBell from header
- Remove notification components
- User can still view notifications via API (no UI, but data accessible)

---

## Prompt Pack
- `prompt-plan.md`: Explore existing patterns (EP-006), API contract
- `prompt-apply.md`: Implementation after plan approval
- `prompt-review.md`: Review after apply completion
