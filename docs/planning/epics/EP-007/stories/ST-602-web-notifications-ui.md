# Story: ST-602 — Web Notifications UI

## Sources of Truth
- Epic: `docs/planning/epics/EP-007/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-notifications-realtime.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml` (notifications endpoints)

---

## Status
**Ready** — DoR complete, pending sprint commitment

## Priority
P1 (Core)

## Points
5

---

## Description
Build the web UI for viewing and managing notifications: bell icon in header, dropdown panel with notification list, mark as read functionality.

### User Value
As a household member, I want to see my notifications in a convenient location (header bell), quickly scan what happened, and mark them as read, so I stay informed without extra navigation.

### Technical Approach
- Bell icon component in header
- Unread count badge (red dot with number)
- Dropdown panel with notification list
- Individual notification items with type icons
- Click to mark as read (single)
- **"Mark all as read" — sequential calls to existing endpoint** (no new bulk endpoint)
- Hook for fetching and managing notifications state

---

## Acceptance Criteria

### AC-1: Bell Icon Visible
```gherkin
Given user is on any household page
Then bell icon is visible in header
```

### AC-2: Unread Count Badge
```gherkin
Given user has 5 unread notifications
Then bell icon shows badge with "5"
And badge is red/highlighted

Given user has 0 unread notifications
Then no badge shown (or badge hidden)

Given user has 99+ unread notifications
Then badge shows "99+"
```

### AC-3: Dropdown Opens on Click
```gherkin
Given user clicks bell icon
Then notifications dropdown opens
And shows list of recent notifications
And dropdown closes on click outside
```

### AC-4: Notification Item Display
```gherkin
Given notification of type "task_assigned"
Then item shows:
  - Task icon
  - Summary: "Task assigned: Clean kitchen"
  - Relative time: "2 min ago"
  - Unread indicator (bold/dot)
```

### AC-5: Mark as Read on Click (Single)
```gherkin
Given unread notification
When user clicks on notification item
Then POST /notifications/{id}/read is called
And item becomes read (no bold/dot)
And unread count decreases
```

### AC-6: Mark All as Read (Sequential Calls)
```gherkin
Given multiple unread notifications visible in dropdown
When user clicks "Mark all as read"
Then POST /notifications/{id}/read is called for each visible unread notification
And all items become read
And unread count becomes 0
And button is disabled during operation
```

**Note:** No bulk endpoint required. Implementation calls existing single-mark endpoint sequentially with `Promise.all()`. For typical notification counts (< 20 visible), this is acceptable UX.

### AC-7: Empty State
```gherkin
Given user has no notifications
When dropdown opens
Then shows "No notifications yet"
```

### AC-8: Loading State
```gherkin
Given notifications are loading
Then show skeleton/spinner in dropdown
```

---

## Test Strategy

### Manual Tests
- Open dropdown, verify notification items
- Click notification, verify mark as read
- Verify badge count updates
- Click "Mark all as read", verify all marked
- Test empty state
- Test loading state

### Component Structure
```
Header
├── NotificationBell
│   ├── BellIcon
│   ├── UnreadBadge
│   └── NotificationDropdown (when open)
│       ├── DropdownHeader ("Notifications" + "Mark all as read" button)
│       ├── NotificationList
│       │   └── NotificationItem (x N)
│       └── EmptyState
```

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Using existing API, no new endpoints |
| adr_needed | no | Standard UI patterns |
| diagrams_needed | no | |
| security_sensitive | no | |
| traceability_critical | no | |

---

## Dependencies
- Existing GET /households/{householdId}/notifications API
- Existing POST /notifications/{id}/read API

## Blocked By
- None (can use polling initially, SSE in ST-603)

---

## Implementation Notes

### Components to Create
```
clients/web/src/components/notifications/
├── NotificationBell.tsx        # Bell icon + badge + dropdown trigger
├── NotificationDropdown.tsx    # Dropdown container with header
├── NotificationList.tsx        # List of notifications
├── NotificationItem.tsx        # Single notification
├── UnreadBadge.tsx            # Count badge
├── EmptyNotifications.tsx      # Empty state
└── index.ts                    # Barrel exports
```

### Hook: useNotifications
```typescript
export function useNotifications(householdId: string) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const unreadCount = useMemo(() =>
    notifications.filter(n => !n.readAt).length,
    [notifications]
  );

  const markAsRead = async (id: string) => {
    await api.markNotificationRead(id);
    setNotifications(prev =>
      prev.map(n => n.id === id ? { ...n, readAt: new Date().toISOString() } : n)
    );
  };

  // Sequential calls, no bulk endpoint
  const markAllAsRead = async () => {
    const unread = notifications.filter(n => !n.readAt);
    await Promise.all(unread.map(n => api.markNotificationRead(n.id)));
    setNotifications(prev =>
      prev.map(n => ({ ...n, readAt: n.readAt || new Date().toISOString() }))
    );
  };

  const refresh = async () => { ... };

  // Add notification from realtime (ST-603)
  const addNotification = (notification: Notification) => {
    setNotifications(prev => {
      if (prev.some(n => n.id === notification.id)) return prev;
      return [notification, ...prev];
    });
  };

  return { notifications, unreadCount, loading, error, markAsRead, markAllAsRead, refresh, addNotification };
}
```

### Notification Type Icons
| Type | Icon |
|------|------|
| task_assigned | CheckSquare |
| task_completed | CheckCircle |
| shopping_item_added | ShoppingCart |
| shopping_item_purchased | ShoppingBag |
| invite_accepted | UserPlus |

### CSS Classes
```css
.notification-bell { }
.notification-badge { }
.notification-dropdown { }
.notification-dropdown-header { }
.notification-item { }
.notification-item--unread { }
.notification-icon { }
.notification-summary { }
.notification-time { }
.mark-all-read-btn { }
.mark-all-read-btn:disabled { }
```
