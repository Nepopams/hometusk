# Sprint S14 — ASR Integration Hardening

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- Previous Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## Goal

**Production-ready ASR proxy with guardrails, observability, and security hardening.**

Deliver the hardening layer (Milestone M3) for EP-011: input validation, rate limiting, idempotency support, structured metrics/logs, and comprehensive security boundary tests. After S14, ASR proxy is production-ready.

**Success Metric:**
- All guardrails enforced (size/format/rate limits)
- Metrics exposed at /actuator/prometheus (asr_*)
- 100% IDOR test coverage (no cross-household access)
- All integration tests pass

**Product Goal Alignment:** Reliability as a Feature (guardrails, observability, security hardening).

---

## Prioritization Rationale

This sprint completes EP-011 (ASR Integration Foundation):

1. **S13 foundation complete:** ST-1101, ST-1102, ST-1103 delivered E2E proxy
2. **S14 = Milestone M3:** "Production ready" — guardrails, metrics, security tests
3. **All stories unblocked:** ST-1103 (endpoints) exists, all S14 stories ready

**Why S14 now:**
- S13 established working proxy (can POST/GET)
- Production deployment requires guardrails (abuse prevention)
- Production deployment requires observability (monitoring)
- Security tests must pass before any external usage

---

## Scope

### Committed (DoR-ready)

| Story | Title | Points | Priority | Dependencies | Flags |
|-------|-------|--------|----------|--------------|-------|
| ST-1104 | Guardrails (Validation + Rate Limiting) | 3 | P1 | ST-1103 | - |
| ST-1105 | Observability (Metrics + Structured Logs) | 3 | P2 | ST-1103 | - |
| ST-1106 | Security Boundaries + Integration Tests | 3 | P1 | ST-1103 | security_sensitive |

**Total committed:** 9 points

**Deliverables:**
- Input validation (file size max 10MB, format validation)
- Rate limiting (POST 5/min, GET 30/min per user per household)
- Idempotency support via AsrIdempotencyRecord
- Micrometer metrics (asr_requests_total, asr_latency_ms, asr_failures_total)
- Structured logs with correlationId, userId, householdId
- Comprehensive security integration tests (IDOR, API key leak prevention, timeout handling)

---

### Out of Scope (explicit)

| Item | Reason | Deferred To |
|------|--------|-------------|
| Circuit breaker | Enhancement, not blocking | LATER |
| Audio duration detection (local) | Complex codec parsing | OUT (validated by upstream) |
| Per-household rate limit config | Admin feature | LATER |
| Alerting rules (Grafana) | Ops concern, not dev | LATER |
| Distributed tracing (spans) | v0 uses correlationId only | LATER |
| Web UI for voice input | Separate initiative | LATER |
| Penetration testing | Separate process | LATER |

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- ST-1104: 1-2 days (validation + Bucket4j + idempotency)
- ST-1105: 1-2 days (Micrometer setup + structured logging)
- ST-1106: 1-2 days (security tests, WireMock edge cases)
- Total: ~4-6 days for committed scope

**Constraints:**
- Stories can run in parallel (all depend only on ST-1103, not on each other)
- Recommended order: ST-1104 -> ST-1105 -> ST-1106 (validation before observability before security tests)
- WireMock already set up in S13

**Buffer:** 20% (~2 points) for:
- Bucket4j edge cases
- Micrometer configuration
- Security test edge cases

---

## Assumptions

1. S13 completed successfully (ST-1101, ST-1102, ST-1103 merged)
2. WireMock infrastructure from S13 reusable
3. Micrometer/Actuator available in Spring Boot stack
4. Bucket4j or similar available for rate limiting
5. Existing logging framework supports structured JSON

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| S13 stories (ST-1101, ST-1102, ST-1103) | Previous sprint | DONE | NONE |
| Bucket4j library | External | Available | LOW |
| Micrometer | Spring Boot | Included | LOW |
| WireMock | Test infra | Set up in S13 | LOW |
| Structured logging | Backend | Existing pattern | LOW |

**No blocking dependencies.** All S14 stories are unblocked by S13 completion.

---

## Risks & Mitigations (ROAM-lite)

| Risk | Impact | Probability | Strategy | Notes |
|------|--------|-------------|----------|-------|
| Bucket4j configuration complexity | MEDIUM | LOW | Mitigate | Use simple token bucket, standard config |
| Rate limit key collision | LOW | LOW | Resolve | Key: `asr:{endpoint}:{householdId}:{userId}` |
| Metrics cardinality explosion | MEDIUM | LOW | Mitigate | Fixed label values, no unbounded dimensions |
| Idempotency digest performance | LOW | LOW | Accept | SHA-256 fast for 10MB max files |
| Security tests miss edge case | HIGH | LOW | Mitigate | Follow epic AC, peer review tests |

---

## Definition of Ready Check

**DoR Status:** PASS

| Story | DoR | Notes |
|-------|-----|-------|
| ST-1104 | PASS | AC defined, test strategy clear |
| ST-1105 | PASS | AC defined, metrics list specific |
| ST-1106 | PASS | AC defined, security scenarios explicit |

**All prerequisites:**
- [x] S13 stories completed (ST-1103 exists)
- [x] All stories have AC with Given/When/Then
- [x] Test strategies defined for each story
- [x] Flags identified (security_sensitive for ST-1106)
- [x] No blocking external dependencies

---

## Gate B Ask

**Request:** Approve Sprint S14 goal, committed scope (ST-1104, ST-1105, ST-1106), and capacity note.

**What we commit to:**
1. Input validation: file size (10MB max), format (audio/* whitelist)
2. Rate limiting: POST 5/min, GET 30/min per user per household
3. Idempotency: AsrIdempotencyRecord with SHA-256 digest, 24h TTL
4. Metrics: asr_requests_total, asr_latency_ms, asr_failures_total
5. Structured logs: correlationId, userId, householdId, sizeBytes, durationMs
6. Security tests: IDOR prevention, API key leak prevention, timeout/5xx handling
7. All integration tests pass with WireMock

**What we will NOT do:**
- Circuit breaker
- Local audio duration detection
- Alerting/dashboards
- Distributed tracing
- Per-household rate limit config

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (9 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S14/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S14/demo.md` |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S14/retro.md` |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. File size validation rejects >10MB (ST-1104)
3. Format validation rejects non-audio MIME types (ST-1104)
4. Rate limiting returns 429 with Retry-After (ST-1104)
5. Idempotency returns cached result or 409 conflict (ST-1104)
6. Metrics exposed at /actuator/prometheus (ST-1105)
7. Structured logs include correlationId/userId/householdId (ST-1105)
8. IDOR tests pass (ST-1106)
9. API key never in logs/responses (ST-1106)
10. `./gradlew build` passes
11. Demo prepared

**Sprint fails if:**
- Guardrails not enforced
- Metrics not exposed
- Cross-household access possible
- API key leaks in any scenario
- Integration tests fail

---

## Epic Completion Note

After S14:
- **Milestone M3: Production ready** achieved
- **EP-011 complete:** ASR Integration Foundation done
- All epic exit criteria met (see epic.md)
- Ready for voice input UI initiative (separate epic)
