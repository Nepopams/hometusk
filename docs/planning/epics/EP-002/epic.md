# Epic: MVP Iteration 2 — Clarification Loop & Exit

**ID:** EP-002
**Status:** in_progress
**Owner:** Claude Code (Arch/BA)
**Target PI:** 2026Q1-PI01
**Iterations:** Iter-2a, Iter-2b

---

## Goal

Complete the command clarification loop (NEEDS_INPUT → continue → executed) and close MVP exit criteria.

---

## User Value

As a user who receives a clarification question from the system, I want to provide my answer and have my command complete successfully, so that I don't have to start over.

As a product owner, I want all MVP exit criteria verified and documented, so that I can confidently ship the MVP.

---

## In Scope

- JDK/CI environment setup (unblock testing)
- `POST /commands/{id}/continue` endpoint for clarification loop
- Scope decision on `start_task` command
- (Conditional) `start_task` implementation
- MVP validation and closure documentation

---

## Out of Scope (per MVP.md)

- Availability-based assignee selection ("умная доступность")
- New intents beyond MVP scope
- Performance optimizations
- UI/UX changes

---

## Stories

| ID | Title | Iter | Status | Points | Flags |
|----|-------|------|--------|--------|-------|
| ST-101 | Setup JDK/CI environment | 2a | ready | 1 | — |
| ST-102 | Implement command continuation endpoint | 2a | ready | 3 | contract_impact: yes |
| ST-103 | Decide start_task scope | 2b | ready | 1 | adr_needed: lite |
| ST-104 | Implement start_task command | 2b | blocked | 2 | contract_impact: yes |
| ST-105 | MVP validation & closure | 2b | blocked | 2 | — |

---

## Acceptance Criteria

- [ ] `./scripts/test.sh` runs and reports results
- [ ] NEEDS_INPUT commands can be continued via `/continue` endpoint
- [ ] start_task scope documented (implement or defer)
- [ ] All MVP exit criteria verified
- [ ] `docs/planning/mvp-closure-report.md` exists

---

## Dependencies

| Depends On | Type | Notes |
|------------|------|-------|
| JDK 21 | Environment | Human setup required |
| ADR-004 | Decision | AI Platform clarify flow |
| ADR-005 | Decision | Guardrails clarification |
| ADR-012 | Decision | Idempotency for continuation |

---

## Related Artifacts

| Artifact | Path |
|----------|------|
| MVP Scope | `docs/planning/mvp.md` |
| Gap Analysis | `docs/planning/mvp-gap-analysis.md` |
| Service Catalog | `docs/architecture/service-catalog.md` |
| Commands Contract | `docs/contracts/http/commands.openapi.yaml` |
| AI Platform Contract | `docs/contracts/external/ai-platform.decision.openapi.yaml` |

---

## Flags Summary

| Story | contract_impact | adr_needed | diagrams_needed |
|-------|-----------------|------------|-----------------|
| ST-101 | no | no | no |
| ST-102 | **yes** | no | no |
| ST-103 | no | **lite** | no |
| ST-104 | **yes** | no | no |
| ST-105 | no | no | no |
