# Codex APPLY Prompt: ST-503 — Command History

## Mode
**APPLY** — Implement the approved plan. Create/modify files as specified.

---

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-503-command-history.md`
- Workpack: `docs/planning/workpacks/ST-503/workpack.md`
- Approved Plan: ST-503 PLAN output

---

## Task
Implement ST-503 (Command History) — localStorage-based command history with list view, details expansion, and clear functionality.

---

## Files to Change

| File | Action |
|------|--------|
| `clients/web/src/types/api.ts` | MODIFY — add CommandStatus type |
| `clients/web/src/lib/commandHistory.ts` | CREATE |
| `clients/web/src/hooks/useCommandHistory.ts` | CREATE |
| `clients/web/src/hooks/useCommand.ts` | MODIFY — save to history |
| `clients/web/src/components/commands/CommandHistoryEntry.tsx` | CREATE |
| `clients/web/src/components/commands/CommandHistory.tsx` | CREATE |
| `clients/web/src/components/commands/index.ts` | MODIFY — exports |
| `clients/web/src/routes/HouseholdLayout.tsx` | MODIFY — render history |
| `clients/web/src/styles/index.css` | MODIFY — add styles |

---

## Step 0: Add CommandStatus type

**File:** `clients/web/src/types/api.ts`

Add near other Command types (after CommandType):

```typescript
export type CommandStatus = 'executed' | 'needs_input' | 'rejected' | 'executed_degraded';
```

---

## Step 1: Create commandHistory.ts

**File:** `clients/web/src/lib/commandHistory.ts`

```typescript
import type { CommandRequest, CommandResponse, CommandType, CommandStatus } from '../types/api';

export interface CommandHistoryEntry {
  id: string;
  displayText: string;
  commandType: CommandType;
  status: CommandStatus;
  timestamp: string;
  correlationId: string;
  commandId: string;
  householdId: string;
  request: CommandRequest;
  response: CommandResponse;
}

const MAX_HISTORY_ENTRIES = 50;
const STORAGE_KEY_PREFIX = 'hometusk:commandHistory:';
const HISTORY_UPDATE_EVENT = 'hometusk:historyUpdate';

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
  try {
    localStorage.setItem(key, JSON.stringify(updated));
  } catch {
    // localStorage full or unavailable - silent fail
  }
}

export function clearHistory(householdId: string): void {
  const key = getStorageKey(householdId);
  localStorage.removeItem(key);
}

export function generateEntryId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export function createDisplayText(request: CommandRequest): string {
  if (request.type === 'create_task') {
    const payload = request.payload as { title?: string };
    const title = payload.title || 'Untitled';
    return `Create task: ${title.length > 40 ? title.slice(0, 40) + '...' : title}`;
  }
  if (request.type === 'complete_task') {
    const payload = request.payload as { taskId?: string };
    return `Complete task: ${payload.taskId || 'Unknown'}`;
  }
  return `Command: ${request.type}`;
}

export function dispatchHistoryUpdate(householdId: string): void {
  window.dispatchEvent(
    new CustomEvent(HISTORY_UPDATE_EVENT, { detail: { householdId } })
  );
}

export function subscribeToHistoryUpdates(callback: () => void): () => void {
  const handleStorage = (e: StorageEvent) => {
    if (e.key?.startsWith(STORAGE_KEY_PREFIX)) {
      callback();
    }
  };
  const handleCustom = () => {
    callback();
  };

  window.addEventListener('storage', handleStorage);
  window.addEventListener(HISTORY_UPDATE_EVENT, handleCustom);

  return () => {
    window.removeEventListener('storage', handleStorage);
    window.removeEventListener(HISTORY_UPDATE_EVENT, handleCustom);
  };
}
```

---

## Step 2: Create useCommandHistory.ts

**File:** `clients/web/src/hooks/useCommandHistory.ts`

```typescript
import { useCallback, useSyncExternalStore } from 'react';
import {
  getHistory,
  clearHistory as clearStorageHistory,
  dispatchHistoryUpdate,
  subscribeToHistoryUpdates,
} from '../lib/commandHistory';
import type { CommandHistoryEntry } from '../lib/commandHistory';

