# Story: ST-1005 — Routines Page (List + Create/Edit Form)

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Хочу видеть все свои рутины в одном месте, создавать новые и редактировать существующие."

---

## Description
Implement web UI for routine management:
- Routines list page with status indicators
- Create routine form with rule builder (presets)
- Edit routine form
- Delete confirmation

---

## In Scope
- `/routines` page in web app
- List view: routine name, zone, frequency, status, assignee policy
- Create form:
  - Title (required)
  - Description (optional)
  - Zone selector (optional)
  - Frequency presets: Daily, Weekly (day picker), Monthly (day picker), Every N days
  - Assignment policy: Fixed (user selector), Round-robin, Manual
- Edit form: same as create, pre-populated
- Delete button with confirmation
- Navigation from sidebar

## Out of Scope
- Pause/resume buttons (ST-1006)
- Upcoming instances view (ST-1006)
- Custom RRULE input (advanced)
- Drag-and-drop ordering
- Bulk operations

---

## Acceptance Criteria

### AC-1: Routines page accessible
```
Given authenticated user in household
When navigating to /routines
Then routines list page displayed
And navigation shows "Routines" as active
```

### AC-2: List shows active routines
```
Given household with 3 routines (2 active, 1 paused)
When viewing routines page
Then 3 routines displayed
And each shows: title, zone, frequency text, status badge, policy icon
```

### AC-3: Create routine form opens
```
Given routines page
When clicking "Create Routine" button
Then create form displayed
With empty fields and preset selectors
```

### AC-4: Create routine - DAILY
```
Given create form
When filling:
  - title = "Помыть посуду"
  - frequency = "Daily"
  - policy = "Round-robin"
And clicking Save
Then routine created
And redirected to routines list
And success toast shown
```

### AC-5: Create routine - WEEKLY
```
Given create form
When selecting frequency = "Weekly"
Then day-of-week checkboxes shown
And user can select multiple days
```

### AC-6: Create routine - MONTHLY
```
Given create form
When selecting frequency = "Monthly"
Then day-of-month selector shown (1-31)
```

### AC-7: Create routine - Every N days
```
Given create form
When selecting frequency = "Every N days"
Then interval input shown
And validates minimum 1
```

### AC-8: Policy FIXED requires user selection
```
Given create form
When selecting policy = "Fixed"
Then user selector dropdown appears
And user must be selected before save
```

### AC-9: Edit routine form pre-populated
```
Given existing routine "Clean kitchen"
When clicking edit
Then form opened with:
  - title = "Clean kitchen"
  - frequency matching routine's rule
  - policy matching routine's setting
```

### AC-10: Delete routine with confirmation
```
Given existing routine
When clicking delete
Then confirmation dialog shown: "Delete routine? Pending tasks will remain."
When confirming
Then routine deleted (soft)
And removed from list
```

### AC-11: Validation errors shown
```
Given create form
When saving without title
Then error displayed: "Title is required"
And form not submitted
```

### AC-12: Empty state
```
Given household with no routines
When viewing routines page
Then empty state shown: "No routines yet. Create your first routine to automate recurring tasks."
```

---

## UI Mockup

### Routines List
```
+------------------------------------------------------------------+
| Routines                                    [+ Create Routine]   |
+------------------------------------------------------------------+
| Clean kitchen      | Kitchen | Daily        | Round-robin | ACTIVE  |
| Take out trash     | —       | Mon, Thu     | Fixed: Alex | ACTIVE  |
| Monthly deep clean | All     | 1st of month | Manual      | PAUSED  |
+------------------------------------------------------------------+
```

### Create/Edit Form
```
+----------------------------------+
| Create Routine                   |
+----------------------------------+
| Title*: [________________]       |
| Description: [________________]  |
| Zone: [Kitchen v]                |
|                                  |
| Frequency:                       |
| ( ) Daily                        |
| ( ) Weekly: [M][T][W][T][F][S][S]|
| ( ) Monthly: [15 v] day          |
| ( ) Every [3] days               |
|                                  |
| Assignment:                      |
| ( ) Round-robin (fair rotation)  |
| ( ) Fixed: [Alex v]              |
| ( ) Manual (assign later)        |
|                                  |
| [Cancel]              [Save]     |
+----------------------------------+
```

---

## Test Strategy

### Unit Tests (React)
- `RoutineForm.test.tsx`:
  - `renders_emptyForm_forCreate`
  - `renders_prePopulated_forEdit`
  - `validation_titleRequired`
  - `weeklyFrequency_showsDayPicker`
  - `fixedPolicy_showsUserSelector`

### Integration Tests (E2E)
- `routines.spec.ts`:
  - `createRoutine_dailyRoundRobin_succeeds`
  - `editRoutine_changesTitle_succeeds`
  - `deleteRoutine_removesFromList`
  - `emptyState_showsMessage`

---

## Points
**5 points**

## Dependencies
- ST-1001 (Routine CRUD endpoints)

## Flags
- contract_impact: no (uses existing endpoints)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no
