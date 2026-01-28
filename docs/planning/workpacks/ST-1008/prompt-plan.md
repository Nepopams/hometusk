# Codex PLAN Prompt: ST-1008 — Security Boundaries + Integration Tests

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
AGENTS.md (project root)
docs/planning/workpacks/ST-1008/workpack.md
docs/planning/epics/EP-010/stories/ST-1008-security-boundaries.md
docs/contracts/http/routines.openapi.yaml
docs/_governance/dod.md
```

---

## Task Summary

Explore the codebase to prepare for implementing security boundaries on routines feature:
1. Add 3 missing endpoints (pause, resume, upcoming) to RoutineController
2. Add business logic to RoutineService
3. Create integration tests proving 403 for non-members on all endpoints
4. Create integration tests proving no cross-household data leaks

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

### 1. Examine existing RoutineController structure
```bash
cat services/backend/src/main/java/com/hometusk/routines/api/RoutineController.java
```

**Questions to answer:**
- What endpoints already exist?
- How is membership enforcement done (annotation or explicit call)?
- What is the pattern for endpoint methods?
- What response types are used?

### 2. Examine existing RoutineService
```bash
cat services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java
```

**Questions to answer:**
- What methods exist?
- How is household scoping enforced?
- Is there a `findById` that checks household?
- What exceptions are thrown for invalid states?

### 3. Examine Routine entity for pausedAt field
```bash
cat services/backend/src/main/java/com/hometusk/routines/domain/Routine.java
```

**Questions to answer:**
- Does `pausedAt` field exist?
- What is the RoutineStatus enum (ACTIVE, PAUSED, DELETED)?
- Are there status transition methods?

### 4. Find existing integration test patterns
```bash
cat services/backend/src/test/java/com/hometusk/integration/IntegrationTestBase.java
rg "403|Forbidden" services/backend/src/test/java/com/hometusk/integration/ -l
```

**Questions to answer:**
- What base class do integration tests extend?
- How are test users and households created?
- How is authentication handled in tests?
- Are there existing 403 tests to reference?

### 5. Check RecurrenceRuleParser for upcoming method
```bash
cat services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java
```

**Questions to answer:**
- Does `getOccurrencesInRange` method exist?
- What parameters does it take?
- Can it be used for upcoming endpoint?

### 6. Check RoutineDto structure
```bash
rg "class RoutineDto" services/backend/src/main/java/com/hometusk/routines/ -A 30
```

**Questions to answer:**
- What fields does RoutineDto have?
- Does it include status?
- Does it include pausedAt?

### 7. Check OpenAPI contract for endpoint signatures
```bash
cat docs/contracts/http/routines.openapi.yaml
```

**Questions to answer:**
- What are the exact paths for pause, resume, upcoming?
- What request/response bodies are expected?
- What error responses are defined?

### 8. Find MembershipService usage pattern
```bash
rg "requireMembership" services/backend/src/main/java/com/hometusk/ -B 2 -A 2
```

**Questions to answer:**
- What is the exact method signature?
- Does it throw an exception or return boolean?
- What exception type does it throw for non-members?

---

## Expected Output Format

After exploration, produce a structured report:

```markdown
## PLAN Phase Findings: ST-1008

### 1. RoutineController Structure
- Existing endpoints: [list]
- Membership enforcement pattern: [description]
- Response type pattern: [description]

### 2. RoutineService Structure
- Existing methods: [list]
- Household scoping pattern: [description]
- Exception pattern: [description]

### 3. Routine Entity
- pausedAt field exists: [yes/no]
- Status enum values: [list]
- Status transition methods: [yes/no]

### 4. Integration Test Patterns
- Base class: [class name]
- User/household setup: [description]
- Authentication pattern: [description]
- Existing 403 test examples: [yes/no, location]

### 5. RecurrenceRuleParser
- getOccurrencesInRange exists: [yes/no]
- Parameters: [list]
- Suitable for upcoming: [yes/no]

### 6. RoutineDto Structure
- Fields: [list]
- Includes status: [yes/no]
- Includes pausedAt: [yes/no]

### 7. OpenAPI Contract Details
- Pause path: [path]
- Resume path: [path]
- Upcoming path: [path]
- Response schemas: [summary]

### 8. MembershipService Pattern
- Method signature: [signature]
- Exception type: [type]
- Enforcement location: [controller/service]

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
- pause/resume/upcoming endpoints already exist
- RoutineSecurityIntegrationTest already exists
- Conflicting membership enforcement patterns
- Missing dependencies or unexpected codebase state
- RecurrenceRuleParser missing or different signature

---

## Verification (at end of PLAN phase)

Confirm these items before completing:
- [ ] RoutineController structure understood
- [ ] RoutineService structure understood
- [ ] Routine entity structure understood
- [ ] Integration test pattern identified
- [ ] RecurrenceRuleParser verified
- [ ] OpenAPI contract reviewed
- [ ] MembershipService pattern identified
- [ ] No conflicting existing code found
- [ ] Ready for APPLY phase
