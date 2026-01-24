# Prompt Pack: ST-701 + ST-702 — Backend Analytics + Balance Score

> Generated: 2026-01-24
> Source workpack: `docs/planning/workpacks/ST-701-702/workpack.md`

---

## PLAN Prompt (No Edits / No Commands)

```
=== CODEX PLAN PROMPT: ST-701 + ST-702 (Backend Analytics + Balance Score) ===

## ANCHOR (Read First — Do Not Skip)
Before any action, read and internalize:
1. AGENTS.md (project rules)
2. Workpack: docs/planning/workpacks/ST-701-702/workpack.md
3. Epic: docs/planning/epics/EP-008/epic.md
4. Stories:
   - docs/planning/epics/EP-008/stories/ST-701-analytics-endpoints.md
   - docs/planning/epics/EP-008/stories/ST-702-fairness-index.md
5. Existing API contract: docs/contracts/http/commands.openapi.yaml
6. DoD: docs/_governance/dod.md

## TASK
Plan the implementation of:
- GET /api/v1/households/{householdId}/analytics?period=7d|30d
- GiniCalculator utility class
- AnalyticsService + AnalyticsController
- Unit tests (GiniCalculatorTest, AnalyticsServiceTest)
- Integration tests (AnalyticsControllerIntegrationTest)

## ALLOWED ACTIONS (Read-Only)
- Read files: cat, head, tail, less
- Search: rg, grep, find, ls
- Git status: git status, git diff (read-only)
- sed -n (non-destructive)

## FORBIDDEN ACTIONS (PLAN Phase)
- NO file edits (no Write, no Edit)
- NO git commit/push
- NO ./gradlew build (wait for APPLY phase)
- NO network requests
- NO package installs
- NO migrations

## PLAN OUTPUT STRUCTURE
Produce a plan with:
1. **Files to create** (exact paths)
2. **Files to modify** (exact paths)
3. **Dependencies** (what existing classes/methods are needed)
4. **Test cases** (list by name)
5. **Open questions** (if any — STOP-THE-LINE if blocking)

## FILES TO CREATE (Expect These)
| Path | Purpose |
|------|---------|
| services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java | REST endpoint |
| services/backend/src/main/java/com/hometusk/analytics/service/AnalyticsService.java | Business logic |
| services/backend/src/main/java/com/hometusk/analytics/service/GiniCalculator.java | Gini calculation |
| services/backend/src/main/java/com/hometusk/analytics/dto/AnalyticsSummaryResponse.java | Response DTO |
| services/backend/src/main/java/com/hometusk/analytics/dto/MemberStats.java | Member stats record |
| services/backend/src/main/java/com/hometusk/analytics/dto/ZoneStats.java | Zone stats record |
| services/backend/src/main/java/com/hometusk/analytics/dto/FairnessInfo.java | Fairness data record |
| services/backend/src/main/java/com/hometusk/analytics/dto/OverdueTask.java | Overdue item record |
| services/backend/src/test/java/com/hometusk/analytics/service/GiniCalculatorTest.java | Unit tests |
| services/backend/src/test/java/com/hometusk/analytics/service/AnalyticsServiceTest.java | Service unit tests |
| services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsControllerIntegrationTest.java | Integration tests |

## FILES TO MODIFY (Expect These)
| Path | Change |
|------|--------|
| docs/contracts/http/commands.openapi.yaml | Add analytics endpoint + schemas |

## FORBIDDEN PATHS (Do Not Touch)
- services/backend/src/main/resources/db/migration/** (no new migrations unless DB schema needed)
- Any files outside services/backend/ and docs/contracts/
- AGENTS.md, CLAUDE.md (read-only)

## GINI FORMULA (Must Use Exactly)
```java
// If sum(workloads) = 0 → return null
// Else:
//   Sort workloads ascending
//   G = Σᵢ (2i - n - 1) × wᵢ / (n × Σ wᵢ)
//   gini = Math.abs(G), clamped to 0..1
//   balance = round((1 - gini) * 100) or null
```

## EDGE CASES (Must Handle)
| Condition | gini | balance |
|-----------|------|---------|
| sum(workload) = 0 | null | null |
| Single member with tasks | 0 | 100 |
| All members equal | 0 | 100 |
| n = 0 members | null | null |

## TEST CASES (Must Include)
GiniCalculatorTest:
- calculate_equalDistribution_returnsZero
- calculate_completeInequality_returnsHigh
- calculate_typicalDistribution_returnsMedium
- calculate_emptyWorkloads_returnsNull
- calculate_allZeros_returnsNull
- calculate_singleNonZero_calculatesCorrectly

AnalyticsControllerIntegrationTest:
- getAnalytics_asMember_returnsData
- getAnalytics_notMember_returns403
- getAnalytics_periodFilter_filtersCorrectly
- getAnalytics_crossHousehold_noLeaks
- getAnalytics_overdueTop_limitedTo5
- getAnalytics_noTasks_returnsNullBalance

## OUTPUT FORMAT
After reading sources, produce:
```
## PLAN: ST-701 + ST-702

