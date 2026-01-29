# Codex PLAN Prompt: ST-1007 — Task Card "From Routine" Indicator

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
docs/planning/workpacks/ST-1007/workpack.md
docs/planning/epics/EP-010/stories/ST-1007-task-routine-indicator.md
docs/_governance/dod.md
```

---

## Task Summary

Explore the codebase to prepare for implementing:
1. `RoutineSummaryDto` for routine info in task responses
2. Add `routine` field to `TaskDto`
3. Add `routine` field to `TaskDetailDto`
4. Update OpenAPI contract
5. Unit and integration tests

**Key constraint:** Handle deleted routines (show status=DELETED).

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

### 1. Examine TaskDto structure
```bash
cat services/backend/src/main/java/com/hometusk/tasks/dto/TaskDto.java
```

**Questions to answer:**
- Current record fields?
- How is `from(Task)` implemented?
- What imports are needed for Routine?

### 2. Examine TaskDetailDto structure
```bash
cat services/backend/src/main/java/com/hometusk/tasks/dto/TaskDetailDto.java
```

**Questions to answer:**
- Current record fields?
- How is `from(Task, List<ShoppingItem>)` implemented?

### 3. Check Task entity for routine relationship
```bash
cat services/backend/src/main/java/com/hometusk/tasks/domain/Task.java | grep -A5 "routine"
```

**Questions to answer:**
- Is routine LAZY or EAGER fetch?
- How to get routine from task?
- How to get routine status?

### 4. Check Routine entity
```bash
cat services/backend/src/main/java/com/hometusk/routines/domain/Routine.java | grep -A5 "getStatus\|getTitle"
```

**Questions to answer:**
- RoutineStatus enum values?
- How to check if deleted?

### 5. Examine OpenAPI contract structure
```bash
cat docs/contracts/http/commands.openapi.yaml | grep -A20 "Task:"
```

**Questions to answer:**
- Where to add RoutineSummary schema?
- How is Task schema structured?
- Existing nullable patterns?

### 6. Check existing summary DTOs for pattern
```bash
cat services/backend/src/main/java/com/hometusk/tasks/dto/UserSummaryDto.java
```

**Questions to answer:**
- Pattern for summary DTOs?
- Static `from()` method signature?

### 7. Check existing integration tests for Task API
```bash
find services/backend/src/test -name "*TaskController*" -o -name "*Task*Integration*"
cat services/backend/src/test/java/com/hometusk/integration/TaskControllerIntegrationTest.java 2>/dev/null || echo "not found"
```

**Questions to answer:**
- Existing test patterns?
- How to create test data with routine?

### 8. Check if RoutineSummaryDto already exists
```bash
find services/backend/src/main/java -name "*RoutineSummary*"
rg "RoutineSummaryDto" services/backend/src/main/java
```

**Questions to answer:**
- Does it exist in routines or tasks package?
- Need to create or reuse?

---

## Expected Output Format

After exploration, produce a structured report:

```markdown
## PLAN Phase Findings: ST-1007

### 1. TaskDto Structure
- Current fields: [list]
- from() method: [description]
- Import for Routine: [import statement]

### 2. TaskDetailDto Structure
- Current fields: [list]
- from() method: [description]

### 3. Task-Routine Relationship
- Fetch type: [LAZY/EAGER]
- Getter: [method]
- Can be null: [yes/no]

### 4. Routine Entity
- Status enum: [values]
- Status getter: [method]

### 5. OpenAPI Structure
- Task schema location: [section]
- Schema pattern: [description]
- Nullable syntax: [example]

### 6. Summary DTO Pattern
- Pattern: [description]
- from() signature: [example]

### 7. Integration Test Pattern
- Test class: [name]
- Data setup: [approach]
- Task with routine test: [exists/needs adding]

### 8. RoutineSummaryDto
- Exists: [yes/no]
- Location: [package]

### 9. Files to Create/Modify
1. [file path] - [action]
2. [file path] - [action]
...

### 10. Risks/Questions for Human Gate
- [Any unexpected findings]
- [Any clarifications needed]

### 11. Ready for APPLY?
- [ ] Yes, proceed with implementation
- [ ] No, need clarification on: [items]
```

---

## STOP-THE-LINE Rule

If you discover ANY of the following, STOP and report:
- RoutineSummaryDto already exists with different structure
- TaskDto already has routine field
- Circular dependency issues
- Missing Routine import in tasks package

---

## Verification (at end of PLAN phase)

Confirm these items before completing:
- [ ] TaskDto structure understood
- [ ] TaskDetailDto structure understood
- [ ] Task.routine relationship understood
- [ ] OpenAPI schema pattern identified
- [ ] Summary DTO pattern identified
- [ ] Test patterns identified
- [ ] No conflicting existing code found
- [ ] Ready for APPLY phase
