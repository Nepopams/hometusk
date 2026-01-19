# PI Capacity: 2026Q1-PI01

## Team Structure

| Role | Allocation | Notes |
|------|------------|-------|
| Human | Environment setup, gates | Part-time, gates only |
| Claude Code | Artifacts, workpacks | On-demand |
| Codex | Implementation | Per workpack |

---

## Story Point Budget

| Iteration | Available | Committed | Buffer |
|-----------|-----------|-----------|--------|
| Iter-2a | 6 | 4 (ST-101: 1, ST-102: 3) | 2 (33%) |
| Iter-2b | 6 | 5 (ST-103: 1, ST-104: 2, ST-105: 2) | 1 (17%) |
| **Total** | **12** | **9** | **3 (25%)** |

Note: ST-104 is conditional. If "defer", budget frees up.

---

## Capacity Assumptions

1. **JDK setup:** 1 Codex session or human manual setup
2. **Continuation endpoint:** 2-3 Codex sessions (design + implement + test)
3. **start_task (if yes):** 1-2 Codex sessions (similar to complete_task)
4. **Validation:** Manual verification + test runs
5. **Human availability:** Timely gates (< 24h turnaround)

---

## Velocity Baseline

No historical velocity. This PI establishes baseline.

Expected throughput: 4-5 SP per iteration.

---

## Buffer Allocation

25% buffer for:
- Environment issues
- Test failures requiring fixes
- Rework from review feedback
