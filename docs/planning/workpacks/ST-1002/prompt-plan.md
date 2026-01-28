# Codex PLAN Prompt: ST-1002 — Recurrence Rule Parser

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
docs/planning/workpacks/ST-1002/workpack.md
docs/planning/epics/EP-010/stories/ST-1002-recurrence-parser.md
docs/_governance/dod.md
```

---

## Task Summary

Explore the codebase to prepare for implementing `RecurrenceRuleParser` — a service that converts `RecurrenceRule` sealed interface instances to sequences of dates.

**Story Goal:** Parser converts recurrence rules (DAILY, WEEKLY, MONTHLY, EVERY_N_DAYS) to date sequences with:
- `getOccurrencesInRange(rule, fromDateInclusive, count)` → List<LocalDate>
- `getNextOccurrence(rule, afterDateExclusive)` → LocalDate

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

### 1. Examine RecurrenceRule sealed interface
```bash
cat services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java
```

**Questions to answer:**
- What are the 4 record subtypes?
- What fields does each record have?
- Is there any existing validation logic?

### 2. Find existing exception patterns
```bash
rg "class.*Exception" services/backend/src/main/java/com/hometusk --type java -l
cat services/backend/src/main/java/com/hometusk/shared/exception/ValidationException.java
```

**Questions to answer:**
- What exception class should `InvalidRecurrenceRuleException` extend?
- What constructor pattern is used?

### 3. Check existing service patterns in routines module
```bash
ls services/backend/src/main/java/com/hometusk/routines/service/
cat services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java
```

**Questions to answer:**
- What annotations do services use?
- What's the package structure for services?

### 4. Check existing test patterns
```bash
ls services/backend/src/test/java/com/hometusk/routines/service/
cat services/backend/src/test/java/com/hometusk/routines/service/RoutineServiceTest.java
```

**Questions to answer:**
- What testing framework is used (JUnit 5, AssertJ)?
- What's the test class naming convention?
- How are test methods named?

### 5. Verify existing validation in RoutineService
```bash
rg "WEEKLY|daysOfWeek" services/backend/src/main/java/com/hometusk/routines/
```

**Questions to answer:**
- Does RoutineService already validate recurrence rules?
- Should parser validation duplicate or replace service validation?

---

## Expected Output Format

After exploration, produce a structured report:

```markdown
## PLAN Phase Findings: ST-1002

### 1. RecurrenceRule Structure
- Daily: [fields]
- Weekly: [fields]
- Monthly: [fields]
- EveryNDays: [fields]

### 2. Exception Pattern
- Base class to extend: [class]
- Constructor pattern: [description]

### 3. Service Pattern
- Annotation: @Service
- Package: com.hometusk.routines.service

### 4. Test Pattern
- Framework: [JUnit 5 / AssertJ]
- Naming: [pattern]

### 5. Validation Overlap
- Existing validation in: [location]
- Recommendation: [keep both / consolidate]

### 6. Files to Create
1. `InvalidRecurrenceRuleException.java`
2. `RecurrenceRuleParser.java`
3. `RecurrenceRuleParserTest.java`

### 7. Risks/Questions for Human Gate
- [Any unexpected findings]
- [Any clarifications needed]

### 8. Ready for APPLY?
- [ ] Yes, proceed with implementation
- [ ] No, need clarification on: [items]
```

---

## STOP-THE-LINE Rule

If you discover ANY of the following, STOP and report:
- RecurrenceRuleParser already exists
- InvalidRecurrenceRuleException already exists
- Existing validation logic that conflicts with story requirements
- Missing dependencies or unexpected codebase state

---

## Verification (at end of PLAN phase)

Confirm these items before completing:
- [ ] RecurrenceRule interface structure understood
- [ ] Exception pattern identified
- [ ] Service pattern identified
- [ ] Test pattern identified
- [ ] No conflicting existing code found
- [ ] Ready for APPLY phase
