# Sprint S10 — Demo Plan

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S10/sprint.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- OpenAPI: `docs/contracts/http/routines.openapi.yaml`

---

## Demo Goal

Demonstrate that the Routine entity foundation is complete:
- CRUD operations work via API
- Recurrence rules parse correctly
- Security boundaries are enforced

---

## Demonstrable Increment

### 1. Routine CRUD via API (ST-1001)

**Demo script:**

1. **Create Routine** (daily dishwashing)
   ```bash
   POST /api/v1/households/{householdId}/routines
   {
     "title": "Myt posudu",
     "zoneId": "<kitchen-zone-id>",
     "recurrenceRule": { "type": "DAILY" },
     "assignmentPolicy": "ROUND_ROBIN"
   }
   ```
   **Expected:** 201 Created, routine with status=ACTIVE

2. **List Routines**
   ```bash
   GET /api/v1/households/{householdId}/routines
   ```
   **Expected:** 200 OK, array with created routine

3. **Get Single Routine**
   ```bash
   GET /api/v1/households/{householdId}/routines/{routineId}
   ```
   **Expected:** 200 OK, full routine object

4. **Update Routine** (change title)
   ```bash
   PATCH /api/v1/households/{householdId}/routines/{routineId}
   { "title": "Pomyt posudu tshatelno" }
   ```
   **Expected:** 200 OK, updated title, other fields unchanged

5. **Delete Routine** (soft delete)
   ```bash
   DELETE /api/v1/households/{householdId}/routines/{routineId}
   ```
   **Expected:** 204 No Content

6. **Verify Soft Delete**
   ```bash
   GET /api/v1/households/{householdId}/routines
   ```
   **Expected:** Deleted routine not in list

---

### 2. Recurrence Rule Parser (ST-1002)

**Demo script:** Unit test run with visual output

1. **DAILY pattern**
   - Input: `fromDateInclusive=2026-01-28, count=3`
   - Output: `[2026-01-28, 2026-01-29, 2026-01-30]`

2. **WEEKLY pattern** (Saturday)
   - Input: `afterDateExclusive=2026-01-28` (Wednesday)
   - Output: `2026-02-01` (next Saturday)

3. **MONTHLY pattern** (day 31 in February)
   - Input: `afterDateExclusive=2026-02-01, dayOfMonth=31`
   - Output: `2026-02-28` (clamped to last day)

4. **EVERY_N_DAYS pattern** (every 3 days)
   - Input: `fromDateInclusive=2026-01-28, interval=3, count=4`
   - Output: `[2026-01-28, 2026-01-31, 2026-02-03, 2026-02-06]`

**Command:**
```bash
./gradlew test --tests "RecurrenceRuleParserTest"
```
**Expected:** All tests pass (10+ test cases)

---

### 3. Security Boundaries (ST-1008)

**Demo script:**

1. **Cross-household access blocked**
   - User A is member of Household A
   - User A tries to access Household B routines
   ```bash
   GET /api/v1/households/{householdB}/routines
   Authorization: Bearer <token-for-user-A>
   ```
   **Expected:** 403 Forbidden

2. **Routine in wrong household returns 404**
   - Routine R1 belongs to Household A
   - User B (member of Household B) tries to access R1 via Household B URL
   ```bash
   GET /api/v1/households/{householdB}/routines/{R1-id}
   Authorization: Bearer <token-for-user-B>
   ```
   **Expected:** 404 Not Found (not 403, to prevent existence leak)

3. **Integration test suite**
   ```bash
   ./gradlew test --tests "RoutineSecurityIntegrationTest"
   ```
   **Expected:** All security tests pass

---

## Test Suite Results

**Required for demo:**
- [ ] `RoutineControllerIntegrationTest` - all pass
- [ ] `RecurrenceRuleParserTest` - all pass
- [ ] `RoutineSecurityIntegrationTest` - all pass
- [ ] Full build passes: `./gradlew build`

---

## Demo Environment

- Backend running locally or on dev environment
- PostgreSQL database with migration applied
- Test household with zone (kitchen)
- Test users: 2 users in different households

---

## Success Criteria

Demo is **successful** if:
1. All CRUD operations return expected status codes
2. Recurrence parser handles all 4 patterns correctly
3. Security tests demonstrate no cross-household leaks
4. No manual workarounds or configuration changes needed
5. OpenAPI contract matches actual responses

---

## Demo Notes

**Limitations to mention:**
- Scheduler not yet implemented (S11)
- No UI yet (S12)
- Pause/resume endpoints not in this sprint
- Assignment policies not enforced yet (S11)

**What to highlight:**
- Partial unique index for dedup (future scheduler idempotency)
- Task entity extension (routineId + scheduledDate)
- Soft delete pattern (status=DELETED)
- Polymorphic JSON serialization for recurrence rules

---

## Post-Demo Actions

1. Collect feedback on API ergonomics
2. Confirm ADR-013 decisions still valid
3. Verify no blockers for S11 (scheduler)
4. Update story statuses if needed
