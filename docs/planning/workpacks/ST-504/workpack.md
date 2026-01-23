# Workpack: ST-504 — needs_input Basic Display

## Sources of Truth
- Story: `docs/planning/epics/EP-006/stories/ST-504-needs-input-display.md`
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Current NeedsInputResult: `clients/web/src/components/commands/NeedsInputResult.tsx`

---

## Goal
Enhance NeedsInputResult component with human-readable field labels, better suggestions display, policy explanations, original input reference, and guidance message.

## User Value
As a household member, when my command is ambiguous or incomplete, I want clear guidance on what information is missing, so that I can retype my command with the right details.

---

## Scope

### In Scope
- Human-readable field labels (zoneId → "Zone")
- Per-field suggestions display with labels
- Policy label mapping (ZONE_REQUIRED → explanation)
- Original input reference (show what was submitted)
- Guidance message with example
- Retain "Edit & Retry" action

### Out of Scope
- Form-based continuation flow (POST /commands/{commandId}/continue)
- Auto-population of suggested values
- Inline field editing
- Command continuation

---

## Anchors (Non-negotiables)

| Artifact | Path | Constraint |
|----------|------|------------|
| NeedsInputResult | `components/commands/NeedsInputResult.tsx` | Enhance, not replace |
| CommandResult | `components/commands/CommandResult.tsx` | May need to pass request |
| CommandInput | `components/commands/CommandInput.tsx` | Source of original request |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/fieldLabels.ts` | CREATE | Field and policy label mappings |
| `clients/web/src/components/commands/NeedsInputResult.tsx` | MODIFY | Enhanced display |
| `clients/web/src/components/commands/CommandResult.tsx` | MODIFY | Pass request for original input |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY | Pass request to CommandResult |
| `clients/web/src/styles/index.css` | MODIFY | Enhanced styles |

---

## Implementation Plan

### Step 1: Create fieldLabels.ts

```typescript
// clients/web/src/lib/fieldLabels.ts

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
};

export function getPolicyLabel(policyName: string): string {
  return POLICY_LABELS[policyName] || `Policy: ${policyName}`;
}
```

### Step 2: Update CommandInput to track last request

Pass `lastRequest` to CommandResult when response is available.

### Step 3: Update CommandResult to pass request

Add `request` prop and pass to NeedsInputResult.

### Step 4: Enhance NeedsInputResult

```typescript
interface NeedsInputResultProps {
  data: CommandNeedsInputResponse;
  request?: CommandRequest; // original request for reference
  onRetry: () => void;
}
```

Enhanced display:
- Question in callout box
- Required fields with human-readable labels
- Per-field suggestions grouped
- Original input reference
- Guidance message with example
- Policy with explanation tooltip

### Step 5: Add CSS styles

- Callout box for question
- Field list styling
- Suggestion pills
- Guidance tip box

---

## UI Structure

```
+----------------------------------------------------------+
| [?] More information needed                               |
|                                                          |
| "Which zone should this task be in?"                     |
|                                                          |
| Missing information:                                      |
|   - Zone (required)                                      |
|     Suggestions: Kitchen, Bathroom, Living Room          |
|   - Deadline (required)                                  |
|     Suggestions: Today, Tomorrow                         |
|                                                          |
| Your command: "Create task: Clean tomorrow"              |
|                                                          |
| Tip: Try adding more details, e.g.                       |
| "Clean the kitchen tomorrow at 6pm"                      |
|                                                          |
| [Edit & Retry]                                           |
|                                                          |
| Policy: Zone is required for task creation               |
| Trace: abc-123 | 150ms                                   |
+----------------------------------------------------------+
```

---

## Tests & Checks

### Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npm run lint
```

### Manual Testing
1. Submit ambiguous command → verify enhanced needs_input UI
2. Verify question displayed in callout
3. Verify field labels are human-readable
4. Verify suggestions grouped per field
5. Verify original input shown
6. Verify guidance message displayed
7. Verify policy explanation shown
8. Verify "Edit & Retry" works

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Unknown field names | No label | Fallback to raw name |
| Unknown policy names | No explanation | Fallback to raw name |
| No request available | Can't show original | Graceful fallback |

---

## Done Criteria

| AC | DoD Check |
|----|-----------|
| AC1: Question displayed | Callout box with question |
| AC2: Required fields | Human-readable labels |
| AC3: Suggestions | Per-field suggestions |
| AC4: Policy name | Explanation shown |
| AC5: Original input | Request reference |
| AC6: Guidance | Tip with example |
| AC7: No form fields | No continuation form |

---

## Anti-Scope-Creep

DO NOT:
- Add form fields for input continuation
- Add "Submit Additional Info" button
- Add auto-population of suggestions
- Add inline field editing
