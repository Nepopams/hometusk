# Codex PLAN Prompt: ST-1004 — Assignment Policies (Fixed/Round-Robin/Manual)

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
docs/planning/workpacks/ST-1004/workpack.md
docs/planning/epics/EP-010/stories/ST-1004-assignment-policies.md
docs/planning/epics/EP-010/epic.md
docs/_governance/dod.md
```

---

## Task Summary

Explore the codebase to prepare for implementing:
1. `RoundRobinState` record for JSON serialization
2. `AssignmentPolicyService` with strategy pattern (FIXED/ROUND_ROBIN/MANUAL)
3. Pessimistic lock on routine row for concurrent access
4. Integration with `RoutineSchedulerService.createTaskForDate()`
5. Unit and integration tests

**Key constraints:**
- Round-robin state updated atomically with task creation (same transaction)
- State only advances when task INSERT succeeds (not on skip due to duplicate)
- Pessimistic lock per routine row (not global scheduler lock for v0)

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

### 1. Examine RoutineSchedulerService for integration points
```bash
cat services/backend/src/main/java/com/hometusk/routines/service/RoutineSchedulerService.java
```

**Questions to answer:**
- Where is `createTaskForDate()` called?
- How to inject `AssignmentPolicyService`?
- Where to lock routine row?
- Where to save routine after state update?

### 2. Examine Routine entity for state fields
```bash
cat services/backend/src/main/java/com/hometusk/routines/domain/Routine.java
```

**Questions to answer:**
- `roundRobinStateJson` field type and getter/setter?
- How to update state?
- `fixedAssignee` field type?

### 3. Check MembershipRepository for household members
```bash
cat services/backend/src/main/java/com/hometusk/users/repository/MembershipRepository.java
```

**Questions to answer:**
- Method to get members: `findByHousehold_IdWithUser()`?
- Returns List<Membership> or List<User>?
- How to extract User from Membership?

### 4. Examine Membership entity
```bash
cat services/backend/src/main/java/com/hometusk/users/domain/Membership.java
```

**Questions to answer:**
- How to get User from Membership?
- Is there a join order or createdAt for ordering?

### 5. Check existing JSON serialization patterns
```bash
rg "ObjectMapper|@JdbcTypeCode|SqlTypes.JSON" services/backend/src/main/java --type java -l
cat services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java | grep -A5 "objectMapper"
```

**Questions to answer:**
- Is ObjectMapper injected or created?
- How to serialize/deserialize JSON?

### 6. Find @Lock usage patterns
```bash
rg "@Lock|LockModeType|PESSIMISTIC" services/backend/src/main/java --type java
```

**Questions to answer:**
- Is @Lock already used somewhere?
- What import is needed?
- How to combine with JpaRepository method?

### 7. Examine Task entity for assignee setter
```bash
cat services/backend/src/main/java/com/hometusk/tasks/domain/Task.java | grep -A5 "setAssignee"
```

**Questions to answer:**
- Method signature for setAssignee()?
- Takes User or UUID?

### 8. Check existing RoutineRepository methods
```bash
cat services/backend/src/main/java/com/hometusk/routines/repository/RoutineRepository.java
```

**Questions to answer:**
- Existing findById methods?
- How to add findByIdForUpdate()?

### 9. Check existing AssignmentPolicyService (if any)
```bash
find services/backend/src/main/java -name "*AssignmentPolicy*"
ls services/backend/src/main/java/com/hometusk/routines/service/
```

**Questions to answer:**
- Does AssignmentPolicyService already exist?
- What services are in the routines package?

### 10. Examine existing RoutineSchedulerIntegrationTest
```bash
cat services/backend/src/test/java/com/hometusk/integration/RoutineSchedulerIntegrationTest.java
```

**Questions to answer:**
- Test patterns to follow?
- How to create test data with specific assignment policy?
- How to verify assignee in tests?

---

## Expected Output Format

After exploration, produce a structured report:

```markdown
## PLAN Phase Findings: ST-1004

### 1. RoutineSchedulerService Integration
- createTaskForDate() location: [line number]
- Injection point for AssignmentPolicyService: [description]
- Lock strategy: [approach]
- State save location: [description]

### 2. Routine Entity
- roundRobinStateJson type: [type]
- setRoundRobinStateJson method: [exists/needs adding]
- fixedAssignee getter: [signature]

### 3. MembershipRepository
- Method to get members: [signature]
- Return type: [type]
- User extraction: [how to get User]

### 4. Membership Entity
- User getter: [method]
- Ordering field: [field name or approach]

### 5. JSON Serialization
- ObjectMapper source: [how to get]
- Serialization pattern: [example]

### 6. @Lock Pattern
- Existing usage: [yes/no]
- Import needed: [import]
- Method pattern: [example]

### 7. Task.setAssignee
- Signature: [method signature]
- Parameter type: [User/UUID]

### 8. RoutineRepository
- Existing methods: [list]
- Lock method to add: [signature]

### 9. AssignmentPolicyService
- Exists: [yes/no]
- Services in routines package: [list]

### 10. Test Patterns
- Integration test base: [class]
- Data creation: [approach]
- Assertion patterns: [examples]

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
- AssignmentPolicyService already exists
- Round-robin logic already implemented
- Conflicting pessimistic lock mechanism
- Missing dependencies or unexpected codebase state

---

## Verification (at end of PLAN phase)

Confirm these items before completing:
- [ ] RoutineSchedulerService integration points identified
- [ ] Routine entity state fields understood
- [ ] MembershipRepository query available
- [ ] JSON serialization pattern identified
- [ ] @Lock pattern identified
- [ ] Task.setAssignee signature confirmed
- [ ] Test patterns identified
- [ ] No conflicting existing code found
- [ ] Ready for APPLY phase
