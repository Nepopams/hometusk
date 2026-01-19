# Sprint S01 Scope Detail

## In Scope

### ST-101: Setup JDK/CI Environment (P0 BLOCKER)
- Install JDK 21 on development machine
- Configure JAVA_HOME and PATH
- Verify gradle commands work
- Verify test execution (results may fail, but must run)

**Type:** Human runbook (no code changes)

### ST-102: Implement Command Continuation Endpoint (P0 CRITICAL)
- Add `POST /commands/{commandId}/continue` endpoint
- Add `ContinueCommandRequest` DTO
- Add continuation logic to CommandService
- Update OpenAPI contract
- Add integration tests

**Type:** Implementation (code changes, contract update)

---

## Stretch

### ST-103: Decide start_task Scope
- Determine if `start_task` command is MVP-required
- Document decision with rationale
- Update MVP.md or unblock ST-104

**Type:** Decision only (no code changes)

---

## Out of Scope

| Item | Rationale |
|------|-----------|
| Availability heuristic | Explicitly out of MVP scope per MVP.md |
| New intents (add_shopping, mark_purchased) | Already implemented per gap analysis |
| Performance optimizations | Not in MVP exit criteria |
| UI/UX changes | Out of backend scope |
| start_task implementation | Blocked by ST-103 decision |
| Additional guardrail policies | Not in current sprint focus |

---

## Readiness Notes

### ST-101
- **Ready:** All criteria met
- **Prerequisites:** sudo access or Docker
- **Human action required:** Manual JDK installation

### ST-102
- **Ready:** DoR met after ST-101 completes
- **Prerequisites:** Tests must run (ST-101)
- **Contract change:** Requires review before implementation
- **References:** ADR-004, ADR-005, ADR-012

### ST-103 (Stretch)
- **Ready:** Can proceed independently
- **Prerequisites:** None
- **Human decision required:** Product scope decision
