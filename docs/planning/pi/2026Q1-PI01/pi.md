# PI Charter: 2026Q1-PI01 — MVP Iteration 2 (Closure)

**PI ID:** 2026Q1-PI01
**Duration:** 2 iterations (Iter-2a, Iter-2b)
**Start Date:** 2026-01-20
**Goal:** Close MVP Exit Criteria gaps and enable exit review

---

## Context

Per `docs/planning/mvp-gap-analysis.md`, MVP is ~75% complete with critical gaps blocking exit review.

**Source of Truth:**
- MVP Scope: `docs/planning/mvp.md`
- Gap Analysis: `docs/planning/mvp-gap-analysis.md`
- Service Catalog: `docs/architecture/service-catalog.md`

---

## PI Theme

**"MVP Iteration 2 — Clarification Loop & Exit Review"**

Complete the command lifecycle (NEEDS_INPUT continuation) and validate all exit criteria.

---

## Goals

1. **Unblock exit review:** Setup JDK/CI environment to run tests
2. **Complete clarification loop:** Implement `POST /commands/{id}/continue` endpoint
3. **Scope decision:** Determine if `start_task` command is required for MVP
4. **Validate & close:** Run tests, validate metrics, produce closure report

---

## Non-Goals (Out of Scope per MVP.md)

- "Умная доступность" (availability-based assignment) — explicitly excluded
- Voice input, mobile app, push notifications
- Recurring tasks, dependencies, calendar integration
- WebSocket/SSE realtime
- Advanced RBAC beyond household membership

---

## Exit Criteria (from mvp.md)

| # | Criterion | Current | Target |
|---|-----------|---------|--------|
| 1 | Commands API returns business status | ✅ DONE | ✅ |
| 2 | Intents lead to correct domain changes | ⚠️ PARTIAL | ✅ |
| 3 | Task: household + zone + assignee policy | ✅ DONE | ✅ |
| 4 | DecisionLog + correlationId traceability | ✅ DONE | ✅ |
| 5 | Degraded mode works | ✅ DONE | ✅ |
| 6 | Idempotency (replay + conflict) | ✅ DONE | ✅ |
| 7 | Invites flow (create + accept + 410) | ✅ DONE | ✅ |
| 8 | Notifications (list + mark read) | ✅ DONE | ✅ |
| 9 | OpenAPI matches implementation | ⚠️ PARTIAL | ✅ |
| 10 | Integration tests pass | ⚠️ BLOCKED | ✅ |

---

## Dependencies

| Dependency | Type | Status | Reference |
|------------|------|--------|-----------|
| JDK 21 | Environment | **BLOCKER** | — |
| Commands API | Internal | Ready | ADR-003, ADR-012 |
| AI Platform contract | External | Ready | ADR-004, ADR-006 |
| Guardrails pipeline | Internal | Ready | ADR-005 |

---

## Related ADRs

| ADR | Title | Relevance |
|-----|-------|-----------|
| [ADR-003](../../architecture/decisions/003-stage1-commands-api.md) | Stage 1 Commands API | Core pipeline |
| [ADR-004](../../architecture/decisions/004-stage2-ai-platform-integration.md) | AI Platform Integration | NEEDS_INPUT flow |
| [ADR-005](../../architecture/decisions/005-stage3-guardrails-pipeline.md) | Guardrails Pipeline | Clarification triggers |
| [ADR-012](../../architecture/decisions/012-command-reliability-idempotency.md) | Idempotency | Command continuation |

---

## Stakeholders

| Role | Responsibility |
|------|----------------|
| Product Owner | Scope decisions (start_task), exit approval |
| Claude Code | Artifacts, workpacks, prompt packs |
| Codex | Implementation per workpacks |
| Human | Gates, final approval |
