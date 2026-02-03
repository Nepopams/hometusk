# Sprint S13 — ASR Integration Foundation

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- PI Charter: `docs/planning/pi/2026Q1-PI01/pi.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`

---

## Goal

Deliver end-to-end ASR proxy: a HomeTusk user can submit audio via proxy endpoint and poll for transcription result, without knowing asr-service internals or API keys.

**Success Metric:** Can create transcription job via `POST /households/{id}/asr/transcriptions` and retrieve result via `GET /households/{id}/asr/transcriptions/{id}`.

**Product Goal Alignment:** Reliability as a Feature (contract-first, error mapping, observability, secure secret handling).

---

## Prioritization Rationale

This sprint establishes EP-011 foundation (ASR Integration):

1. **Contract already exists:** ST-1101 contract was created ahead of implementation (`docs/contracts/http/asr-proxy.openapi.yaml`) — story is nearly complete, needs validation only
2. **Sequential dependencies:** ST-1102 (AsrClient) is prerequisite for ST-1103 (controller)
3. **Milestone M2:** "E2E proxy works" — can create + poll transcription via HomeTusk

**Why S13 (after S12):**
- S12 completed Routines feature (EP-010)
- S13 starts new epic (EP-011) — fresh technical domain
- Foundation must be stable before hardening (S14: guardrails, observability, security tests)

---

## Scope

### Committed (DoR-ready, P1)

| Story | Title | Points | Status | Dependencies | Flags |
|-------|-------|--------|--------|--------------|-------|
| ST-1101 | ASR Proxy Contract (OpenAPI) | 2 | Ready | - | contract_impact |
| ST-1102 | AsrClient HTTP Adapter | 5 | Draft* | ST-1101 | security_sensitive |
| ST-1103 | ASR Proxy Endpoints (Controller) | 5 | Draft* | ST-1102 | contract_impact, security_sensitive |

**Total committed:** 12 points

*Draft status will become Ready once ST-1101 is validated (contract already created).

**Note on ST-1101:** Contract file already exists at `docs/contracts/http/asr-proxy.openapi.yaml`. Story scope is primarily **validation** (spectral lint, review, index update), not creation.

**Deliverables:**
- Validated OpenAPI contract (`asr-proxy.openapi.yaml`)
- Contracts-index.md updated
- `AsrClient` interface + `AsrClientImpl` (HTTP adapter)
- `AsrProperties` configuration class
- `AsrException` hierarchy with error mapping
- `AsrController` with POST/GET endpoints
- `AsrTranscriptionRef` entity for IDOR prevention
- JWT + membership enforcement
- Integration tests (happy path + auth)

---

### Out of Scope (explicit)

| Item | Reason | Deferred To |
|------|--------|-------------|
| Rate limiting (Bucket4j) | Hardening story | S14 (ST-1104) |
| Input validation (size/format/duration) | Hardening story | S14 (ST-1104) |
| Metrics (asr_requests_total, etc.) | Observability story | S14 (ST-1105) |
| Structured logs beyond basic | Observability story | S14 (ST-1105) |
| Security edge case tests (IDOR, etc.) | Hardening story | S14 (ST-1106) |
| Circuit breaker | Later enhancement | LATER |
| Streaming partial results | Out of epic scope | LATER |
| Web UI for voice input | Separate initiative | LATER |
| Idempotency record persistence | Deferred to S14 | S14 (ST-1104) |

---

## Capacity Note

**Assumptions:**
- 1 developer (Codex)
- ST-1101: 1 day (validation only, contract exists)
- ST-1102: 2-3 days (HTTP adapter with tests)
- ST-1103: 2-3 days (controller with integration tests)
- Total: ~5-7 days for committed scope

**Constraints:**
- Sequential execution: ST-1101 -> ST-1102 -> ST-1103
- ST-1102 can partially overlap with ST-1101 review
- WireMock setup needed for asr-service mocking

**Buffer:** 20% (~2 points) for:
- HTTP client edge cases
- WireMock integration complexity
- Review feedback rework

---

## Assumptions

