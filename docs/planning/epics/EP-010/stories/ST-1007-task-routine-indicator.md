# Story: ST-1007 — Task Card "From Routine" Indicator

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Вижу в списке задач какие из них от рутины, а какие созданы вручную. Могу кликнуть и перейти к рутине."

---

## Description
Add visual indicator on task cards/list showing if task was generated from a routine:
- Badge/icon on task card
- Link to source routine
- API returns routine info with task

---

## In Scope
- Task API response includes `routine` summary (if applicable)
- Task card component shows "From routine: X" badge
- Click on badge navigates to routine detail
- Task detail page shows routine link section

## Out of Scope
- Editing routine from task detail
- "Generate more" from task
- Routine chain visualization

---

## Acceptance Criteria

### AC-1: Task API includes routine info
```
Given task with routineId set
When GET /households/{hid}/tasks/{tid}
Then response includes:
  {
    ...
    "routine": {
      "id": "...",
      "title": "Clean kitchen"
    }
  }
```

### AC-2: Task list includes routine indicator
```
Given tasks list response
Then each task with routineId has routine object
And tasks without routineId have routine = null
```

### AC-3: Task card shows badge
```
Given task list in UI
And task was generated from routine "Clean kitchen"
Then task card shows badge: "From routine" with icon
Or tooltip with routine name
```

### AC-4: Badge is clickable link
```
Given task card with routine badge
When clicking badge
Then navigates to /routines/{routineId}
```

### AC-5: Task detail shows routine section
```
Given task detail page
And task from routine
Then section shows:
  "This task was generated from routine: [Clean kitchen]"
With link to routine
```

### AC-6: Manual tasks show no indicator
```
Given task created manually (routineId = null)
Then no routine badge shown
And no routine section in detail
```

### AC-7: Deleted routine still shows (with label)
```
Given task from routine that was deleted
When viewing task
Then shows: "From routine: Clean kitchen (deleted)"
And link disabled or shows "Routine no longer exists"
```

---

## API Contract Update

```yaml
# Update Task schema
Task:
  properties:
    routine:
      $ref: '#/components/schemas/RoutineSummary'
      nullable: true
      description: Source routine if this task was auto-generated

RoutineSummary:
  type: object
  properties:
    id:
      type: string
      format: uuid
    title:
      type: string
    status:
      type: string
      enum: [ACTIVE, PAUSED, DELETED]
```

---

## UI Components

### Task Card
```
+----------------------------------------+
| Clean the kitchen                      |
| Kitchen - Due today                    |
| [Repeat icon] From: Weekly cleaning    |
+----------------------------------------+
```

### Task Detail
```
+----------------------------------------+
| Clean the kitchen                      |
| Status: Open                           |
| Zone: Kitchen                          |
| Due: 2026-01-28                        |
|                                        |
| Generated from routine                 |
| [Weekly cleaning routine ->]           |
+----------------------------------------+
```

---

## Test Strategy

### Unit Tests
- TaskMapper includes routine summary
- RoutineSummary mapping

### Integration Tests
- Task list response includes routine
- Task detail includes routine
- Null routine for manual tasks

### UI Tests
- Badge renders for routine tasks
- Badge click navigates
- No badge for manual tasks

---

## Points
**2 points**

## Dependencies
- ST-1001 (Routine entity, Task.routineId field)

## Flags
- contract_impact: yes (Task schema extended)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no
