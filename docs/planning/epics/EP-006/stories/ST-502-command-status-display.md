# Story: ST-502 — Command Status Display

## Sources of Truth
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Display command execution results with distinct UI for each status type.

## User Value
As a household member, I want to see clear feedback about what happened with my command, so that I know if it succeeded, needs more info, or was rejected.

---

## In Scope
- Display UI for all 4 response statuses:
  - `executed` — success with task summary
  - `needs_input` — clarification prompt (basic, links to ST-504)
  - `rejected` — error with reason and code
  - `executed_degraded` — success with "limited mode" warning
- Visual distinction (colors, icons, badges)
- "What to do next" action hints
- Display key fields: commandId, correlationId, executionMs
- Retry button for failed commands
- Link to trace (links to ST-505)

## Out of Scope
- Full needs_input form (ST-504 shows details, NEXT has form)
- Command history persistence (ST-503)
- Full trace viewer (ST-505)

---

## Acceptance Criteria

### AC1: Executed status
```gherkin
Given command API returns status "executed"
When result is displayed
Then I see a success indicator (green checkmark)
And message "Command executed successfully"
And result summary shows:
  - Created task: (task title if available)
  - Assigned to: (assignee name if available)
  - Confidence: (percentage)
And "View task" link is shown (if taskId present)
And correlationId is shown (copyable)
```

### AC2: needs_input status
```gherkin
Given command API returns status "needs_input"
When result is displayed
Then I see a question indicator (yellow/orange)
And the question field is displayed prominently
And required fields are listed
And suggestions are shown if present
And message "Please provide more details and try again"
And original input is retained for editing
```

### AC3: rejected status
```gherkin
Given command API returns status "rejected"
When result is displayed
Then I see an error indicator (red X)
And errorCode is shown (e.g., "ZONE_NOT_FOUND")
And reason is shown in plain language
And "Retry" button is available
And correlationId is shown for support reference
```

### AC4: executed_degraded status
```gherkin
Given command API returns status "executed_degraded"
When result is displayed
Then I see a warning indicator (yellow/amber)
And message "Command completed with limitations"
And degradedReason is shown:
  - ai_unavailable: "AI service temporarily unavailable"
  - ai_timeout: "AI service timed out"
  - ai_low_confidence: "Low confidence result"
And fallbackStrategy is shown (e.g., "Used rule-based assignment")
And result summary is shown (same as executed)
And correlationId is shown
```

### AC5: Action buttons
```gherkin
Given any command result is displayed
When I want to take action
Then I see contextual buttons:
  - executed: "View Task", "New Command"
  - needs_input: "Edit & Retry"
  - rejected: "Retry", "New Command"
  - executed_degraded: "View Task", "New Command"
```

### AC6: Transition to new command
```gherkin
Given a command result is displayed
When I click "New Command"
Then the result area clears
And input is focused for new entry
```

---

## UI Specification

**Executed:**
```
+----------------------------------------------------------+
| [✓] Command executed successfully                        |
|                                                          |
| Created task: "Clean the kitchen"                        |
| Assigned to: Alice                                       |
| Confidence: 95%                                          |
|                                                          |
| [View Task]  [New Command]                               |
|                                                          |
| Trace: abc-123-def (copy)  |  42ms                       |
+----------------------------------------------------------+
```

**needs_input:**
```
+----------------------------------------------------------+
| [?] More information needed                               |
|                                                          |
| "Which zone should this task be in?"                     |
|                                                          |
| Required: zoneId                                          |
| Suggestions: Kitchen, Bathroom, Living Room              |
|                                                          |
| [Edit & Retry]                                           |
+----------------------------------------------------------+
```

**Rejected:**
```
+----------------------------------------------------------+
| [✗] Command rejected                                      |
|                                                          |
| Error: ZONE_NOT_FOUND                                    |
| The specified zone does not exist in this household.     |
|                                                          |
| [Retry]  [New Command]                                   |
|                                                          |
| Trace: abc-123-def (copy)                                |
+----------------------------------------------------------+
```

**executed_degraded:**
```
+----------------------------------------------------------+
| [!] Command completed with limitations                    |
|                                                          |
| AI service was unavailable. Used rule-based assignment.  |
|                                                          |
| Created task: "Clean the kitchen"                        |
| Assigned to: You (fallback)                              |
|                                                          |
| [View Task]  [New Command]                               |
|                                                          |
| Trace: abc-123-def (copy)  |  1250ms                     |
+----------------------------------------------------------+
```

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/components/commands/CommandResult.tsx` — create
- `clients/web/src/components/commands/StatusBadge.tsx` — create
- `clients/web/src/components/commands/ExecutedResult.tsx` — create
- `clients/web/src/components/commands/NeedsInputResult.tsx` — create
- `clients/web/src/components/commands/RejectedResult.tsx` — create
- `clients/web/src/components/commands/DegradedResult.tsx` — create

**Response type handling (discriminated union):**
```typescript
function CommandResult({ response }: { response: CommandResponse }) {
  switch (response.status) {
    case 'executed':
      return <ExecutedResult data={response} />;
    case 'needs_input':
      return <NeedsInputResult data={response} />;
    case 'rejected':
      return <RejectedResult data={response} />;
    case 'executed_degraded':
      return <DegradedResult data={response} />;
  }
}
```

**Degraded reason mapping (per OpenAPI CommandDegradedResponse):**
```typescript
// degradedReason is REQUIRED (enum)
const DEGRADED_REASON_LABELS: Record<string, string> = {
  ai_unavailable: 'AI service temporarily unavailable',
  ai_timeout: 'AI service timed out',
  ai_low_confidence: 'Low confidence result',
};

// fallbackStrategy is OPTIONAL (string)
// Only display if present in response
```

---

## Test Strategy

**Manual tests:**
- Submit command that succeeds → verify executed UI
- Submit ambiguous command → verify needs_input UI
- Submit invalid command → verify rejected UI
- (requires backend setup or mock) → verify degraded UI

**Unit tests:**
- CommandResult renders correct component for each status
- ExecutedResult shows task info
- NeedsInputResult shows question and required fields
- RejectedResult shows error code and reason
- DegradedResult shows warning and fallback info

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-501 (Command Input Box) — provides CommandResponse

## Points
3 (4 status variants + action buttons + styling)

## Priority
P1 (essential feedback)
