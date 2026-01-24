# Codex PLAN Prompt: ST-701 + ST-702 — Backend Analytics + Balance Score

## Mode
**PLAN ONLY** — Read-only exploration. NO file modifications allowed.

## Allowed Commands (Whitelist)
```
ls, find, cat, rg, grep, sed -n, head, tail, git status, git diff
```

## Forbidden
- Any file edits/writes/moves/deletes
- Network access
- Package install
- git commit/push
- Database operations
- ./gradlew build (wait for APPLY phase)

---

## Task
Plan the implementation of analytics endpoint with Gini-based balance score for workload fairness measurement.

## Sources of Truth (MUST READ)
1. `docs/planning/workpacks/ST-701-702/workpack.md` — Implementation plan (AUTHORITATIVE)
2. `docs/planning/epics/EP-008/epic.md` — Epic with API contract schema
3. `docs/planning/epics/EP-008/stories/ST-701-analytics-endpoints.md` — Endpoint story
4. `docs/planning/epics/EP-008/stories/ST-702-fairness-index.md` — Balance score story
5. `docs/contracts/http/commands.openapi.yaml` — Existing API contract
6. `services/backend/src/main/java/com/hometusk/tasks/` — Existing task domain
7. `services/backend/src/main/java/com/hometusk/users/` — Existing user/membership domain
8. `services/backend/src/main/java/com/hometusk/households/` — Existing household domain

---

## Critical Constraints (MUST FOLLOW)

### 1. Gini Formula (Use Exactly)
```java
// If sum(workloads) = 0 → return null
// Else:
//   Sort workloads ascending
//   G = Σᵢ (2i - n - 1) × wᵢ / (n × Σ wᵢ)
//   gini = Math.abs(G), clamped to 0..1
//   balance = round((1 - gini) * 100) or null
```

### 2. Edge Cases (Must Handle)
| Condition | gini | balance |
|-----------|------|---------|
| sum(workload) = 0 | null | null |
| n = 0 members | null | null |
| Single member with tasks | 0 | 100 |
| All members equal | 0 | 100 |

### 3. Security (Household Boundary)
- `membershipService.requireMembership()` MUST be called in controller
- All queries MUST filter by `household_id`
- Return 403 (not 404) for non-members

### 4. Package Structure
- All new classes in `com.hometusk.analytics.*`
- Sub-packages: `api/`, `service/`, `dto/`

---

## Exploration Tasks

### Task 1: Understand Task Repository
- Read `services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java`
- What query methods exist?
- Can we add custom queries for analytics?
- What is the Task entity structure (status, completed_at, deadline, assignee, zone)?

### Task 2: Understand Membership Infrastructure
- Read `services/backend/src/main/java/com/hometusk/users/service/MembershipService.java`
- How does `requireMembership()` work?
- What exceptions does it throw?

### Task 3: Check Existing Controller Patterns
- Read any existing controller (e.g., `NotificationController.java`, `TaskController.java`)
- How is `CurrentUser` resolved?
- How is membership enforced?
- What response patterns are used?

### Task 4: Check Zone Repository
- Read `services/backend/src/main/java/com/hometusk/zones/` (if exists) or `households/`
- How are zones queried?
- Can we join tasks with zones for zone stats?

### Task 5: Verify OpenAPI Structure
- Read `docs/contracts/http/commands.openapi.yaml`
- Where should new endpoint be added?
- What schema patterns are used?

### Task 6: Check for Existing DTOs
- Are there record-based DTOs in the codebase?
- What naming conventions are used?
- Is there a common pattern for response objects?

---

## Files Expected to Create
| Path | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/analytics/api/AnalyticsController.java` | REST endpoint |
| `services/backend/src/main/java/com/hometusk/analytics/service/AnalyticsService.java` | Business logic |
| `services/backend/src/main/java/com/hometusk/analytics/service/GiniCalculator.java` | Gini calculation utility |
| `services/backend/src/main/java/com/hometusk/analytics/dto/AnalyticsSummaryResponse.java` | Response DTO |
| `services/backend/src/main/java/com/hometusk/analytics/dto/MemberStats.java` | Member stats record |
| `services/backend/src/main/java/com/hometusk/analytics/dto/ZoneStats.java` | Zone stats record |
| `services/backend/src/main/java/com/hometusk/analytics/dto/FairnessInfo.java` | Fairness data record |
| `services/backend/src/main/java/com/hometusk/analytics/dto/OverdueTask.java` | Overdue item record |
| `services/backend/src/test/java/com/hometusk/analytics/service/GiniCalculatorTest.java` | Gini unit tests |
| `services/backend/src/test/java/com/hometusk/analytics/service/AnalyticsServiceTest.java` | Service unit tests |
| `services/backend/src/test/java/com/hometusk/analytics/api/AnalyticsControllerIntegrationTest.java` | Integration tests |

## Files Expected to Modify
| Path | Change |
|------|--------|
| `docs/contracts/http/commands.openapi.yaml` | Add analytics endpoint + schemas |

## Forbidden Paths (Do NOT Touch)
- `services/backend/src/main/resources/db/migration/**` (no migrations unless confirmed)
- Files outside `services/backend/` and `docs/contracts/`
- `AGENTS.md`, `CLAUDE.md` (read-only)
- Existing controllers (do not modify, create new AnalyticsController)

---

## Output Format

After exploration, provide:

### 1. Verification of Assumptions
- Confirm TaskRepository structure and available methods
- Confirm Task entity fields (status enum, completed_at type, deadline type)
- Confirm MembershipService.requireMembership() signature
- Confirm Zone entity/repository exists

### 2. Repository Queries Needed
For AnalyticsService, identify:
- Query for completed tasks by assignee in period
- Query for open/overdue tasks by assignee
- Query for completed/overdue tasks by zone
- Query for top N overdue tasks

### 3. Files to Create (with key interfaces/methods)
List each file with:
- Purpose
- Key methods/fields

### 4. Files to Modify (with specific changes)
List each file with:
- What section to change
- What to add

### 5. Risks/Blockers
- Any unexpected codebase structure?
- Missing dependencies?
- Query performance concerns?

### 6. Questions (if any)
- Clarifications needed before APPLY phase

---

## Stop Conditions

If any of these occur, STOP and describe:
- Task entity structure differs from expected (no completed_at, no assignee, etc.)
- No MembershipService or equivalent for boundary enforcement
- Zone entity/repository missing
- Existing analytics package conflicts
- Missing test infrastructure (Testcontainers, etc.)

Do NOT guess without evidence from codebase.
