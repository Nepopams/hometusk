# EP-006 Test Playbook: Command Box UI

## Overview
Manual test cases and regression checklist for EP-006 (Command Box UI).

---

## Prerequisites

1. Web client running: `cd clients/web && npm run dev`
2. Backend running with valid API
3. Authenticated user with household membership
4. Browser DevTools open (Console + Network + Application tabs)

---

## Test Environment Setup

```bash
# Start web client
cd clients/web
npm run dev

# Open browser at http://localhost:5173
# Login and navigate to household
```

---

## ST-501: Command Input Box

### TC-501-01: Mode Toggle
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to household page | CommandInput visible |
| 2 | Observe default mode | "Create Task" button active |
| 3 | Click "Complete Task" | Mode switches, form changes |
| 4 | Click "Create Task" | Mode switches back |

### TC-501-02: Create Task Form
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Select "Create Task" mode | Form shows title, description, zone, assignee, deadline |
| 2 | Leave title empty, submit | Validation error "Title is required" |
| 3 | Enter title > 500 chars | Validation error on length |
| 4 | Enter valid title | No validation error |
| 5 | Select zone from dropdown | Zone selected |
| 6 | Select assignee from dropdown | Assignee selected |
| 7 | Set deadline in past | Validation error "Deadline must be in the future" |
| 8 | Set deadline in future | No validation error |
| 9 | Click Submit | Loading state, then response |

### TC-501-03: Complete Task Form
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Select "Complete Task" mode | Form shows task selector |
| 2 | Observe task list | Only open tasks shown |
| 3 | Select task | Task selected |
| 4 | Click Submit | Loading state, then response |

### TC-501-04: Idempotency Key
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Network tab | Ready to inspect |
| 2 | Submit command | Request has `Idempotency-Key` header |
| 3 | Check key format | UUID format |
| 4 | Submit same form again (success) | New idempotency key generated |
| 5 | Trigger 409 conflict | New idempotency key generated |

### TC-501-05: Correlation ID
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit command | Request has `X-Correlation-ID` header |
| 2 | Check response | Response contains matching correlationId |

---

## ST-502: Command Status Display

### TC-502-01: Executed Status
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit valid create_task | Response status = "executed" |
| 2 | Observe display | Green StatusBadge "Command executed successfully" |
| 3 | Check result info | Task ID shown (if created) |
| 4 | Check buttons | "View Task" (if taskId), "New Command" visible |
| 5 | Click "View Task" | Alert shows taskId |
| 6 | Click "New Command" | Form resets, result clears |

### TC-502-02: needs_input Status
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit ambiguous command (mock) | Response status = "needs_input" |
| 2 | Observe display | Blue/info StatusBadge "More information needed" |
| 3 | Check question | Question displayed in callout |
| 4 | Check required fields | Listed with human labels |
| 5 | Check buttons | "Edit & Retry" visible |
| 6 | Click "Edit & Retry" | Result clears, form retains values, focus on input |

### TC-502-03: Rejected Status
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit invalid command (mock) | Response status = "rejected" |
| 2 | Observe display | Red StatusBadge "Command rejected" |
| 3 | Check error | errorCode and reason displayed |
| 4 | Check buttons | "Retry", "New Command" visible |

### TC-502-04: Executed Degraded Status
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit when AI unavailable (mock) | Response status = "executed_degraded" |
| 2 | Observe display | Yellow/warning StatusBadge "Command completed with limitations" |
| 3 | Check degraded reason | Human-readable label shown |
| 4 | Check fallback strategy | Shown if present |

---

## ST-503: Command History

### TC-503-01: History Persistence
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit command | Command added to history |
| 2 | Check localStorage | Key `hometusk:commandHistory:{householdId}` exists |
| 3 | Refresh page | History persists |

### TC-503-02: History Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View history section | "Recent Commands" title with count |
| 2 | Check entry | displayText, StatusBadge, relative timestamp |
| 3 | Check order | Newest first |

### TC-503-03: History Limit
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit 51 commands | |
| 2 | Check localStorage | Max 50 entries |
| 3 | Oldest entry | Pruned |

### TC-503-04: History Expand
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click history entry | Details expand |
| 2 | Check details | Full timestamp, commandId, correlationId, request/response JSON |
| 3 | Click "Copy" on correlationId | Copied to clipboard |
| 4 | Click entry again | Collapses |

### TC-503-05: Clear History
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Clear" button | Confirmation prompt |
| 2 | Cancel | History unchanged |
| 3 | Confirm | History cleared |
| 4 | Check display | Empty state "No commands yet" |

### TC-503-06: Household Scoping
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit commands in household A | History shows commands |
| 2 | Switch to household B | Different history (or empty) |
| 3 | Return to household A | Original history visible |

