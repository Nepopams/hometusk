# Codex PLAN Prompt: ST-604 — Degraded Fallback (Polling)

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

---

## Task
Plan the implementation of polling fallback when SSE is unavailable.

## Sources of Truth (MUST READ)
1. `docs/planning/epics/EP-007/stories/ST-604-degraded-fallback.md` — Story spec with ACs
2. `clients/web/src/hooks/useNotificationStream.ts` — Current SSE hook (ST-603)
3. `clients/web/src/hooks/useNotifications.ts` — Notification state management
4. `clients/web/src/lib/api.ts` — listNotifications with `since` parameter

## Architecture Context (Already Known)
- `useNotificationStream` handles SSE connection with exponential backoff
- After MAX_RETRIES (10), currently calls `onAuthError` → redirect to login
- `listNotifications(householdId, { since })` already exists
- `useNotifications.addNotification()` has dedup by ID
- Story says MAX_SSE_RETRIES = 5 for switching to polling (not 10)

---

## Critical Constraints (MUST FOLLOW)

### 1. Polling Fallback Logic
- After 5 failed SSE retries → switch to polling mode
- Poll every 30 seconds using `listNotifications(householdId, { since })`
- Track `lastFetchTimestamp` for incremental fetch
- Periodically attempt SSE reconnect (every 60s)

### 2. Mode States
```typescript
type ConnectionMode = 'sse' | 'polling';
type StreamStatus = 'connecting' | 'connected' | 'disconnected' | 'polling' | 'error';
```

### 3. Degraded Indicator
- Subtle visual indicator when in polling mode
- Tooltip: "Updates may be delayed"
- No intrusive error modals

### 4. Auto-recovery
- While polling, try SSE reconnect every 60s
- If SSE connects successfully → switch back to SSE mode
- Clear polling intervals

### 5. Silent Degradation
- No console.error for expected polling fallback
- Use console.debug for logging
- App continues to function normally

---

## Exploration Tasks

### Task 1: Understand Current Hook Structure
- Read `useNotificationStream.ts` — current retry logic
- Where does MAX_RETRIES trigger error state?
- How to add mode switching?

### Task 2: Check API Integration
- Read `api.ts` — confirm `listNotifications` supports `since`
- Read `useNotifications.ts` — confirm `addNotification` has dedup

### Task 3: Check NotificationBell Integration
- Read `NotificationBell.tsx` — where is `useNotificationStream` called?
- Where to show degraded indicator?

### Task 4: Check Existing CSS Patterns
- Read `index.css` — existing warning/status styles
- How to add degraded indicator styles?

---

## Output Format

After exploration, provide:

1. **Verification of Assumptions**
   - Confirm hook structure
   - Confirm API support for `since`
   - Confirm dedup exists

2. **Files to Modify** (with specific changes)
   - `useNotificationStream.ts` — add polling logic
   - `NotificationBell.tsx` — show degraded indicator
   - `index.css` — add degraded styles

3. **Key Implementation Details**
   - Mode state management
   - Polling interval setup
   - SSE reconnect attempts
   - Cleanup on unmount

4. **Risks/Blockers** (if any)

5. **Questions** (if clarification needed)

---

## Stop Conditions
- If hook structure differs significantly → STOP and describe
- Do NOT guess without evidence from codebase