export function useCommandHistory(householdId: string | null) {
  const subscribe = useCallback((callback: () => void) => {
    return subscribeToHistoryUpdates(callback);
  }, []);

  const getSnapshot = useCallback(() => {
    if (!householdId) return '[]';
    return JSON.stringify(getHistory(householdId));
  }, [householdId]);

  const getServerSnapshot = useCallback(() => '[]', []);

  const entriesJson = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  const entries: CommandHistoryEntry[] = JSON.parse(entriesJson);

  const clearHistory = useCallback(() => {
    if (householdId) {
      clearStorageHistory(householdId);
      dispatchHistoryUpdate(householdId);
    }
  }, [householdId]);

  return { entries, clearHistory };
}
```

---

## Step 3: Modify useCommand.ts

**File:** `clients/web/src/hooks/useCommand.ts`

Add imports at top:

```typescript
import {
  addToHistory,
  generateEntryId,
  createDisplayText,
  dispatchHistoryUpdate,
} from '../lib/commandHistory';
```

In the `execute` function, after `setState({ isLoading: false, response, error: null, errorStatus: null });` and before `return response;`, add:

```typescript
// Save to history
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
dispatchHistoryUpdate(request.householdId);
```

---

## Step 4: Create CommandHistoryEntry.tsx

**File:** `clients/web/src/components/commands/CommandHistoryEntry.tsx`

```typescript
import { useState } from 'react';
import { StatusBadge } from './StatusBadge';
import type { CommandHistoryEntry as HistoryEntry } from '../../lib/commandHistory';
import type { CommandStatus } from '../../types/api';

interface CommandHistoryEntryProps {
  entry: HistoryEntry;
  expanded: boolean;
  onToggle: () => void;
}

const STATUS_VARIANT: Record<CommandStatus, 'success' | 'warning' | 'error' | 'info'> = {
  executed: 'success',
  executed_degraded: 'warning',
  rejected: 'error',
  needs_input: 'info',
};

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

