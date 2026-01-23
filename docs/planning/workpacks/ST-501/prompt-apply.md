# Codex APPLY Prompt: ST-501 — Command Input Box

## Mode
**APPLY** — Implementation mode. Write code, create files, modify existing files.

---

## Task
Implement ST-501 (Command Input Box) — UI component for submitting structured commands to `POST /api/v1/commands`.

---

## Sources of Truth (Reference during implementation)

```
docs/planning/epics/EP-006/stories/ST-501-command-input-box.md
docs/contracts/http/commands.openapi.yaml
docs/planning/workpacks/ST-501/workpack.md
```

---

## Critical Constraints (MUST follow exactly)

### OpenAPI Contract
```typescript
// CommandRequest - MUST match exactly
{
  householdId: string;           // from useAuth().householdId
  type: 'create_task' | 'complete_task';
  payload: CreateTaskPayload | CompleteTaskPayload;
  source: 'web';                 // hardcoded for this client
  clientTimestamp?: string;      // optional ISO 8601
}

// CreateTaskPayload
{
  title: string;        // required, 1-500 chars
  description?: string; // optional, max 2000
  zoneId?: string;      // optional UUID
  assigneeId?: string;  // optional UUID
  deadline?: string;    // optional ISO 8601, must be future
}

// CompleteTaskPayload
{
  taskId: string;       // required UUID
}
```

### Headers (REQUIRED)
```typescript
headers: {
  'Idempotency-Key': string,    // UUID, unique per submission
  'X-Correlation-ID': string,   // UUID v4
}
// Authorization header is already set by apiFetch
```

### Response Status Union
```typescript
type CommandResponse =
  | { status: 'executed'; ... }
  | { status: 'needs_input'; ... }
  | { status: 'rejected'; ... }
  | { status: 'executed_degraded'; ... }
```

---

## Implementation Steps

### Step 1: Add Command types to `clients/web/src/types/api.ts`

Add after existing types (around line 96):

```typescript
// ============================================
// Command API Types (per commands.openapi.yaml)
// ============================================

export type CommandType = 'create_task' | 'complete_task';
export type CommandSource = 'api' | 'web' | 'mobile';

export interface CreateTaskPayload {
  title: string;
  description?: string;
  zoneId?: string;
  assigneeId?: string;
  deadline?: string;
}

export interface CompleteTaskPayload {
  taskId: string;
}

export interface CommandRequest {
  householdId: string;
  type: CommandType;
  payload: CreateTaskPayload | CompleteTaskPayload;
  source: CommandSource;
  clientTimestamp?: string;
}

export interface CommandResult {
  taskId?: string;
  assigneeId?: string;
  decisionConfidence?: number;
}

export interface CommandExecutedResponse {
  commandId: string;
  correlationId: string;
  status: 'executed';
  result: CommandResult;
  executionMs: number;
  initiatorId: string;
}

export interface CommandNeedsInputResponse {
  commandId: string;
  correlationId: string;
  status: 'needs_input';
  question: string;
  requiredFields: string[];
  suggestions?: Record<string, unknown>;
  policyName?: string;
  executionMs: number;
  initiatorId: string;
}

export interface CommandRejectedResponse {
  commandId: string;
  correlationId: string;
  status: 'rejected';
  errorCode: string;
  reason: string;
  executionMs: number;
  initiatorId: string;
}

export type DegradedReason = 'ai_unavailable' | 'ai_timeout' | 'ai_low_confidence';

export interface CommandDegradedResponse {
  commandId: string;
  correlationId: string;
  status: 'executed_degraded';
  result: CommandResult;
  executionMs: number;
  initiatorId: string;
  degradedReason: DegradedReason;
  fallbackStrategy?: string;
}

export type CommandResponse =
  | CommandExecutedResponse
  | CommandNeedsInputResponse
  | CommandRejectedResponse
  | CommandDegradedResponse;

export interface ValidationError {
  path: string;
  code: string;
  message: string;
}

export interface BusinessViolation {
  rule: string;
  message: string;
}

export interface CommandErrorResponse {
  correlationId: string;
  errorCode: string;
  message: string;
  validationErrors?: ValidationError[];
  violations?: BusinessViolation[];
}
```

---

### Step 2: Add executeCommand to `clients/web/src/lib/api.ts`

Add imports at top:
```typescript
import type {
  // ... existing imports ...
  CommandRequest,
  CommandResponse,
} from '../types/api';
```

