# Workpack: ST-701 + ST-702 — Backend Analytics + Balance Score

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- Stories: `docs/planning/epics/EP-008/stories/ST-701-analytics-endpoints.md`, `docs/planning/epics/EP-008/stories/ST-702-fairness-index.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`

---

## Status
**Ready** — Approved for implementation

---

## Outcome
Backend analytics endpoint returning:
1. Per-member stats: completed, overdue, open counts
2. Per-zone stats: completed, overdue counts
3. Balance score: Gini-based (0..1 gini, 0..100 balance)
4. Top overdue tasks (optional, max 5)

---

## Key Decisions (Resolved)

### Fairness Formula: Gini Coefficient
```
gini = Gini(workload[])           // 0..1
balance = round((1 - gini) * 100) // 0..100 or null
```

### Workload Definition
- Count of completed tasks per member in period
- No weighting in v0 (explicit limitation)

### Edge Cases
| Condition | gini | balance |
|-----------|------|---------|
| sum(workload) = 0 | null | null |
| Single member with tasks | calculated normally | reflects distribution |
| All members equal | 0 | 100 |

---

## Scope

### In Scope
- `GET /api/v1/households/{id}/analytics?period=7d|30d`
- AnalyticsSummary response with all fields
- GiniCalculator utility class
- Household boundary check
- Unit + integration tests

### Out of Scope
- Shopping analytics
- Custom date ranges
- Weighted workload
- Aggregation jobs (defer if queries < 200ms)
- Web UI (separate workpack)

---

## Files to Change/Create

### New Files
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java` | REST endpoint |
| `services/backend/src/main/java/com/hometusk/analytics/service/AnalyticsService.java` | Business logic |
| `services/backend/src/main/java/com/hometusk/analytics/service/GiniCalculator.java` | Gini calculation |
| `services/backend/src/main/java/com/hometusk/analytics/dto/AnalyticsSummaryResponse.java` | Response DTO |
| `services/backend/src/main/java/com/hometusk/analytics/dto/MemberStats.java` | Member stats |
| `services/backend/src/main/java/com/hometusk/analytics/dto/ZoneStats.java` | Zone stats |
| `services/backend/src/main/java/com/hometusk/analytics/dto/FairnessInfo.java` | Fairness data |
| `services/backend/src/main/java/com/hometusk/analytics/dto/OverdueTask.java` | Overdue item |
| `services/backend/src/test/java/com/hometusk/analytics/service/GiniCalculatorTest.java` | Gini unit tests |
| `services/backend/src/test/java/com/hometusk/analytics/service/AnalyticsServiceTest.java` | Service tests |
| `services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsControllerIntegrationTest.java` | Integration tests |

### Modified Files
| Path | Changes |
|------|---------|
| `docs/contracts/http/commands.openapi.yaml` | Add analytics endpoint + schemas |

---

## Implementation Plan

### Step 1: Create GiniCalculator
```java
package com.hometusk.analytics.service;

import java.util.Arrays;

public class GiniCalculator {

    /**
     * Calculate Gini coefficient for workload distribution.
     * @param workloads array of task counts per member
     * @return Gini coefficient (0..1), or null if sum=0
     */
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

    /**
     * Calculate balance score from Gini.
     * @param gini Gini coefficient (0..1) or null
     * @return Balance score (0..100) or null
     */
    public static Integer toBalance(Double gini) {
        if (gini == null) {
            return null;
        }
        return (int) Math.round((1 - gini) * 100);
    }
}
```

### Step 2: Create DTOs
```java
// AnalyticsSummaryResponse.java
public record AnalyticsSummaryResponse(
    UUID householdId,
    String period,
    Instant periodStart,
    Instant periodEnd,
    List<MemberStats> perMember,
    List<ZoneStats> perZone,
    FairnessInfo fairness,
    List<OverdueTask> overdueTop
) {}

// MemberStats.java
public record MemberStats(
    UUID memberId,
    String memberName,
    int completedCount,
    int overdueCount,
    int openCount
) {}

