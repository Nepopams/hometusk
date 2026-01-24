# Epic: EP-008 — Analytics & Fairness Dashboard v0

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Existing Backend: `services/backend/src/main/java/com/hometusk/`

---

## Status
**Done** — Closed 2026-01-24 (Sprint S07)

## Initiative Alignment
This epic implements the **NOW** increment of INIT-2026Q2-analytics-fairness-dashboard:
- Analytics endpoint (task/zone/member breakdown)
- Fairness index with transparent Gini-based formula
- Web analytics page with period filters
- Household boundary enforcement

**Product Goal Pillar:** Fairness & Transparency + Analytics-first Web

---

## Epic Goal
Enable household members to:
1. See **who does what** — completed tasks by member, zone, period
2. See **workload distribution** — balance score with transparent Gini-based formula
3. Identify **bottlenecks** — overdue tasks, underserved zones
4. Filter by **time period** — 7 days / 30 days

**Non-AI commitment:** All calculations are deterministic formulas with documented logic. No ML, no magic.

---

## Outcome (User Value)
> "Открываю Analytics → вижу кто сколько сделал за неделю, какие зоны запущены, насколько нагрузка сбалансирована. Формула понятна (Gini), данные прозрачны, спор переходит в факты."

---

## Key Decisions (Resolved)

### Decision A: Fairness Metric — Gini-based Balance Score
**Choice:** Gini coefficient (inverted, scaled to 0–100)

**Formula:**
```
g = Gini(workload[])           // 0..1, where 0 = perfect equality
balance = round((1 - g) * 100) // 0..100, where 100 = perfect balance
```

**Why Gini:**
- Bounded 0..1 (no negative values, no overflow)
- Industry standard for inequality measurement
- Stable for any distribution

**Edge cases:**
- `sum(workload) = 0` → `balance = null`, UI shows "N/A — no tasks completed in period"
- `members.length < 2` → calculated normally, interpretation reflects distribution

**Rejected alternatives:**
- CV (stdev/mean): Unstable when mean≈0, can exceed 1 → negative fairness
- Range ratio: Ignores middle values

### Decision B: Workload Definition — Count of Completed Tasks
**Choice:** Simple count of completed tasks per member in period.

**Limitations (explicit):**
- Does NOT capture task complexity or effort
- All tasks weighted equally
- Weighting by points/complexity deferred to future initiative (gamification)

### Decision C: Visibility — All Members (with mitigation)
**Choice:** Analytics visible to all household members by default.

**Mitigation (non-toxic wording):**
- UI uses "balance" not "fairness score"
- No "winner/loser" language
- Interpretation text focuses on "balance" and "distribution", not blame
- Future option (not v0): household toggle to hide balance score

### Decision D: Time Windows — Fixed 7d/30d
**Choice:** Fixed periods only in v0. Custom date range deferred.

### Decision E: Scope — Tasks Only
**Choice:** Tasks only in v0. Shopping analytics deferred to next iteration.

---

## Explainability: How Balance Score Works

### The Gini Coefficient
The Gini coefficient measures inequality in a distribution. Originally used for income inequality, it works perfectly for workload distribution.

**Calculation (simplified):**
1. Sort members by workload (completed tasks) ascending
2. Calculate cumulative share vs. equal share
3. Gini = area between actual curve and perfect equality line

**Formula (discrete):**
```
Given workloads w[] sorted ascending, n = length:
G = Σᵢ (2i - n - 1) × wᵢ / (n × Σ wᵢ)
```

**Example:**
```
Household with 3 members:
- Alice: 8 tasks completed
- Bob: 6 tasks completed
- Carol: 2 tasks completed
Total: 16 tasks

Sorted workloads: [2, 6, 8]

Gini calculation (using mean absolute difference formula):
G = Σ|xi - xj| / (2 × n × mean)
  = (|2-6| + |2-8| + |6-8|) / (2 × 3 × 5.33)
  = (4 + 6 + 2) / 32 = 12/32 = 0.375

Balance = round((1 - 0.375) × 100) = 63

Interpretation: "Balance score 63 — moderate balance.
Carol completed fewer tasks than others this period."
```