1. ST-1101 contract (`asr-proxy.openapi.yaml`) is correct and needs only validation
2. External ASR contract (`docs/contracts/external/asr-service/asr/openapi.yaml`) is accurate
3. asr-service will be available in dev/test environment (or mocked via WireMock)
4. Existing Spring Security + MembershipService patterns apply
5. RestClient/WebClient is available in project

---

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| ASR Proxy Contract (ST-1101) | This sprint | Created, needs validation | LOW |
| External ASR Contract | External | Available | LOW |
| Spring Security (JWT) | Backend | Exists | LOW |
| MembershipService | Backend | Exists | LOW |
| WireMock/MockServer | Test infra | May need setup | MEDIUM |

**Critical path:** ST-1101 (validate) -> ST-1102 (adapter) -> ST-1103 (controller)

---

## Risks & Mitigations (ROAM-lite)

| Risk | Impact | Probability | Strategy | Notes |
|------|--------|-------------|----------|-------|
| External ASR contract drift | MEDIUM | LOW | Mitigate | Pin version, contract tests in S14 |
| WireMock setup complexity | MEDIUM | MEDIUM | Mitigate | Use existing patterns from notifications |
| API key secret handling | HIGH | LOW | Resolve | Standard Spring externalized config |
| Multipart file handling edge cases | MEDIUM | MEDIUM | Mitigate | Test with real audio files |
| No asr-service access in dev | HIGH | LOW | Own | WireMock stubs sufficient for S13 |

---

## Definition of Ready Check

**DoR Status:** CONDITIONAL PASS

| Story | DoR | Notes |
|-------|-----|-------|
| ST-1101 | PASS | Contract exists, needs validation |
| ST-1102 | PASS* | Blocked by ST-1101 validation |
| ST-1103 | PASS* | Blocked by ST-1102 |

*Stories become fully Ready once predecessor completes.

**All prerequisites:**
- [x] Epic EP-011 approved
- [x] Initiative INIT-2026Q2-asr-integration-foundation approved
- [x] Contract file created (ST-1101 artifact)
- [x] External ASR contract available
- [x] All stories have AC with Given/When/Then
- [x] Test strategies defined for each story
- [x] Flags identified (contract_impact, security_sensitive)

---

## Gate B Ask

**Request:** Approve Sprint S13 goal, committed scope (ST-1101, ST-1102, ST-1103), and capacity note.

**What we commit to:**
1. Validate ASR Proxy OpenAPI contract (ST-1101)
2. Update contracts-index.md
3. Implement AsrClient HTTP adapter with error mapping (ST-1102)
4. Implement AsrController with POST/GET endpoints (ST-1103)
5. Persist AsrTranscriptionRef for IDOR prevention
6. JWT + membership enforcement on endpoints
7. Integration tests with WireMock
8. Correlation ID propagation end-to-end

**What we will NOT do:**
- Rate limiting
- Input validation (size/format/duration checks)
- Metrics/structured logs
- Security edge case tests
- Circuit breaker
- Idempotency record persistence

**Approval needed:**
- [ ] Sprint goal approved
- [ ] Committed scope (12 points) approved
- [ ] Out of scope explicit list approved
- [ ] Risks accepted

---

## Sprint Artifacts

| Artifact | Path |
|----------|------|
| Sprint Plan | `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md` (this file) |
| Scope Detail | `docs/planning/pi/2026Q1-PI01/sprints/S13/scope.md` |
| Demo Plan | `docs/planning/pi/2026Q1-PI01/sprints/S13/demo.md` |
| Retro | `docs/planning/pi/2026Q1-PI01/sprints/S13/retro.md` |

---

## Exit Criteria

Sprint is **done** when:
1. All committed stories completed (AC verified, DoD met)
2. OpenAPI contract validated (spectral lint passes)
3. contracts-index.md updated
4. AsrClient implemented with unit tests
5. AsrController implemented with integration tests
6. `./gradlew build` passes
7. WireMock tests cover: happy path, auth failure, membership failure
8. Correlation ID flows end-to-end
9. Demo prepared

**Sprint fails if:**
- Cannot POST audio via HomeTusk endpoint
- Cannot GET transcription result
- Auth/membership not enforced
- No integration tests
- Contract validation fails
