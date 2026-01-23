# Codex APPLY Prompt: ST-505 — Minimal Trace Viewer

## Mode
**APPLY** — Implement the approved plan. Create/modify files as specified.

---

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-505-trace-viewer.md`
- Workpack: `docs/planning/workpacks/ST-505/workpack.md`
- Approved Plan: ST-505 PLAN output

---

## Task
Enhance TraceInfo with expandable details view, create RawJsonViewer, and extract CopyButton as reusable component.

---

## Files to Change

| File | Action |
|------|--------|
| `clients/web/src/components/ui/CopyButton.tsx` | CREATE |
| `clients/web/src/components/commands/RawJsonViewer.tsx` | CREATE |
| `clients/web/src/components/commands/TraceInfo.tsx` | MODIFY |
| `clients/web/src/components/commands/ExecutedResult.tsx` | MODIFY |
| `clients/web/src/components/commands/DegradedResult.tsx` | MODIFY |
| `clients/web/src/components/commands/NeedsInputResult.tsx` | MODIFY |
| `clients/web/src/components/commands/RejectedResult.tsx` | MODIFY |
| `clients/web/src/components/commands/CommandHistoryEntry.tsx` | MODIFY |
| `clients/web/src/components/commands/index.ts` | MODIFY |
| `clients/web/src/styles/index.css` | MODIFY |

---

## Step 1: Create CopyButton.tsx

**File:** `clients/web/src/components/ui/CopyButton.tsx`

```typescript
import { useState } from 'react';

interface CopyButtonProps {
  text: string;
  label?: string;
  successLabel?: string;
  className?: string;
}

export function CopyButton({
  text,
  label = 'Copy',
  successLabel = 'Copied',
  className = '',
}: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    if (copied) return;

    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Silent fail
    }
  };

  return (
    <button
      type="button"
      className={`copy-button ${className}`.trim()}
      onClick={handleCopy}
    >
      {copied ? successLabel : label}
    </button>
  );
}
```

---

## Step 2: Create RawJsonViewer.tsx

**File:** `clients/web/src/components/commands/RawJsonViewer.tsx`

```typescript
import { useState } from 'react';
import { CopyButton } from '../ui/CopyButton';

interface RawJsonViewerProps {
  data: unknown;
  label?: string;
}

export function RawJsonViewer({ data, label = 'Raw Response' }: RawJsonViewerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const jsonString = JSON.stringify(data, null, 2);

  return (
    <div className="raw-json-viewer">
      <div className="raw-json-viewer__header">
        <button
          type="button"
          className="raw-json-viewer__toggle"
          onClick={() => setIsOpen(!isOpen)}
        >
          {isOpen ? 'Hide' : 'Show'} {label}
        </button>
        {isOpen && <CopyButton text={jsonString} label="Copy JSON" />}
      </div>
      {isOpen && (
        <pre className="raw-json-viewer__code">{jsonString}</pre>
      )}
    </div>
  );
}
```

---

## Step 3: Modify TraceInfo.tsx

**File:** `clients/web/src/components/commands/TraceInfo.tsx`

Complete rewrite with expandable view:

```typescript
import { useState } from 'react';
import type { CommandResponse, DegradedReason } from '../../types/api';
import { CopyButton } from '../ui/CopyButton';
import { RawJsonViewer } from './RawJsonViewer';

interface TraceInfoProps {
  response: CommandResponse;
}

const DEGRADED_REASON_LABELS: Record<DegradedReason, string> = {
  ai_unavailable: 'AI service temporarily unavailable',
  ai_timeout: 'AI service timed out',
  ai_low_confidence: 'Low confidence result',
};

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

function formatConfidence(confidence?: number): string {
  if (confidence === undefined) return 'N/A';
  return `${Math.round(confidence * 100)}%`;
}

