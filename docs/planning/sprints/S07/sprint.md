# Sprint S07: Analytics & Fairness Dashboard v0

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md`
- Epic: `docs/planning/epics/EP-008/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Human Gate approved (2026-01-24)

---

## Sprint Goal
Deliver **E2E thin slice**: user opens Analytics page → sees task breakdown by member/zone for last 7 days + balance score (Gini-based) with explanation. Non-toxic wording, no charts — just working MVP.

---

## Thin Slice Definition
**"Open Analytics → see who did what this week + balance score"**

This validates:
1. Backend analytics query works
2. Gini calculation is correct
3. Web UI displays data with non-toxic wording
4. Household boundaries enforced

---

## Key Decisions (Resolved)

| Decision | Choice |
|----------|--------|
| A) Fairness metric | Gini-based balance score (0-100) |
| B) Workload definition | Count of completed tasks (no weighting) |
| C) Visibility | All members (non-toxic wording) |
| D) Time windows | Fixed 7d/30d only |
| E) Scope | Tasks only (no shopping) |

---

## Scope

### Committed (Must Deliver)

| ID | Story | Points | Workpack |
|----|-------|--------|----------|
| ST-701 | Analytics Summary Endpoint | 3 | `workpacks/ST-701-702/` |
| ST-702 | Balance Score Calculation (Gini) | 3 | `workpacks/ST-701-702/` |
| ST-703 | Web Analytics Page | 5 | `workpacks/ST-703-704/` |
| ST-705 | Security & Boundary Tests | 2 | `workpacks/ST-705-706/` |

**Total Committed:** 13 points

### Stretch (If Capacity)

| ID | Story | Points | Workpack |
|----|-------|--------|----------|
| ST-704 | Period Filters (30d toggle) | 2 | `workpacks/ST-703-704/` |
| ST-706 | Observability Hooks | 1 | `workpacks/ST-705-706/` |

**Total with Stretch:** 16 points

### Out of Scope (Explicit)

| Item | Reason |
|------|--------|
| Shopping analytics | Tasks only in v0 |
| Custom date ranges | Fixed periods only |
| Weighted workload | Count only, no complexity |
| Charts/graphs | Simple lists for v0 |
| Household toggle for balance | Documented for future |

---

## Dependencies

| Dependency | Status |
|------------|--------|
| EP-003 (Web Foundation) | Done |
| EP-004 (Auth) | Done |
| EP-005 (Household) | Done |
| Tasks table | Done |

---

## Delivery Sequence

```
Day 1-2: ST-701 + ST-702 (Backend)
         - GiniCalculator utility
         - AnalyticsService
         - AnalyticsController
         - DTOs
         - Unit + integration tests
         ↓
Day 3-4: ST-703 (Web UI)
         - AnalyticsPage
         - BalanceScoreCard
         - MemberStatsList
         - ZoneStatsList
         - OverdueTasksList
         - useAnalytics hook
         - API client
         ↓
Day 5:   ST-705 (Security tests)
         - Boundary tests
         - Integration verification
         ↓
(Stretch) ST-704, ST-706
```

---

## Risks

| Risk | Mitigation |
|------|------------|
| Gini misunderstood | Clear formula + interpretation |
| "Balance" triggers conflict | Non-toxic wording, no blame |
| Performance | Add index, test early |

---

## Acceptance Criteria (Sprint-level)

### Core Flow
- [ ] Navigate to /households/{id}/analytics
- [ ] See member stats (completed, overdue, open)
- [ ] See zone stats (completed, overdue)
- [ ] See top overdue tasks
- [ ] See balance score with explanation
- [ ] "How calculated" expandable works
- [ ] Non-members get 403
- [ ] No tasks → balance shows "N/A"

### Technical
- [ ] Gini calculated correctly (unit tests pass)
- [ ] Balance 0-100 or null
- [ ] Non-toxic interpretation text
- [ ] OpenAPI contract updated
- [ ] Build passes (backend + web)

---

## Definition of Done (Sprint)

Sprint is **Done** when:
1. All committed stories pass DoD
2. Security tests pass (ST-705)
3. Manual QA: navigate → view data → verify balance calculation
4. Code review approved
5. Merged to main branch
6. Demo-ready

---

## Capacity Notes
- Backend: ~6 points (ST-701, ST-702)
- Web: ~5 points (ST-703)
- Tests: ~2 points (ST-705)
- Buffer: 3 points for unknowns

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Initiative | `docs/planning/initiatives/INIT-2026Q2-analytics-fairness-dashboard.md` |
| Epic | `docs/planning/epics/EP-008/epic.md` |
| Workpack ST-701-702 | `docs/planning/workpacks/ST-701-702/` |
| Workpack ST-703-704 | `docs/planning/workpacks/ST-703-704/` |
| Workpack ST-705-706 | `docs/planning/workpacks/ST-705-706/` |

---

## Next Steps After Sprint

1. If all committed delivered → close sprint, update epic status
2. Generate prompt packs for Codex (PLAN → APPLY → REVIEW)
3. Review and merge
4. Consider ST-704, ST-706 for S08 if not completed as stretch