---

## ST-504: needs_input Basic Display

### TC-504-01: Question Callout
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Trigger needs_input response | Question shown in callout box |
| 2 | Check icon | Question mark icon visible |

### TC-504-02: Required Fields Labels
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Check required fields | Human-readable labels (not raw field names) |
| 2 | Example: zoneId | Shows as "Zone (required)" |

### TC-504-03: Suggestions Per Field
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Response has suggestions | Suggestions grouped per field |
| 2 | Array suggestions | Comma-separated |

### TC-504-04: Original Input
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Check "Your command" section | Original request summary shown |

### TC-504-05: Guidance Tip
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Check tip section | "Please retype your command with more details" |
| 2 | Check example | Example command shown |

### TC-504-06: Policy Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Response has policyName | Policy shown with tooltip |
| 2 | Hover on policy | Explanation appears |

---

## ST-505: Minimal Trace Viewer

### TC-505-01: Collapsed Trace
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit any command | Trace row visible below result |
| 2 | Check content | correlationId (truncated), executionMs, "View Details" |

### TC-505-02: Expanded Trace
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "View Details" | Trace expands |
| 2 | Check fields | commandId, correlationId, initiatorId, status, time |
| 3 | Check result section | taskId, assigneeId, confidence (if applicable) |
| 4 | Check degraded section | reason, fallback (if degraded) |
| 5 | Click "Hide Details" | Collapses |

### TC-505-03: Copy Functionality
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Copy" on correlationId | Button shows "Copied" |
| 2 | Check clipboard | correlationId copied |
| 3 | Wait 2 seconds | Button resets to "Copy" |

### TC-505-04: Raw JSON Viewer
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Expand trace | "Show Raw Response" visible |
| 2 | Click "Show Raw Response" | JSON displayed, formatted |
| 3 | Click "Copy JSON" | JSON copied to clipboard |
| 4 | Click "Hide Raw Response" | JSON hidden |

### TC-505-05: Trace from History
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Expand history entry | "View Trace" button visible |
| 2 | Click "View Trace" | TraceInfo renders with response data |

---

## Regression Checklist

Run these tests after any changes to EP-006 components:

### Critical Path
- [ ] TC-501-02: Create task form submission
- [ ] TC-501-03: Complete task form submission
- [ ] TC-502-01: Executed status display
- [ ] TC-503-01: History persistence
- [ ] TC-503-06: Household scoping

### Status Display
- [ ] TC-502-01: Executed
- [ ] TC-502-02: needs_input
- [ ] TC-502-03: Rejected
- [ ] TC-502-04: Executed degraded

### History
- [ ] TC-503-02: Display
- [ ] TC-503-04: Expand/collapse
- [ ] TC-503-05: Clear

### Trace
- [ ] TC-505-01: Collapsed view
- [ ] TC-505-02: Expanded view
- [ ] TC-505-03: Copy

### Integration
- [ ] Build passes: `npm run build`
- [ ] Lint passes: `npm run lint`
- [ ] No console errors in browser

---

## Mock Response Examples

### executed
```json
{
  "commandId": "cmd-123",
  "correlationId": "corr-456",
  "status": "executed",
  "result": {
    "taskId": "task-789",
    "assigneeId": "user-abc",
    "decisionConfidence": 0.95
  },
  "executionMs": 150,
  "initiatorId": "user-123"
}
```

### needs_input
```json
{
  "commandId": "cmd-123",
  "correlationId": "corr-456",
  "status": "needs_input",
  "question": "Which zone should this task be in?",
  "requiredFields": ["zoneId", "deadline"],
  "suggestions": {
    "zoneId": ["Kitchen", "Bathroom"],
    "deadline": ["Today", "Tomorrow"]
  },
  "policyName": "ZONE_REQUIRED",
  "executionMs": 50,
  "initiatorId": "user-123"
}
```

### rejected
```json
{
  "commandId": "cmd-123",
  "correlationId": "corr-456",
  "status": "rejected",
  "errorCode": "INVALID_TASK",
  "reason": "Task not found",
  "executionMs": 30,
  "initiatorId": "user-123"
}
```

### executed_degraded
```json
{
  "commandId": "cmd-123",
  "correlationId": "corr-456",
  "status": "executed_degraded",
  "result": {
    "taskId": "task-789",
    "decisionConfidence": 0.6
  },
  "degradedReason": "ai_timeout",
  "fallbackStrategy": "Used default assignment",
  "executionMs": 5000,
  "initiatorId": "user-123"
}
```

---

## Sign-off

| Tester | Date | Status |
|--------|------|--------|
| | | [ ] All tests passed |
