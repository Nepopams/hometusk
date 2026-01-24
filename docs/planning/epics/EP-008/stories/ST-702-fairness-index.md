# Story: ST-702 — Balance Score Calculation (Gini-based)

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Part of Sprint S07 committed scope

## User Value
> "Хочу видеть один понятный индекс 'насколько сбалансировано распределение' с объяснением как он считается — чтобы можно было обсудить факты, а не ощущения."

---

## Description
Implement balance score calculation using Gini coefficient:
- `gini` = Gini coefficient of workload distribution (0..1)
- `balance` = round((1 - gini) * 100) (0..100)
- Handle edge cases explicitly
- Return formula explanation + interpretation

**Workload definition (v0):** Count of completed tasks per member in period.
**Limitation:** No complexity/effort weighting in v0.

---

## Formula

### Gini Coefficient
```
Given workloads w[] for n members:

If sum(w) = 0:
    gini = null
    balance = null

Else:
    Sort w ascending
    G = Σᵢ (2i - n - 1) × wᵢ / (n × Σ wᵢ)
    gini = abs(G)  // ensure 0..1
    balance = round((1 - gini) × 100)
```

### Edge Cases

| Condition | gini | balance | interpretation |
|-----------|------|---------|----------------|
| sum(workload) = 0 | null | null | "N/A — no tasks completed in this period" |
| n = 1 member with tasks | 0 | 100 | "Single active member" |
| All equal | 0 | 100 | "Perfect balance" |
| One has all | ~0.67 (for n=3) | ~33 | "Significant imbalance" |

---

## Acceptance Criteria

### AC-1: Balance calculated correctly for equal distribution
```
Given members A, B, C each completed 5 tasks
When calculating balance
Then gini ≈ 0
And balance = 100
And interpretation contains "excellent" or "perfect"
```

### AC-2: Balance calculated correctly for unequal distribution
```
Given members: A=10 tasks, B=5 tasks, C=0 tasks
When calculating balance
Then gini > 0.3
And balance < 70
And interpretation mentions "imbalance"
```

### AC-3: Edge case — no tasks in period
```
Given 0 completed tasks in period
When calculating balance
Then gini = null
And balance = null
And interpretation = "N/A — no tasks completed in this period"
```

### AC-4: Edge case — single member with tasks
```
Given only member A has tasks (others have 0)
When calculating balance
Then gini calculated normally (reflects inequality)
And interpretation reflects distribution
```

### AC-5: Formula explanation included
```
When response returned
Then fairness.formula = "Balance = 100 × (1 - Gini coefficient)"
```

### AC-6: Interpretation text non-toxic
```
When generating interpretation
Then text uses "balance" not "fairness"
And no "winner/loser" language
And no blame ("Carol did less" OK, "Carol is lazy" NOT OK)
```

---

## Score Interpretation Logic

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

---

## Data Sources

| Table | Fields |
|-------|--------|
| `tasks` | assignee_id, status=DONE, completed_at, household_id |

Query:
```sql
SELECT assignee_id, COUNT(*) as completed_count
FROM tasks
WHERE household_id = :householdId
  AND status = 'DONE'
  AND completed_at >= :periodStart
GROUP BY assignee_id
```

---

## Contract Impact
**Yes** — FairnessInfo schema in AnalyticsSummary (see epic.md).

---

## Test Notes

### Unit Tests: GiniCalculatorTest
- `calculate_equalDistribution_returnsZero`
- `calculate_completeInequality_returnsHigh`
- `calculate_typicalDistribution_returnsMedium`
- `calculate_emptyWorkloads_returnsNull`
- `calculate_singleNonZero_calculatesCorrectly`
- `calculate_allZeros_returnsNull`

### Integration Tests
- `getAnalytics_withTasks_returnsBalanceScore`
- `getAnalytics_noTasks_returnsNullBalance`

---

## Points
**3 points**

---

## Flags
- contract_impact: yes
- security_sensitive: no (calculation only)
