# Codex PLAN Prompt: ST-1003 — RoutineSchedulerService + Idempotent Generation

## Mode: PLAN ONLY

**CRITICAL:** This is a READ-ONLY exploration phase.
- NO file edits
- NO file writes
- NO git commits
- NO network access
- NO package installation

---

## Anchors (read first)

```
CLAUDE.md (project root)
docs/planning/workpacks/ST-1003/workpack.md
docs/planning/epics/EP-010/stories/ST-1003-scheduler-service.md
docs/planning/epics/EP-010/epic.md
docs/adr/013-routine-scheduler-design.md
docs/_governance/dod.md
```

---

## Task Summary

Explore the codebase to prepare for implementing:
1. Add `routineId` and `scheduledDate` fields to Task entity
2. Create DB migration with unique partial index for idempotency
3. Create `RoutineSchedulerService` that generates task instances
4. Configure `@Scheduled` job for periodic execution
5. Unit and integration tests

**Key constraint:** NO backfill for past dates. Only generate for [today, today+windowDays].

---

## Allowed Commands (whitelist)

```bash
ls, find                    # List files/directories
cat                         # Read file contents
rg, grep                    # Search in files
sed -n, head, tail          # View portions of files
git status, git diff        # Read-only git inspection
```

---

## Forbidden Commands

```bash
# ANY command that modifies files or state:
edit, write, mv, rm, mkdir, touch
git add, git commit, git push
npm, gradle (except read-only tasks)
curl, wget (network access)
```

---

## Exploration Tasks

### 1. Examine Task entity structure
```bash
cat services/backend/src/main/java/com/hometusk/tasks/domain/Task.java
```

**Questions to answer:**
- What fields exist?
- Is there a constructor pattern for creating tasks?
- How is deadline handled?
- What imports are needed for new fields?

### 2. Find existing DB migrations pattern
```bash
ls services/backend/src/main/resources/db/migration/
cat services/backend/src/main/resources/db/migration/V*.sql | head -50
```

**Questions to answer:**
- What is the migration version numbering scheme?
- What is the next version number?
- How are foreign keys defined?
- How are partial indexes created?

### 3. Examine TaskRepository
```bash
cat services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java
```

**Questions to answer:**
- What methods exist?
- How to add existence check for routineId+scheduledDate?
- JPA method naming convention?

### 4. Examine RoutineRepository for finding active routines
```bash
cat services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java
```

**Questions to answer:**
- Is there a method to find by status?
- How to get all ACTIVE routines?

### 5. Check RecurrenceRuleParser usage
```bash
cat services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java
```

**Questions to answer:**
- What is the exact signature for getOccurrencesInRange?
- How to use it for generating dates?

### 6. Find @Scheduled examples in codebase
```bash
rg "@Scheduled|@EnableScheduling" services/backend/src/main/java --type java
```

**Questions to answer:**
- Is @EnableScheduling already configured?
- Are there existing scheduled jobs to reference?
- How is cron configured in application.yml?

### 7. Check application.yml structure
```bash
cat services/backend/src/main/resources/application.yml
```

**Questions to answer:**
- Where to add scheduler config?
- What is the config prefix pattern?

### 8. Examine TaskService for task creation pattern
```bash
cat services/backend/src/main/java/com/hometusk/tasks/service/TaskService.java
```

**Questions to answer:**
- Is there a createTask method?
- Should scheduler use TaskService or create directly?
- What notifications/events are triggered on task creation?

### 9. Check Routine entity for fields to inherit
```bash
cat services/backend/src/main/java/com/hometusk/routines/domain/Routine.java
```

**Questions to answer:**
- What fields should be copied to Task?
- How to get zone from routine?
- How to get generationWindowDays?

### 10. Find existing scheduler/batch patterns (if any)
```bash
rg "scheduler|batch|cron" services/backend/src/main/java --type java -l
```

**Questions to answer:**
- Are there existing patterns to follow?
- Any scheduler lock mechanism?

---

## Expected Output Format

After exploration, produce a structured report:

```markdown
## PLAN Phase Findings: ST-1003

### 1. Task Entity Structure
- Existing fields: [list]
- Constructor pattern: [description]
- Deadline handling: [description]

### 2. DB Migration Pattern
- Version scheme: [e.g., V1, V2]
- Next version: [number]
- FK syntax: [example]
- Partial index syntax: [example]

### 3. TaskRepository
- Existing methods: [list]
- Suggested method: [signature]

### 4. RoutineRepository
- Method for active routines: [exists/needs adding]

### 5. RecurrenceRuleParser
- Signature: [exact signature]
- Usage example: [code]

### 6. @Scheduled Pattern
- @EnableScheduling: [yes/no, location]
- Existing jobs: [yes/no]
- Cron config pattern: [example]

### 7. Application Config
- Config prefix: [e.g., hometusk.*]
- Where to add scheduler config: [section]

### 8. Task Creation Pattern
- Use TaskService: [yes/no]
- Notifications triggered: [yes/no]
- Events published: [yes/no]

### 9. Routine Fields to Inherit
- title, description, zone: [how to access]
- generationWindowDays: [how to access]

### 10. Existing Scheduler Patterns
- Lock mechanism: [yes/no]
- Batch patterns: [description]

### 11. Files to Create/Modify
1. [file path] - [action]
2. [file path] - [action]
...

### 12. Risks/Questions for Human Gate
- [Any unexpected findings]
- [Any clarifications needed]

### 13. Ready for APPLY?
- [ ] Yes, proceed with implementation
- [ ] No, need clarification on: [items]
```

---

## STOP-THE-LINE Rule

If you discover ANY of the following, STOP and report:
- RoutineSchedulerService already exists
- Task already has routineId field
- Existing scheduler infrastructure conflicts with plan
- Missing dependencies or unexpected codebase state

---

## Verification (at end of PLAN phase)

Confirm these items before completing:
- [ ] Task entity structure understood
- [ ] DB migration pattern identified
- [ ] TaskRepository patterns identified
- [ ] RoutineRepository patterns identified
- [ ] RecurrenceRuleParser verified
- [ ] @Scheduled pattern identified
- [ ] Application config pattern identified
- [ ] Task creation flow understood
- [ ] No conflicting existing code found
- [ ] Ready for APPLY phase
