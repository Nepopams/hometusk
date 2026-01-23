# Workpack: ST-501 — Command Input Box

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-501-command-input-box.md`
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Create a command input UI component for submitting structured commands (create_task, complete_task) to `POST /api/v1/commands` with proper headers and error handling.

## User Value
As a household member, I want to quickly create tasks or mark them complete through a command interface, so that I can manage household tasks efficiently.

---

## Scope

### In Scope
- Command form UI (quick task creation or completion)
- Submit button + Enter key shortcut
- `POST /api/v1/commands` integration (Stage 1 structured commands)
- Generate `Idempotency-Key` header (UUID, unique per user for 24h)
- Generate/pass `X-Correlation-ID` header (UUID v4)
- Loading state during API call
- Pass `householdId` from current HouseholdContext
- Pass `type`, `payload`, `source: 'web'` in request body
- Basic feedback on submission (success toast or inline)
- Error handling for 400/401/403/409 responses

### Out of Scope
- Full status display (ST-502)
- Command history (ST-503)
- needs_input form continuation (NEXT)
- Rich suggestions/autocomplete
- Voice input
- NL parsing (Stage 2+)

---

## Anchors (Non-negotiables)

| Artifact | Path | Constraint |
|----------|------|------------|
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` | Request/response schema MUST match exactly |
| Command types | OpenAPI enum | Only `create_task` and `complete_task` supported |
| Idempotency-Key | OpenAPI header spec | UUID, unique per user for 24h, pattern: `^[A-Za-z0-9._-]{1,128}$` |
| Source | OpenAPI enum | MUST be `'web'` for this client |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/types/api.ts` | MODIFY | Add Command types from OpenAPI |
| `clients/web/src/lib/api.ts` | MODIFY | Add `executeCommand()` function |
| `clients/web/src/hooks/useCommand.ts` | CREATE | Mutation hook for command execution |
| `clients/web/src/components/commands/CommandInput.tsx` | CREATE | Main command input UI |
| `clients/web/src/components/commands/CreateTaskForm.tsx` | CREATE | Create task form component |
| `clients/web/src/components/commands/CompleteTaskForm.tsx` | CREATE | Complete task form component |
| `clients/web/src/components/commands/index.ts` | CREATE | Barrel export |
| `clients/web/src/routes/HouseholdLayout.tsx` | MODIFY | Add CommandInput to layout |

---

## Implementation Plan

### Step 1: Add Command types to api.ts
**Expected result:** TypeScript types match OpenAPI schemas exactly.
**Files touched:**
- `clients/web/src/types/api.ts`

```typescript
// Add these types:
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

export type CommandStatus = 'executed' | 'needs_input' | 'rejected' | 'executed_degraded';

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

export interface CommandErrorResponse {
  correlationId: string;
  errorCode: string;
  message: string;
  validationErrors?: ValidationError[];
  violations?: BusinessViolation[];
}

export interface ValidationError {
  path: string;
  code: string;
  message: string;
}

export interface BusinessViolation {
  rule: string;
  message: string;
}
```

### Step 2: Add executeCommand to lib/api.ts
**Expected result:** API function with proper headers (Idempotency-Key, X-Correlation-ID).
**Files touched:**
- `clients/web/src/lib/api.ts`

```typescript
// Add helper functions and executeCommand:
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

### Step 3: Create useCommand hook
**Expected result:** React hook managing command submission state (loading, error, result).
**Files touched:**
- `clients/web/src/hooks/useCommand.ts`

Hook responsibilities:
- Generate Idempotency-Key per submission
- Track loading state
- Handle success/error responses
- Provide reset function
- Map error codes to user messages

### Step 4: Create CommandInput component structure
**Expected result:** Directory with barrel export and main component.
**Files touched:**
- `clients/web/src/components/commands/index.ts`
- `clients/web/src/components/commands/CommandInput.tsx`

Component responsibilities:
- Mode toggle (create_task / complete_task)
- Render appropriate form based on mode
- Pass householdId from context
- Handle form submission via useCommand
- Display loading/error/success states

### Step 5: Create CreateTaskForm component
**Expected result:** Form with title (required), zoneId (optional), deadline (optional).
**Files touched:**
- `clients/web/src/components/commands/CreateTaskForm.tsx`

