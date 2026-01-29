# Checklist: ST-1006 — Pause/Resume + Upcoming Instances View

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1006/workpack.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1006-pause-resume-upcoming.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

### AC-1: Pause routine
- [ ] POST `/routines/{id}/pause` returns 200
- [ ] Routine status changes to PAUSED
- [ ] `pausedAt` set to current timestamp

### AC-2: Pause idempotent
- [ ] Already PAUSED routine -> POST pause returns 200
- [ ] Status remains PAUSED
- [ ] No error

### AC-3: Resume routine
- [ ] POST `/routines/{id}/resume` returns 200
- [ ] Routine status changes to ACTIVE
- [ ] `pausedAt` cleared (null)

### AC-4: Resume idempotent
- [ ] Already ACTIVE routine -> POST resume returns 200
- [ ] Status remains ACTIVE
- [ ] No error

### AC-5: Cannot pause/resume DELETED routine
- [ ] DELETED routine -> POST pause returns 400
- [ ] DELETED routine -> POST resume returns 400
- [ ] Error message: "Cannot change status of deleted routine"

### AC-6: Upcoming instances endpoint
- [ ] GET `/routines/{id}/upcoming?days=7` returns array
- [ ] Array has correct number of dates
- [ ] Each item has `scheduledDate`, `exists`, `taskId`, `projectedAssignee`

### AC-7: Upcoming excludes already-created tasks
- [ ] Tasks that exist have `exists: true` and `taskId` set
- [ ] Future dates have `exists: false` and `taskId: null`

### AC-8: Upcoming for PAUSED routine is empty
- [ ] PAUSED routine -> GET upcoming returns empty array `[]`

### AC-9: UI pause button
- [ ] Pause button visible for ACTIVE routines
- [ ] Click shows confirmation dialog
- [ ] Confirm calls pause API
- [ ] Status badge updates to PAUSED

### AC-10: UI resume button
- [ ] Resume button visible for PAUSED routines
- [ ] Click immediately calls resume API (no confirmation)
- [ ] Status badge updates to ACTIVE

### AC-11: Upcoming instances view in detail
- [ ] Routine detail/expand shows "Upcoming Tasks" section
- [ ] Shows next 7 scheduled dates
- [ ] Shows projected assignee for each
- [ ] Shows "Created" badge with link for existing tasks

---

## Technical Checklist

### Backend

#### DTO
- [ ] `UpcomingInstanceDto.java` created
- [ ] Fields: scheduledDate, exists, taskId, projectedAssignee
- [ ] Swagger annotations present

#### Service
- [ ] `RoutineService.pause()` implemented
- [ ] `RoutineService.resume()` implemented
- [ ] `RoutineService.getUpcomingInstances()` implemented
- [ ] Uses RecurrenceRuleParser for date calculation
- [ ] Queries existing tasks by (routineId, scheduledDate)
- [ ] Projects assignee using AssignmentPolicyService (without mutation)

#### Controller
- [ ] `POST /pause` endpoint added
- [ ] `POST /resume` endpoint added
- [ ] `GET /upcoming` endpoint added
- [ ] Membership enforcement on all endpoints
- [ ] Proper error handling (400 for DELETED)

#### Tests
- [ ] `pause_activeRoutine_changesStatus` passes
- [ ] `pause_alreadyPaused_returns200` passes
- [ ] `pause_deletedRoutine_returns400` passes
- [ ] `resume_pausedRoutine_changesStatus` passes
- [ ] `resume_alreadyActive_returns200` passes
- [ ] `resume_deletedRoutine_returns400` passes
- [ ] `upcoming_activeRoutine_returnsInstances` passes
- [ ] `upcoming_pausedRoutine_returnsEmpty` passes
- [ ] `upcoming_marksExistingTasks` passes

### Frontend

#### Types
- [ ] `UpcomingInstance` type added to `api.ts`

#### API Functions
- [ ] `pauseRoutine()` function added
- [ ] `resumeRoutine()` function added
- [ ] `getUpcomingInstances()` function added

#### Components
- [ ] `PauseResumeButton.tsx` created
- [ ] Pause shows confirmation dialog
- [ ] Resume is immediate
- [ ] `UpcomingInstances.tsx` created
- [ ] Shows loading state
- [ ] Shows empty state for PAUSED
- [ ] Shows dates with status

#### Integration
- [ ] RoutineRow updated with PauseResumeButton
- [ ] Routines page shows UpcomingInstances (detail/expand)
- [ ] Actions trigger list refetch

---

## DoD Checklist

### Code Quality (Backend)
- [ ] Spotless formatting applied
- [ ] No compiler warnings
- [ ] Consistent code style

### Code Quality (Frontend)
- [ ] TypeScript strict mode passes
- [ ] ESLint passes
- [ ] No console errors

### Build
- [ ] `./gradlew build` passes
- [ ] `npm run build` passes
- [ ] `npm run typecheck` passes

### Testing
- [ ] All integration tests pass
- [ ] Manual testing: pause routine
- [ ] Manual testing: resume routine
- [ ] Manual testing: view upcoming instances
- [ ] Manual testing: PAUSED shows empty upcoming

### Contract
- [ ] OpenAPI updated with new endpoints
- [ ] OpenAPI updated with UpcomingInstance schema
- [ ] Implementation matches contract

---

## Evidence

| Criterion | Evidence |
|-----------|----------|
| Pause works | POST pause request + status change |
| Resume works | POST resume request + status change |
| Upcoming works | GET upcoming response with dates |
| PAUSED empty | GET upcoming returns [] for PAUSED |
| UI buttons | Screenshot of pause/resume buttons |
| Upcoming view | Screenshot of upcoming instances |
| Tests pass | `./gradlew test` output |
| Build passes | `./gradlew build` + `npm run build` output |