**Score interpretation:**
| Balance | Meaning |
|---------|---------|
| 90–100 | Excellent balance |
| 70–89 | Good balance |
| 50–69 | Moderate imbalance |
| 30–49 | Significant imbalance |
| 0–29 | Severe imbalance |
| null | N/A (no tasks in period) |

---

## In Scope (This Epic)

### Analytics Endpoint (ST-701)
- `GET /api/v1/households/{householdId}/analytics`
  - Period filter: `period=7d|30d` (default: 7d)
  - Returns: AnalyticsSummary (see schema below)

### Balance Score Calculation (ST-702)
- Gini-based formula as defined above
- Edge case handling
- Formula explanation in response

### Web Analytics Page (ST-703)
- New route: `/households/{id}/analytics`
- Member contribution list
- Zone breakdown
- Overdue highlight (top 5)
- Balance score with expandable explanation
- Non-toxic wording throughout

### Period Filters (ST-704)
- 7 days / 30 days toggle
- Default: 7 days

### Security & Boundary Tests (ST-705)
- Household boundary enforcement
- No cross-household data
- Integration tests for 403 scenarios

### Observability (ST-706)
- Structured logging (no PII)
- Metrics for analytics requests

---

## Out of Scope (Explicit)

### Deferred to NEXT
- **Shopping analytics** — tasks only in v0
- **Custom date ranges** — fixed periods only
- **Weighted workload** — all tasks equal in v0
- **Household toggle for balance visibility** — documented for future
- **Trends/comparisons** ("better than last week")
- **Charts/graphs** — simple lists for v0
- **Export/download**

### Never in Scope
- ML/AI predictions
- Cross-household benchmarks
- Leaderboards with ranking
- Quality scoring

---

## Security & Privacy Checklist

| Check | Status | Notes |
|-------|--------|-------|
| Household boundary on all endpoints | Required | 403 if not member |
| No cross-household aggregation | Required | Single household per query |
| No IDOR | Required | UUID + membership check |
| Visible to all members | Default | Non-toxic wording mitigates |
| No PII in logs | Required | Only householdId, userId hashes |

---

## Data Sources

### Primary: `tasks` table
- `status` (DONE for completed)
- `assignee_id` (who completed)
- `zone_id` (where)
- `household_id` (scope)
- `completed_at` (when)
- `deadline` (for overdue check)

### Workload = COUNT(tasks WHERE status=DONE AND completed_at in period)

**Limitation:** No complexity weighting in v0.

---

## API Contract

### Endpoint
```yaml
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
        description: Time period for analytics
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
```

