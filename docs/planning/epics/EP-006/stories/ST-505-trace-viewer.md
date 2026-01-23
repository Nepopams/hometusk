# Story: ST-505 — Minimal Trace Viewer

## Sources of Truth
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Display command trace information including correlationId, execution time, and decision details.

## User Value
As a household member (or support person), I want to see trace information for a command, so that I can debug issues or reference the exact execution details.

---

## In Scope
- Display `correlationId` prominently (copyable)
- Display `commandId` (copyable)
- Display `executionMs` (formatted)
- Display `result` object (taskId, assigneeId, decisionConfidence)
- Display `degradedReason` and `fallbackStrategy` (if present)
- Display `initiatorId`
- Expandable raw JSON view (developer-friendly)
- Accessible from command result and history entry

## Out of Scope
- Server-side DecisionLog retrieval (no API for this in web)
- Full audit trail
- Export to file
- Share trace link

---

## Acceptance Criteria

### AC1: Trace section in result
```gherkin
Given command API returns any response
When result is displayed
Then a "Trace" section is visible
And shows:
  - Correlation ID: <uuid> [copy button]
  - Execution time: <N>ms
```

### AC2: Expanded trace view
```gherkin
Given command result is displayed
When I click "View Trace Details"
Then expanded view shows:
  - Command ID: <uuid>
  - Correlation ID: <uuid>
  - Initiator: <user name or ID>
  - Execution time: <N>ms
  - Status: <status>
And for executed/degraded:
  - Task ID: <uuid> (link to task if available)
  - Assignee: <name>
  - Decision Confidence: <percentage>
And for degraded:
  - Degraded Reason: <reason label>
  - Fallback Strategy: <description>
```

### AC3: Raw JSON view
```gherkin
Given expanded trace view is open
When I click "Show Raw Response"
Then JSON is displayed in a code block
And JSON is formatted/pretty-printed
And "Copy JSON" button is available
```

### AC4: Copy functionality
```gherkin
Given trace view shows correlationId
When I click copy button
Then correlationId is copied to clipboard
And toast confirms "Copied to clipboard"
```

### AC5: Trace from history
```gherkin
Given command history entry is expanded
When I click "View Trace"
Then trace viewer opens with that command's data
```

### AC6: Minimal footprint
```gherkin
Given trace view is displayed
When I look at the UI
Then it is compact and collapsible
And does not overwhelm the main result area
And is clearly labeled as "technical details"
```

---

## UI Specification

**Inline trace (collapsed):**
```
+----------------------------------------------------------+
| Trace: abc-123-def [copy]  |  42ms                       |
+----------------------------------------------------------+
```

**Expanded trace view:**
```
+----------------------------------------------------------+
| Trace Details                                    [Close] |
|----------------------------------------------------------|
| Command ID:      xyz-789-abc                    [copy]   |
| Correlation ID:  abc-123-def                    [copy]   |
| Initiator:       alice@example.com                       |
| Execution Time:  42ms                                    |
| Status:          executed                                |
|                                                          |
| Result:                                                  |
|   Task ID:       task-456          [View Task]           |
|   Assignee:      Bob                                     |
|   Confidence:    95%                                     |
|                                                          |
| [Show Raw Response]                                      |
+----------------------------------------------------------+
```

**Raw JSON view:**
```
+----------------------------------------------------------+
| Raw Response                            [Copy] [Hide]    |
|----------------------------------------------------------|
| {                                                        |
|   "commandId": "xyz-789-abc",                            |
|   "correlationId": "abc-123-def",                        |
|   "status": "executed",                                  |
|   "result": {                                            |
|     "taskId": "task-456",                                |
|     "assigneeId": "user-789",                            |
|     "decisionConfidence": 0.95                           |
|   },                                                     |
|   "executionMs": 42,                                     |
|   "initiatorId": "user-123"                              |
| }                                                        |
+----------------------------------------------------------+
```

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/components/commands/TraceViewer.tsx` — create
- `clients/web/src/components/commands/TraceInline.tsx` — create
- `clients/web/src/components/commands/RawJsonViewer.tsx` — create
- `clients/web/src/components/ui/CopyButton.tsx` — create (reusable)

**Copy to clipboard:**
```typescript
async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}
```

**Format execution time:**
```typescript
function formatExecutionTime(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}
```

**Format confidence:**
```typescript
function formatConfidence(confidence: number): string {
  return `${Math.round(confidence * 100)}%`;
}
```

---

## Test Strategy

**Manual tests:**
- Submit command → verify trace section appears
- Copy correlationId → verify clipboard
- Expand trace → verify all fields displayed
- Show raw JSON → verify formatting
- Access trace from history → verify same data

**Unit tests:**
- TraceViewer renders all fields
- CopyButton copies text and shows toast
- RawJsonViewer formats JSON correctly
- formatExecutionTime handles edge cases

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-501 (Command Input Box) — provides response data
- ST-502 (Command Status Display) — container component
- ST-503 (Command History) — optional integration

## Points
2 (trace UI + JSON viewer + copy functionality)

## Priority
P2 (useful for debugging, not blocking core flow)
