# Story: ST-705 — Security & Boundary Tests

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Part of Sprint S07 committed scope

## User Value
> "Как product owner, хочу быть уверен что аналитика одного домохозяйства недоступна другим — это база доверия."

---

## Description
Comprehensive security tests for analytics endpoint:
- Household membership enforcement
- No cross-household data leaks
- IDOR prevention
- Input validation

---

## Acceptance Criteria

### AC-1: 403 for non-members
```
Given user not member of householdId
When GET /households/{householdId}/analytics
Then response 403 Forbidden
And no data returned
```

### AC-2: No cross-household leak
```
Given households H1 (user member) and H2 (user not member)
And H1 has 5 tasks, H2 has 10 tasks
When requesting analytics for H1
Then only H1 data returned (perMember totals = 5)
And H2 data never exposed
```

### AC-3: IDOR test
```
Given valid householdId format but user not member
When requesting with guessed UUID
Then 403 returned (not 404)
```

### AC-4: Invalid period parameter
```
Given period=invalid
When requesting analytics
Then either 400 Bad Request
Or defaults to 7d (graceful handling)
```

### AC-5: SQL injection prevention
```
Given malicious householdId parameter
When requesting
Then parameter validated as UUID
And no SQL error exposed
```

---

## Test Implementation

```java
@Testcontainers
class AnalyticsSecurityIntegrationTest {

    @Test
    void getAnalytics_notMember_returns403() {
        // Setup: user not in household
        // Act: GET /households/{id}/analytics
        // Assert: 403
    }

    @Test
    void getAnalytics_differentHousehold_noDataLeak() {
        // Setup: H1 with tasks, H2 with tasks, user in H1 only
        // Act: GET /households/{H1}/analytics
        // Assert: only H1 tasks in response
    }

    @Test
    void getAnalytics_idorAttempt_returns403() {
        // Setup: random UUID not belonging to any household user is in
        // Act: GET /households/{random}/analytics
        // Assert: 403
    }

    @Test
    void getAnalytics_invalidPeriod_handledGracefully() {
        // Act: GET /households/{id}/analytics?period=invalid
        // Assert: 400 or default to 7d
    }
}
```

---

## Points
**2 points**

---

## Flags
- security_sensitive: yes
