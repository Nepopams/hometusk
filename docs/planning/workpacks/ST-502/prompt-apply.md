# Codex APPLY Prompt: ST-502 — Command Status Display

## Mode
**APPLY** — Implementation mode. Write code, create files, modify existing files.

---

## Task
Implement ST-502 (Command Status Display) — UI components to display detailed command execution results for all 4 status types with distinct styling, action buttons, and trace info.

---

## Sources of Truth

```
docs/planning/epics/EP-006/stories/ST-502-command-status-display.md
docs/contracts/http/commands.openapi.yaml
docs/planning/workpacks/ST-502/workpack.md
clients/web/src/types/api.ts (CommandResponse types from ST-501)
clients/web/src/components/commands/CommandInput.tsx (integration point)
```

---

## Critical Constraints

### Response Types (already in types/api.ts)
```typescript
type CommandResponse =
  | CommandExecutedResponse      // status: 'executed'
  | CommandNeedsInputResponse    // status: 'needs_input'
  | CommandRejectedResponse      // status: 'rejected'
  | CommandDegradedResponse;     // status: 'executed_degraded'
```

### Scope Boundaries
- "View Task" button: Show taskId, button shows alert with taskId (no navigation for MVP)
- Result summary: Show IDs only (assigneeId, taskId), not names (lookup out of scope)
- Copy correlationId: Use navigator.clipboard.writeText

---

## Implementation Steps

### Step 1: Create `clients/web/src/components/commands/StatusBadge.tsx`

```typescript
import type { ReactNode } from 'react';

type StatusVariant = 'success' | 'warning' | 'error' | 'info';

interface StatusBadgeProps {
  variant: StatusVariant;
  title: string;
  icon?: ReactNode;
}

const VARIANT_STYLES: Record<StatusVariant, { bg: string; text: string; icon: string }> = {
  success: { bg: '#dcfce7', text: '#166534', icon: '✓' },
  warning: { bg: '#fef3c7', text: '#b45309', icon: '!' },
  error: { bg: '#fecaca', text: '#b91c1c', icon: '✗' },
  info: { bg: '#e0f2fe', text: '#0369a1', icon: '?' },
};

export function StatusBadge({ variant, title, icon }: StatusBadgeProps) {
  const styles = VARIANT_STYLES[variant];

  return (
    <div
      className="status-badge"
      style={{ backgroundColor: styles.bg, color: styles.text }}
    >
      <span className="status-badge__icon">{icon ?? styles.icon}</span>
      <span className="status-badge__title">{title}</span>
    </div>
  );
}
```

---

### Step 2: Create `clients/web/src/components/commands/TraceInfo.tsx`

```typescript
import { useState } from 'react';

interface TraceInfoProps {
  commandId: string;
  correlationId: string;
  executionMs: number;
}

export function TraceInfo({ commandId, correlationId, executionMs }: TraceInfoProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(correlationId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback: show alert
      alert(`Correlation ID: ${correlationId}`);
    }
  };

  const formatMs = (ms: number): string => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  return (
    <div className="trace-info">
      <span className="trace-info__item">
        Trace: <code>{correlationId.slice(0, 8)}...</code>
        <button
          type="button"
          className="trace-info__copy"
          onClick={handleCopy}
          title="Copy correlation ID"
        >
          {copied ? 'Copied!' : 'Copy'}
        </button>
      </span>
      <span className="trace-info__item trace-info__time">
        {formatMs(executionMs)}
      </span>
    </div>
  );
}
```

---

### Step 3: Create `clients/web/src/components/commands/ExecutedResult.tsx`