export function CommandHistoryEntry({ entry, expanded, onToggle }: CommandHistoryEntryProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(entry.correlationId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Silent fail
    }
  };

  return (
    <div className="command-history-entry">
      <button
        type="button"
        className="command-history-entry__summary"
        onClick={onToggle}
        aria-expanded={expanded}
      >
        <span className="command-history-entry__title">{entry.displayText}</span>
        <div className="command-history-entry__meta">
          <StatusBadge variant={STATUS_VARIANT[entry.status]} title={entry.status} />
          <span className="command-history-entry__timestamp">
            {formatRelativeTime(entry.timestamp)}
          </span>
          <span className="command-history-entry__toggle">{expanded ? '−' : '+'}</span>
        </div>
      </button>

      {expanded && (
        <div className="command-history-entry__details">
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">Submitted:</span>
            <span>{new Date(entry.timestamp).toLocaleString()}</span>
          </div>
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">Command ID:</span>
            <code>{entry.commandId}</code>
          </div>
          <div className="command-history-entry__row">
            <span className="command-history-entry__label">Correlation ID:</span>
            <code>{entry.correlationId}</code>
            <button
              type="button"
              className="command-history-entry__copy"
              onClick={handleCopy}
            >
              {copied ? 'Copied' : 'Copy'}
            </button>
          </div>

          <div className="command-history-entry__section">
            <span className="command-history-entry__label">Request:</span>
            <pre className="command-history-entry__json">
              {JSON.stringify(entry.request, null, 2)}
            </pre>
          </div>

          <div className="command-history-entry__section">
            <span className="command-history-entry__label">Response:</span>
            <pre className="command-history-entry__json">
              {JSON.stringify(entry.response, null, 2)}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
```

---

## Step 5: Create CommandHistory.tsx

**File:** `clients/web/src/components/commands/CommandHistory.tsx`

```typescript
import { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useCommandHistory } from '../../hooks/useCommandHistory';
import { CommandHistoryEntry } from './CommandHistoryEntry';

export function CommandHistory() {
  const { householdId } = useAuth();
  const { entries, clearHistory } = useCommandHistory(householdId);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const handleClear = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm('Clear all command history for this household?')) {
      clearHistory();
      setExpandedId(null);
    }
  };

  const handleToggle = (id: string) => {
    setExpandedId((current) => (current === id ? null : id));
  };

  if (!householdId) {
    return null;
  }

  return (
    <div className="command-history">
      <div className="command-history__header">
        <h3 className="command-history__title">
          Recent Commands {entries.length > 0 && `(${entries.length})`}
        </h3>
        {entries.length > 0 && (
          <button type="button" className="ghost-button" onClick={handleClear}>
            Clear
          </button>
        )}
      </div>

      {entries.length === 0 ? (
        <div className="command-history__empty">
          <p>No commands yet.</p>
          <p className="command-history__hint">Type a command above to get started.</p>
        </div>
      ) : (
        <div className="command-history__list">
          {entries.map((entry) => (
            <CommandHistoryEntry
              key={entry.id}
              entry={entry}
              expanded={expandedId === entry.id}
              onToggle={() => handleToggle(entry.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
```

---

## Step 6: Update index.ts exports

**File:** `clients/web/src/components/commands/index.ts`

Add exports:

```typescript
export { CommandHistory } from './CommandHistory';
export { CommandHistoryEntry } from './CommandHistoryEntry';
```

---

## Step 7: Update HouseholdLayout.tsx

**File:** `clients/web/src/routes/HouseholdLayout.tsx`

Update to:

```typescript
import { Outlet } from 'react-router-dom';
import { CommandInput, CommandHistory } from '../components/commands';
import Layout from '../components/Layout/Layout';

export default function HouseholdLayout() {
  return (
    <Layout>
      <CommandInput />
      <CommandHistory />
      <Outlet />
    </Layout>
  );
}
```

---

## Step 8: Add CSS styles

**File:** `clients/web/src/styles/index.css`

Add at the end:

```css
/* ========================================
   ST-503: Command History
   ======================================== */

.command-history {
  margin-top: 1.5rem;
}

.command-history__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.command-history__title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

.command-history__empty {
  padding: 1.5rem;
  text-align: center;
  color: var(--color-text-secondary, #6b7280);
  background: var(--color-bg-secondary, #f9fafb);
  border-radius: 8px;
}

.command-history__empty p {
  margin: 0;
}

.command-history__hint {
  font-size: 0.875rem;
  margin-top: 0.5rem !important;
}

.command-history__list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* History Entry */
.command-history-entry {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}

.command-history-entry__summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0.75rem 1rem;
  background: var(--color-bg-primary, #fff);
  border: none;
  cursor: pointer;
  text-align: left;
  gap: 1rem;
}

.command-history-entry__summary:hover {
  background: var(--color-bg-secondary, #f9fafb);
}

.command-history-entry__title {
  flex: 1;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.command-history-entry__meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.command-history-entry__timestamp {
  font-size: 0.75rem;
  color: var(--color-text-secondary, #6b7280);
}

.command-history-entry__toggle {
  font-size: 1rem;
  font-weight: bold;
  color: var(--color-text-secondary, #6b7280);
  width: 1.5rem;
  text-align: center;
}

.command-history-entry__details {
  padding: 1rem;
  background: var(--color-bg-secondary, #f9fafb);
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.command-history-entry__row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.command-history-entry__label {
  font-weight: 500;
  color: var(--color-text-secondary, #6b7280);
}

.command-history-entry__section {
  margin-top: 1rem;
}

.command-history-entry__section .command-history-entry__label {
  display: block;
  margin-bottom: 0.5rem;
}

.command-history-entry__json {
  background: var(--color-bg-primary, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  padding: 0.75rem;
  font-size: 0.75rem;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
}

.command-history-entry__copy {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  background: var(--color-bg-primary, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  cursor: pointer;
}

.command-history-entry__copy:hover {
  background: var(--color-bg-secondary, #f9fafb);
}
```

---

## Verification

```bash
cd clients/web
npm run lint
npm run build
```

---

## Manual Testing

1. Submit command → check localStorage has entry
2. Refresh page → history persists
3. Click entry → details expand
4. Click again → details collapse
5. Copy correlationId → verify clipboard
6. Clear → confirm → empty state
7. Submit 51+ commands → verify max 50

---

## STOP Conditions

STOP if:
- Types don't compile
- useSyncExternalStore causes issues
- Build fails

Otherwise continue to completion.
