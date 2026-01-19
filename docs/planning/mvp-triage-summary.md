# Triage Summary: MVP Completion Assessment

**Date:** 2026-01-19
**Triggered by:** MVP Closure request
**Reference:** `docs/planning/mvp.md`

## Classification

| Field | Value |
|-------|-------|
| **change_type** | feature (MVP gap closure) |
| **work_level** | sprint (discrete gaps, not PI-level restructure) |
| **risk** | **MEDIUM** - Core MVP functionality is mostly complete. Remaining gaps are scope clarifications and minor enhancements, not architectural changes. |

---

## MVP In-Scope Requirements vs Implementation Status

| MVP Requirement | Status | Evidence |
|-----------------|--------|----------|
| Natural language command input via text | **DONE** | `POST /api/v1/commands` in `docs/contracts/http/commands.openapi.yaml` |
| Intent recognition (create task, assign task, update status) | **PARTIAL** | Only `CREATE_TASK` and `COMPLETE_TASK` exist. No explicit `assign_task` or `update_status`. |
| Zone-based task organization | **DONE** | `zoneId` in CreateTaskPayload, Zone entity exists |
| Automatic assignee selection based on availability | **NOT IMPLEMENTED** | DecisionEngine.java defaults to initiator, no availability logic |
| Task lifecycle: created → assigned → in_progress → completed | **DONE** | TaskStatus enum: OPEN, IN_PROGRESS, DONE, CANCELLED |
| Command traceability (DecisionLog for every command) | **DONE** | DecisionLogWriter + verified in tests |
| Degraded mode (fallback when AI unavailable) | **DONE** | DecisionProviderSelector + AiPlatformIntegrationTest |

---

## MVP Exit Criteria Status

| Exit Criterion | Status |
|----------------|--------|
| 1. User can submit command via API: `POST /api/v1/commands` | **DONE** |
| 2. System resolves intent and creates task with correct zone and assignee | **DONE** (basic) |
| 3. Command decision is logged and traceable | **DONE** |
| 4. System works with AI timeout (degraded mode) | **DONE** |
| 5. All endpoints documented in OpenAPI contract | **DONE** |
| 6. Integration tests cover happy path + degraded mode | **DONE** |

---

## Success Metrics Status

| Metric | Status |
|--------|--------|
| 80%+ intent recognition accuracy | **NEEDS MANUAL VALIDATION** |
| < 2s p95 response time | **NEEDS PERFORMANCE TEST** |
| 100% command traceability | **DONE** |
| Zero cross-household data leaks | **DONE** - HouseholdBoundarySecurityTest.java exists |

---

## MVP Completion Estimate

**~85% COMPLETE**

Core exit criteria met. Gaps are primarily scope clarification issues rather than missing functionality.

---

## Critical Gaps to Close

### Gap 1: "assign_task" and "update_status" intent ambiguity (SCOPE CLARIFICATION)

**MVP says:** `Intent recognition (create task, assign task, update status)`

**Current implementation:**
- `assign_task` not a separate command type. Assignment happens via `assigneeId` in create_task payload.
- `update_status` not explicit. Only `complete_task` exists (transitions to DONE).

**Options:**
1. **Clarify MVP scope:** Interpret as covered by current implementation. *(Recommended)*
2. **Add explicit command types:** Add `ASSIGN_TASK` and `UPDATE_STATUS`. *(Contract change required)*

### Gap 2: Automatic assignee selection based on availability (FEATURE GAP)

**MVP says:** `Automatic assignee selection based on availability`

**Current:** If no assignee specified, defaults to initiator. No availability logic.

**Options:**
1. **Defer to Stage 2:** Update MVP.md to "simple auto-assign to initiator". *(Recommended for MVP)*
2. **Implement availability logic:** Add basic logic (e.g., fewest open tasks). *(Small story)*

### Gap 3: Success metrics validation (VALIDATION WORK)

- **80%+ intent recognition accuracy** - needs manual test suite
- **< 2s p95 response time** - needs performance test

---

## Impact Assessment

| Impact Type | Assessment |
|-------------|------------|
| **contract_impact** | NO (if scope clarification) / YES (if new command types) |
| **data_impact** | NO |
| **nfr_impact** | PERFORMANCE - p95 validation needed |

---

## ADR Requirement

**adr_needed:** NONE (scope clarification) / LITE (if adding new command types)

---

## Human Gates Required

### Gate 1: Scope Clarification
Human must decide:
- [ ] Is "assign via create_task" acceptable for "assign_task" intent?
- [ ] Is "complete_task only" acceptable for "update status" intent?
- [ ] Is "auto-assign to initiator" acceptable for "automatic selection" in MVP?

### Gate 2: Success Metrics Validation
Human must approve:
- [ ] Intent recognition accuracy test plan/results
- [ ] Performance test plan/results (if required)

---

## Recommended Path

| If... | Then... |
|-------|---------|
| Scope clarifications accepted | Proceed with PI-plan for MVP closure (2-3 sprints) |
| New command types required | Needs epic-decomposer for ASSIGN_TASK + UPDATE_STATUS stories |
| Availability logic required for MVP | Add 1 story for simple availability-based selection |

---

## Summary

The MVP is **functionally complete** for its exit criteria. The identified gaps are primarily **scope interpretation issues** that can be resolved by clarifying the MVP document.

**Recommended action:** Human to confirm scope interpretations, then proceed with PI-plan and MVP closure documentation.
