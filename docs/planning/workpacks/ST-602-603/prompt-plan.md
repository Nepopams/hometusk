# Codex PLAN Prompt: ST-602 + ST-603 — Web Notifications UI + Realtime Subscribe

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
Plan the implementation of web notifications UI (bell icon, dropdown, list) and real-time SSE subscription.

## Sources of Truth (MUST READ)
1. `docs/planning/workpacks/ST-602-603/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-007/stories/ST-602-web-notifications-ui.md` — UI story spec
3. `docs/planning/epics/EP-007/stories/ST-603-web-realtime-subscribe.md` — Realtime story spec
4. `docs/contracts/http/commands.openapi.yaml` — API contract (notifications endpoints)
5. `clients/web/src/components/Layout/Header.tsx` — Where to add NotificationBell
6. `clients/web/src/hooks/useTasks.ts` — Pattern for hooks
7. `clients/web/src/lib/api.ts` — Existing API functions (including createAuthSession)
8. `clients/web/src/types/api.ts` — Existing types

## Architecture Context (Already Known)
- ST-601 implemented: SSE endpoint at `/households/{householdId}/notifications/stream`
- Cookie-based auth: `createAuthSession()` already in api.ts
- Backend uses `withCredentials: true` for EventSource
- Hook patterns: useState, useCallback, useEffect
- Component structure: organized by domain (tasks/, commands/, ui/)

---

## Critical Constraints (MUST FOLLOW)

### 1. Notification Types (from OpenAPI)
```typescript
type NotificationType =
  | 'invite_accepted'
  | 'task_assigned'
  | 'task_completed'
  | 'shopping_item_added'
  | 'shopping_item_purchased';

interface Notification {
  id: string;
  householdId: string;
  userId: string;
  type: NotificationType;
  payload: NotificationPayload;
  createdAt: string;
  readAt: string | null;
}
```

### 2. API Endpoints (from OpenAPI)
- `GET /households/{householdId}/notifications` — list notifications
- `GET /households/{householdId}/notifications/stream` — SSE stream
- `POST /notifications/{notificationId}/read` — mark as read

### 3. Mark All as Read (Sequential Calls)
- No bulk endpoint exists
- Use `Promise.all()` with individual mark-read calls
- Acceptable for typical counts (< 20 visible notifications)

### 4. SSE with Cookie Auth
```typescript
new EventSource(url, { withCredentials: true });
```

### 5. Component Placement
- NotificationBell goes in Header.tsx (after HouseholdDropdown)
- Hook integration in HouseholdLayout or via Context

---

## Exploration Tasks

### Task 1: Understand Existing Patterns
- Read `Header.tsx` — where to add NotificationBell
- Read `useTasks.ts` — hook pattern (state, loading, error, refetch)
- Read `api.ts` — API call patterns
- Read `types/api.ts` — type definition patterns

### Task 2: Verify API Contract
- Read OpenAPI `/households/{householdId}/notifications` response schema
- Read OpenAPI `/notifications/{notificationId}/read` request/response
- Confirm `since` parameter for incremental fetch

### Task 3: Check Existing UI Components
- Read `components/ui/Spinner.tsx` — loading pattern
- Read `components/ui/ErrorMessage.tsx` — error pattern
- Read `components/tasks/EmptyTasks.tsx` — empty state pattern
- Check for existing dropdown patterns

### Task 4: Check CSS Patterns
- Read `styles/index.css` — existing CSS variables and patterns
- Check for existing badge styles
- Check for existing dropdown styles

### Task 5: Understand Layout Integration
- Read `routes/HouseholdLayout.tsx` — where hooks are used
- Read `context/AuthContext.tsx` — how context is provided
- How to pass `addNotification` callback to stream hook

---

## Output Format

After exploration, provide:

1. **Verification of Assumptions**
   - Confirm Header structure
   - Confirm hook patterns
   - Confirm API function patterns
   - Confirm CSS patterns

2. **Files to Create** (with purpose)
   - `components/notifications/NotificationBell.tsx`
   - `components/notifications/NotificationDropdown.tsx`
   - `components/notifications/NotificationList.tsx`
   - `components/notifications/NotificationItem.tsx`
   - `components/notifications/UnreadBadge.tsx`
   - `components/notifications/EmptyNotifications.tsx`
   - `components/notifications/index.ts`
   - `hooks/useNotifications.ts`
   - `hooks/useNotificationStream.ts`
   - `types/notification.ts`

3. **Files to Modify** (with specific changes)
   - `components/Layout/Header.tsx` — add NotificationBell
   - `lib/api.ts` — add notification API functions
   - `types/api.ts` — add notification type exports
   - `styles/index.css` — add notification styles

4. **Key Implementation Details**
   - Hook state management
   - SSE connection lifecycle
   - Reconnect with exponential backoff
   - Click outside to close dropdown
   - Type icons mapping

5. **Risks/Blockers** (if any)

6. **Questions** (if clarification needed)

---

## Stop Conditions
- If existing patterns differ significantly from expected → STOP and describe
- If missing dependencies → STOP and list
- Do NOT guess without evidence from codebase