### Schema: AnalyticsSummary
```yaml
AnalyticsSummary:
  type: object
  required:
    - householdId
    - period
    - periodStart
    - periodEnd
    - perMember
    - perZone
    - fairness
  properties:
    householdId:
      type: string
      format: uuid
    period:
      type: string
      enum: [7d, 30d]
    periodStart:
      type: string
      format: date-time
    periodEnd:
      type: string
      format: date-time
    perMember:
      type: array
      description: Stats per household member
      items:
        $ref: '#/components/schemas/MemberStats'
    perZone:
      type: array
      description: Stats per zone
      items:
        $ref: '#/components/schemas/ZoneStats'
    fairness:
      $ref: '#/components/schemas/FairnessInfo'
    overdueTop:
      type: array
      description: Top overdue tasks (optional, max 5)
      items:
        $ref: '#/components/schemas/OverdueTask'

MemberStats:
  type: object
  required:
    - memberId
    - memberName
    - completedCount
    - overdueCount
    - openCount
  properties:
    memberId:
      type: string
      format: uuid
    memberName:
      type: string
    completedCount:
      type: integer
      minimum: 0
      description: Tasks completed in period
    overdueCount:
      type: integer
      minimum: 0
      description: Currently overdue tasks assigned to member
    openCount:
      type: integer
      minimum: 0
      description: Currently open tasks assigned to member

ZoneStats:
  type: object
  required:
    - zoneId
    - zoneName
    - completedCount
    - overdueCount
  properties:
    zoneId:
      type: string
      format: uuid
    zoneName:
      type: string
    completedCount:
      type: integer
      minimum: 0
      description: Tasks completed in zone during period
    overdueCount:
      type: integer
      minimum: 0
      description: Currently overdue tasks in zone

FairnessInfo:
  type: object
  required:
    - gini
    - balance
    - formula
    - interpretation
  properties:
    gini:
      type: number
      format: float
      minimum: 0
      maximum: 1
      nullable: true
      description: Gini coefficient (0=equal, 1=unequal). Null if no tasks.
    balance:
      type: integer
      minimum: 0
      maximum: 100
      nullable: true
      description: Balance score = round((1-gini)*100). Null if no tasks.
    formula:
      type: string
      description: Human-readable formula explanation
      example: "Balance = 100 × (1 - Gini coefficient)"
    interpretation:
      type: string
      description: What this score means for the household
      example: "Balance score 75 — good distribution of tasks among members."

OverdueTask:
  type: object
  required:
    - taskId
    - title
    - assigneeName
    - daysOverdue
  properties:
    taskId:
      type: string
      format: uuid
    title:
      type: string
    assigneeName:
      type: string
    daysOverdue:
      type: integer
      minimum: 1
```

---

## Stories

| ID | Title | Status | Priority | Points |
|----|-------|--------|----------|--------|
| ST-701 | Analytics Summary Endpoint | Ready | P1 | 3 |
| ST-702 | Balance Score Calculation (Gini) | Ready | P1 | 3 |
| ST-703 | Web Analytics Page | Ready | P1 | 5 |
| ST-704 | Period Filters | Ready | P2 | 2 |
| ST-705 | Security & Boundary Tests | Ready | P1 | 2 |
| ST-706 | Observability Hooks | Ready | P3 | 1 |

**Total:** 16 points

### Sprint Delivery
- **Sprint S07:** ST-701, ST-702, ST-703, ST-705 (13 points) — committed
- **Stretch:** ST-704, ST-706 (3 points)

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| EP-003 (Web Foundation) | Internal | Done |
| EP-004 (Auth/Session) | Internal | Done |
| EP-005 (Household Lifecycle) | Internal | Done |
| Tasks table | Internal | Done |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Balance score misunderstood | User confusion | Clear formula + interpretation in UI |
| "Balance" triggers conflict | UX harm | Non-toxic wording: no blame |
| Performance on large households | Slow queries | Index on completed_at; test early |
| Future: need to hide balance | Privacy | Document toggle option for v1 |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Analytics page load (p95) | < 500ms |
| Analytics adoption | 30% households view 1x/week |
| Query performance | < 200ms |
| Zero cross-household leaks | 0 incidents |

---

## Exit Criteria

1. Analytics endpoint returns correct perMember/perZone/fairness data
2. Balance score calculated with Gini formula
3. Edge cases handled (no tasks → null)
4. Web page displays data with non-toxic wording
5. Household boundary enforced (403 tests)
6. Build passes, tests pass
7. OpenAPI contract updated

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | yes | New analytics endpoint |
| adr_needed | no | Standard query + formula |
| diagrams_needed | no | No structural changes |
| security_sensitive | yes | Household boundary |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md` |
| OpenAPI | `docs/contracts/http/commands.openapi.yaml` |
| Sprint | `docs/planning/sprints/S07/sprint.md` |
| Workpack ST-701-702 | `docs/planning/workpacks/ST-701-702/` |
| Workpack ST-703-704 | `docs/planning/workpacks/ST-703-704/` |
| Workpack ST-705-706 | `docs/planning/workpacks/ST-705-706/` |
