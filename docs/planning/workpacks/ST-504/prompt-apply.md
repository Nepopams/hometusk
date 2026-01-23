# Codex APPLY Prompt: ST-504 — needs_input Basic Display

## Mode
**APPLY** — Implement the approved plan. Create/modify files as specified.

---

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-504-needs-input-display.md`
- Workpack: `docs/planning/workpacks/ST-504/workpack.md`
- Approved Plan: ST-504 PLAN output

---

## Task
Enhance NeedsInputResult with human-readable field labels, per-field suggestions, policy explanations, original input reference, guidance message, and focus helper.

---

## Files to Change

| File | Action |
|------|--------|
| `clients/web/src/lib/fieldLabels.ts` | CREATE |
| `clients/web/src/components/commands/RequiredFieldsList.tsx` | CREATE |
| `clients/web/src/components/commands/FieldSuggestions.tsx` | CREATE |
| `clients/web/src/components/commands/NeedsInputResult.tsx` | MODIFY |
| `clients/web/src/components/commands/CommandResult.tsx` | MODIFY |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY |
| `clients/web/src/components/commands/index.ts` | MODIFY |
| `clients/web/src/styles/index.css` | MODIFY |

---

## Step 1: Create fieldLabels.ts

**File:** `clients/web/src/lib/fieldLabels.ts`

```typescript
export const FIELD_LABELS: Record<string, string> = {
  zoneId: 'Zone',
  deadline: 'Deadline',
  assigneeId: 'Assignee',
  title: 'Task title',
  description: 'Description',
  taskId: 'Task',
};

export function getFieldLabel(field: string): string {
  return FIELD_LABELS[field] || field;
}

export const POLICY_LABELS: Record<string, string> = {
  ZONE_REQUIRED: 'Zone is required for task creation',
  DEADLINE_REQUIRED: 'Deadline must be specified',
  ASSIGNEE_AMBIGUOUS: 'Multiple possible assignees found',
  TITLE_REQUIRED: 'Task title is required',
  TASK_NOT_FOUND: 'The specified task was not found',
};

export function getPolicyLabel(policyName: string): string {
  return POLICY_LABELS[policyName] || `Policy: ${policyName}`;
}
```

---

## Step 2: Create FieldSuggestions.tsx

**File:** `clients/web/src/components/commands/FieldSuggestions.tsx`

```typescript
interface FieldSuggestionsProps {
  field: string;
  values: unknown;
}

export function FieldSuggestions({ values }: FieldSuggestionsProps) {
  if (!values) return null;

  const displayValue = Array.isArray(values)
    ? values.join(', ')
    : String(values);

  if (!displayValue) return null;

  return (
    <div className="needs-input-suggestions">
      <span className="needs-input-suggestions__label">Suggestions:</span>
      <span className="needs-input-suggestions__values">{displayValue}</span>
    </div>
  );
}
```

---

## Step 3: Create RequiredFieldsList.tsx

**File:** `clients/web/src/components/commands/RequiredFieldsList.tsx`

```typescript
import { getFieldLabel } from '../../lib/fieldLabels';
import { FieldSuggestions } from './FieldSuggestions';

interface RequiredFieldsListProps {
  requiredFields: string[];
  suggestions?: Record<string, unknown>;
}

