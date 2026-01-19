# Checklist: ST-001 — Availability-Based Assignee Selection

## Acceptance Criteria Verification

### AC1: Happy Path - Auto-assign to least loaded member
- [ ] Test exists: household with 3 members, different task counts
- [ ] Task assigned to member with fewest open tasks
- [ ] DecisionLog contains reason "availability_heuristic"

### AC2: Tie-breaker - Multiple members with same count
- [ ] Test exists: 2+ members with equal task counts
- [ ] Assignment is deterministic (same result on retry)
- [ ] Tie-breaker documented (first in member list)

### AC3: Single member household
- [ ] Test exists: household with only initiator
- [ ] Task assigned to initiator

### AC4: Explicit assignee still works
- [ ] Test exists: create_task with assigneeId provided
- [ ] Heuristic not applied, explicit assignee used

### AC5: Backward compatibility
- [ ] All existing CommandPipelineTest tests pass
- [ ] All existing MvpJourneyIntegrationTest tests pass
- [ ] No regression in other integration tests

---

## DoD Verification

### Code Quality
- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied: `./gradlew spotlessApply`
- [ ] No compiler warnings introduced
- [ ] No SonarLint critical issues

### Tests Required
- [ ] Unit tests written: DecisionEngineTest
- [ ] Unit tests passing: `./gradlew test --tests "*DecisionEngineTest*"`
- [ ] Integration test written: CommandPipelineTest
- [ ] Integration test passing
- [ ] All tests pass: `./scripts/test.sh`

### Documentation Updates
- [ ] No contract changes (commands.openapi.yaml unchanged)
- [ ] No ADR needed
- [ ] No diagram updates needed

### Observability
- [ ] DecisionLog contains decision reason field
- [ ] Logs include assignee selection context

### Security
- [ ] No cross-household data leaks (query scoped to householdId)
- [ ] No new auth/authz changes

---

## Final Verification Commands

```bash
# 1. Format check
./gradlew spotlessCheck

# 2. Compile
./gradlew :services:backend:compileJava

# 3. Unit tests
./gradlew :services:backend:test --tests "*DecisionEngineTest*"

# 4. Integration tests
./gradlew :services:backend:test --tests "*CommandPipelineTest*"

# 5. All tests
./scripts/test.sh

# 6. Application starts
./gradlew :services:backend:bootRun --args='--spring.profiles.active=test' &
sleep 10 && curl http://localhost:8080/actuator/health && kill %1
```

---

## Sign-off

| Role | Approved | Date |
|------|----------|------|
| Codex (Implementation) | [ ] | |
| Claude (Review) | [ ] | |
| Human (Final Gate) | [ ] | |