### 1. Files to Create
[list with paths]

### 2. Files to Modify
[list with paths and changes]

### 3. Dependencies Identified
[existing classes/interfaces needed]

### 4. Test Cases
[list test method names]

### 5. Open Questions
[if any — STOP and ask]

### 6. Ready for APPLY?
[YES/NO]
```

DO NOT PROCEED TO EDITS. Output plan only.
```

---

## APPLY Prompt (Implementation)

```
=== CODEX APPLY PROMPT: ST-701 + ST-702 (Backend Analytics + Balance Score) ===

## ANCHOR (Read First — Do Not Skip)
Before any action, read and internalize:
1. AGENTS.md (project rules)
2. Workpack: docs/planning/workpacks/ST-701-702/workpack.md
3. Epic: docs/planning/epics/EP-008/epic.md
4. DoD checklist: docs/planning/workpacks/ST-701-702/checklist.md
5. OpenAPI: docs/contracts/http/commands.openapi.yaml

## PREREQUISITE
PLAN phase must be complete and approved.

## TASK
Implement ST-701 + ST-702:
1. Create GiniCalculator with edge case handling
2. Create DTOs (records)
3. Create AnalyticsService with query methods
4. Create AnalyticsController with membership check
5. Write unit tests (GiniCalculatorTest, AnalyticsServiceTest)
6. Write integration tests (AnalyticsControllerIntegrationTest)
7. Update OpenAPI contract
8. Run Spotless formatting
9. Run all tests

## STOP-THE-LINE RULE
If you encounter ANY deviation from the plan, specification, or contract — STOP IMMEDIATELY and report:
- What deviated
- Why it's blocking
- Proposed resolution

DO NOT improvise. DO NOT invent new features.

## ALLOWED ACTIONS
- Create/edit files in: services/backend/src/**
- Update: docs/contracts/http/commands.openapi.yaml
- Run: ./gradlew build, ./gradlew test, ./gradlew spotlessApply

## FORBIDDEN ACTIONS
- DO NOT create new DB migrations (unless schema change confirmed)
- DO NOT touch files outside services/backend/ and docs/contracts/
- DO NOT add dependencies to build.gradle without approval
- DO NOT implement shopping analytics (out of scope)
- DO NOT implement custom date ranges (out of scope)
- DO NOT implement weighted workload (out of scope)

## REQUIRED FILE PATHS (Create These)
```
services/backend/src/main/java/com/hometusk/analytics/
├── api/
│   └── AnalyticsController.java
├── service/
│   ├── AnalyticsService.java
│   └── GiniCalculator.java
└── dto/
    ├── AnalyticsSummaryResponse.java
    ├── MemberStats.java
    ├── ZoneStats.java
    ├── FairnessInfo.java
    └── OverdueTask.java

services/backend/src/test/java/com/hometusk/analytics/
├── service/
│   ├── GiniCalculatorTest.java
│   └── AnalyticsServiceTest.java
└── api/
    └── AnalyticsControllerIntegrationTest.java