export function TraceInfo({ response }: TraceInfoProps) {
  const [expanded, setExpanded] = useState(false);
  const { commandId, correlationId, executionMs, status, initiatorId } = response;

  const hasResult = status === 'executed' || status === 'executed_degraded';
  const result = hasResult ? (response as { result?: { taskId?: string; assigneeId?: string; decisionConfidence?: number } }).result : undefined;

  const isDegraded = status === 'executed_degraded';
  const degradedReason = isDegraded ? (response as { degradedReason: DegradedReason }).degradedReason : undefined;
  const fallbackStrategy = isDegraded ? (response as { fallbackStrategy?: string }).fallbackStrategy : undefined;

  return (
    <div className={`trace-info ${expanded ? 'trace-info--expanded' : ''}`}>
      {/* Collapsed row */}
      <div className="trace-info__summary">
        <div className="trace-info__item">
          <span className="trace-info__label">Trace:</span>
          <code className="trace-info__value trace-info__value--truncate">{correlationId}</code>
          <CopyButton text={correlationId} className="trace-info__copy" />
        </div>
        <div className="trace-info__item">
          <span className="trace-info__value">{formatDuration(executionMs)}</span>
        </div>
        <button
          type="button"
          className="trace-info__toggle"
          onClick={() => setExpanded(!expanded)}
          aria-expanded={expanded}
        >
          {expanded ? 'Hide Details' : 'View Details'}
        </button>
      </div>

      {/* Expanded details */}
      {expanded && (
        <div className="trace-info__details">
          <div className="trace-info__row">
            <span className="trace-info__label">Command ID:</span>
            <code className="trace-info__value">{commandId}</code>
            <CopyButton text={commandId} className="trace-info__copy" />
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Correlation ID:</span>
            <code className="trace-info__value">{correlationId}</code>
            <CopyButton text={correlationId} className="trace-info__copy" />
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Initiator:</span>
            <span className="trace-info__value">{initiatorId}</span>
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Status:</span>
            <span className="trace-info__value">{status}</span>
          </div>
          <div className="trace-info__row">
            <span className="trace-info__label">Execution Time:</span>
            <span className="trace-info__value">{formatDuration(executionMs)}</span>
          </div>

          {/* Result info */}
          {hasResult && result && (
            <div className="trace-info__section">
              <span className="trace-info__section-title">Result:</span>
              {result.taskId && (
                <div className="trace-info__row">
                  <span className="trace-info__label">Task ID:</span>
                  <code className="trace-info__value">{result.taskId}</code>
                </div>
              )}
              {result.assigneeId && (
                <div className="trace-info__row">
                  <span className="trace-info__label">Assignee:</span>
                  <span className="trace-info__value">{result.assigneeId}</span>
                </div>
              )}
              <div className="trace-info__row">
                <span className="trace-info__label">Confidence:</span>
                <span className="trace-info__value">{formatConfidence(result.decisionConfidence)}</span>
              </div>
            </div>
          )}

          {/* Degraded info */}
          {isDegraded && degradedReason && (
            <div className="trace-info__section">
              <span className="trace-info__section-title">Degraded:</span>
              <div className="trace-info__row">
                <span className="trace-info__label">Reason:</span>
                <span className="trace-info__value">{DEGRADED_REASON_LABELS[degradedReason]}</span>
              </div>
              {fallbackStrategy && (
                <div className="trace-info__row">
                  <span className="trace-info__label">Fallback:</span>
                  <span className="trace-info__value">{fallbackStrategy}</span>
                </div>
              )}
            </div>
          )}

          {/* Raw JSON */}
          <RawJsonViewer data={response} />
        </div>
      )}
    </div>
  );
}
```

---

## Step 4: Update ExecutedResult.tsx

**File:** `clients/web/src/components/commands/ExecutedResult.tsx`

Change TraceInfo usage:

```typescript
// Before:
<TraceInfo commandId={commandId} correlationId={correlationId} executionMs={executionMs} />

// After:
<TraceInfo response={data} />
```

Remove destructuring of commandId, correlationId, executionMs if only used for TraceInfo.

---

## Step 5: Update DegradedResult.tsx

Same pattern as ExecutedResult:

```typescript
<TraceInfo response={data} />
```

---

## Step 6: Update NeedsInputResult.tsx

Same pattern:

```typescript
<TraceInfo response={data} />
```

---

## Step 7: Update RejectedResult.tsx

Same pattern:

```typescript
<TraceInfo response={data} />
```

---

## Step 8: Update CommandHistoryEntry.tsx

Add "View Trace" toggle in expanded details:

```typescript
// Add state
const [showTrace, setShowTrace] = useState(false);

// In expanded area, add:
<div className="command-history-entry__trace-section">
  <button
    type="button"
    className="ghost-button"
    onClick={() => setShowTrace(!showTrace)}
  >
    {showTrace ? 'Hide Trace' : 'View Trace'}
  </button>
  {showTrace && <TraceInfo response={entry.response} />}
</div>
```

Import TraceInfo at top.

---

## Step 9: Update index.ts exports

**File:** `clients/web/src/components/commands/index.ts`

Add:

```typescript
export { RawJsonViewer } from './RawJsonViewer';
```

---

## Step 10: Add CSS styles

**File:** `clients/web/src/styles/index.css`

Add at the end:

```css
/* ========================================
   ST-505: Trace Viewer & Copy Button
   ======================================== */

/* Copy Button */
.copy-button {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  background: var(--color-bg-primary, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}

.copy-button:hover {
  background: var(--color-bg-secondary, #f9fafb);
}

/* Trace Info Enhanced */
.trace-info {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  font-size: 0.875rem;
}

.trace-info__summary {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.trace-info__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.trace-info__label {
  color: var(--color-text-secondary, #6b7280);
  font-size: 0.75rem;
}

.trace-info__value {
  font-family: monospace;
  font-size: 0.75rem;
}

.trace-info__value--truncate {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-info__copy {
  font-size: 0.625rem;
  padding: 0.125rem 0.375rem;
}

.trace-info__toggle {
  margin-left: auto;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  background: transparent;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-secondary, #6b7280);
}

.trace-info__toggle:hover {
  background: var(--color-bg-secondary, #f9fafb);
}

/* Expanded details */
.trace-info--expanded {
  background: var(--color-bg-secondary, #f9fafb);
  padding: 1rem;
  border-radius: 8px;
  margin-top: 1rem;
  border-top: none;
}

.trace-info__details {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
}

.trace-info__row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.trace-info__section {
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--color-border, #e5e7eb);
}

.trace-info__section-title {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--color-text-secondary, #6b7280);
}

/* Raw JSON Viewer */
.raw-json-viewer {
  margin-top: 1rem;
}

.raw-json-viewer__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.raw-json-viewer__toggle {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  background: transparent;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-secondary, #6b7280);
}

.raw-json-viewer__toggle:hover {
  background: var(--color-bg-primary, #fff);
}

.raw-json-viewer__code {
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: var(--color-bg-primary, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 4px;
  font-size: 0.75rem;
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
}

/* History entry trace section */
.command-history-entry__trace-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed var(--color-border, #e5e7eb);
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

1. Submit command → verify trace row shows correlationId + time
2. Click "View Details" → verify expanded info
3. Copy correlationId → verify clipboard
4. Show raw JSON → verify formatting
5. Copy JSON → verify clipboard
6. From history → click "View Trace" → verify same display

---

## STOP Conditions

STOP if:
- Type errors with CommandResponse access
- Build fails
- TraceInfo props incompatible

Otherwise continue to completion.
