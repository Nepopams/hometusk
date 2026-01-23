# Codex PLAN Prompt: ST-503 — Command History

## Mode
**PLAN ONLY** — Read-only exploration. NO file edits, NO file writes.

---

## Task
Plan the implementation of ST-503 (Command History) — localStorage-based command history with list view, details expansion, and clear functionality.

---

## Sources of Truth (MUST read first)

```bash
# 1. Story specification
cat docs/planning/epics/EP-006/stories/ST-503-command-history.md

# 2. Workpack
cat docs/planning/workpacks/ST-503/workpack.md

# 3. Existing useCommand hook (to integrate history save)
cat clients/web/src/hooks/useCommand.ts

# 4. Existing types
cat clients/web/src/types/api.ts

# 5. Existing StatusBadge (reuse)
cat clients/web/src/components/commands/StatusBadge.tsx

# 6. HouseholdLayout (integration point)
cat clients/web/src/routes/HouseholdLayout.tsx

# 7. Existing lib structure
ls -la clients/web/src/lib/

# 8. Current components structure
ls -la clients/web/src/components/commands/
```

---

## Critical Constraints

### localStorage Schema
```typescript
interface CommandHistoryEntry {
  id: string;                    // unique entry ID
  displayText: string;           // "Create task: Clean kitchen"
  commandType: CommandType;      // 'create_task' | 'complete_task'
  status: CommandStatus;         // 'executed' | 'needs_input' | 'rejected' | 'executed_degraded'
  timestamp: string;             // ISO 8601
  correlationId: string;
  commandId: string;
  householdId: string;
  request: CommandRequest;
  response: CommandResponse;
}

// Key: `hometusk:commandHistory:${householdId}`
// Value: CommandHistoryEntry[]
// Max entries: 50
```

### Integration Points
1. **useCommand.ts** — must call `addToHistory()` after successful response
2. **HouseholdLayout.tsx** — must render `<CommandHistory />` below `<CommandInput />`

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

### 1. Analyze useCommand integration point

```bash
# Where to add history save
cat clients/web/src/hooks/useCommand.ts

# Check how response is handled
rg "setState.*response" clients/web/src/hooks/useCommand.ts
```

### 2. Check existing patterns

```bash
# How other hooks use useSyncExternalStore or similar
rg "useSyncExternalStore|useCallback" clients/web/src/hooks/

# Existing lib patterns
cat clients/web/src/lib/api.ts
```

### 3. Verify types needed

```bash
# CommandStatus type exists?
rg "CommandStatus|status.*executed" clients/web/src/types/api.ts
```

### 4. Check HouseholdLayout structure

```bash
cat clients/web/src/routes/HouseholdLayout.tsx
```

---

## Expected Plan Output

### A. File structure

```
clients/web/src/
├── lib/
│   └── commandHistory.ts      # NEW: localStorage helpers
├── hooks/
│   ├── useCommand.ts          # MODIFY: add history save
│   └── useCommandHistory.ts   # NEW: hook for reading history
├── components/commands/
│   ├── CommandHistory.tsx     # NEW: list component
│   ├── CommandHistoryEntry.tsx # NEW: entry with expand
│   └── index.ts               # MODIFY: exports
├── routes/
│   └── HouseholdLayout.tsx    # MODIFY: render history
└── styles/
    └── index.css              # MODIFY: add styles
```

### B. Component interfaces

Define props for:
- CommandHistory (householdId from useAuth)
- CommandHistoryEntry (entry, onExpand/onCollapse, expanded)

### C. State management

- useSyncExternalStore for localStorage reactivity
- Dispatch custom storage event for same-tab updates

### D. Styling approach

- Collapsible entries
- StatusBadge reuse
- Relative timestamps

---

## STOP Conditions

STOP and request input if:
- CommandStatus type not found in api.ts
- useCommand structure differs from workpack assumptions
- HouseholdLayout has different structure

---

## Deliverable

A detailed implementation plan with:
1. File-by-file changes
2. Component props interfaces
3. localStorage helper signatures
4. Integration code for useCommand
5. CSS class names

**DO NOT write any code files. PLAN ONLY.**