```typescript
import type { CommandExecutedResponse } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface ExecutedResultProps {
  data: CommandExecutedResponse;
  onNewCommand: () => void;
}

export function ExecutedResult({ data, onNewCommand }: ExecutedResultProps) {
  const { result, commandId, correlationId, executionMs } = data;

  const formatConfidence = (confidence?: number): string => {
    if (confidence === undefined) return 'N/A';
    return `${Math.round(confidence * 100)}%`;
  };

  const handleViewTask = () => {
    if (result.taskId) {
      alert(`Task ID: ${result.taskId}`);
    }
  };

  return (
    <div className="command-result command-result--success">
      <StatusBadge variant="success" title="Command executed successfully" />

      <div className="command-result__body">
        {result.taskId && (
          <div className="command-summary__row">
            <span className="command-summary__label">Task ID:</span>
            <code>{result.taskId}</code>
          </div>
        )}
        {result.assigneeId && (
          <div className="command-summary__row">
            <span className="command-summary__label">Assignee ID:</span>
            <code>{result.assigneeId}</code>
          </div>
        )}
        <div className="command-summary__row">
          <span className="command-summary__label">Confidence:</span>
          <span>{formatConfidence(result.decisionConfidence)}</span>
        </div>
      </div>

      <div className="command-result__actions">
        {result.taskId && (
          <button type="button" className="ghost-button" onClick={handleViewTask}>
            View Task
          </button>
        )}
        <button type="button" className="button" onClick={onNewCommand}>
          New Command
        </button>
      </div>

      <TraceInfo
        commandId={commandId}
        correlationId={correlationId}
        executionMs={executionMs}
      />
    </div>
  );
}
```

---

### Step 4: Create `clients/web/src/components/commands/NeedsInputResult.tsx`

```typescript
import type { CommandNeedsInputResponse } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface NeedsInputResultProps {
  data: CommandNeedsInputResponse;
  onRetry: () => void;
}

export function NeedsInputResult({ data, onRetry }: NeedsInputResultProps) {
  const { question, requiredFields, suggestions, policyName, commandId, correlationId, executionMs } = data;

  const renderSuggestions = () => {
    if (!suggestions || Object.keys(suggestions).length === 0) return null;

    return (
      <div className="command-result__suggestions">
        <span className="command-summary__label">Suggestions:</span>
        <ul>
          {Object.entries(suggestions).map(([field, values]) => (
            <li key={field}>
              <strong>{field}:</strong> {Array.isArray(values) ? values.join(', ') : String(values)}
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div className="command-result command-result--info">
      <StatusBadge variant="info" title="More information needed" />

      <div className="command-result__body">
        <p className="command-result__question">"{question}"</p>

        <div className="command-summary__row">
          <span className="command-summary__label">Required fields:</span>
          <span>{requiredFields.join(', ')}</span>
        </div>

        {renderSuggestions()}

        {policyName && (
          <div className="command-summary__row command-summary__row--muted">
            <span className="command-summary__label">Policy:</span>
            <code>{policyName}</code>
          </div>
        )}
      </div>

      <div className="command-result__actions">
        <button type="button" className="button" onClick={onRetry}>
          Edit & Retry
        </button>
      </div>

      <TraceInfo
        commandId={commandId}
        correlationId={correlationId}
        executionMs={executionMs}
      />
    </div>
  );
}
```

---

### Step 5: Create `clients/web/src/components/commands/RejectedResult.tsx`

```typescript
import type { CommandRejectedResponse } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface RejectedResultProps {
  data: CommandRejectedResponse;
  onRetry: () => void;
  onNewCommand: () => void;
}

export function RejectedResult({ data, onRetry, onNewCommand }: RejectedResultProps) {
  const { errorCode, reason, commandId, correlationId, executionMs } = data;

  return (
    <div className="command-result command-result--error">
      <StatusBadge variant="error" title="Command rejected" />

      <div className="command-result__body">
        <div className="command-summary__row">
          <span className="command-summary__label">Error:</span>
          <code>{errorCode}</code>
        </div>
        <p className="command-result__reason">{reason}</p>
      </div>

      <div className="command-result__actions">
        <button type="button" className="ghost-button" onClick={onRetry}>
          Retry
        </button>
        <button type="button" className="button" onClick={onNewCommand}>
          New Command
        </button>
      </div>

      <TraceInfo
        commandId={commandId}
        correlationId={correlationId}
        executionMs={executionMs}
      />
    </div>
  );
}
```

---

### Step 6: Create `clients/web/src/components/commands/DegradedResult.tsx`

