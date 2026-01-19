# PI Risks: 2026Q1-PI01 (ROAM-lite)

## Risk Register

| ID | Risk | L | I | Status | Owner | Mitigation |
|----|------|---|---|--------|-------|------------|
| R1 | JDK/CI setup fails | M | H | **OWNED** | Human | Docker fallback, document exact steps |
| R2 | Continuation endpoint breaks idempotency | L | H | **MITIGATED** | Claude/Codex | Review ADR-012, preserve idempotency semantics |
| R3 | start_task decision delays exit | M | M | **OWNED** | PO | Make decision early in Iter-2b |
| R4 | Tests fail after long time not running | M | M | **ACCEPTED** | — | Fix as discovered, small scope |
| R5 | Scope creep during closure | L | M | **MITIGATED** | Claude | Strict adherence to mvp.md |

---

## ROAM Definitions

| Status | Meaning |
|--------|---------|
| **RESOLVED** | Risk eliminated |
| **OWNED** | Assigned owner actively managing |
| **ACCEPTED** | Team accepts risk, no further action |
| **MITIGATED** | Actions in place to reduce impact/likelihood |

---

## Risk Details

### R1: JDK/CI Setup Fails

**Description:** Environment may have issues preventing JDK installation.

**Likelihood:** Medium (unknown environment state)
**Impact:** High (blocks all testing)

**Mitigation:**
1. Document exact installation steps for Ubuntu/macOS
2. Provide Docker-based fallback: `docker run gradle:jdk21`
3. Test in isolated environment first

**Owner:** Human (environment access)

---

### R2: Continuation Endpoint Breaks Idempotency

**Description:** New `/continue` endpoint may conflict with idempotency semantics.

**Likelihood:** Low (careful design)
**Impact:** High (replay safety compromised)

**Mitigation:**
1. Review ADR-012 before implementation
2. Continuation uses commandId, not new Idempotency-Key
3. Original command's idempotency key still protects initial request
4. Test both paths in integration test

**Reference:** ADR-012 section on "Command Continuation"

---

### R3: start_task Decision Delays Exit

**Description:** Lengthy debate on whether start_task is MVP-required.

**Likelihood:** Medium
**Impact:** Medium (delays Iter-2b)

**Mitigation:**
1. Make decision at start of Iter-2b, not end
2. Default to "defer" if no strong product requirement
3. Document decision clearly for future reference

**Owner:** Product Owner

---

### R4: Tests Fail After Long Time

**Description:** Tests may fail due to accumulated drift or environment changes.

**Likelihood:** Medium
**Impact:** Medium (debugging time)

**Mitigation:**
1. Accept as normal
2. Fix issues as discovered
3. Small iteration scope allows focus

---

### R5: Scope Creep During Closure

**Description:** Temptation to add "one more feature" during MVP closure.

**Likelihood:** Low (discipline enforced)
**Impact:** Medium (delays exit)

**Mitigation:**
1. Strict adherence to mvp.md scope
2. Human gates enforce boundaries
3. New requests → backlog for post-MVP
