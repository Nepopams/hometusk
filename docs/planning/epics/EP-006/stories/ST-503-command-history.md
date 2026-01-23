# Story: ST-503 — Command History

## Sources of Truth
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Persist and display recent commands per household in localStorage.

## User Value
As a household member, I want to see my recent commands, so that I can understand what I've done and reference past interactions.

---

## In Scope
- Store last N commands (e.g., 50) per household in localStorage
- Display command history list
- Show: input text, status badge, timestamp, correlationId
- Click to expand/view details
- Clear history option
- Prune old entries when limit exceeded

## Out of Scope
- Server-side command history (backend does not store command list for retrieval)
- Search/filter in history
- Export history
- Cross-device sync

---

## Acceptance Criteria

### AC1: Command saved to history
```gherkin
Given I submit a command
When API returns any response (executed/needs_input/rejected/degraded)
Then command is saved to localStorage
And entry contains:
  - inputText: original command text
  - status: response status
  - timestamp: submission time
  - correlationId: from response
  - commandId: from response
  - householdId: current household
```

### AC2: History displayed
```gherkin
Given I have submitted commands previously
When I view the command history section
Then I see a list of recent commands
And each entry shows:
  - Input text (truncated if long)
  - Status badge (executed/needs_input/rejected/degraded)
  - Relative timestamp (e.g., "2 minutes ago")
And list is ordered by timestamp (newest first)
```

### AC3: History scoped to household
```gherkin
Given I have commands in household A
And I switch to household B
When I view command history
Then I see only commands from household B
And household A commands are not visible
```

### AC4: Expand history entry
```gherkin
Given command history is displayed
When I click on a history entry
Then details expand or modal shows:
  - Full input text
  - Full response data
  - correlationId (copyable)
  - Timestamp (full date/time)
```

### AC5: History limit enforced
```gherkin
Given history has 50 entries
When I submit a new command
Then oldest entry is removed
And new entry is added
And total count remains at 50
```

### AC6: Clear history
```gherkin
Given command history is displayed
When I click "Clear History"
Then confirmation prompt appears
When I confirm
Then all history for current household is deleted
And empty state is shown
```

### AC7: Empty state
```gherkin
Given no commands have been submitted
When I view command history
Then I see "No commands yet"
And suggestion "Type a command above to get started"
```

---

## UI Specification

**History list:**
```
+----------------------------------------------------------+
| Recent Commands                            [Clear]        |
|----------------------------------------------------------|
| > Clean the kitchen tomorrow          [executed] 2m ago  |
| > Complete task for groceries         [executed] 1h ago  |
| > Do something ambiguous              [needs_input] 2h   |
+----------------------------------------------------------+
```

**Expanded entry:**
```
+----------------------------------------------------------+
| Command Details                                           |
|----------------------------------------------------------|
| Input: "Clean the kitchen tomorrow"                       |
| Status: executed                                          |
| Submitted: Jan 23, 2026 10:30:00                         |
| Correlation ID: abc-123-def-456 [copy]                   |
| Command ID: xyz-789                                       |
|                                                          |
| [View Trace]  [Close]                                    |
+----------------------------------------------------------+
```

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/components/commands/CommandHistory.tsx` — create
- `clients/web/src/components/commands/CommandHistoryEntry.tsx` — create
- `clients/web/src/hooks/useCommandHistory.ts` — create
- `clients/web/src/lib/commandHistory.ts` — localStorage helper

**localStorage schema:**
```typescript
interface CommandHistoryEntry {
  id: string; // unique entry ID
  inputText: string;
  status: 'executed' | 'needs_input' | 'rejected' | 'executed_degraded';
  timestamp: string; // ISO 8601
  correlationId: string;
  commandId: string;
  householdId: string;
  response: CommandResponse; // full response for details view
}

// Key: `hometusk:commandHistory:${householdId}`
// Value: CommandHistoryEntry[]
```

**Utility functions:**
```typescript
const MAX_HISTORY_ENTRIES = 50;

function addToHistory(householdId: string, entry: CommandHistoryEntry): void;
function getHistory(householdId: string): CommandHistoryEntry[];
function clearHistory(householdId: string): void;
function pruneOldEntries(entries: CommandHistoryEntry[]): CommandHistoryEntry[];
```

---

## Test Strategy

**Manual tests:**
- Submit commands → verify localStorage updated
- Refresh page → verify history persists
- Switch households → verify history scoped
- Submit 51 commands → verify oldest is pruned
- Clear history → verify empty

**Unit tests:**
- useCommandHistory hook returns entries
- addToHistory adds entry to localStorage
- getHistory returns sorted entries
- clearHistory removes all entries
- pruneOldEntries caps at MAX_HISTORY_ENTRIES

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-501 (Command Input Box) — triggers history save
- ST-502 (Command Status Display) — provides response data

## Points
2 (localStorage + UI list + details view)

## Priority
P2 (enhances UX, not blocking core flow)
