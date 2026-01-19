# PI Roadmap: 2026Q1-PI01

## Iteration Overview

| Iteration | Goal | Stories | Blocker? |
|-----------|------|---------|----------|
| **Iter-2a** | Unblock + Clarification Loop | ST-101, ST-102 | Yes |
| **Iter-2b** | Scope decision + Validation + Closure | ST-103, ST-104, ST-105 | No |

---

## Iteration 2a: Unblock & Core Loop

**Iteration Goal:** Enable test execution and complete clarification loop.

### Stories

| ID | Title | Points | Priority | DoR |
|----|-------|--------|----------|-----|
| ST-101 | Setup JDK/CI environment | 1 | P0 (BLOCKER) | Ready |
| ST-102 | Implement command continuation endpoint | 3 | P0 (CRITICAL) | Ready |

### Deliverables
- Working test environment
- `POST /commands/{id}/continue` endpoint
- Updated OpenAPI contract
- Integration test for full clarification flow

### Exit Gate
- [ ] `./scripts/test.sh` runs
- [ ] Clarification loop test passes

---

## Iteration 2b: Validation & Closure

**Iteration Goal:** Complete scope decisions and close MVP.

### Stories

| ID | Title | Points | Priority | DoR |
|----|-------|--------|----------|-----|
| ST-103 | Decide start_task scope | 1 | P1 | Ready |
| ST-104 | (Conditional) Implement start_task | 2 | P2 | Blocked by ST-103 |
| ST-105 | MVP validation & closure | 2 | P1 | Blocked by ST-101, ST-102 |

### Deliverables
- Scope decision documented
- (If needed) start_task command
- All tests passing
- MVP Closure Report

### Exit Gate
- [ ] All exit criteria verified
- [ ] Human approval for MVP closure

---

## Dependencies Graph

```
ST-101 (JDK setup) ─────────────┐
                                ├──> ST-105 (Validation & Closure)
ST-102 (Continuation endpoint) ─┘

ST-103 (start_task decision) ──> ST-104 (implement, conditional)
                                      │
                                      └──> ST-105
```

---

## Timeline (No Dates, Just Sequence)

```
Week 1:
  ├─ ST-101: JDK setup (parallel)
  └─ ST-102: Continuation endpoint (main work)

Week 2:
  ├─ ST-103: start_task decision (quick)
  ├─ ST-104: start_task implementation (if decided yes)
  └─ ST-105: Validation & closure
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| JDK setup fails | Document exact steps, fallback to Docker |
| Continuation endpoint complex | Keep minimal: just resume with new input |
| start_task delays MVP | Scope it out if decision is "defer" |
