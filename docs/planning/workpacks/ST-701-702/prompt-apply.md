# Codex APPLY Prompt: ST-701 + ST-702 — Backend Analytics + Balance Score

## Mode
**APPLY** — Implementation mode. File modifications allowed.

## Allowed Operations
```
- Create/edit Java files in services/backend/src/
- Edit OpenAPI contract in docs/contracts/
- Run gradle build/test commands
```

## Forbidden
- Modifying files outside listed paths
- Changing unrelated functionality
- Adding features not in the plan
- Skipping tests
- Hardcoding secrets or credentials
- Creating DB migrations (not needed for this story)

---

## Task
Implement analytics endpoint with Gini-based balance score for workload fairness measurement.

## Sources of Truth (MUST READ BEFORE IMPLEMENTATION)
1. `docs/planning/workpacks/ST-701-702/workpack.md` — Implementation plan (AUTHORITATIVE)
2. `docs/planning/epics/EP-008/epic.md` — Epic with full API contract schema
3. `docs/planning/workpacks/ST-701-702/checklist.md` — DoD checklist
4. `docs/_governance/dod.md` — Definition of Done

---

## From PLAN Phase Verification

### Confirmed Codebase Structure
- **TaskRepository** provides household/assignee/zone filters; no period/overdue aggregates exist yet — need to add custom queries
- **Task entity** has `status` (OPEN/IN_PROGRESS/DONE/CANCELLED), `completedAt` and `deadline` as `Instant`, nullable `assignee` and `zone`
- **MembershipService.requireMembership(UUID userId, UUID householdId)** throws `AccessDeniedException` → 403
- **Zone entity and ZoneRepository** exist with household-scoped lookups
- **completedAt has no setter** — integration tests need reflection or direct SQL for historical dates

### Decisions Resolved (Human Gate Approved)
| Decision | Choice |
|----------|--------|
| `openCount` definition | Active tasks EXCLUDING overdue (open = active - overdue) |
| Overdue scope | Current only (no period filter on overdue) |
| assignee = null | Show as "Unassigned" in stats and overdueTop |
| zone = null | Exclude from zone stats (v0 simplicity) |
| Invalid period param | Default to 7d (no 400 error) |
| deadline = null | Exclude from overdue queries (WHERE deadline IS NOT NULL) |

---

## Critical Constraints (MUST FOLLOW)

### 1. Gini Formula (Use Exactly)
```java
public class GiniCalculator {

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
}
```

### 2. Edge Cases
| Condition | gini | balance |
|-----------|------|---------|
| sum(workload) = 0 | null | null |
| n = 0 members | null | null |
| Single member with tasks | 0 | 100 |
| All members equal | 0 | 100 |

### 3. Interpretation Text (Use Exactly)
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

### 4. Formula Field
Always return: `"Balance = 100 × (1 - Gini coefficient)"`

### 5. Controller Security
```java
@GetMapping
public ResponseEntity<AnalyticsSummaryResponse> getAnalytics(
        @PathVariable UUID householdId,
        @RequestParam(defaultValue = "7d") String period) {

    CurrentUser user = userResolver.resolveCurrentUser();
    membershipService.requireMembership(user.id(), householdId);  // REQUIRED - 403 if not member

    AnalyticsSummaryResponse response = analyticsService.getAnalytics(householdId, period);
    return ResponseEntity.ok(response);
}
```

---

## Implementation Steps

### Step 1: Create Package Structure
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
```

### Step 2: Create DTOs (as Java records)
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

### Step 3: Create GiniCalculator
Use exact formula from Critical Constraints section above.

### Step 4: Add Repository Queries to TaskRepository
```java
// Count completed tasks by assignee in period
@Query("""
    SELECT t.assignee.id, COUNT(t)
    FROM Task t
    WHERE t.household.id = :householdId
      AND t.status = 'DONE'
      AND t.completedAt >= :periodStart
      AND t.completedAt < :periodEnd
    GROUP BY t.assignee.id
    """)
List<Object[]> countCompletedByAssigneeInPeriod(
    @Param("householdId") UUID householdId,
    @Param("periodStart") Instant periodStart,
    @Param("periodEnd") Instant periodEnd);

// Count overdue tasks by assignee (current, deadline NOT NULL)
@Query("""
    SELECT t.assignee.id, COUNT(t)
    FROM Task t
    WHERE t.household.id = :householdId
      AND t.status IN ('OPEN', 'IN_PROGRESS')
      AND t.deadline IS NOT NULL
      AND t.deadline < :now
    GROUP BY t.assignee.id
    """)
List<Object[]> countOverdueByAssignee(
    @Param("householdId") UUID householdId,
    @Param("now") Instant now);

// Count open (non-overdue) tasks by assignee
@Query("""
    SELECT t.assignee.id, COUNT(t)
    FROM Task t
    WHERE t.household.id = :householdId
      AND t.status IN ('OPEN', 'IN_PROGRESS')
      AND (t.deadline IS NULL OR t.deadline >= :now)
    GROUP BY t.assignee.id
    """)