Add functions at end of file:
```typescript
export function generateIdempotencyKey(): string {
  return crypto.randomUUID();
}

export function generateCorrelationId(): string {
  return crypto.randomUUID();
}

export async function executeCommand(
  request: CommandRequest,
  idempotencyKey: string
): Promise<CommandResponse> {
  const correlationId = generateCorrelationId();

  return apiFetch<CommandResponse>('/commands', {
    method: 'POST',
    body: request,
    headers: {
      'Idempotency-Key': idempotencyKey,
      'X-Correlation-ID': correlationId,
    },
  });
}
```

---

### Step 3: Create `clients/web/src/hooks/useCommand.ts`

```typescript
import { useCallback, useRef, useState } from 'react';
import { executeCommand, generateIdempotencyKey } from '../lib/api';
import { ApiError } from '../lib/errors';
import type { CommandRequest, CommandResponse } from '../types/api';

interface UseCommandState {
  isLoading: boolean;
  response: CommandResponse | null;
  error: string | null;
}

interface UseCommandReturn extends UseCommandState {
  execute: (request: CommandRequest) => Promise<CommandResponse | null>;
  reset: () => void;
}

const ERROR_MESSAGES: Record<number, string> = {
  400: 'Invalid request. Please check your input.',
  403: 'Access denied. You are not a member of this household.',
  409: 'Command already submitted.',
};

export function useCommand(): UseCommandReturn {
  const [state, setState] = useState<UseCommandState>({
    isLoading: false,
    response: null,
    error: null,
  });

  // Keep idempotency key stable for retries until reset
  const idempotencyKeyRef = useRef<string>(generateIdempotencyKey());

  const execute = useCallback(async (request: CommandRequest): Promise<CommandResponse | null> => {
    setState({ isLoading: true, response: null, error: null });

    try {
      const response = await executeCommand(request, idempotencyKeyRef.current);
      setState({ isLoading: false, response, error: null });
      // Generate new key for next submission
      idempotencyKeyRef.current = generateIdempotencyKey();
      return response;
    } catch (err) {
      let errorMessage = 'An unexpected error occurred.';

      if (err instanceof ApiError) {
        errorMessage = ERROR_MESSAGES[err.status] || err.body?.message || errorMessage;

        // On 409, generate new key since this one is "used"
        if (err.status === 409) {
          idempotencyKeyRef.current = generateIdempotencyKey();
        }
      }

      setState({ isLoading: false, response: null, error: errorMessage });
      return null;
    }
  }, []);

  const reset = useCallback(() => {
    setState({ isLoading: false, response: null, error: null });
    idempotencyKeyRef.current = generateIdempotencyKey();
  }, []);

  return {
    ...state,
    execute,
    reset,
  };
}
```

---

### Step 4: Create `clients/web/src/components/commands/CreateTaskForm.tsx`

```typescript
import { useState, type FormEvent } from 'react';
import { useZones } from '../../hooks/useZones';
import { useMembers } from '../../hooks/useMembers';
import { Select } from '../ui/Select';
import type { CreateTaskPayload } from '../../types/api';

interface CreateTaskFormProps {
  householdId: string;
  onSubmit: (payload: CreateTaskPayload) => void;
  onCancel: () => void;
  isLoading: boolean;
}

export function CreateTaskForm({ householdId, onSubmit, onCancel, isLoading }: CreateTaskFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [zoneId, setZoneId] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [deadline, setDeadline] = useState('');
  const [validationError, setValidationError] = useState('');

  const { zones } = useZones(householdId);
  const { members } = useMembers(householdId);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    // Validation
    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      setValidationError('Title is required');
      return;
    }
    if (trimmedTitle.length > 500) {
      setValidationError('Title must be 500 characters or less');
      return;
    }
    if (description.length > 2000) {
      setValidationError('Description must be 2000 characters or less');
      return;
    }
    if (deadline && new Date(deadline) <= new Date()) {
      setValidationError('Deadline must be in the future');
      return;
    }

    const payload: CreateTaskPayload = {
      title: trimmedTitle,
      ...(description && { description }),
      ...(zoneId && { zoneId }),
      ...(assigneeId && { assigneeId }),
      ...(deadline && { deadline: new Date(deadline).toISOString() }),
    };

    onSubmit(payload);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="title" className="block text-sm font-medium mb-1">
          Title <span className="text-red-500">*</span>
        </label>
        <input
          id="title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="What needs to be done?"
          className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          disabled={isLoading}
          maxLength={500}
          autoFocus
        />
      </div>

      <div>
        <label htmlFor="zone" className="block text-sm font-medium mb-1">
          Zone
        </label>
        <Select
          id="zone"
          value={zoneId}
          onChange={(e) => setZoneId(e.target.value)}
          disabled={isLoading}
        >
          <option value="">Select zone (optional)</option>
          {zones.map((zone) => (
            <option key={zone.id} value={zone.id}>
              {zone.name}
            </option>
          ))}
        </Select>
      </div>

      <div>
        <label htmlFor="assignee" className="block text-sm font-medium mb-1">
          Assign to
        </label>
        <Select
          id="assignee"
          value={assigneeId}
          onChange={(e) => setAssigneeId(e.target.value)}
          disabled={isLoading}
        >
          <option value="">Auto-assign</option>
          {members.map((member) => (
            <option key={member.userId} value={member.userId}>
              {member.displayName}
            </option>
          ))}
        </Select>
      </div>

      <div>
        <label htmlFor="deadline" className="block text-sm font-medium mb-1">
          Deadline
        </label>
        <input
          id="deadline"
          type="datetime-local"
          value={deadline}
          onChange={(e) => setDeadline(e.target.value)}
          className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          disabled={isLoading}
        />
      </div>

      {validationError && (
        <p className="text-red-500 text-sm">{validationError}</p>
      )}

      <div className="flex gap-2 justify-end">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-gray-600 hover:text-gray-800"
          disabled={isLoading}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:opacity-50"
          disabled={isLoading}
        >
          {isLoading ? 'Creating...' : 'Create Task'}
        </button>
      </div>
    </form>
  );
}
```

