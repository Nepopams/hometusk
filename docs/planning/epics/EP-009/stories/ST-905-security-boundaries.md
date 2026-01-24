# Story: ST-905 — Security & Boundary Tests

## Sources of Truth
- Epic: `docs/planning/epics/EP-009/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — SAFE S08 Scope (no manual adjustment endpoint)

## User Value
> "Как product owner, хочу быть уверен что геймификация одного домохозяйства недоступна другим — это база доверия."

---

## Description
Comprehensive security tests for gamification endpoints (SAFE S08 scope):
- Household membership enforcement
- No cross-household data leaks
- IDOR prevention
- User can only see "my progress" for self

**S08 Constraints:**
- No privacy toggle tests (ST-906 deferred)
- No manual adjustment audit tests (admin endpoint deferred)

---

## Acceptance Criteria

### AC-1: 403 for non-members
```
Given user not member of householdId
When GET /households/{householdId}/gamification/progress
Then response 403 Forbidden
And no data returned
```

### AC-2: No cross-household leak
```
Given households H1 (user member) and H2 (user not member)
And H1 has 100 points, H2 has 500 points
When requesting progress for H1
Then only H1 data returned
And H2 points never exposed
```

### AC-3: IDOR test
```
Given valid householdId format but user not member
When requesting with guessed UUID
Then 403 returned (not 404)
```

### AC-4: User sees only own progress
```
Given authenticated user in household
When requesting /gamification/progress
Then response includes:
  - Own userId, totalPoints, badges
  - Household aggregate (totalTasks, totalPoints)
And does NOT include:
  - Other members' individual points
  - Other members' badges
```

### AC-5: Household aggregate includes all members
```
Given household with members A (50 pts), B (30 pts), C (20 pts)
When any member requests household progress
Then aggregate shows totalPoints = 100
And aggregate shows correct totalTasks
```

### AC-6: DEFERRED — Privacy toggle tests (S09)
```
Note: Privacy toggle enforcement tests require ST-906 implementation.
Deferred to S09 when privacy settings exist.
```

### AC-7: DEFERRED — Manual adjustment audit (S09+)
```
Note: Manual adjustment endpoint deferred.
No audit tests in S08.
```

---

## Test Implementation (S08 Scope)

```java
@DisplayName("Gamification Security Integration Tests — S08")
class GamificationSecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void getProgress_notMember_returns403() {}

    @Test
    void getProgress_differentHousehold_noDataLeak() {}

    @Test
    void getProgress_idorAttempt_returns403() {}

    @Test
    void getProgress_userSeesOnlyOwnProgress() {}

    @Test
    void getProgress_householdAggregateIncludesAllMembers() {}

    // DEFERRED to S09:
    // void getHouseholdProgress_privacyToggleRespected() {}
    // void manualAdjustment_auditLogCreated() {}
}
```

---

## Points
**2 points**

---

## Flags
- security_sensitive: yes
