# Workpack: ST-502 — Command Status Display

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-502-command-status-display.md`
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- ST-501 Implementation: `clients/web/src/components/commands/`

---

## Goal
Display detailed command execution results with distinct UI for each status type (executed, needs_input, rejected, executed_degraded), including visual indicators, result details, and action buttons.

## User Value
As a household member, I want to see clear feedback about what happened with my command, so that I know if it succeeded, needs more info, or was rejected.

---

## Scope

### In Scope
- Display UI for all 4 response statuses with distinct styling
- Visual indicators (icons, colors, badges)
- Result details: taskId, correlationId, executionMs, confidence
- "What to do next" action hints
- Action buttons: "View Task", "New Command", "Retry"
- Copy correlationId functionality
- Degraded reason labels mapping

### Out of Scope
- Full needs_input form (ST-504)
- Command history (ST-503)
- Full trace viewer (ST-505)
- Navigation to task detail page (just show taskId for now)

---

## Anchors (Non-negotiables)

| Artifact | Path | Constraint |
|----------|------|------------|
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` | Response schemas must match exactly |
| ST-501 | `clients/web/src/components/commands/` | Integrate with existing CommandInput |
| CommandResponse | `types/api.ts` | Use existing discriminated union types |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/components/commands/CommandResult.tsx` | CREATE | Main result dispatcher by status |
| `clients/web/src/components/commands/ExecutedResult.tsx` | CREATE | Success status display |
| `clients/web/src/components/commands/NeedsInputResult.tsx` | CREATE | Clarification prompt display |
| `clients/web/src/components/commands/RejectedResult.tsx` | CREATE | Error display |
| `clients/web/src/components/commands/DegradedResult.tsx` | CREATE | Limited success display |
| `clients/web/src/components/commands/StatusBadge.tsx` | CREATE | Reusable status indicator |
| `clients/web/src/components/commands/TraceInfo.tsx` | CREATE | correlationId + executionMs display |
| `clients/web/src/components/commands/index.ts` | MODIFY | Export new components |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY | Use CommandResult instead of inline messages |

---

## Implementation Plan

### Step 1: Create StatusBadge component
Reusable badge with icon and color per status.

```typescript
type StatusType = 'success' | 'warning' | 'error' | 'info';

interface StatusBadgeProps {
  type: StatusType;
  message: string;
}
```

### Step 2: Create TraceInfo component
Display correlationId (copyable) and executionMs.

```typescript
interface TraceInfoProps {
  correlationId: string;
  executionMs: number;
}
```

### Step 3: Create ExecutedResult component
- StatusBadge: success, "Command executed successfully"
- Result summary: taskId, assigneeId, decisionConfidence (as %)
- Actions: "View Task" (if taskId), "New Command"
- TraceInfo

### Step 4: Create NeedsInputResult component
- StatusBadge: info, "More information needed"
- Question prominently displayed
- Required fields list
- Suggestions (if present)
- Actions: "Edit & Retry"
- TraceInfo

### Step 5: Create RejectedResult component
- StatusBadge: error, "Command rejected"
- errorCode displayed
- reason in plain text
- Actions: "Retry", "New Command"
- TraceInfo

### Step 6: Create DegradedResult component
- StatusBadge: warning, "Command completed with limitations"
- degradedReason mapped to human label
- fallbackStrategy (if present)
- Result summary (same as executed)
- Actions: "View Task", "New Command"
- TraceInfo

### Step 7: Create CommandResult dispatcher
Switch on `response.status` to render appropriate component.

```typescript
function CommandResult({ response, onNewCommand, onRetry }: Props) {
  switch (response.status) {
    case 'executed': return <ExecutedResult ... />;
    case 'needs_input': return <NeedsInputResult ... />;
    case 'rejected': return <RejectedResult ... />;
    case 'executed_degraded': return <DegradedResult ... />;
  }
}
```

### Step 8: Integrate into CommandInput
Replace inline `responseMessage` with `<CommandResult />` component.
Pass callbacks for actions (onNewCommand, onRetry).

### Step 9: Update barrel export
Add new components to index.ts.

---

## Action Button Mapping

| Status | Buttons |
|--------|---------|
| executed | "View Task" (if taskId), "New Command" |
| needs_input | "Edit & Retry" |
| rejected | "Retry", "New Command" |
| executed_degraded | "View Task" (if taskId), "New Command" |

---

## Degraded Reason Labels

```typescript
const DEGRADED_REASON_LABELS: Record<DegradedReason, string> = {
  ai_unavailable: 'AI service temporarily unavailable',
  ai_timeout: 'AI service timed out',
  ai_low_confidence: 'Low confidence result',
};
```

---

## Tests & Checks

### Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

### Manual Testing
1. Submit successful command → verify ExecutedResult UI
2. Submit command that needs input → verify NeedsInputResult UI
3. Submit invalid command → verify RejectedResult UI
4. Mock degraded response → verify DegradedResult UI
5. Click "Copy" on correlationId → verify clipboard
6. Click "New Command" → verify form resets
7. Click "Retry" → verify form retains values

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| No taskId in result | Can't show "View Task" | Conditionally render button |
| No assigneeId in result | Incomplete display | Show "Unassigned" fallback |
| Copy to clipboard fails | Poor UX | Show fallback toast |

---

## Done Criteria

| AC | DoD Check |
|----|-----------|
| AC1: Executed status | Green badge, task info, View Task button |
| AC2: needs_input status | Question, required fields, suggestions |
| AC3: rejected status | Red badge, errorCode, reason |
| AC4: executed_degraded | Warning badge, degraded reason, fallback |
| AC5: Action buttons | Contextual per status |
| AC6: New Command | Resets form and clears result |

---

## Anti-Scope-Creep

DO NOT:
- Implement full needs_input form (ST-504)
- Implement command history (ST-503)
- Implement full trace viewer (ST-505)
- Add navigation to task detail page
- Add complex animations
