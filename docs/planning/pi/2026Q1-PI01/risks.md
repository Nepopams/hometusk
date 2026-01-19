# PI Risks: 2026Q1-PI01 (ROAM-lite)

## Risk Register

| ID | Risk | Likelihood | Impact | Status | Mitigation |
|----|------|------------|--------|--------|------------|
| R1 | Intent accuracy below 80% | Low | Medium | ACCEPTED | Current implementation is rule-based (100% deterministic for supported commands). Risk is minimal. |
| R2 | Performance p95 above 2s | Low | Medium | ACCEPTED | Current codebase is straightforward Spring Boot. No known performance issues. |
| R3 | Availability heuristic introduces regression | Medium | Low | MITIGATED | Existing tests provide safety net. New tests required. |
| R4 | Scope creep during MVP closure | Low | Medium | OWNED | Human gates enforce scope boundaries. |

---

## ROAM Status Definitions

| Status | Meaning |
|--------|---------|
| **RESOLVED** | Risk eliminated |
| **OWNED** | Assigned owner actively managing |
| **ACCEPTED** | Team accepts risk, no further action |
| **MITIGATED** | Actions in place to reduce impact/likelihood |

---

## Risk Details

### R1: Intent Accuracy Below 80%

**Description:** MVP success metric requires 80%+ intent recognition accuracy.

**Analysis:** Current implementation uses rule-based decision engine (not AI). For supported command types (create_task, complete_task), accuracy is 100% deterministic. Risk applies only if testing reveals edge cases.

**Mitigation:** If accuracy issues found, document specific failure cases for Stage 2 improvement.

---

### R2: Performance p95 Above 2s

**Description:** MVP success metric requires < 2s p95 response time.

**Analysis:** Command pipeline is straightforward: validation → decision → action → log. No external calls in manual mode. With AI Platform, external call is the main latency source (handled by fallback).

**Mitigation:** If performance issues found, profile and document bottlenecks. Fallback mode already provides degraded-but-fast path.

---

### R3: Availability Heuristic Regression

**Description:** Adding availability logic to DecisionEngine could break existing behavior.

**Analysis:** DecisionEngine is well-tested. Change is additive (new logic when assignee not specified).

**Mitigation:**
- Keep existing "default to initiator" as fallback
- Add unit tests for new heuristic
- Integration test for auto-assignment scenario

---

### R4: Scope Creep

**Description:** Additional features requested during MVP closure.

**Analysis:** Human gates are in place. Scope is defined in this PI charter.

**Mitigation:**
- Refer new requests to Stage 2+ backlog
- Human gate approval required for any scope changes