```

## GINI FORMULA (Use Exactly — From Workpack)
```java
public static Double calculate(int[] workloads) {
    if (workloads == null || workloads.length == 0) {
        return null;
    }
    long sum = Arrays.stream(workloads).asLongStream().sum();
    if (sum == 0) {
        return null; // No tasks → N/A
    }
    int n = workloads.length;
    if (n == 1) {
        return 0.0; // Single member → perfect equality
    }
    int[] sorted = Arrays.copyOf(workloads, n);
    Arrays.sort(sorted);
    double numerator = 0;
    for (int i = 0; i < n; i++) {
        numerator += (2.0 * (i + 1) - n - 1) * sorted[i];
    }
    double gini = Math.abs(numerator / (n * sum));
    return Math.min(gini, 1.0); // Clamp to 0..1
}

public static Integer toBalance(Double gini) {
    if (gini == null) {
        return null;
    }
    return (int) Math.round((1 - gini) * 100);
}
```

## INTERPRETATION TEXT (Use Exactly — From Workpack)
```java
private String generateInterpretation(Integer balance) {
    if (balance == null) {
        return "N/A — no tasks completed in this period";
    }
    if (balance >= 90) {
        return "Excellent balance — tasks evenly distributed among members.";
    }
    if (balance >= 70) {
        return "Good balance — workload reasonably distributed.";
    }
    if (balance >= 50) {
        return "Moderate imbalance — some members completed more tasks than others.";
    }
    if (balance >= 30) {
        return "Significant imbalance — workload concentrated on fewer members.";
    }
    return "Severe imbalance — most tasks completed by one or two members.";
}
```

## CONTROLLER SECURITY (Required)
```java
@GetMapping
public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
        @PathVariable UUID householdId,
        @RequestParam(defaultValue = "7d") String period) {

    CurrentUser user = userResolver.resolveCurrentUser();
    membershipService.requireMembership(user.id(), householdId);  // <-- REQUIRED

    // ... rest of implementation
}
```

## VERIFICATION COMMANDS (Run Before Completion)
```bash
cd /home/vad/Документы/hometusk/services/backend

# Format code
./gradlew spotlessApply

# Run unit tests
./gradlew test --tests "*GiniCalculatorTest*"
./gradlew test --tests "*AnalyticsServiceTest*"

# Run integration tests
./gradlew test --tests "*AnalyticsControllerIntegrationTest*"

# Run all analytics tests
./gradlew test --tests "*Analytics*" --tests "*Gini*"

# Full build
./gradlew build
```

## DOD MUST-HAVES (Check Before Done)
- [ ] GiniCalculator handles all edge cases (null, empty, zero sum, single member)
- [ ] Balance formula: round((1 - gini) * 100)
- [ ] Interpretation text is non-toxic (no blame language)
- [ ] Formula field = "Balance = 100 × (1 - Gini coefficient)"
- [ ] membershipService.requireMembership() called in controller
- [ ] No cross-household data leaks in queries
- [ ] OpenAPI updated with endpoint + all schemas
- [ ] All tests pass
- [ ] Spotless check passes

## OUTPUT FORMAT
After implementation, report:
```
## APPLY COMPLETE: ST-701 + ST-702

### Files Created
[list paths]

### Files Modified
[list paths]

### Tests Passed
[list test results]

### DoD Checklist Status
[all items checked? YES/NO]

### Issues Encountered
[none / list if any]
```
```

---

## REVIEW Prompt (Exit Evidence)

