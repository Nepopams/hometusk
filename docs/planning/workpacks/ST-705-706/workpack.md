# Workpack: ST-705 + ST-706 — Security Tests + Observability

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- Stories: `docs/planning/epics/EP-008/stories/ST-705-security-boundary-tests.md`, `docs/planning/epics/EP-008/stories/ST-706-observability.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — ST-705 committed, ST-706 stretch

---

## Outcome
Comprehensive security tests proving household boundaries, plus observability hooks for analytics endpoint.

---

## Scope

### In Scope (ST-705 — Committed)
- Integration tests: 403 for non-members
- Integration tests: no cross-household data leaks
- IDOR prevention test
- Invalid parameter handling

### In Scope (ST-706 — Stretch)
- Structured logging for analytics requests
- Metrics (if Micrometer configured)
- No PII in logs

### Out of Scope
- Rate limiting
- Alerting rules
- Performance benchmarks

---

## Files to Change/Create

### New Files (ST-705)
| Path | Purpose |
|------|---------|
| `services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsSecurityIntegrationTest.java` | Security tests |

### Modified Files (ST-706)
| Path | Changes |
|------|---------|
| `services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java` | Add logging |

---

## Implementation Plan

### ST-705: Security Tests

```java
package com.hometusk.analytics.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AnalyticsSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void getAnalytics_notMember_returns403() {
        // Given: user authenticated but not member of household
        // When: GET /households/{householdId}/analytics
        // Then: 403 Forbidden
    }

    @Test
    void getAnalytics_memberOfDifferentHousehold_returns403() {
        // Given: user is member of H1, requesting H2
        // When: GET /households/{H2}/analytics
        // Then: 403 Forbidden
    }

    @Test
    void getAnalytics_noDataLeakBetweenHouseholds() {
        // Given: H1 with 5 tasks, H2 with 10 tasks
        // Given: user is member of H1 only
        // When: GET /households/{H1}/analytics
        // Then: response contains only H1 data
        // And: total completed across perMember = 5
    }

    @Test
    void getAnalytics_idorAttempt_returns403() {
        // Given: random UUID that doesn't exist
        // When: GET /households/{randomUUID}/analytics
        // Then: 403 (not 404, to prevent enumeration)
    }

    @Test
    void getAnalytics_invalidPeriod_handledGracefully() {
        // When: GET /households/{id}/analytics?period=invalid
        // Then: either 400 or defaults to 7d
    }

    @Test
    void getAnalytics_unauthenticated_returns401() {
        // Given: no auth token
        // When: GET /households/{id}/analytics
        // Then: 401 Unauthorized
    }
}
```

### ST-706: Observability

```java
// In AnalyticsController.java
@GetMapping
public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
        @PathVariable UUID householdId,
        @RequestParam(defaultValue = "7d") String period) {

    long startTime = System.currentTimeMillis();
    CurrentUser user = userResolver.resolveCurrentUser();

    try {
        membershipService.requireMembership(user.id(), householdId);
        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, period);

        logRequest(householdId, period, System.currentTimeMillis() - startTime, 200);
        return ResponseEntity.ok(response);

    } catch (AccessDeniedException e) {
        logRequest(householdId, period, System.currentTimeMillis() - startTime, 403);
        throw e;
    }
}

private void logRequest(UUID householdId, String period, long latencyMs, int status) {
    // Structured log - NO PII
    log.info("analytics_request household_id={} period={} latency_ms={} status={}",
             householdId, period, latencyMs, status);
}
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Run security tests
./gradlew test --tests "*AnalyticsSecurityIntegrationTest*"

# Run all analytics tests
./gradlew test --tests "*Analytics*"

# Check logs format (manual)
./gradlew bootRun
# Then make request and check console output
```

---

## Test Data Setup

```java
// Test fixture helper
@BeforeEach
void setup() {
    // Create household H1 with user1 as member
    // Create household H2 with user2 as member
    // Create tasks in H1 (5 tasks)
    // Create tasks in H2 (10 tasks)
    // Authenticate as user1
}
```

---

## Risks
| Risk | Mitigation |
|------|------------|
| Test flakiness | Use Testcontainers, explicit data setup |
| Missing edge cases | Review AC checklist |

---

## Rollback
N/A — tests only (no production code changes except logging)

---

## DoD Checklist
See `checklist.md`
