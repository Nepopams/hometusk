# Workpack: ST-1106 — Security Boundaries + Integration Tests

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md`
- Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Goal
Comprehensive security-focused integration tests for ASR proxy: household boundary, IDOR prevention, API key non-exposure, error mapping.

---

## Scope

### In Scope
- JWT required tests (401)
- Membership required tests (403)
- IDOR prevention tests (404 for wrong household)
- API key non-exposure tests
- Upstream error mapping tests (503, 429, timeout, 401)

### Out of Scope
- Rate limiting implementation (ST-1104)
- Input validation (ST-1104)
- Metrics (ST-1105)

---

## Files to Change

| File | Action |
|------|--------|
| `AsrSecurityIntegrationTest.java` | CREATE |

---

## Test Cases

### Authentication
- `createTranscription_noAuth_returns401`
- `getTranscription_noAuth_returns401`

### Membership
- `createTranscription_notMember_returns403`
- `getTranscription_notMember_returns403`

### IDOR Prevention
- `getTranscription_wrongHousehold_returns404`
- `getTranscription_nonExistentId_returns404`

### API Key Non-Exposure
- `createTranscription_responseDoesNotContainApiKey`
- `errorResponse_doesNotContainApiKey`

### Error Mapping
- `upstream503_returnsMappedAsrUnavailable`
- `upstream429_returnsMappedAsrRateLimited`
- `upstreamTimeout_returnsAsrUnavailable`
- `upstream401_returnsInternalError`

---

## Verification Commands

```bash
./gradlew test --tests "AsrSecurityIntegrationTest"
./gradlew test --tests "*Asr*"
./gradlew build
```

---

## Epic Exit Criteria Addressed
- Criterion 7: No API key visible
- Criterion 8: Household boundary enforced
- Criterion 9: IDOR tests pass
- Criterion 11: WireMock tests
