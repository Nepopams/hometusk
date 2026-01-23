# Codex PLAN Prompt: ST-505 — Minimal Trace Viewer

## Mode
**PLAN ONLY** — Read-only exploration. NO file edits, NO file writes.

---

## Task
Plan the enhancement of TraceInfo with expandable details view, RawJsonViewer, and reusable CopyButton component.

---

## Sources of Truth (MUST read first)

```bash
# 1. Story specification
cat docs/planning/epics/EP-006/stories/ST-505-trace-viewer.md

# 2. Workpack
cat docs/planning/workpacks/ST-505/workpack.md

# 3. Current TraceInfo (to enhance)
cat clients/web/src/components/commands/TraceInfo.tsx

# 4. How TraceInfo is used in result components
rg "TraceInfo" clients/web/src/components/commands/ -l
cat clients/web/src/components/commands/ExecutedResult.tsx
cat clients/web/src/components/commands/DegradedResult.tsx

# 5. Types for CommandResponse
cat clients/web/src/types/api.ts | head -200

# 6. Existing UI components
ls -la clients/web/src/components/ui/

# 7. Check history entry for existing JSON display
cat clients/web/src/components/commands/CommandHistoryEntry.tsx
```

---

## Critical Constraints

### TraceInfo receives limited props currently
```typescript
interface TraceInfoProps {
  commandId: string;
  correlationId: string;
  executionMs: number;
}
```

To show full details, need to either:
1. Pass full `response: CommandResponse` prop
2. Or pass individual fields (result, status, etc.)

### CopyButton should be reusable
Extract copy logic from TraceInfo into separate component.

### RawJsonViewer
Collapsible JSON display with:
- Pretty-printed formatting
- Copy JSON button
- Max-height with scroll

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

### 1. Analyze current TraceInfo usage

```bash
# Where is TraceInfo used?
rg "TraceInfo" clients/web/src/components/commands/ -A 3

# What props are passed?
rg "<TraceInfo" clients/web/src/components/commands/ -B 2 -A 5
```

### 2. Check CommandResponse fields available

```bash
# What fields exist on response?
rg "CommandExecutedResponse|CommandDegradedResponse" clients/web/src/types/api.ts -A 10
```

### 3. Check existing copy patterns

```bash
# How is copy done currently?
rg "navigator.clipboard" clients/web/src/components/
```

### 4. Check existing JSON display

```bash
# History entry already shows JSON
rg "JSON.stringify" clients/web/src/components/commands/
```

---

## Expected Plan Output

### A. New files

1. `components/ui/CopyButton.tsx` - Reusable copy button
2. `components/commands/RawJsonViewer.tsx` - JSON display

### B. TraceInfo props change

```typescript
interface TraceInfoProps {
  commandId: string;
  correlationId: string;
  executionMs: number;
  status: CommandStatus;
  initiatorId?: string;
  result?: { taskId?: string; assigneeId?: string; decisionConfidence?: number };
  degradedReason?: DegradedReason;
  fallbackStrategy?: string;
  rawResponse?: CommandResponse; // for JSON viewer
}
```

### C. Component structure

```
TraceInfo
├── Collapsed row (existing)
│   ├── CopyButton (commandId)
│   ├── CopyButton (correlationId)
│   └── executionMs
├── Expand toggle
├── Expanded details (new)
│   ├── Status
│   ├── Initiator
│   ├── Result info (conditional)
│   └── Degraded info (conditional)
└── RawJsonViewer (toggle)
```

### D. CSS additions

- `.trace-info--expanded`
- `.trace-info__toggle`
- `.trace-info__details`
- `.raw-json-viewer`
- `.copy-button`

---

## STOP Conditions

STOP and request input if:
- TraceInfo usage differs significantly from expected
- Result components don't have access to full response
- Significant refactoring needed

---

## Deliverable

A detailed implementation plan with:
1. CopyButton component structure
2. RawJsonViewer component structure
3. TraceInfo enhancement details
4. Props changes for result components
5. CSS class names

**DO NOT write any code files. PLAN ONLY.**
