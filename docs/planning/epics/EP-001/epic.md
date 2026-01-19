# Epic: MVP Closure

**ID:** EP-001
**Status:** in_progress
**Owner:** Claude Code (Arch/BA)
**Target PI:** 2026Q1-PI01
**Sprints:** S01, S02

---

## Goal

Close MVP scope by implementing the remaining feature (availability-based assignee selection) and validating all success metrics (intent accuracy, response time, traceability, security).

---

## User Value

As a household member, I want tasks to be automatically assigned to the person with the most capacity, so that workload is distributed fairly without manual coordination.

As a product owner, I want documented evidence that MVP success criteria are met, so that I can confidently declare MVP complete.

---

## In Scope

- Simple availability heuristic: "assign to member with minimum open tasks"
- Intent recognition accuracy validation (80%+ target)
- Response time validation (< 2s p95 target)
- MVP documentation updates and closure report

---

## Out of Scope

- Calendar-based availability
- AI/ML-based assignment optimization
- Complex load balancing algorithms
- New command types (assign_task, update_status)
- UI changes

---

## Stories

| ID | Title | Sprint | Status | Points |
|----|-------|--------|--------|--------|
| ST-001 | Implement availability-based assignee selection | S01 | ready | 3 |
| ST-002 | Validate intent recognition accuracy | S01 | ready | 2 |
| ST-003 | Validate response time performance | S02 | ready | 2 |
| ST-004 | MVP closure documentation | S02 | blocked (by ST-001,002,003) | 1 |

---

## Acceptance Criteria

- [ ] When no assigneeId provided, task is assigned to household member with fewest open tasks
- [ ] Intent recognition accuracy documented as >= 80%
- [ ] Response time p95 documented as < 2s
- [ ] `docs/planning/mvp.md` shows all items checked
- [ ] MVP Closure Report exists with validation results

---

## Dependencies

| Depends On | Type | Notes |
|------------|------|-------|
| Existing DecisionEngine | Internal | Add heuristic logic |
| Existing test infrastructure | Internal | Ready |
| TaskRepository | Internal | Query open tasks per member |

---

## Risks

| Risk | Mitigation |
|------|------------|
| Heuristic introduces regression | Existing tests + new unit tests |
| Validation fails targets | Document gaps for Stage 2 |

---

## Flags Summary

| Story | contract_impact | adr_needed | diagrams_needed | security_sensitive |
|-------|-----------------|------------|-----------------|-------------------|
| ST-001 | no | no | no | no |
| ST-002 | no | no | no | no |
| ST-003 | no | no | no | no |
| ST-004 | no | no | no | no |

No contract/ADR/diagram changes required for this epic.
