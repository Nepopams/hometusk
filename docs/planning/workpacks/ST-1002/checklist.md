# Checklist: ST-1002 — Recurrence Rule Parser

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1002/workpack.md`
- Story: `docs/planning/epics/EP-010/stories/ST-1002-recurrence-parser.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Definition of Ready (DoR)

- [x] Story has clear title and description
- [x] Acceptance criteria defined (Given/When/Then) — 10 ACs
- [x] In scope / out of scope explicit
- [x] Technical approach identified (sealed interface pattern matching)
- [x] Dependencies identified (ST-1001 RecurrenceRule exists)
- [x] Files to change listed (3 files)
- [x] Verification commands defined
- [x] Test strategy defined (unit tests only, no integration)

**DoR Status: READY**

---

## Definition of Done (DoD)

### Code Quality
- [ ] `InvalidRecurrenceRuleException.java` created
- [ ] `RecurrenceRuleParser.java` created with `@Service` annotation
- [ ] Pattern matching on sealed interface used (Java 21)
- [ ] No compiler warnings introduced
- [ ] Spotless formatting applied: `./gradlew spotlessApply`
- [ ] No SonarLint critical issues

### Functionality
- [ ] `getOccurrencesInRange` returns correct dates from inclusive start
- [ ] `getNextOccurrence` returns date strictly after exclusive date
- [ ] DAILY pattern: consecutive days
- [ ] WEEKLY pattern: correct days of week
- [ ] MONTHLY pattern: correct day of month with clamping
- [ ] EVERY_N_DAYS pattern: correct interval spacing
- [ ] Validation throws for invalid WEEKLY (no daysOfWeek)
- [ ] Validation throws for invalid MONTHLY (no dayOfMonth)
- [ ] Validation throws for invalid EVERY_N_DAYS (interval <= 0)

### Tests
- [ ] Unit test class `RecurrenceRuleParserTest` created
- [ ] Test: `daily_returnsConsecutiveDaysFromInclusiveStart` (AC-1)
- [ ] Test: `weekly_singleDay_findsNextOccurrenceExclusive` (AC-2)
- [ ] Test: `weekly_multipleDays_returnsCorrectSequenceFromInclusiveStart` (AC-3)
- [ ] Test: `monthly_normalDay_findsNextOccurrenceExclusive` (AC-4)
- [ ] Test: `monthly_dayExceedsMonthLength_clampsToLastDay` (AC-5)
- [ ] Test: `everyNDays_spacesCorrectlyFromInclusiveStart` (AC-6)
- [ ] Test: `validation_weeklyWithoutDaysOfWeek_throws` (AC-7)
- [ ] Test: `validation_monthlyWithoutDayOfMonth_throws` (AC-8)
- [ ] Test: `validation_everyNDaysWithZeroInterval_throws` (AC-9)
- [ ] Test: `getNextOccurrence_neverReturnsAfterDateItself` (AC-10)
- [ ] Test: `leapYear_february29Handled` (edge case)
- [ ] Test: `yearBoundary_crossesCorrectly` (edge case)
- [ ] All tests pass: `./gradlew test`

### Documentation
- [ ] N/A — No contract/ADR/diagram changes (internal logic)

### Security
- [ ] N/A — Pure domain logic, no external input

---

## Acceptance Criteria Verification

| AC | Description | Status | Evidence |
|----|-------------|--------|----------|
| AC-1 | DAILY returns consecutive days from inclusive start | [ ] | Test passes |
| AC-2 | WEEKLY single day, exclusive after | [ ] | Test passes |
| AC-3 | WEEKLY multiple days, inclusive start | [ ] | Test passes |
| AC-4 | MONTHLY exclusive after | [ ] | Test passes |
| AC-5 | MONTHLY day > month length -> clamp | [ ] | Test passes |
| AC-6 | EVERY_N_DAYS inclusive start | [ ] | Test passes |
| AC-7 | WEEKLY without daysOfWeek throws | [ ] | Test passes |
| AC-8 | MONTHLY without dayOfMonth throws | [ ] | Test passes |
| AC-9 | EVERY_N_DAYS interval <= 0 throws | [ ] | Test passes |
| AC-10 | getNextOccurrence never returns afterDate | [ ] | Test passes |

---

## Verification Commands

```bash
# Build
cd services/backend && ./gradlew build

# Run all tests
cd services/backend && ./gradlew test

# Run specific test class
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.RecurrenceRuleParserTest"

# Check formatting
cd services/backend && ./gradlew spotlessCheck

# Apply formatting
cd services/backend && ./gradlew spotlessApply
```

---

## Files Created/Modified

| File | Status |
|------|--------|
| `services/backend/src/main/java/com/hometusk/routines/domain/InvalidRecurrenceRuleException.java` | [ ] Created |
| `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java` | [ ] Created |
| `services/backend/src/test/java/com/hometusk/routines/service/RecurrenceRuleParserTest.java` | [ ] Created |

---

## Scope Compliance (Anti-Scope-Creep)

- [ ] NO scheduler integration added (ST-1003)
- [ ] NO API endpoints added (internal service only)
- [ ] NO timezone handling added (deferred)
- [ ] NO complex RRULE support added (exceptions, BYSETPOS)
- [ ] NO changes to existing RecurrenceRule interface

---

## Rollback Checklist

If rollback needed:
- [ ] Delete `InvalidRecurrenceRuleException.java`
- [ ] Delete `RecurrenceRuleParser.java`
- [ ] Delete `RecurrenceRuleParserTest.java`
- [ ] Verify build still passes: `./gradlew build`