List<Object[]> countOpenByAssignee(
    @Param("householdId") UUID householdId,
    @Param("now") Instant now);

// Count completed tasks by zone in period (exclude null zone)
@Query("""
    SELECT t.zone.id, COUNT(t)
    FROM Task t
    WHERE t.household.id = :householdId
      AND t.status = 'DONE'
      AND t.completedAt >= :periodStart
      AND t.completedAt < :periodEnd
      AND t.zone IS NOT NULL
    GROUP BY t.zone.id
    """)
List<Object[]> countCompletedByZoneInPeriod(
    @Param("householdId") UUID householdId,
    @Param("periodStart") Instant periodStart,
    @Param("periodEnd") Instant periodEnd);

// Count overdue tasks by zone (exclude null zone)
@Query("""
    SELECT t.zone.id, COUNT(t)
    FROM Task t
    WHERE t.household.id = :householdId
      AND t.status IN ('OPEN', 'IN_PROGRESS')
      AND t.deadline IS NOT NULL
      AND t.deadline < :now
      AND t.zone IS NOT NULL
    GROUP BY t.zone.id
    """)
List<Object[]> countOverdueByZone(
    @Param("householdId") UUID householdId,
    @Param("now") Instant now);

// Top N overdue tasks
@Query("""
    SELECT t
    FROM Task t
    LEFT JOIN FETCH t.assignee
    WHERE t.household.id = :householdId
      AND t.status IN ('OPEN', 'IN_PROGRESS')
      AND t.deadline IS NOT NULL
      AND t.deadline < :now
    ORDER BY t.deadline ASC
    """)
List<Task> findTopOverdueTasks(
    @Param("householdId") UUID householdId,
    @Param("now") Instant now,
    Pageable pageable);
```

### Step 5: Create AnalyticsService
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final MembershipRepository membershipRepository;
    private final ZoneRepository zoneRepository;

    public AnalyticsSummaryResponse getAnalytics(UUID householdId, String period) {
        Instant now = Instant.now();
        Instant periodStart = calculatePeriodStart(period, now);
        Instant periodEnd = now;

        // Get all members for the household (to include zero-count members)
        List<Membership> memberships = membershipRepository.findByHousehold_Id(householdId);
        Map<UUID, String> memberNames = memberships.stream()
            .collect(Collectors.toMap(
                m -> m.getUser().getId(),
                m -> m.getUser().getDisplayName() != null
                    ? m.getUser().getDisplayName()
                    : "Unassigned"
            ));

        // Query stats
        Map<UUID, Long> completedByMember = toMap(
            taskRepository.countCompletedByAssigneeInPeriod(householdId, periodStart, periodEnd));
        Map<UUID, Long> overdueByMember = toMap(
            taskRepository.countOverdueByAssignee(householdId, now));
        Map<UUID, Long> openByMember = toMap(
            taskRepository.countOpenByAssignee(householdId, now));

        // Build perMember (include all members, even with zero counts)
        List<MemberStats> perMember = memberships.stream()
            .map(m -> {
                UUID id = m.getUser().getId();
                return new MemberStats(
                    id,
                    memberNames.getOrDefault(id, "Unassigned"),
                    completedByMember.getOrDefault(id, 0L).intValue(),
                    overdueByMember.getOrDefault(id, 0L).intValue(),
                    openByMember.getOrDefault(id, 0L).intValue()
                );
            })
            .toList();

        // Add stats for unassigned tasks (assignee = null)
        // Note: queries return null key for unassigned, handle separately if needed

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

        // Zone stats
        List<Zone> zones = zoneRepository.findByHouseholdId(householdId);
        Map<UUID, String> zoneNames = zones.stream()
            .collect(Collectors.toMap(Zone::getId, Zone::getName));

        Map<UUID, Long> completedByZone = toMap(
            taskRepository.countCompletedByZoneInPeriod(householdId, periodStart, periodEnd));
        Map<UUID, Long> overdueByZone = toMap(
            taskRepository.countOverdueByZone(householdId, now));

        List<ZoneStats> perZone = zones.stream()
            .map(z -> new ZoneStats(
                z.getId(),
                z.getName(),
                completedByZone.getOrDefault(z.getId(), 0L).intValue(),
                overdueByZone.getOrDefault(z.getId(), 0L).intValue()
            ))
            .toList();

        // Top overdue
        List<Task> overdueTasks = taskRepository.findTopOverdueTasks(
            householdId, now, PageRequest.of(0, 5));
        List<OverdueTask> overdueTop = overdueTasks.stream()
            .map(t -> new OverdueTask(
                t.getId(),
                t.getTitle(),
                t.getAssignee() != null
                    ? (t.getAssignee().getDisplayName() != null
                        ? t.getAssignee().getDisplayName()
                        : "Unassigned")
                    : "Unassigned",
                (int) ChronoUnit.DAYS.between(t.getDeadline(), now)
            ))
            .toList();

        return new AnalyticsSummaryResponse(
            householdId, period, periodStart, periodEnd,
            perMember, perZone, fairness, overdueTop
        );
    }

    private Instant calculatePeriodStart(String period, Instant now) {
        return switch (period) {
            case "30d" -> now.minus(30, ChronoUnit.DAYS);
            default -> now.minus(7, ChronoUnit.DAYS); // Default to 7d for any invalid value
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

    private Map<UUID, Long> toMap(List<Object[]> results) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : results) {
            UUID key = (UUID) row[0];
            Long count = (Long) row[1];
            if (key != null) {
                map.put(key, count);
            }
        }
        return map;
    }
}
```