---

### Step 5: Create `clients/web/src/components/commands/CompleteTaskForm.tsx`

```typescript
import { useState, type FormEvent } from 'react';
import { useTasks } from '../../hooks/useTasks';
import { Select } from '../ui/Select';
import type { CompleteTaskPayload } from '../../types/api';

interface CompleteTaskFormProps {
  householdId: string;
  onSubmit: (payload: CompleteTaskPayload) => void;
  onCancel: () => void;
  isLoading: boolean;
}

export function CompleteTaskForm({ householdId, onSubmit, onCancel, isLoading }: CompleteTaskFormProps) {
  const [taskId, setTaskId] = useState('');
  const [validationError, setValidationError] = useState('');

  // Fetch only open tasks for completion
  const { tasks, isLoading: tasksLoading } = useTasks(householdId, { status: 'open' });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setValidationError('');

    if (!taskId) {
      setValidationError('Please select a task to complete');
      return;
    }

    onSubmit({ taskId });
  };

  const openTasks = tasks.filter((t) => t.status === 'open');

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="task" className="block text-sm font-medium mb-1">
          Task to Complete <span className="text-red-500">*</span>
        </label>
        <Select
          id="task"
          value={taskId}
          onChange={(e) => setTaskId(e.target.value)}
          disabled={isLoading || tasksLoading}
        >
          <option value="">Select a task...</option>
          {openTasks.map((task) => (
            <option key={task.id} value={task.id}>
              {task.title}
            </option>
          ))}
        </Select>
        {tasksLoading && <p className="text-sm text-gray-500 mt-1">Loading tasks...</p>}
        {!tasksLoading && openTasks.length === 0 && (
          <p className="text-sm text-gray-500 mt-1">No open tasks to complete</p>
        )}
      </div>

      {validationError && (
        <p className="text-red-500 text-sm">{validationError}</p>
      )}

      <div className="flex gap-2 justify-end">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-gray-600 hover:text-gray-800"
          disabled={isLoading}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 bg-green-500 text-white rounded-md hover:bg-green-600 disabled:opacity-50"
          disabled={isLoading || !taskId}
        >
          {isLoading ? 'Completing...' : 'Mark Complete'}
        </button>
      </div>
    </form>
  );
}
```

---

### Step 6: Create `clients/web/src/components/commands/CommandInput.tsx`

