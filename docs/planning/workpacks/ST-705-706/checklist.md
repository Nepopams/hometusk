# DoD Checklist: ST-705 + ST-706

## ST-705: Security Tests

### Tests Written
- [ ] `getAnalytics_notMember_returns403`
- [ ] `getAnalytics_memberOfDifferentHousehold_returns403`
- [ ] `getAnalytics_noDataLeakBetweenHouseholds`
- [ ] `getAnalytics_idorAttempt_returns403`
- [ ] `getAnalytics_invalidPeriod_handledGracefully`
- [ ] `getAnalytics_unauthenticated_returns401`

### Tests Pass
- [ ] All security tests pass: `./gradlew test --tests "*AnalyticsSecurityIntegrationTest*"`
- [ ] No false positives (tests fail when security broken)

### Coverage
- [ ] 403 returned for non-members (not 404)
- [ ] Cross-household data leak prevented
- [ ] Invalid input handled gracefully

## ST-706: Observability (Stretch)

### Logging
- [ ] Request logged with: householdId, period, latency_ms, status
- [ ] No PII in logs (no email, no display_name)
- [ ] Log level: INFO for normal requests

### Metrics (if Micrometer present)
- [ ] Request count available
- [ ] Latency percentiles available

### Verification
- [ ] Manual test: make request, check log output
- [ ] Confirm no user email appears in logs
