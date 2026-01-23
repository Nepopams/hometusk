# Workpack: ST-503 — Command History

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-503-command-history.md`
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- ST-501: `clients/web/src/hooks/useCommand.ts`
- ST-502: `clients/web/src/components/commands/StatusBadge.tsx`

---

## Goal
Persist and display recent commands per household in localStorage with history list, details view, and clear functionality.

## User Value
As a household member, I want to see my recent commands, so that I can understand what I've done and reference past interactions.

---

## Scope

### In Scope
- Store last 50 commands per household in localStorage
- Display command history list with status badges and timestamps
- Click to expand/view details
- Clear history option with confirmation
- Prune old entries when limit exceeded
- Household-scoped history (switch household = different history)

### Out of Scope
- Server-side command history
- Search/filter in history
- Export history
- Cross-device sync

---

## Anchors (Non-negotiables)

| Artifact | Path | Constraint |
|----------|------|------------|
| Types | `types/api.ts` | Use existing CommandRequest, CommandResponse |
| StatusBadge | `components/commands/StatusBadge.tsx` | Reuse for history entries |
| useCommand | `hooks/useCommand.ts` | Integrate history save on success |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/commandHistory.ts` | CREATE | localStorage helper functions |
| `clients/web/src/hooks/useCommandHistory.ts` | CREATE | Hook for reading/clearing history |
| `clients/web/src/components/commands/CommandHistory.tsx` | CREATE | Main history list component |
| `clients/web/src/components/commands/CommandHistoryEntry.tsx` | CREATE | Single entry with expand/collapse |
| `clients/web/src/hooks/useCommand.ts` | MODIFY | Save to history on response |
| `clients/web/src/components/commands/index.ts` | MODIFY | Export new components |
| `clients/web/src/styles/index.css` | MODIFY | Add history styles |
| `clients/web/src/routes/HouseholdLayout.tsx` | MODIFY | Render CommandHistory below CommandInput |

---

## Implementation Plan

### Step 1: Create localStorage helper (commandHistory.ts)

```typescript
// clients/web/src/lib/commandHistory.ts
import type { CommandRequest, CommandResponse, CommandType, CommandStatus } from '../types/api';

export interface CommandHistoryEntry {
  id: string;
  displayText: string;
  commandType: CommandType;
  status: CommandStatus;
  timestamp: string; // ISO 8601
  correlationId: string;
  commandId: string;
  householdId: string;
  request: CommandRequest;
  response: CommandResponse;
}

const MAX_HISTORY_ENTRIES = 50;
const STORAGE_KEY_PREFIX = 'hometusk:commandHistory:';

function getStorageKey(householdId: string): string {
  return `${STORAGE_KEY_PREFIX}${householdId}`;
}

export function getHistory(householdId: string): CommandHistoryEntry[] {
  const key = getStorageKey(householdId);
  const data = localStorage.getItem(key);
  if (!data) return [];
  try {
    return JSON.parse(data) as CommandHistoryEntry[];
  } catch {
    return [];
  }
}

export function addToHistory(entry: CommandHistoryEntry): void {
  const key = getStorageKey(entry.householdId);
  const history = getHistory(entry.householdId);
  const updated = [entry, ...history].slice(0, MAX_HISTORY_ENTRIES);
  localStorage.setItem(key, JSON.stringify(updated));
}

export function clearHistory(householdId: string): void {
  const key = getStorageKey(householdId);
  localStorage.removeItem(key);
}

export function generateEntryId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export function createDisplayText(request: CommandRequest): string {
  if (request.type === 'create_task') {
    const payload = request.payload as { title?: string };
    return `Create task: ${payload.title || 'Untitled'}`;
  }
  if (request.type === 'complete_task') {
    const payload = request.payload as { taskId?: string };
    return `Complete task: ${payload.taskId || 'Unknown'}`;
  }
  return `Command: ${request.type}`;
}
```

### Step 2: Create useCommandHistory hook

```typescript
// clients/web/src/hooks/useCommandHistory.ts
import { useCallback, useSyncExternalStore } from 'react';
import { getHistory, clearHistory as clearStorageHistory } from '../lib/commandHistory';
import type { CommandHistoryEntry } from '../lib/commandHistory';

export function useCommandHistory(householdId: string | null) {
  const subscribe = useCallback((callback: () => void) => {
    const handler = (e: StorageEvent) => {
      if (e.key?.startsWith('hometusk:commandHistory:')) {
        callback();
      }
    };
    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  const getSnapshot = useCallback(() => {
    if (!householdId) return '[]';
    return JSON.stringify(getHistory(householdId));
  }, [householdId]);

  const entriesJson = useSyncExternalStore(subscribe, getSnapshot, () => '[]');
  const entries: CommandHistoryEntry[] = JSON.parse(entriesJson);

  const clearHistory = useCallback(() => {
    if (householdId) {
      clearStorageHistory(householdId);
      window.dispatchEvent(new StorageEvent('storage', {
        key: `hometusk:commandHistory:${householdId}`,
      }));
    }
  }, [householdId]);

  return { entries, clearHistory };
}
```

### Step 3: Modify useCommand to save history

In `useCommand.ts`, after successful response, call `addToHistory`:

```typescript
// Add import
import { addToHistory, generateEntryId, createDisplayText } from '../lib/commandHistory';

// In execute function, after successful response:
if (response) {
  addToHistory({
    id: generateEntryId(),
    displayText: createDisplayText(request),
    commandType: request.type,
    status: response.status,
    timestamp: new Date().toISOString(),
    correlationId: response.correlationId,
    commandId: response.commandId,
    householdId: request.householdId,
    request,
    response,
  });
  // Trigger storage event for other tabs/hooks
  window.dispatchEvent(new StorageEvent('storage', {
    key: `hometusk:commandHistory:${request.householdId}`,
  }));
}
```

### Step 4: Create CommandHistoryEntry component

Display single entry with expand/collapse.

### Step 5: Create CommandHistory component

List of entries with Clear button, empty state.

### Step 6: Integrate into HouseholdLayout

Render `<CommandHistory />` below `<CommandInput />`.

### Step 7: Add CSS styles

Styles for history list, entries, expanded state.

### Step 8: Update barrel exports

---

## Relative Timestamp Helper

```typescript
function formatRelativeTime(timestamp: string): string {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diffMs = now - then;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${diffDays}d ago`;
}
```

---

## Tests & Checks

### Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

### Manual Testing
1. Submit command → verify localStorage has entry
2. Refresh page → verify history persists
3. Submit 51 commands → verify oldest pruned (50 max)
4. Click entry → verify details expand
5. Click Clear → confirm → verify empty
6. Switch household → verify different history

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| localStorage full | History not saved | Try/catch, silent fail |
| Large payload sizes | Storage bloat | 50 entry limit |
| useSyncExternalStore complexity | Re-render issues | Test carefully |

---

## Done Criteria

| AC | DoD Check |
|----|-----------|
| AC1: Command saved | localStorage updated after command |
| AC2: History displayed | List with badges and timestamps |
| AC3: Household scoped | Different history per household |
| AC4: Expand entry | Details view with full data |
| AC5: Limit enforced | Max 50 entries |
| AC6: Clear history | Confirmation + delete |
| AC7: Empty state | "No commands yet" message |

---

## Anti-Scope-Creep

DO NOT:
- Add server-side history storage
- Add search/filter functionality
- Add export feature
- Add retry from history (future story)
