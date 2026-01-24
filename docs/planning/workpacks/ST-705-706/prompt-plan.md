# Codex PLAN Prompt: ST-705 + ST-706 — Security Tests + Observability

## Mode
**PLAN ONLY** — Read-only exploration. NO file modifications allowed.

## Allowed Commands (Whitelist)
```
ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
```

## Forbidden
- Any file edits/writes/moves/deletes
- Network access
- Package install (./gradlew build is OK for read-only)
- git commit/push

---

## Task
Plan implementation of additional security tests for analytics endpoint and structured logging.

## Sources of Truth (MUST READ)
1. `docs/planning/workpacks/ST-705-706/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-008/stories/ST-705-security-boundary-tests.md` — Story spec
3. `docs/planning/epics/EP-008/stories/ST-706-observability.md` — Story spec
4. `services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsControllerIntegrationTest.java` — Existing tests
5. `services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java` — Endpoint
6. `services/backend/src/test/java/com/hometusk/integration/IntegrationTestBase.java` — Test base class

---

## Already Implemented (DO NOT DUPLICATE)
These tests already exist in `AnalyticsControllerIntegrationTest.java`:
- `getAnalytics_notMember_returns403()` — basic 403 test
- `getAnalytics_unauthenticated_returns401()` — 401 without auth

---

## Tests to Add (ST-705)

### 1. Cross-Household Access Test
```java
@Test
void getAnalytics_memberOfDifferentHousehold_returns403() {
    // Given: user1 is member of H1 only
    // Given: H2 exists with user2 as member
    // When: user1 requests GET /households/{H2}/analytics
    // Then: 403 Forbidden
}
```

### 2. No Data Leak Test
```java
@Test
void getAnalytics_noDataLeakBetweenHouseholds() {
    // Given: H1 with 3 completed tasks
    // Given: H2 with 10 completed tasks
    // Given: user1 is member of H1 only
    // When: user1 requests analytics for H1
    // Then: perMember shows only H1 data (sum of completed = 3)
    // And: H2 data never appears in response
}
```

### 3. IDOR Prevention Test
```java
@Test
void getAnalytics_idorAttempt_returns403() {
    // Given: random UUID that doesn't correspond to any household
    // When: authenticated user requests analytics for that UUID
    // Then: 403 (not 404) — prevents enumeration
}
```

### 4. Invalid Period Handling
```java
@Test
void getAnalytics_invalidPeriod_handledGracefully() {
    // When: GET /households/{id}/analytics?period=invalid
    // Then: either 400 Bad Request
    //   OR defaults to 7d (check period in response = "7d")
}
```

---

## ST-706: Observability

### Structured Logging
Add to `AnalyticsController.java`:
```java
private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

@GetMapping
public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(...) {
    long startTime = System.currentTimeMillis();
    // ... existing logic ...
    long latencyMs = System.currentTimeMillis() - startTime;
    log.info("analytics_request household_id={} period={} latency_ms={} status=200",
             householdId, period, latencyMs);
    return ResponseEntity.ok(response);
}
```

**Critical:** NO PII in logs (no user email, no display name, no task titles).

---

## Exploration Tasks

### Task 1: Understand Test Base Class
- Read `IntegrationTestBase.java` — what fixtures are available?
- How are testUser, testUser2, testHousehold created?
- How to create additional households for cross-household tests?

### Task 2: Understand JWT Helpers
- How does `jwt()` work?
- How does `jwtForUser(user)` work?
- Can we authenticate as different users easily?

### Task 3: Check Current Period Handling
- How does `AnalyticsController` handle invalid period?
- Is it validated at controller level or service level?
- What's the current behavior?

### Task 4: Check Logging Setup
- Is SLF4J/Logback configured?
- What's the log format?
- Are there existing logging patterns in other controllers?

---

## Output Format

After exploration, provide:

### 1. Test Base Class Analysis
- Available fixtures (users, households, zones)
- How to create additional test data
- JWT authentication helpers

### 2. Tests to Implement
For each test:
- Method signature
- Setup steps
- Assertions
- Expected behavior

### 3. Logging Implementation
- Logger pattern from other controllers
- Where to add timing measurement
- Log format recommendation

### 4. Risks/Blockers
- Missing fixtures?
- Need to extend test base?
- Logging dependency issues?

### 5. Questions (if any)
- Clarifications needed before APPLY phase

---

## Stop Conditions

If any of these occur, STOP and describe:
- Test base class doesn't support multiple households
- No JWT helper for different users
- Invalid period already tested elsewhere
- Logging framework not configured
