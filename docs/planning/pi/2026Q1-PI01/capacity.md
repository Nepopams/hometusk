# PI Capacity: 2026Q1-PI01

## Team Structure

| Role | Allocation | Notes |
|------|------------|-------|
| Claude Code (Arch/BA) | Analysis & artifacts | Produces workpacks, prompt packs |
| Codex (Dev) | Implementation | Executes workpacks |
| Human (PO) | Approvals | Human gates |

---

## Story Point Budget

| Sprint | Available Points | Committed | Buffer |
|--------|------------------|-----------|--------|
| S01 | 8 | 5 (ST-001: 3, ST-002: 2) | 3 |
| S02 | 8 | 3 (ST-003: 2, ST-004: 1) | 5 |
| **Total** | **16** | **8** | **8 (50%)** |

---

## Capacity Assumptions

1. **Codex execution time:** Each story can be completed within 1-2 Codex sessions
2. **Human availability:** Timely approvals for human gates
3. **No blockers:** Test infrastructure is ready, no external dependencies

---

## Buffer Allocation

50% buffer is intentional for:
- Unexpected issues during validation
- Human gate delays
- Rework if validation fails targets

---

## Risk-Adjusted Capacity

| Scenario | Points Used | Outcome |
|----------|-------------|---------|
| Best case | 8 | MVP closed in 2 sprints |
| Likely case | 10-12 | Minor rework needed |
| Worst case | 16 | Significant validation failures require fixes |

---

## Velocity Baseline

This is the first PI with structured tracking. Baseline velocity will be established after S01 completion.
