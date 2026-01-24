# DoD Checklist: ST-701 + ST-702

## Code Quality
- [ ] Code follows project conventions (Java 21, Spring Boot idioms)
- [ ] Spotless formatting applied (`./gradlew spotlessApply`)
- [ ] No compiler warnings introduced
- [ ] Package structure: `com.hometusk.analytics.*`

## Tests
- [ ] GiniCalculator unit tests pass
  - [ ] Equal distribution → gini=0
  - [ ] Unequal distribution → gini>0
  - [ ] Empty/zero → null
  - [ ] Single member → 0
- [ ] AnalyticsService unit tests pass
  - [ ] Period calculation correct
  - [ ] Interpretation text generated
- [ ] Integration tests pass
  - [ ] `getAnalytics_asMember_returnsData`
  - [ ] `getAnalytics_notMember_returns403`
  - [ ] `getAnalytics_periodFilter_works`
- [ ] All tests pass: `./gradlew test`

## Contract
- [ ] OpenAPI updated with `/households/{id}/analytics` endpoint
- [ ] AnalyticsSummary schema documented
- [ ] MemberStats, ZoneStats, FairnessInfo, OverdueTask schemas
- [ ] All fields marked required/optional correctly
- [ ] Response codes: 200, 401, 403

## Gini/Balance Correctness
- [ ] Equal distribution → gini≈0, balance=100
- [ ] No tasks → gini=null, balance=null
- [ ] Typical unequal → 0 < gini < 1, 0 < balance < 100
- [ ] Interpretation text is non-toxic
- [ ] Formula field = "Balance = 100 × (1 - Gini coefficient)"

## Security
- [ ] `membershipService.requireMembership()` called in controller
- [ ] No cross-household data in queries (WHERE household_id = ?)
- [ ] No PII in logs (only UUIDs)
- [ ] IDOR not possible (403 on invalid household)

## Performance
- [ ] Query execution < 200ms on test data
- [ ] Index added if needed
- [ ] No N+1 queries
