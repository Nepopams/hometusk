# Sprint S01 (Iter-2a): Unblock & Complete Clarification Loop

**PI:** 2026Q1-PI01
**Sprint ID:** S01
**Epic:** [EP-002 — MVP Iteration 2](../../../../epics/EP-002/epic.md)
**Duration:** ~1 week (estimate, no hard deadline)

---

## Sprint Goal

> **Unblock test execution and implement command continuation endpoint to complete the clarification loop.**

This sprint focuses on removing the JDK blocker and implementing the critical missing feature (continuation endpoint) that allows commands in `NEEDS_INPUT` status to be completed.

---

## Committed Scope

| ID | Story | Points | Status | Workpack |
|----|-------|--------|--------|----------|
| ST-101 | [Setup JDK/CI Environment](../../../../epics/EP-002/stories/ST-101-jdk-setup.md) | 1 | ready | [workpack](../../../../workpacks/ST-101/workpack.md) |
| ST-102 | [Implement Command Continuation Endpoint](../../../../epics/EP-002/stories/ST-102-command-continuation.md) | 3 | ready | [workpack](../../../../workpacks/ST-102/workpack.md) |

**Total committed:** 4 points

---

## Stretch Scope

| ID | Story | Points | Notes |
|----|-------|--------|-------|
| ST-103 | [Decide start_task Scope](../../../../epics/EP-002/stories/ST-103-start-task-decision.md) | 1 | Decision-only, no code changes |

**Rationale:** ST-103 is low-risk (decision only, no implementation). If ST-101 and ST-102 complete quickly, we can make the start_task scope decision to unblock planning for next sprint.

---

## Out of Scope

- Availability heuristic / auto-assign by task count
- New intents (add_shopping, mark_purchased, etc.)
- Performance optimizations
- UI/UX changes
- start_task implementation (ST-104) — blocked by ST-103 decision

---

## Dependencies

| Dependency | Impact | Status |
|------------|--------|--------|
| JDK 21 available | ST-102 can't run tests without it | **BLOCKER** — ST-101 must complete first |
| No external dependencies | — | — |

**Dependency chain:** ST-101 → ST-102 → (ST-103 if stretch)

---

## Risks (ROAM-lite)

| Risk | Category | Likelihood | Impact | Mitigation |
|------|----------|------------|--------|------------|
| JDK install fails | Technical | Low | High | Docker fallback documented in workpack |
| Tests reveal new issues | Technical | Medium | Medium | Document and triage; don't block sprint |
| Contract review delays | Process | Low | Medium | Contract change is small and follows existing patterns |

---

## Capacity Assumptions

- **Human:** Available for JDK setup (ST-101) and approvals
- **Codex:** Available for ST-102 implementation after ST-101 complete
- **Buffer:** 20% for test failures discovery and triage

---

## Exit Criteria

Sprint is **Done** when:
- [ ] ST-101: JDK configured, tests run
- [ ] ST-102: Continuation endpoint implemented, tests pass
- [ ] (Stretch) ST-103: Decision documented

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| Epic | `docs/planning/epics/EP-002/epic.md` |
| PI Plan | `docs/planning/pi/2026Q1-PI01/pi.md` |
| MVP Gap Analysis | `docs/planning/mvp-gap-analysis.md` |
| ADR-004 | `docs/architecture/decisions/004-stage2-ai-platform-integration.md` |
| ADR-005 | `docs/architecture/decisions/005-stage3-guardrails-pipeline.md` |
| ADR-012 | `docs/architecture/decisions/012-command-reliability-idempotency.md` |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` |