export function RequiredFieldsList({ requiredFields, suggestions }: RequiredFieldsListProps) {
  if (requiredFields.length === 0) return null;

  return (
    <div className="needs-input-fields">
      <span className="needs-input-fields__title">Missing information:</span>
      <ul className="needs-input-fields__list">
        {requiredFields.map((field) => (
          <li key={field} className="needs-input-field">
            <span className="needs-input-field__label">
              {getFieldLabel(field)} <span className="needs-input-field__required">(required)</span>
            </span>
            {suggestions && suggestions[field] && (
              <FieldSuggestions field={field} values={suggestions[field]} />
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

---

## Step 4: Modify CommandInput.tsx

**File:** `clients/web/src/components/commands/CommandInput.tsx`

Add `lastRequest` state and pass to CommandResult:

```typescript
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useCommand } from '../../hooks/useCommand';
import { CommandResult } from './CommandResult';
import { CreateTaskForm } from './CreateTaskForm';
import { CompleteTaskForm } from './CompleteTaskForm';
import type { CommandRequest, CommandType, CreateTaskPayload, CompleteTaskPayload } from '../../types/api';

export function CommandInput() {
  const { householdId } = useAuth();
  const { execute, isLoading, response, error, errorStatus, reset } = useCommand();
  const [mode, setMode] = useState<CommandType>('create_task');
  const [formKey, setFormKey] = useState(0);
  const [lastRequest, setLastRequest] = useState<CommandRequest | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (errorStatus === 409) {
      setFormKey((prev) => prev + 1);
    }
  }, [errorStatus]);

  useEffect(() => {
    if (!response) return;
    if (response.status === 'executed' || response.status === 'executed_degraded') {
      setFormKey((prev) => prev + 1);
      setLastRequest(null);
    }
  }, [response]);

  if (!householdId) {
    return null;
  }

  const handleModeChange = (nextMode: CommandType) => {
    if (nextMode === mode) return;
    setMode(nextMode);
    reset();
    setLastRequest(null);
    setFormKey((prev) => prev + 1);
  };

  const handleCancel = () => {
    reset();
    setLastRequest(null);
    setFormKey((prev) => prev + 1);
  };

  const handleNewCommand = () => {
    reset();
    setLastRequest(null);
    setMode('create_task');
    setFormKey((prev) => prev + 1);
  };

  const handleRetry = () => {
    reset();
    // Focus first input after reset
    requestAnimationFrame(() => {
      const input = containerRef.current?.querySelector<HTMLElement>(
        'input, select, textarea'
      );
      input?.focus();
    });
  };

  const handleCreateTask = async (payload: CreateTaskPayload) => {
    const request: CommandRequest = {
      householdId,
      type: 'create_task',
      payload,
      source: 'web',
    };
    setLastRequest(request);
    await execute(request);
  };

  const handleCompleteTask = async (payload: CompleteTaskPayload) => {
    const request: CommandRequest = {
      householdId,
      type: 'complete_task',
      payload,
      source: 'web',
    };
    setLastRequest(request);
    await execute(request);
  };

  return (
    <div className="card command-input" ref={containerRef}>
      <div className="create-household__actions">
        <button
          type="button"
          className={mode === 'create_task' ? 'button' : 'ghost-button'}
          onClick={() => handleModeChange('create_task')}
          disabled={isLoading}
        >
          Create Task
        </button>
        <button
          type="button"
          className={mode === 'complete_task' ? 'button' : 'ghost-button'}
          onClick={() => handleModeChange('complete_task')}
          disabled={isLoading}
        >
          Complete Task
        </button>
      </div>

      {error && (
        <div className="create-household__error" role="alert">
          {error}
        </div>
      )}

      {response && (
        <CommandResult
          response={response}
          request={lastRequest}
          onNewCommand={handleNewCommand}
          onRetry={handleRetry}
        />
      )}

      {mode === 'create_task' ? (
        <CreateTaskForm
          key={`create-${formKey}`}
          householdId={householdId}
          onSubmit={handleCreateTask}
          onCancel={handleCancel}
          isLoading={isLoading}
        />
      ) : (
        <CompleteTaskForm
          key={`complete-${formKey}`}
          householdId={householdId}
          onSubmit={handleCompleteTask}
          onCancel={handleCancel}
          isLoading={isLoading}
        />
      )}
    </div>
  );
}
```

---

## Step 5: Modify CommandResult.tsx

**File:** `clients/web/src/components/commands/CommandResult.tsx`

Add `request` prop and pass to NeedsInputResult:

```typescript
import type { CommandRequest, CommandResponse } from '../../types/api';
import { DegradedResult } from './DegradedResult';
import { ExecutedResult } from './ExecutedResult';
import { NeedsInputResult } from './NeedsInputResult';
import { RejectedResult } from './RejectedResult';

interface CommandResultProps {
  response: CommandResponse;
  request?: CommandRequest | null;
  onNewCommand: () => void;
  onRetry: () => void;
}

export function CommandResult({ response, request, onNewCommand, onRetry }: CommandResultProps) {
  switch (response.status) {
    case 'executed':
      return <ExecutedResult data={response} onNewCommand={onNewCommand} />;
    case 'needs_input':
      return <NeedsInputResult data={response} request={request} onRetry={onRetry} />;
    case 'rejected':
      return <RejectedResult data={response} onRetry={onRetry} onNewCommand={onNewCommand} />;
    case 'executed_degraded':
      return <DegradedResult data={response} onNewCommand={onNewCommand} />;
    default:
      return null;
  }
}
```

---

## Step 6: Modify NeedsInputResult.tsx

**File:** `clients/web/src/components/commands/NeedsInputResult.tsx`

Complete rewrite with enhanced display:

```typescript
import type { CommandNeedsInputResponse, CommandRequest } from '../../types/api';
import { getPolicyLabel } from '../../lib/fieldLabels';
import { RequiredFieldsList } from './RequiredFieldsList';
import { StatusBadge } from './StatusBadge';
import { TraceInfo } from './TraceInfo';

interface NeedsInputResultProps {
  data: CommandNeedsInputResponse;
  request?: CommandRequest | null;
  onRetry: () => void;
}

function getOriginalInputSummary(request: CommandRequest | null | undefined): string | null {
  if (!request) return null;

  if (request.type === 'create_task') {
    const payload = request.payload as { title?: string; description?: string };
    if (payload.title) {
      return `Create task: "${payload.title}"`;
    }
  }

  if (request.type === 'complete_task') {
    const payload = request.payload as { taskId?: string };
    if (payload.taskId) {
      return `Complete task: ${payload.taskId}`;
    }
  }

  return `Command: ${request.type}`;
}

export function NeedsInputResult({ data, request, onRetry }: NeedsInputResultProps) {
  const { question, requiredFields, suggestions, policyName, commandId, correlationId, executionMs } =
    data;

  const originalInput = getOriginalInputSummary(request);

  return (
    <div className="command-result command-result--info">
      <StatusBadge variant="info" title="More information needed" />

      <div className="command-result__body">
        {/* Question callout */}
        <div className="needs-input-callout">
          <span className="needs-input-callout__icon">?</span>
          <p className="needs-input-callout__question">{question}</p>
        </div>

        {/* Required fields with suggestions */}
        <RequiredFieldsList
          requiredFields={requiredFields}
          suggestions={suggestions}
        />

        {/* Original input reference */}
        {originalInput && (
          <div className="needs-input-original">
            <span className="needs-input-original__label">Your command:</span>
            <span className="needs-input-original__value">{originalInput}</span>
          </div>
        )}

        {/* Guidance tip */}
        <div className="needs-input-tip">
          <p className="needs-input-tip__text">
            Please retype your command with more details.
          </p>
          <p className="needs-input-tip__example">
            e.g., "Clean the kitchen tomorrow at 6pm"
          </p>
        </div>

        {/* Policy (technical details) */}
        {policyName && (
          <div className="needs-input-policy" title={getPolicyLabel(policyName)}>
            <span className="needs-input-policy__label">Policy:</span>
            <code className="needs-input-policy__value">{policyName}</code>
          </div>
        )}
      </div>

      <div className="command-result__actions">
        <button type="button" className="button" onClick={onRetry}>
          Edit & Retry
        </button>
      </div>

      <TraceInfo commandId={commandId} correlationId={correlationId} executionMs={executionMs} />
    </div>
  );
}
```

---

## Step 7: Update index.ts exports

**File:** `clients/web/src/components/commands/index.ts`

Add exports:

```typescript
export { RequiredFieldsList } from './RequiredFieldsList';
export { FieldSuggestions } from './FieldSuggestions';
```

---

## Step 8: Add CSS styles

**File:** `clients/web/src/styles/index.css`

Add at the end:

```css
/* ========================================
   ST-504: needs_input Enhanced Display
   ======================================== */

.command-input {
  position: relative;
}

/* Question callout */
.needs-input-callout {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--color-info-bg, #eff6ff);
  border: 1px solid var(--color-info-border, #bfdbfe);
  border-radius: 8px;
  margin-bottom: 1rem;
}

.needs-input-callout__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  background: var(--color-info, #3b82f6);
  color: white;
  border-radius: 50%;
  font-weight: bold;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.needs-input-callout__question {
  margin: 0;
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text-primary, #1f2937);
}

/* Required fields list */
.needs-input-fields {
  margin-bottom: 1rem;
}

.needs-input-fields__title {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--color-text-secondary, #6b7280);
}

.needs-input-fields__list {
  margin: 0;
  padding-left: 1.25rem;
  list-style: disc;
}

.needs-input-field {
  margin-bottom: 0.5rem;
}

.needs-input-field__label {
  font-weight: 500;
}

.needs-input-field__required {
  font-weight: normal;
  color: var(--color-text-secondary, #6b7280);
  font-size: 0.875rem;
}

/* Suggestions */
.needs-input-suggestions {
  margin-top: 0.25rem;
  margin-left: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary, #6b7280);
}

.needs-input-suggestions__label {
  font-style: italic;
}

.needs-input-suggestions__values {
  margin-left: 0.25rem;
}

/* Original input */
.needs-input-original {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--color-bg-secondary, #f9fafb);
  border-radius: 6px;
  margin-bottom: 1rem;
  font-size: 0.875rem;
}

.needs-input-original__label {
  color: var(--color-text-secondary, #6b7280);
}

.needs-input-original__value {
  font-weight: 500;
}

/* Guidance tip */
.needs-input-tip {
  padding: 0.75rem;
  background: var(--color-bg-secondary, #f9fafb);
  border-left: 3px solid var(--color-info, #3b82f6);
  border-radius: 0 6px 6px 0;
  margin-bottom: 1rem;
}

.needs-input-tip__text {
  margin: 0 0 0.25rem 0;
  font-size: 0.875rem;
  color: var(--color-text-primary, #1f2937);
}

.needs-input-tip__example {
  margin: 0;
  font-size: 0.875rem;
  font-style: italic;
  color: var(--color-text-secondary, #6b7280);
}

/* Policy (technical details) */
.needs-input-policy {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-secondary, #6b7280);
  margin-top: 0.5rem;
  cursor: help;
}

.needs-input-policy__label {
  font-weight: 500;
}

.needs-input-policy__value {
  font-family: monospace;
  background: var(--color-bg-secondary, #f9fafb);
  padding: 0.125rem 0.375rem;
  border-radius: 3px;
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

1. Trigger needs_input response → verify callout shows question
2. Verify required fields have human-readable labels
3. Verify suggestions grouped per field
4. Verify policy shown with tooltip (title attribute)
5. Verify original input summary displayed
6. Verify guidance tip with example
7. Click "Edit & Retry" → verify focus moves to first input
8. Verify form values retained after retry

---

## STOP Conditions

STOP if:
- Types don't compile
- Focus helper causes issues
- Build fails

Otherwise continue to completion.
