# ST-102 Verification Checklist

## Acceptance Criteria

- [ ] **AC1:** `POST /api/v1/commands/{commandId}/continue` endpoint exists and accepts body
- [ ] **AC2:** Command in NEEDS_INPUT → continue → EXECUTED (happy path works)
- [ ] **AC3:** Command NOT in NEEDS_INPUT → 400 with COMMAND_NOT_CONTINUABLE
- [ ] **AC4:** Non-existent commandId → 404
- [ ] **AC5:** Different user → 403 ACCESS_DENIED
- [ ] **AC6:** OpenAPI updated with endpoint and schemas

## DoD Items

### Code Quality
- [ ] Spotless formatting applied (`./gradlew spotlessApply`)
- [ ] No compiler warnings
- [ ] Code reviewed (PR)

### Tests
- [ ] `CommandContinuationIntegrationTest` exists
- [ ] Happy path test passes
- [ ] Invalid state test passes
- [ ] Not found test passes
- [ ] Auth error test passes
- [ ] All existing tests pass (`./gradlew test`)

### Documentation
- [ ] OpenAPI contract updated: `docs/contracts/http/commands.openapi.yaml`
- [ ] Endpoint documented: `POST /commands/{commandId}/continue`
- [ ] `ContinueCommandRequest` schema defined
- [ ] Error responses documented (400, 403, 404)

### Observability
- [ ] correlationId propagated through continuation flow
- [ ] DecisionLog entry updated/created for continuation

### Security
- [ ] Ownership check implemented (initiator only can continue)
- [ ] No cross-household access possible

## Test Commands

```bash
# Compile check
./gradlew compileJava

# Run specific test
./gradlew test --tests "*CommandContinuationIntegrationTest*"

# Run all tests
./gradlew test

# Format check
./gradlew spotlessCheck
```

## Sign-off

| Criterion | Status | Evidence |
|-----------|--------|----------|
| AC1-AC6 verified | | Test results |
| Contract updated | | OpenAPI diff |
| Tests pass | | CI output |
| Code reviewed | | PR link |

---

**Completed by:** _______________
**Date:** _______________
**PR:** _______________
