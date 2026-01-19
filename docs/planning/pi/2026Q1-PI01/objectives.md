# PI Objectives: 2026Q1-PI01

## Objective 1: Unblock Test Execution (BLOCKER)

**Description:** Setup JDK/CI to enable running integration tests.

**Key Results:**
- [ ] JDK 21 available (`java -version` works)
- [ ] `./scripts/test.sh` runs without environment errors
- [ ] All existing tests execute (pass/fail determined)

**Acceptance:** `./gradlew test` completes with test results.

**Reference:** Gap Analysis GAP-1

---

## Objective 2: Complete Clarification Loop (CRITICAL)

**Description:** Implement endpoint to continue NEEDS_INPUT commands with user-provided input.

**Key Results:**
- [ ] `POST /api/v1/commands/{commandId}/continue` endpoint exists
- [ ] Endpoint accepts additional input and resumes processing
- [ ] Resumed command can reach `executed` status
- [ ] OpenAPI contract updated
- [ ] Integration test covers full flow: command → needs_input → continue → executed

**Acceptance:** User can answer clarification questions and complete commands.

**Reference:**
- Gap Analysis GAP-2
- ADR-004 (AI Platform clarify flow)
- ADR-005 (Guardrails clarification)

---

## Objective 3: Resolve start_task Scope (DECISION)

**Description:** Decide if `start_task` command (→ IN_PROGRESS) is required for MVP exit.

**Options:**
1. **Clarify MVP:** "обновить статус" = only `complete_task`. Defer start_task to post-MVP.
2. **Implement:** Add `start_task` command type to complete task lifecycle.

**Key Results:**
- [ ] Decision documented (in this PI or ADR-lite)
- [ ] If implement: story added to backlog

**Acceptance:** Clear scope boundary for exit review.

**Reference:** Gap Analysis GAP-3, MVP.md line 14

---

## Objective 4: Validate & Close MVP

**Description:** Run all tests, validate metrics, produce closure report.

**Key Results:**
- [ ] All integration tests pass
- [ ] Traceability verified (100% commands → DecisionLog)
- [ ] Security verified (0 cross-household leaks)
- [ ] `docs/planning/mvp.md` checkboxes updated
- [ ] `docs/planning/mvp-closure-report.md` produced

**Acceptance:** Exit review can proceed.

---

## Success Metrics (from MVP.md)

| Metric | Target | Validation Method |
|--------|--------|-------------------|
| Intent accuracy | 80%+ | Manual test dataset |
| p95 latency (degraded) | < 2s | Test timing |
| p95 latency (AI path) | < 5s | Test timing |
| Traceability | 100% | DecisionLog query |
| Security | 0 leaks | HouseholdBoundarySecurityTest |