```typescript
import type { CommandDegradedResponse, DegradedReason } from '../../types/api';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface DegradedResultProps {
  data: CommandDegradedResponse;
  onNewCommand: () => void;
}

const DEGRADED_REASON_LABELS: Record<DegradedReason, string> = {
  ai_unavailable: 'AI service temporarily unavailable',
  ai_timeout: 'AI service timed out',
  ai_low_confidence: 'Low confidence result',
};

export function DegradedResult({ data, onNewCommand }: DegradedResultProps) {
  const { result, degradedReason, fallbackStrategy, commandId, correlationId, executionMs } = data;

  const formatConfidence = (confidence?: number): string => {
    if (confidence === undefined) return 'N/A';
    return `${Math.round(confidence * 100)}%`;
  };

  const handleViewTask = () => {
    if (result.taskId) {
      alert(`Task ID: ${result.taskId}`);
    }
  };

  return (
    <div className="command-result command-result--warning">
      <StatusBadge variant="warning" title="Command completed with limitations" />

      <div className="command-result__body">
        <p className="command-result__degraded-reason">
          {DEGRADED_REASON_LABELS[degradedReason]}
          {fallbackStrategy && `. ${fallbackStrategy}`}
        </p>

        {result.taskId && (
          <div className="command-summary__row">
            <span className="command-summary__label">Task ID:</span>
            <code>{result.taskId}</code>
          </div>
        )}
        {result.assigneeId && (
          <div className="command-summary__row">
            <span className="command-summary__label">Assignee ID:</span>
            <code>{result.assigneeId}</code>
          </div>
        )}
        <div className="command-summary__row">
          <span className="command-summary__label">Confidence:</span>
          <span>{formatConfidence(result.decisionConfidence)}</span>
        </div>
      </div>

      <div className="command-result__actions">
        {result.taskId && (
          <button type="button" className="ghost-button" onClick={handleViewTask}>
            View Task
          </button>
        )}
        <button type="button" className="button" onClick={onNewCommand}>
          New Command
        </button>
      </div>

      <TraceInfo
        commandId={commandId}
        correlationId={correlationId}
        executionMs={executionMs}
      />
    </div>
  );
}
```

---

### Step 7: Create `clients/web/src/components/commands/CommandResult.tsx`

```typescript
import type { CommandResponse } from '../../types/api';
import { ExecutedResult } from './ExecutedResult';
import { NeedsInputResult } from './NeedsInputResult';
import { RejectedResult } from './RejectedResult';
import { DegradedResult } from './DegradedResult';

interface CommandResultProps {
  response: CommandResponse;
  onNewCommand: () => void;
  onRetry: () => void;
}

export function CommandResult({ response, onNewCommand, onRetry }: CommandResultProps) {
  switch (response.status) {
    case 'executed':
      return <ExecutedResult data={response} onNewCommand={onNewCommand} />;
    case 'needs_input':
      return <NeedsInputResult data={response} onRetry={onRetry} />;
    case 'rejected':
      return <RejectedResult data={response} onRetry={onRetry} onNewCommand={onNewCommand} />;
    case 'executed_degraded':
      return <DegradedResult data={response} onNewCommand={onNewCommand} />;
  }
}
```

---

### Step 8: Update `clients/web/src/components/commands/CommandInput.tsx`

Replace the inline response message rendering with CommandResult component.

**Changes to make:**

1. Import CommandResult:
```typescript
import { CommandResult } from './CommandResult';
```

2. Remove `renderResponseMessage` function entirely.

3. Replace `{responseMessage && <p>{responseMessage}</p>}` with:
```typescript
{response && (
  <CommandResult
    response={response}
    onNewCommand={handleNewCommand}
    onRetry={handleRetry}
  />
)}
```

4. Add new handlers:
```typescript
const handleNewCommand = () => {
  reset();
  setFormKey((prev) => prev + 1);
  setMode('create_task');
};

const handleRetry = () => {
  // Keep form values, just clear response
  reset();
};
```

