# Codex APPLY Prompt: ST-705 + ST-706 — Security Tests + Observability

## Mode
**APPLY** — Implementation phase. File modifications allowed.

## Sources of Truth (AUTHORITATIVE)
1. `docs/planning/workpacks/ST-705-706/workpack.md` — Implementation plan
2. `docs/planning/epics/EP-008/stories/ST-705-security-boundary-tests.md` — Story spec
3. `docs/planning/epics/EP-008/stories/ST-706-observability.md` — Story spec
4. `docs/_governance/dod.md` — Definition of Done

## PLAN Phase Findings (Incorporated)

### Test Infrastructure
- `IntegrationTestBase` provides: `testHousehold`, `testUser`, `testUser2`, `testMembership`, `testZone`
- Only `testUser` is member of `testHousehold` by default
- `testUser2` available for non-member scenarios
- JWT helpers: `jwt()` for testUser, `jwtForUser(user)` for any user
- Repositories injected: `householdRepository`, `userRepository`, `membershipRepository`, `zoneRepository`
- Cross-household pattern in `HouseholdBoundarySecurityTest.java`

### completedAt Handling
- No setter on Task entity — must use reflection
- Existing helper in `AnalyticsControllerIntegrationTest.java`:
```java
private void setCompletedAt(Task task, Instant completedAt) {
    try {
        Field field = Task.class.getDeclaredField("completedAt");
        field.setAccessible(true);
        field.set(task, completedAt);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

### Current Period Handling
- Invalid period → defaults to "7d" (service level)
- Response returns `$.period = "7d"`

---

## Human Gate Decisions

| Question | Decision |
|----------|----------|
| Dedicated class vs extend? | **Dedicated `AnalyticsSecurityIntegrationTest`** |
| Custom Micrometer metrics? | **No** — rely on default actuator `http_server_requests_seconds` |
| Include AC-5 (invalid UUID)? | **Skip** — Spring handles UUID validation automatically |

---

## Implementation Plan

### Step 1: Create Security Test Class (ST-705)

**File:** `services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsSecurityIntegrationTest.java`

```java
package com.hometusk.analytics.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.households.domain.Household;
import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Analytics Security Integration Tests")
class AnalyticsSecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    private Instant now;

    @BeforeEach
    void setupTime() {
        now = Instant.now();
    }

    @Test
    @DisplayName("Should return 403 when user is member of different household")
    void getAnalytics_memberOfDifferentHousehold_returns403() throws Exception {
        // Given: H2 exists with testUser2 as member
        Household h2 = new Household("Other Household");
        householdRepository.save(h2);
        Membership membership2 = new Membership(testUser2, h2, MembershipRole.admin);
        membershipRepository.save(membership2);

        // When: testUser (member of H1 only) requests H2 analytics
        mockMvc.perform(get("/api/v1/households/{id}/analytics", h2.getId())
                        .with(jwt()))
                // Then: 403 Forbidden
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should not leak data between households")
    void getAnalytics_noDataLeakBetweenHouseholds() throws Exception {
        // Given: H1 with 3 completed tasks
        for (int i = 0; i < 3; i++) {
            Task task = new Task(testHousehold, "H1 Task " + i, testUser);
            task.setAssignee(testUser);
            task.complete();
            setCompletedAt(task, now.minus(1, ChronoUnit.DAYS));
            taskRepository.save(task);
        }

        // Given: H2 with 10 completed tasks (different household)
        Household h2 = new Household("Other Household");
        householdRepository.save(h2);
        Membership membership2 = new Membership(testUser2, h2, MembershipRole.admin);
        membershipRepository.save(membership2);

        for (int i = 0; i < 10; i++) {
            Task task = new Task(h2, "H2 Task " + i, testUser2);
            task.setAssignee(testUser2);
            task.complete();
            setCompletedAt(task, now.minus(1, ChronoUnit.DAYS));
            taskRepository.save(task);
        }

        // When: testUser requests analytics for H1
        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt()))
                // Then: only H1 data returned
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.householdId").value(testHousehold.getId().toString()))
                .andExpect(jsonPath("$.perMember[?(@.memberName == 'Alice Test')].completedCount")
                        .value(org.hamcrest.Matchers.contains(3)));
    }

    @Test
    @DisplayName("Should return 403 for IDOR attempt with random UUID")
    void getAnalytics_idorAttempt_returns403() throws Exception {
        // Given: random UUID that doesn't correspond to any household
        UUID randomId = UUID.randomUUID();

        // When: authenticated user requests analytics for random UUID
        mockMvc.perform(get("/api/v1/households/{id}/analytics", randomId)
                        .with(jwt()))
                // Then: 403 (not 404) - prevents enumeration
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should handle invalid period gracefully by defaulting to 7d")
    void getAnalytics_invalidPeriod_handledGracefully() throws Exception {
        // When: request with invalid period
        mockMvc.perform(get("/api/v1/households/{id}/analytics", testHousehold.getId())
                        .with(jwt())
                        .param("period", "invalid"))
                // Then: defaults to 7d
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("7d"));
    }

    private void setCompletedAt(Task task, Instant completedAt) {
        try {
            Field field = Task.class.getDeclaredField("completedAt");
            field.setAccessible(true);
            field.set(task, completedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Step 2: Add Logging to AnalyticsController (ST-706)

**File:** `services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java`

Add import:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

Add logger field:
```java
private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
```

Modify getAnalytics method:
```java
@GetMapping
@Operation(summary = "Get household analytics summary")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Analytics summary"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "403", description = "Not a member of this household")
})
public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
        @PathVariable UUID householdId, @RequestParam(defaultValue = "7d") String period) {
    long startTime = System.currentTimeMillis();

    CurrentUser user = userResolver.resolveCurrentUser();
    membershipService.requireMembership(user.id(), householdId);

    AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, period);

    long latencyMs = System.currentTimeMillis() - startTime;
    log.info("analytics_request household_id={} period={} latency_ms={} status=200",
             householdId, period, latencyMs);

    return ResponseEntity.ok(response);
}
```

**Note:** 403 logging happens automatically via Spring Security exception handling. The structured log format includes only technical data (no PII):
- `household_id` — UUID
- `period` — request parameter
- `latency_ms` — processing time
- `status` — HTTP status

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Run security tests only
./gradlew test --tests "*AnalyticsSecurityIntegrationTest*"

# Run all analytics tests
./gradlew test --tests "*Analytics*"

# Run all tests
./gradlew test

# Format check
./gradlew spotlessCheck

# Apply formatting if needed
./gradlew spotlessApply
```

---

## Acceptance Criteria Mapping

| AC | Test/Implementation |
|----|---------------------|
| ST-705 AC-1: 403 for non-members | Already in `AnalyticsControllerIntegrationTest` |
| ST-705 AC-2: No cross-household leak | `getAnalytics_noDataLeakBetweenHouseholds()` |
| ST-705 AC-3: IDOR test | `getAnalytics_idorAttempt_returns403()` |
| ST-705 AC-4: Invalid period | `getAnalytics_invalidPeriod_handledGracefully()` |
| ST-706 AC-1: Request logging | Logger in `AnalyticsController` |
| ST-706 AC-2: Metrics | Default actuator `http_server_requests_seconds` |
| ST-706 AC-3: No PII | Only UUIDs and technical data in logs |

---

## Files to Create
| Path | Purpose |
|------|---------|
| `services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsSecurityIntegrationTest.java` | Security tests |

## Files to Modify
| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java` | Add logger + timing |

---

## Forbidden (Do NOT)
- Do NOT modify AnalyticsService
- Do NOT add custom Micrometer metrics
- Do NOT add invalid UUID test (AC-5)
- Do NOT log user email, display name, or task titles

## Stop Conditions
If any of these occur, STOP and describe:
- Test base class missing required fixtures
- Logger import conflicts
- Tests fail unexpectedly
