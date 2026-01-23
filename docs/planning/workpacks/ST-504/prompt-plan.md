# Codex PLAN Prompt: ST-504 — needs_input Basic Display

## Mode
**PLAN ONLY** — Read-only exploration. NO file edits, NO file writes.

---

## Task
Plan the enhancement of NeedsInputResult component with human-readable field labels, per-field suggestions, policy explanations, original input reference, and guidance message.

---

## Sources of Truth (MUST read first)

```bash
# 1. Story specification
cat docs/planning/epics/EP-006/stories/ST-504-needs-input-display.md

# 2. Workpack
cat docs/planning/workpacks/ST-504/workpack.md

# 3. Current NeedsInputResult (to enhance)
cat clients/web/src/components/commands/NeedsInputResult.tsx

# 4. CommandResult (passes props to NeedsInputResult)
cat clients/web/src/components/commands/CommandResult.tsx

# 5. CommandInput (tracks response, needs to track request)
cat clients/web/src/components/commands/CommandInput.tsx

# 6. Types for reference
cat clients/web/src/types/api.ts | head -200

# 7. Existing lib structure
ls -la clients/web/src/lib/
```

---

## Critical Constraints

### Field Label Mapping
```typescript
const FIELD_LABELS: Record<string, string> = {
  zoneId: 'Zone',
  deadline: 'Deadline',
  assigneeId: 'Assignee',
  title: 'Task title',
  description: 'Description',
  taskId: 'Task',
};
```

### Policy Label Mapping
```typescript
const POLICY_LABELS: Record<string, string> = {
  ZONE_REQUIRED: 'Zone is required for task creation',
  DEADLINE_REQUIRED: 'Deadline must be specified',
  ASSIGNEE_AMBIGUOUS: 'Multiple possible assignees found',
};
```

### Out of Scope (DO NOT implement)
- Form fields for continuation
- "Submit Additional Info" button
- Auto-population of suggestions
- POST /commands/{commandId}/continue

---

## Allowed Commands (PLAN mode)

READ-ONLY ONLY:
```bash
ls, find                    # directory exploration
cat                         # read file contents
rg, grep                    # search code
```

FORBIDDEN:
- ANY file modifications
- npm commands

---

## Planning Tasks

### 1. Analyze current NeedsInputResult

```bash
# Current implementation
cat clients/web/src/components/commands/NeedsInputResult.tsx

# What props does it receive?
rg "interface.*Props" clients/web/src/components/commands/NeedsInputResult.tsx
```

### 2. Check CommandResult prop passing

```bash
# How does CommandResult pass props?
cat clients/web/src/components/commands/CommandResult.tsx
```

### 3. Check CommandInput state

```bash
# Does CommandInput track last request?
rg "request|payload" clients/web/src/components/commands/CommandInput.tsx
```

### 4. Verify CommandNeedsInputResponse type

```bash
# What fields are available?
rg "CommandNeedsInputResponse" clients/web/src/types/api.ts -A 10
```

---

## Expected Plan Output

### A. Prop flow changes

```
CommandInput
  - Track lastRequest state
  - Pass to CommandResult

CommandResult
  - Add request?: CommandRequest prop
  - Pass to NeedsInputResult

NeedsInputResult
  - Add request?: CommandRequest prop
  - Use for original input display
```

### B. New file: lib/fieldLabels.ts

```typescript
export const FIELD_LABELS: Record<string, string> = { ... };
export function getFieldLabel(field: string): string;
export const POLICY_LABELS: Record<string, string> = { ... };
export function getPolicyLabel(policy: string): string;
```

### C. NeedsInputResult enhancements

1. Question in callout box (prominent)
2. Required fields with getFieldLabel()
3. Suggestions grouped per field
4. Original input from request
5. Guidance tip message
6. Policy with getPolicyLabel()

### D. CSS additions

- `.needs-input-callout` - question box
- `.needs-input-fields` - field list
- `.needs-input-field` - single field
- `.needs-input-suggestions` - suggestion pills
- `.needs-input-original` - original input
- `.needs-input-tip` - guidance box

---

## STOP Conditions

STOP and request input if:
- CommandNeedsInputResponse type differs from spec
- CommandInput doesn't have access to request
- Significant structural changes needed

---

## Deliverable

A detailed implementation plan with:
1. Prop flow changes (CommandInput → CommandResult → NeedsInputResult)
2. fieldLabels.ts structure
3. NeedsInputResult component changes
4. CSS class names

**DO NOT write any code files. PLAN ONLY.**
