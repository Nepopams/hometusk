# Codex PLAN Prompt: ST-1006 — Pause/Resume + Upcoming Instances View

## Mode: PLAN ONLY (Read-Only)

**CRITICAL:** This is the PLAN phase. You MAY NOT edit files. Read-only commands only.

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1006/workpack.md
docs/planning/epics/EP-010/stories/ST-1006-pause-resume-upcoming.md
docs/_governance/dod.md
```

---

## Allowed Commands (whitelist)

- `ls`, `find` — directory exploration
- `cat`, `head`, `tail` — file reading
- `rg`, `grep` — content search
- `git status`, `git diff` — read-only inspection

**Forbidden:** edit, write, move, delete, git commit, network, package install

---

## Task

Explore the codebase to verify the implementation plan for ST-1006 (Pause/Resume + Upcoming Instances).

### Verify Backend Components

1. **RoutineController structure:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/routines/controller/RoutineController.java
   ```
   Verify: existing endpoints, pattern for adding new endpoints

2. **RoutineService methods:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java
   ```
   Check: existing methods, pattern for state transitions

3. **Routine entity:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/routines/domain/Routine.java
   ```
   Verify: status field, pausedAt field, status enum

4. **RecurrenceRuleParser for upcoming calculation:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java
   ```
   Verify: method to get occurrences in date range

5. **AssignmentPolicyService for projected assignee:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/routines/service/AssignmentPolicyService.java
   ```
   Verify: method to determine assignee (can be used for projection)

6. **TaskRepository for existing tasks check:**
   ```bash
   rg "findByRoutine" services/backend/src/main/java/com/hometusk/tasks/repository/
   ```
   Check: query methods for tasks by routine

7. **UserSummaryDto for response:**
   ```bash
   cat services/backend/src/main/java/com/hometusk/tasks/dto/UserSummaryDto.java
   ```
   Verify: structure for projectedAssignee field

### Verify Frontend Components (from ST-1005)

1. **Routine types exist:**
   ```bash
   rg "Routine" clients/web/src/types/api.ts
   ```
   Verify: Routine, RoutineStatus types added by ST-1005

2. **API functions exist:**
   ```bash
   rg "routine" clients/web/src/lib/api.ts
   ```
   Verify: getRoutines, getRoutine, etc. added by ST-1005

3. **Routines page exists:**
   ```bash
   ls clients/web/src/routes/Routines.tsx
   cat clients/web/src/routes/Routines.tsx | head -50
   ```
   Verify: basic page structure from ST-1005

4. **RoutineRow component exists:**
   ```bash
   ls clients/web/src/components/routines/
   cat clients/web/src/components/routines/RoutineRow.tsx | head -30
   ```
   Verify: row component for adding buttons

### Check OpenAPI Contract

```bash
cat docs/contracts/http/routines.openapi.yaml
```

Check: Are pause/resume/upcoming endpoints already defined or need to be added?

### Check for Existing Implementation

```bash
rg "pause|resume|upcoming" services/backend/src/main/java/com/hometusk/routines/ -l
rg "pauseRoutine|resumeRoutine|getUpcomingInstances" clients/web/src/lib/api.ts
```

Report if any partial implementation exists.

---

## Output Required

Provide a structured report:

### 1. Backend Components Found
- [ ] RoutineController ready for new endpoints
- [ ] RoutineService structure understood
- [ ] Routine.status and Routine.pausedAt fields exist
- [ ] RecurrenceRuleParser has date range method
- [ ] AssignmentPolicyService can project assignee
- [ ] TaskRepository can query by routine

### 2. Frontend Components Found (from ST-1005)
- [ ] Routine types in api.ts
- [ ] API functions in api.ts
- [ ] Routines.tsx page exists
- [ ] RoutineRow.tsx component exists

### 3. Backend Files to Create
- [ ] `UpcomingInstanceDto.java`
- [ ] `RoutineLifecycleControllerTest.java` (or add to existing test)

### 4. Backend Files to Modify
- [ ] `RoutineController.java` — add pause/resume/upcoming endpoints
- [ ] `RoutineService.java` — add pause/resume/getUpcoming methods
- [ ] `routines.openapi.yaml` — add new endpoints and schemas

### 5. Frontend Files to Create
- [ ] `PauseResumeButton.tsx`
- [ ] `UpcomingInstances.tsx`

### 6. Frontend Files to Modify
- [ ] `api.ts` — add pauseRoutine, resumeRoutine, getUpcomingInstances
- [ ] `api.ts` types — add UpcomingInstance
- [ ] `RoutineRow.tsx` — add pause/resume button
- [ ] `Routines.tsx` — add upcoming instances view

### 7. Questions
1. **AssignmentPolicyService projection:** Can we call determineAssignee without mutating round-robin state?
   - If not, how to project assignee for upcoming view?

2. **Existing tasks query:** Does TaskRepository have method to find tasks by (routineId, scheduledDate IN [...])?

3. **OpenAPI update:** Are pause/resume/upcoming endpoints already in contract or need to add?

### 8. Discrepancies
Report any differences from workpack expectations.

---

## STOP-THE-LINE Rule

If you discover:
- ST-1005 not completed (Routines page missing)
- Backend components significantly different
- Missing methods that require major refactoring

**STOP** and report the issue. Do NOT proceed with assumptions.

---

## Dependencies Check

This story depends on:
- ST-1003 (Scheduler Service) — verify RoutineSchedulerService exists
- ST-1005 (Routines Page) — verify basic UI exists

```bash
ls services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java
ls clients/web/src/routes/Routines.tsx
```

---

## Next Step

After PLAN approval, prompt-apply.md will be generated for implementation.
