# PI Roadmap: 2026Q1-PI01

## Sprint Overview

| Sprint | Goal | Stories |
|--------|------|---------|
| S01 | Feature completion + Accuracy validation | ST-001, ST-002 |
| S02 | Performance validation + MVP closure | ST-003, ST-004 |

---

## Sprint 1 (S01): Feature & Accuracy

**Sprint Goal:** Complete availability heuristic and validate intent accuracy

### Stories

| ID | Title | Points | Priority |
|----|-------|--------|----------|
| ST-001 | Implement simple availability-based assignee selection | 3 | P1 |
| ST-002 | Validate intent recognition accuracy (80%+ target) | 2 | P1 |

### Deliverables
- Working availability heuristic in DecisionEngine
- Intent accuracy test results documented
- Updated integration tests

---

## Sprint 2 (S02): Performance & Closure

**Sprint Goal:** Validate performance and close MVP

### Stories

| ID | Title | Points | Priority |
|----|-------|--------|----------|
| ST-003 | Validate response time performance (< 2s p95) | 2 | P1 |
| ST-004 | MVP closure documentation | 1 | P1 |

### Deliverables
- Performance test results documented
- Updated `docs/planning/mvp.md` with all items checked
- MVP Closure Report

---

## Timeline

```
Week 1-2: Sprint 1
  - ST-001: Availability heuristic
  - ST-002: Accuracy validation

Week 3-4: Sprint 2
  - ST-003: Performance validation
  - ST-004: MVP closure docs
```

---

## Dependencies Between Stories

```
ST-001 (availability) ──┐
                        ├──> ST-004 (MVP closure)
ST-002 (accuracy) ──────┤
                        │
ST-003 (performance) ───┘
```

ST-001, ST-002, ST-003 can run in parallel.
ST-004 depends on all validation stories being complete.