```typescript
import { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useCommand } from '../../hooks/useCommand';
import { CreateTaskForm } from './CreateTaskForm';
import { CompleteTaskForm } from './CompleteTaskForm';
import type { CommandType, CreateTaskPayload, CompleteTaskPayload } from '../../types/api';

type CommandMode = CommandType | null;

export function CommandInput() {
  const { householdId } = useAuth();
  const { execute, isLoading, response, error, reset } = useCommand();
  const [mode, setMode] = useState<CommandMode>(null);

  if (!householdId) {
    return null;
  }

  const handleCreateTask = async (payload: CreateTaskPayload) => {
    const result = await execute({
      householdId,
      type: 'create_task',
      payload,
      source: 'web',
    });

    if (result) {
      // Success - close form
      setMode(null);
    }
  };

  const handleCompleteTask = async (payload: CompleteTaskPayload) => {
    const result = await execute({
      householdId,
      type: 'complete_task',
      payload,
      source: 'web',
    });

    if (result) {
      // Success - close form
      setMode(null);
    }
  };

  const handleCancel = () => {
    setMode(null);
    reset();
  };

  // Closed state - show action buttons
  if (mode === null) {
    return (
      <div className="bg-white border rounded-lg p-4 mb-4 shadow-sm">
        <div className="flex gap-2">
          <button
            onClick={() => setMode('create_task')}
            className="flex-1 px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600"
          >
            + Create Task
          </button>
          <button
            onClick={() => setMode('complete_task')}
            className="flex-1 px-4 py-2 bg-green-500 text-white rounded-md hover:bg-green-600"
          >
            ✓ Complete Task
          </button>
        </div>

        {/* Show last response feedback */}
        {response && (
          <div className={`mt-3 p-2 rounded text-sm ${
            response.status === 'executed' || response.status === 'executed_degraded'
              ? 'bg-green-50 text-green-700'
              : response.status === 'needs_input'
              ? 'bg-yellow-50 text-yellow-700'
              : 'bg-red-50 text-red-700'
          }`}>
            {response.status === 'executed' && 'Command executed successfully!'}
            {response.status === 'executed_degraded' && 'Command completed with limitations.'}
            {response.status === 'needs_input' && `More info needed: ${response.question}`}
            {response.status === 'rejected' && `Rejected: ${response.reason}`}
          </div>
        )}

        {error && (
          <div className="mt-3 p-2 rounded text-sm bg-red-50 text-red-700">
            {error}
          </div>
        )}
      </div>
    );
  }

  // Open state - show form
  return (
    <div className="bg-white border rounded-lg p-4 mb-4 shadow-sm">
      <h3 className="text-lg font-medium mb-4">
        {mode === 'create_task' ? 'Create Task' : 'Complete Task'}
      </h3>

      {error && (
        <div className="mb-4 p-2 rounded text-sm bg-red-50 text-red-700">
          {error}
        </div>
      )}

      {mode === 'create_task' && (
        <CreateTaskForm
          householdId={householdId}
          onSubmit={handleCreateTask}
          onCancel={handleCancel}
          isLoading={isLoading}
        />
      )}

      {mode === 'complete_task' && (
        <CompleteTaskForm
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

### Step 7: Create `clients/web/src/components/commands/index.ts`

```typescript
export { CommandInput } from './CommandInput';
export { CreateTaskForm } from './CreateTaskForm';
export { CompleteTaskForm } from './CompleteTaskForm';
```

---

### Step 8: Integrate in `clients/web/src/routes/HouseholdLayout.tsx`

Add import at top:
```typescript
import { CommandInput } from '../components/commands';
```

Add `<CommandInput />` above `<Outlet />` in the component's return:
```tsx
return (
  <Layout>
    <CommandInput />
    <Outlet />
  </Layout>
);
```

---

## Verification

After implementation, run:

```bash
cd clients/web

# Type check
npm run typecheck

# Lint
npm run lint

# Build
npm run build

# Start dev server and test manually
npm run dev
```

### Manual Testing Checklist

1. **Create Task Flow:**
   - Navigate to household view
   - Click "Create Task"
   - Fill title, optionally zone/assignee/deadline
   - Submit → verify loading state → verify success message
   - Check task appears in list

2. **Complete Task Flow:**
   - Have at least one open task
   - Click "Complete Task"
   - Select task from dropdown
   - Submit → verify loading state → verify success message
   - Check task status changed

3. **Error Handling:**
   - Submit empty title → validation error
   - Check DevTools for correct headers (Idempotency-Key, X-Correlation-ID)
   - Check request body structure matches OpenAPI

---

## Anti-Scope-Creep

DO NOT implement:
- Status display details (ST-502)
- Command history (ST-503)
- needs_input form continuation
- Rich suggestions/autocomplete
- NL text input (Stage 2)

---

## Files Changed Summary

| File | Action |
|------|--------|
| `clients/web/src/types/api.ts` | MODIFY - add Command types |
| `clients/web/src/lib/api.ts` | MODIFY - add executeCommand |
| `clients/web/src/hooks/useCommand.ts` | CREATE |
| `clients/web/src/components/commands/CommandInput.tsx` | CREATE |
| `clients/web/src/components/commands/CreateTaskForm.tsx` | CREATE |
| `clients/web/src/components/commands/CompleteTaskForm.tsx` | CREATE |
| `clients/web/src/components/commands/index.ts` | CREATE |
| `clients/web/src/routes/HouseholdLayout.tsx` | MODIFY - add CommandInput |