Form fields:
- Title (text input, required, 1-500 chars)
- Zone (select dropdown from zones list, optional)
- Deadline (datetime input, optional, must be future)
- Submit/Cancel buttons

### Step 6: Create CompleteTaskForm component
**Expected result:** Form with task selector for completing tasks.
**Files touched:**
- `clients/web/src/components/commands/CompleteTaskForm.tsx`

Form fields:
- Task selector (dropdown of open tasks, required)
- Submit button

### Step 7: Integrate CommandInput into layout
**Expected result:** CommandInput visible in household layout.
**Files touched:**
- `clients/web/src/routes/HouseholdLayout.tsx`

Options for integration:
- Floating action button (FAB) that opens modal
- Inline section at top of content area
- Sidebar widget

---

## Tests & Checks

### Unit Tests (to create)
Location: `clients/web/src/components/commands/__tests__/`

| Test | Description |
|------|-------------|
| `CommandInput.test.tsx` | Component renders, mode toggle works |
| `CreateTaskForm.test.tsx` | Form validation, submit triggers callback |
| `CompleteTaskForm.test.tsx` | Task select works, submit triggers callback |
| `useCommand.test.ts` | Hook state management, error handling |

### Verification Commands

```bash
# Build passes
cd clients/web && npm run build

# Lint passes
cd clients/web && npm run lint

# Type check passes
cd clients/web && npm run typecheck

# Dev server starts
cd clients/web && npm run dev
```

---

## Demo Scenario (Manual Verification)

### Scenario 1: Create Task
1. Navigate to household view
2. Open command input (FAB or inline)
3. Select "Create Task" mode
4. Fill in title: "Clean the kitchen"
5. Optionally select zone
6. Click Submit
7. Verify loading state appears
8. Verify success feedback
9. Verify task appears in task list

### Scenario 2: Complete Task
1. Have at least one open task
2. Open command input
3. Select "Complete Task" mode
4. Select task from dropdown
5. Click "Mark Complete"
6. Verify loading state
7. Verify success feedback
8. Verify task status changed to done

### Scenario 3: Error Handling
1. Submit with empty title -> 400 validation error displayed
2. Remove auth token -> 401 redirects to login
3. Submit to household user is not member of -> 403 "Access denied"
4. Submit same idempotency key twice with different payload -> 409 conflict message

---

## Rollout / Rollback

### Rollout Steps
1. Merge to main
2. Deploy to staging
3. Manual smoke test
4. Deploy to production

### Rollback Steps
1. Revert merge commit OR
2. Set `VITE_FEATURE_COMMANDS=false` and redeploy
3. No backend changes needed (consuming existing API)

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Idempotency-Key collision | Duplicate submission rejected | Very Low | UUID v4 is collision-free in practice |
| Zone/member lists not loaded | Form dropdowns empty | Medium | Show loading state, fetch on mount |
| Task list stale after creation | User confusion | Medium | Refetch task list on success |
| 409 conflict UX unclear | User retries unnecessarily | Low | Clear "already submitted" message |
| Auth token expired mid-submit | Submission fails | Low | 401 handler redirects to login |

---

## Done Criteria

### Acceptance Criteria Map

| AC | DoD Check |
|----|-----------|
| Command form visible when authenticated + household selected | UI renders correctly |
| Submit button + Enter key trigger API call | Keyboard handler + click handler |
| Request matches OpenAPI schema exactly | Type safety via TypeScript |
| Idempotency-Key header generated (UUID) | Helper function + header set |
| X-Correlation-ID header generated (UUID v4) | Helper function + header set |
| Loading state during API call | isLoading state in hook |
| 200 response clears loading, passes to status display | State update on success |
| 400 error shows inline message | Error state rendering |
| 401 redirects to login | AuthError handler in apiFetch |
| 403 shows "Access denied" | Error code mapping |
| 409 shows "Command already submitted" | Error code mapping |

---

## Anti-Scope-Creep

DO NOT:
- Implement status display (ST-502)
- Implement command history (ST-503)
- Implement needs_input form continuation
- Add NL text input (Stage 2)
- Add voice input
- Add rich autocomplete/suggestions
- Change backend API