### Step 6: Create AnalyticsController
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

### Step 7: Write Unit Tests

#### GiniCalculatorTest
```java
@Test void calculate_equalDistribution_returnsZero()
@Test void calculate_completeInequality_returnsHigh()
@Test void calculate_typicalDistribution_returnsMedium()
@Test void calculate_emptyArray_returnsNull()
@Test void calculate_nullArray_returnsNull()
@Test void calculate_allZeros_returnsNull()
@Test void calculate_singleNonZero_returnsZero()
@Test void toBalance_nullGini_returnsNull()
@Test void toBalance_zeroGini_returns100()
@Test void toBalance_halfGini_returns50()
```

#### AnalyticsServiceTest
```java
@Test void getAnalytics_withTasks_calculatesBalance()
@Test void getAnalytics_noTasks_returnsNullBalance()
@Test void getAnalytics_periodDefault_uses7d()
@Test void getAnalytics_period30d_uses30d()
@Test void getAnalytics_invalidPeriod_defaultsTo7d()
@Test void generateInterpretation_balance90_returnsExcellent()
@Test void generateInterpretation_balance70_returnsGood()
@Test void generateInterpretation_balance50_returnsModerate()
@Test void generateInterpretation_balance30_returnsSignificant()
@Test void generateInterpretation_balance10_returnsSevere()
@Test void generateInterpretation_null_returnsNA()
```

### Step 8: Write Integration Tests

#### AnalyticsControllerIntegrationTest
Use existing `IntegrationTestBase` pattern with Testcontainers.

```java
@Test void getAnalytics_asMember_returnsData()
@Test void getAnalytics_notMember_returns403()
@Test void getAnalytics_unauthenticated_returns401()
@Test void getAnalytics_periodFilter_filtersCorrectly()
@Test void getAnalytics_crossHousehold_noLeaks()
@Test void getAnalytics_overdueTop_limitedTo5()
@Test void getAnalytics_noTasks_returnsNullBalance()
@Test void getAnalytics_equalDistribution_returns100Balance()
```

For tests needing historical `completedAt`, use reflection:
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

### Step 9: Update OpenAPI Contract

Add to `docs/contracts/http/commands.openapi.yaml`:

```yaml
# Under paths:
  /households/{householdId}/analytics:
    get:
      operationId: getAnalytics
      summary: Get household analytics summary
      tags:
        - Analytics
      parameters:
        - name: householdId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: period
          in: query
          required: false
          description: Time period for analytics (7d or 30d)
          schema:
            type: string
            enum: [7d, 30d]
            default: 7d
      responses:
        '200':
          description: Analytics summary
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AnalyticsSummary'
        '401':
          description: Authentication required
        '403':
          description: Not a member of this household

# Under components/schemas (add all from epic.md):
# AnalyticsSummary, MemberStats, ZoneStats, FairnessInfo, OverdueTask
```

---

## Verification Commands

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

# Spotless check
./gradlew spotlessCheck
```

---

## DoD Checklist (Verify Before Done)

- [ ] Package structure: `com.hometusk.analytics.*`
- [ ] GiniCalculator handles all edge cases (null, empty, zero sum, single member)
- [ ] Balance formula: `round((1 - gini) * 100)`
- [ ] Interpretation text is non-toxic (no blame language)
- [ ] Formula field = `"Balance = 100 × (1 - Gini coefficient)"`
- [ ] `membershipService.requireMembership()` called in controller
- [ ] 403 returned for non-members (not 404)
- [ ] No cross-household data leaks in queries (WHERE household_id = ?)
- [ ] OpenAPI updated with endpoint + all schemas
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Spotless check passes
- [ ] No compiler warnings

---

## STOP-THE-LINE Rules

If any of these occur, STOP and report:
- Cannot add queries to TaskRepository (interface issues)
- MembershipService.requireMembership() not available
- ZoneRepository.findByHouseholdId() missing
- Tests fail unexpectedly
- Missing dependencies
- Reflection for completedAt not working in tests

Do NOT proceed with workarounds without approval.
