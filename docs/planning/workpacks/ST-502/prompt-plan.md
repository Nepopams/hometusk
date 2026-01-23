# Codex PLAN Prompt: ST-502 — Command Status Display

## Mode
**PLAN ONLY** — Read-only exploration. NO file edits, NO file writes.

---

## Task
Plan the implementation of ST-502 (Command Status Display) — UI components to display detailed command execution results for all 4 status types.

---

## Sources of Truth (MUST read first)

```bash
# 1. Story specification
cat docs/planning/epics/EP-006/stories/ST-502-command-status-display.md

# 2. Workpack
cat docs/planning/workpacks/ST-502/workpack.md

# 3. OpenAPI contract (response schemas)
cat docs/contracts/http/commands.openapi.yaml

# 4. Existing command types (from ST-501)
cat clients/web/src/types/api.ts

# 5. Current CommandInput implementation
cat clients/web/src/components/commands/CommandInput.tsx

# 6. Existing components structure
ls -la clients/web/src/components/commands/

# 7. Check for existing UI patterns (buttons, badges)
ls -la clients/web/src/components/ui/
cat clients/web/src/components/ui/*.tsx 2>/dev/null || echo "Check individual files"
```

---

## Critical Constraints

### Response Types (from types/api.ts)
```typescript
type CommandResponse =
  | CommandExecutedResponse      // status: 'executed'
  | CommandNeedsInputResponse    // status: 'needs_input'
  | CommandRejectedResponse      // status: 'rejected'
  | CommandDegradedResponse;     // status: 'executed_degraded'
```

### Required Fields by Status

**executed:**
- commandId, correlationId, status, result, executionMs, initiatorId
- result: { taskId?, assigneeId?, decisionConfidence? }

**needs_input:**
- commandId, correlationId, status, question, requiredFields, executionMs, initiatorId
- suggestions? (object), policyName? (string)

**rejected:**
- commandId, correlationId, status, errorCode, reason, executionMs, initiatorId

**executed_degraded:**
- commandId, correlationId, status, result, executionMs, initiatorId, degradedReason
- fallbackStrategy? (string)
- degradedReason: 'ai_unavailable' | 'ai_timeout' | 'ai_low_confidence'

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

### 1. Analyze current CommandInput

```bash
# How does it currently display response?
cat clients/web/src/components/commands/CommandInput.tsx

# What callbacks/actions exist?
rg "onNewCommand|onRetry|handleCancel" clients/web/src/components/commands/
```

### 2. Check existing UI patterns

```bash
# Existing button styles
rg "button|ghost-button" clients/web/src/components/

# Existing styling approach
cat clients/web/src/App.css | head -100

# Any existing badge/indicator components?
ls clients/web/src/components/ui/
```

### 3. Identify integration point

```bash
# Where to render CommandResult in CommandInput
rg "responseMessage|renderResponseMessage" clients/web/src/components/commands/CommandInput.tsx
```

---

## Expected Plan Output

### A. Component hierarchy

```
CommandInput
└── CommandResult (dispatcher)
    ├── ExecutedResult
    │   ├── StatusBadge
    │   └── TraceInfo
    ├── NeedsInputResult
    │   └── StatusBadge
    ├── RejectedResult
    │   ├── StatusBadge
    │   └── TraceInfo
    └── DegradedResult
        ├── StatusBadge
        └── TraceInfo
```

### B. Props interfaces

Define props for each component:
- CommandResult props
- StatusBadge props
- TraceInfo props
- Each status result props

### C. Integration changes to CommandInput

Identify what to change:
- Remove inline responseMessage rendering
- Add CommandResult component
- Pass action callbacks (onNewCommand, onRetry)

### D. Styling approach

- Use existing CSS classes where available
- Define new classes if needed
- Color scheme per status type

---

## STOP Conditions

STOP and request input if:
- CommandInput structure differs significantly from workpack assumptions
- No clear integration point for CommandResult
- Missing type definitions for response statuses

---

## Deliverable

A detailed implementation plan with:
1. Component file structure
2. Props interfaces for each component
3. Integration changes to CommandInput
4. Styling approach

**DO NOT write any code files. PLAN ONLY.**
