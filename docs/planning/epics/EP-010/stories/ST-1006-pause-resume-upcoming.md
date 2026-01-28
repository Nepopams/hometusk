# Story: ST-1006 — Pause/Resume + Upcoming Instances View

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Уезжаю в отпуск — ставлю рутину на паузу. Вернулся — возобновил. Хочу видеть какие задачи запланированы на неделю."

---

## Description
Implement routine lifecycle operations and upcoming instances preview:
- Pause endpoint: stops generation without deleting
- Resume endpoint: resumes generation
- Upcoming instances endpoint: preview of next N scheduled tasks
- UI controls for pause/resume
- Upcoming instances view in routine detail

---

## In Scope
- `POST /routines/{id}/pause` endpoint
- `POST /routines/{id}/resume` endpoint
- `GET /routines/{id}/upcoming` endpoint (preview, not actual tasks)
- Pause/Resume buttons in routine list and detail
- Upcoming instances section in routine detail view
- Status badges (ACTIVE/PAUSED)

## Out of Scope
- Delete pending instances on pause (keep simple)
- Vacation mode (bulk pause all)
- Calendar view of upcoming
- Push notifications for pause reminders

---

## Acceptance Criteria

### AC-1: Pause routine
```
Given ACTIVE routine
When POST /households/{hid}/routines/{rid}/pause
Then response 200 with updated routine
And routine.status = PAUSED
And routine.pausedAt = now
```

### AC-2: Pause idempotent
```
Given already PAUSED routine
When POST .../pause
Then response 200 (no error)
And status remains PAUSED
```

### AC-3: Resume routine
```
Given PAUSED routine
When POST /households/{hid}/routines/{rid}/resume
Then response 200 with updated routine
And routine.status = ACTIVE
And routine.pausedAt = null
```

### AC-4: Resume idempotent
```
Given already ACTIVE routine
When POST .../resume
Then response 200 (no error)
And status remains ACTIVE
```

### AC-5: Cannot pause/resume DELETED routine
```
Given DELETED routine
When POST .../pause or .../resume
Then response 400 with error "Cannot change status of deleted routine"
```

### AC-6: Upcoming instances endpoint
```
Given ACTIVE routine with DAILY rule
And today = 2026-01-28
When GET /households/{hid}/routines/{rid}/upcoming?days=7
Then response 200 with array of 7 upcoming instances:
  [
    { "scheduledDate": "2026-01-28", "assignee": {...} },
    { "scheduledDate": "2026-01-29", "assignee": {...} },
    ...
  ]
And includes projected assignee (for round-robin preview)
```

### AC-7: Upcoming excludes already-created tasks
```
Given tasks already exist for 2026-01-28, 2026-01-29
When GET .../upcoming?days=7
Then response shows:
  - 2026-01-28: { "exists": true, "taskId": "..." }
  - 2026-01-29: { "exists": true, "taskId": "..." }
  - 2026-01-30 - 2026-02-03: { "exists": false, "assignee": {...} }
```

### AC-8: Upcoming for PAUSED routine is empty
```
Given PAUSED routine
When GET .../upcoming
Then response 200 with empty array
```

### AC-9: UI pause button
```
Given routine list with ACTIVE routine
When clicking pause icon
Then confirmation: "Pause routine? No new tasks will be generated."
When confirming
Then status changes to PAUSED
And badge updates
```

### AC-10: UI resume button
```
Given routine list with PAUSED routine
When clicking resume icon
Then routine immediately ACTIVE
And scheduler will generate on next run
```

### AC-11: Upcoming instances view in detail
```
Given routine detail page
Then "Upcoming Tasks" section shows:
  - Next 7 scheduled dates
  - Projected assignee for each
  - "Already created" indicator for existing tasks
```

---

## API Contract Addition

```yaml
/households/{householdId}/routines/{routineId}/pause:
  post:
    operationId: pauseRoutine
    summary: Pause a routine
    tags: [Routines]
    responses:
      200: Routine paused
      400: Cannot pause (deleted or invalid state)
      403: Not a member
      404: Routine not found

/households/{householdId}/routines/{routineId}/resume:
  post:
    operationId: resumeRoutine
    summary: Resume a paused routine
    tags: [Routines]

/households/{householdId}/routines/{routineId}/upcoming:
  get:
    operationId: getUpcomingInstances
    summary: Preview upcoming task instances
    tags: [Routines]
    parameters:
      - name: days
        in: query
        schema:
          type: integer
          default: 7
          maximum: 30
    responses:
      200: List of upcoming instances

UpcomingInstance:
  type: object
  properties:
    scheduledDate:
      type: string
      format: date
    exists:
      type: boolean
    taskId:
      type: string
      format: uuid
      nullable: true
    projectedAssignee:
      $ref: '#/components/schemas/UserSummary'
```

---

## Test Strategy

### Unit Tests
- `RoutineService.pause()` — state transition
- `RoutineService.resume()` — state transition
- `RoutineService.getUpcoming()` — date calculation + existing check

### Integration Tests
- `pause_activeRoutine_stopsGeneration`
- `resume_pausedRoutine_resumesGeneration`
- `upcoming_showsProjectedAssignments`
- `upcoming_marksExistingTasks`

---

## Points
**3 points**

## Dependencies
- ST-1003 (Scheduler service to verify pause behavior)
- ST-1005 (Routines page to add buttons)

## Flags
- contract_impact: yes (new endpoints)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no