```
=== CODEX REVIEW PROMPT: ST-701 + ST-702 (Backend Analytics + Balance Score) ===

## ANCHOR (Read First — Do Not Skip)
Before review, load context:
1. AGENTS.md (project rules)
2. Workpack: docs/planning/workpacks/ST-701-702/workpack.md
3. DoD checklist: docs/planning/workpacks/ST-701-702/checklist.md
4. DoD: docs/_governance/dod.md

## PREREQUISITE
APPLY phase must be complete.

## TASK
Review the implementation for correctness, completeness, and contract compliance.
Produce EXIT EVIDENCE with GO/NO-GO recommendation.

## REVIEW CHECKLIST

### 1. Code Quality
- [ ] Java 21 idioms (records, pattern matching, etc.)
- [ ] Spring Boot conventions followed
- [ ] Spotless formatting applied
- [ ] No compiler warnings
- [ ] Package structure: com.hometusk.analytics.*

### 2. GiniCalculator Correctness
Run these scenarios mentally or via tests:
- [ ] `[5, 5, 5]` → gini ≈ 0, balance = 100
- [ ] `[0, 0, 10]` → gini > 0.6, balance < 40
- [ ] `[]` → null, null
- [ ] `[0, 0, 0]` → null, null
- [ ] `[5]` → gini = 0, balance = 100

### 3. Controller Security
- [ ] `membershipService.requireMembership()` is called
- [ ] `userResolver.resolveCurrentUser()` is called
- [ ] 403 returned for non-members (not 404)
- [ ] No IDOR possible

### 4. Query Correctness
- [ ] WHERE household_id = :householdId present in all queries
- [ ] Period filter applied correctly
- [ ] No N+1 queries
- [ ] overdueTop limited to 5

### 5. Contract Compliance
- [ ] OpenAPI endpoint matches implementation
- [ ] All required fields present in schemas
- [ ] Nullable fields marked correctly (gini, balance)
- [ ] Response codes: 200, 401, 403

### 6. Tests
- [ ] GiniCalculatorTest covers all edge cases
- [ ] AnalyticsServiceTest covers period logic
- [ ] Integration test: getAnalytics_asMember_returnsData
- [ ] Integration test: getAnalytics_notMember_returns403
- [ ] Integration test: getAnalytics_noTasks_returnsNullBalance
- [ ] All tests pass: ./gradlew test --tests "*Analytics*" --tests "*Gini*"

### 7. Non-Toxic Wording
- [ ] No "winner/loser" language
- [ ] Uses "balance" not "fairness score"
- [ ] Interpretation text focuses on distribution, not blame

## VERIFICATION COMMANDS
```bash
cd /home/vad/Документы/hometusk/services/backend

# Check formatting
./gradlew spotlessCheck

# Run all tests
./gradlew test --tests "*Analytics*" --tests "*Gini*"

# Build
./gradlew build
```

## OUTPUT FORMAT (Exit Evidence)
```
## EXIT EVIDENCE: ST-701 + ST-702

### Review Date: [DATE]

### Implementation Summary
- Endpoint: GET /api/v1/households/{householdId}/analytics
- Files created: [count]
- Tests added: [count]

### Checklist Results

#### Code Quality
- [x] Java 21 idioms
- [x] Spring Boot conventions
- [x] Spotless applied
- [x] No warnings
- [x] Package structure correct

#### Gini Correctness
- [x] Equal distribution → 0
- [x] Unequal → calculated correctly
- [x] Empty/zero → null
- [x] Single member → 0

#### Security
- [x] Membership check present
- [x] 403 for non-members
- [x] No IDOR
- [x] No cross-household leaks

#### Contract
- [x] OpenAPI updated
- [x] Schemas complete
- [x] Nullable fields correct

#### Tests
- [x] Unit tests pass
- [x] Integration tests pass
- [x] Edge cases covered

#### Wording
- [x] Non-toxic language used

### Must-Fix Issues
[NONE / list critical issues]

### Should-Fix Issues
[NONE / list non-critical issues]

### GO/NO-GO Recommendation
[GO / NO-GO + reason]

### Evidence Artifacts
- Test report: [path or inline]
- Build output: [success/fail]
```

## DECISION CRITERIA
- **GO:** All checklist items pass, no must-fix issues
- **NO-GO:** Any must-fix issues, failing tests, or security violations
```

---

## Summary

| Prompt | Purpose | Key Constraints |
|--------|---------|-----------------|
| PLAN | Read-only exploration | NO edits, NO commands |
| APPLY | Implementation | STOP-THE-LINE on deviations |
| REVIEW | Exit evidence | GO/NO-GO recommendation |

**Critical repeats in all prompts:**
- Anchor block with all source files
- Required file paths
- Forbidden paths
- Gini formula (exact)
- Verification commands
- DoD checklist items
