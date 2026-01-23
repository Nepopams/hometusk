# Story: ST-504 — needs_input Basic Display

## Sources of Truth
- Epic: `docs/planning/epics/EP-006/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-command-ux.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Summary
Display detailed information when a command needs additional input, helping users understand what to provide.

## User Value
As a household member, when my command is ambiguous or incomplete, I want clear guidance on what information is missing, so that I can retype my command with the right details.

---

## In Scope
Per OpenAPI `CommandNeedsInputResponse`:
- Display `question` field prominently (REQUIRED)
- List `requiredFields` with labels (REQUIRED, array of strings)
- Display `suggestions` for each field (OPTIONAL, object)
- Display `policyName` (OPTIONAL, string — which guardrail triggered)
- Clear instruction: "Please provide the missing information"
- Retain original command payload for reference
- Copy correlationId for support

## Out of Scope
- Form-based input continuation via `POST /commands/{commandId}/continue`
  (endpoint exists in OpenAPI with `ContinueCommandRequest`, deferred to NEXT)
- Auto-population of suggested values
- Inline field editing
- Command continuation flow (NEXT scope)

---

## Acceptance Criteria

### AC1: Question displayed
```gherkin
Given command API returns status "needs_input"
And response contains question: "Which zone should this task be in?"
When result is displayed
Then question is shown prominently in a callout box
And icon indicates "question" state
```

### AC2: Required fields listed
```gherkin
Given response contains requiredFields: ["zoneId", "deadline"]
When result is displayed
Then I see a list:
  - "Zone (required)"
  - "Deadline (required)"
And each field has a human-readable label
```

### AC3: Suggestions displayed
```gherkin
Given response contains suggestions: { zoneId: ["Kitchen", "Bathroom"] }
When result is displayed
Then suggestions are shown for zoneId:
  - "Suggestions: Kitchen, Bathroom"
And I understand these are valid options
```

### AC4: Policy name shown
```gherkin
Given response contains policyName: "ZONE_REQUIRED"
When result is displayed
Then policy name is shown in a "technical details" section
And tooltip explains: "This rule requires a zone for task creation"
```

### AC5: Original input retained
```gherkin
Given I submitted "Clean tomorrow"
And response is needs_input
When result is displayed
Then original input "Clean tomorrow" is shown as reference
And input field contains the same text for editing
```

### AC6: Guidance message
```gherkin
Given needs_input result is displayed
When I read the UI
Then I see clear instruction:
  "Please retype your command with more details, such as: [suggestions]"
And example is shown: "e.g., 'Clean the kitchen tomorrow at 6pm'"
```

### AC7: No form fields (NOW scope)
```gherkin
Given needs_input result is displayed
When I look for input fields
Then I do NOT see form fields for each required field
And I do NOT see a "Submit Additional Info" button
(These are NEXT scope)
```

---

## UI Specification

**needs_input display:**
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
| Your command: "Clean tomorrow"                           |
|                                                          |
| Tip: Try adding more details, e.g.                       |
| "Clean the kitchen tomorrow at 6pm"                      |
|                                                          |
| [Edit & Retry]                                           |
|                                                          |
| Technical: Policy ZONE_REQUIRED | Trace: abc-123         |
+----------------------------------------------------------+
```

---

## Technical Notes

**Files to create/modify:**
- `clients/web/src/components/commands/NeedsInputResult.tsx` — enhance from ST-502
- `clients/web/src/components/commands/RequiredFieldsList.tsx` — create
- `clients/web/src/components/commands/FieldSuggestions.tsx` — create
- `clients/web/src/lib/fieldLabels.ts` — human-readable field names

**Field label mapping:**
```typescript
const FIELD_LABELS: Record<string, string> = {
  zoneId: 'Zone',
  deadline: 'Deadline',
  assigneeId: 'Assignee',
  title: 'Task title',
  description: 'Description',
};

function getFieldLabel(field: string): string {
  return FIELD_LABELS[field] || field;
}
```

**Policy label mapping:**
```typescript
const POLICY_LABELS: Record<string, string> = {
  ZONE_REQUIRED: 'Zone is required for task creation',
  DEADLINE_REQUIRED: 'Deadline must be specified',
  ASSIGNEE_AMBIGUOUS: 'Multiple possible assignees found',
};
```

---

## Test Strategy

**Manual tests:**
- Submit ambiguous command → verify needs_input UI
- Verify question is displayed prominently
- Verify required fields are listed with labels
- Verify suggestions are shown
- Verify original input is retained
- Verify "Edit & Retry" focuses input

**Unit tests:**
- NeedsInputResult renders question
- RequiredFieldsList renders field labels
- FieldSuggestions renders suggestion pills
- Policy name displays with tooltip

---

## Flags

| Flag | Value |
|------|-------|
| contract_impact | no |
| adr_needed | no |
| security_sensitive | no |

---

## Dependencies
- ST-501 (Command Input Box) — provides input retention
- ST-502 (Command Status Display) — base component

## Points
2 (enhanced display + field mapping)

## Priority
P1 (core to understanding command failures)