// ZoneStats.java
public record ZoneStats(
    UUID zoneId,
    String zoneName,
    int completedCount,
    int overdueCount
) {}

// FairnessInfo.java
public record FairnessInfo(
    Double gini,
    Integer balance,
    String formula,
    String interpretation
) {}

// OverdueTask.java
public record OverdueTask(
    UUID taskId,
    String title,
    String assigneeName,
    int daysOverdue
) {}
```

### Step 3: Create AnalyticsService
```java
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final MembershipRepository membershipRepository;
    private final ZoneRepository zoneRepository;

    public AnalyticsSummaryResponse getAnalytics(UUID householdId, String period) {
        Instant periodStart = calculatePeriodStart(period);
        Instant periodEnd = Instant.now();

        // Query per-member stats
        List<MemberStats> perMember = getMemberStats(householdId, periodStart);

        // Query per-zone stats
        List<ZoneStats> perZone = getZoneStats(householdId, periodStart);

        // Calculate balance
        int[] workloads = perMember.stream()
            .mapToInt(MemberStats::completedCount)
            .toArray();
        Double gini = GiniCalculator.calculate(workloads);
        Integer balance = GiniCalculator.toBalance(gini);

        FairnessInfo fairness = new FairnessInfo(
            gini,
            balance,
            "Balance = 100 × (1 - Gini coefficient)",
            generateInterpretation(balance)
        );

        // Top overdue (max 5)
        List<OverdueTask> overdueTop = getTopOverdue(householdId, 5);

        return new AnalyticsSummaryResponse(
            householdId, period, periodStart, periodEnd,
            perMember, perZone, fairness, overdueTop
        );
    }

    private Instant calculatePeriodStart(String period) {
        return switch (period) {
            case "30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> Instant.now().minus(7, ChronoUnit.DAYS);
        };
    }

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

    // ... repository query methods
}
```

### Step 4: Create AnalyticsController
```java
@RestController
@RequestMapping("/api/v1/households/{householdId}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    @GetMapping
    public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
            @PathVariable UUID householdId,
            @RequestParam(defaultValue = "7d") String period) {

        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, period);
        return ResponseEntity.ok(response);
    }
}
```

### Step 5: Add Repository Queries
Add to TaskRepository or create dedicated query methods:
```java
// Count completed by assignee in period
// Count open/overdue by assignee
// Count by zone
// Get top N overdue tasks
```

### Step 6: Write Unit Tests
- GiniCalculatorTest: various distributions
- AnalyticsServiceTest: mocked repository

### Step 7: Write Integration Tests
- AnalyticsControllerIntegrationTest

### Step 8: Update OpenAPI
Add endpoint and schemas to `docs/contracts/http/commands.openapi.yaml`

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Unit tests
./gradlew test --tests "*GiniCalculatorTest*"
./gradlew test --tests "*AnalyticsServiceTest*"

# Integration tests
./gradlew test --tests "*AnalyticsControllerIntegrationTest*"

# All analytics tests
./gradlew test --tests "*Analytics*" --tests "*Gini*"

# Spotless
./gradlew spotlessCheck
./gradlew spotlessApply
```

---

## DB/Index Notes
- Primary query: tasks grouped by assignee/zone with status filter
- Expected: < 100ms for households with < 1000 tasks
- If slow: add index
```sql
CREATE INDEX idx_tasks_analytics
ON tasks(household_id, status, completed_at);
```

---

## Observability
- Log: householdId, period, latency_ms
- No PII (no emails in logs)

---

## Risks
| Risk | Mitigation |
|------|------------|
| Gini calculation edge cases | Comprehensive unit tests |
| Query performance | Test with 1000+ tasks, add index if needed |
| Null handling in serialization | Use @JsonInclude or explicit null handling |

---

## Rollback
- Remove AnalyticsController
- Revert OpenAPI changes
- Web gracefully shows "Analytics unavailable"

---

## DoD Checklist
See `checklist.md`