5. Optionally hide the form when response is shown (cleaner UX):
```typescript
{!response && (
  mode === 'create_task' ? (
    <CreateTaskForm ... />
  ) : (
    <CompleteTaskForm ... />
  )
)}
```

Or keep form visible below result for context.

---

### Step 9: Update `clients/web/src/components/commands/index.ts`

Add exports for new components:

```typescript
export { CommandInput } from './CommandInput';
export { CreateTaskForm } from './CreateTaskForm';
export { CompleteTaskForm } from './CompleteTaskForm';
export { CommandResult } from './CommandResult';
export { StatusBadge } from './StatusBadge';
export { TraceInfo } from './TraceInfo';
export { ExecutedResult } from './ExecutedResult';
export { NeedsInputResult } from './NeedsInputResult';
export { RejectedResult } from './RejectedResult';
export { DegradedResult } from './DegradedResult';
```

---

### Step 10: Add CSS styles to `clients/web/src/App.css`

Add at the end of the file:

```css
/* Command Result Styles */
.command-result {
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.command-result--success {
  background-color: #dcfce7;
  border: 1px solid #86efac;
}

.command-result--warning {
  background-color: #fef3c7;
  border: 1px solid #fcd34d;
}

.command-result--error {
  background-color: #fecaca;
  border: 1px solid #fca5a5;
}

.command-result--info {
  background-color: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.command-result__body {
  margin: 0.75rem 0;
}

.command-result__question {
  font-style: italic;
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
}

.command-result__reason {
  margin-top: 0.5rem;
  color: #7f1d1d;
}

.command-result__degraded-reason {
  margin-bottom: 0.75rem;
  color: #92400e;
}

.command-result__actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.command-result__suggestions ul {
  margin: 0.25rem 0 0 1rem;
  padding: 0;
}

/* Status Badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  font-weight: 600;
}

.status-badge__icon {
  font-size: 1.1rem;
}

/* Command Summary */
.command-summary__row {
  display: flex;
  gap: 0.5rem;
  margin: 0.25rem 0;
}

.command-summary__row--muted {
  opacity: 0.7;
  font-size: 0.9rem;
}

.command-summary__label {
  font-weight: 500;
  min-width: 100px;
}

/* Trace Info */
.trace-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  font-size: 0.85rem;
  color: #6b7280;
}

.trace-info__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.trace-info__copy {
  padding: 0.125rem 0.5rem;
  font-size: 0.75rem;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  cursor: pointer;
}

.trace-info__copy:hover {
  background: rgba(0, 0, 0, 0.1);
}
```

---

## Verification

After implementation, run:

```bash
cd clients/web

# Lint
npm run lint

# Build
npm run build
```

### Manual Testing

1. Submit successful command → verify ExecutedResult UI (green, task info, confidence)
2. Mock/trigger needs_input → verify NeedsInputResult UI (blue, question, fields)
3. Submit invalid command → verify RejectedResult UI (red, errorCode, reason)
4. Mock degraded response → verify DegradedResult UI (amber, reason label)
5. Click "Copy" on correlationId → verify clipboard
6. Click "New Command" → verify form resets to create_task
7. Click "Retry" → verify form retains values

---

## Files Summary

| File | Action |
|------|--------|
| `components/commands/StatusBadge.tsx` | CREATE |
| `components/commands/TraceInfo.tsx` | CREATE |
| `components/commands/ExecutedResult.tsx` | CREATE |
| `components/commands/NeedsInputResult.tsx` | CREATE |
| `components/commands/RejectedResult.tsx` | CREATE |
| `components/commands/DegradedResult.tsx` | CREATE |
| `components/commands/CommandResult.tsx` | CREATE |
| `components/commands/CommandInput.tsx` | MODIFY |
| `components/commands/index.ts` | MODIFY |
| `App.css` | MODIFY (add styles) |

---

## Anti-Scope-Creep

DO NOT:
- Add navigation to task detail page
- Lookup assignee/task names by ID
- Implement full needs_input form (ST-504)
- Implement command history (ST-503)
- Implement full trace viewer (ST-505)
