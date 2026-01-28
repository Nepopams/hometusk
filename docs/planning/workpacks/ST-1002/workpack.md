# Workpack: ST-1002 — Recurrence Rule Parser

## Sources of Truth
- Story: `docs/planning/epics/EP-010/stories/ST-1002-recurrence-parser.md`
- Epic: `docs/planning/epics/EP-010/epic.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Existing domain: `services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java`

---

## Goal
Implement RRULE-lite parser service that converts RecurrenceRule sealed interface instances to sequences of dates, supporting DAILY, WEEKLY, MONTHLY, and EVERY_N_DAYS patterns with proper validation.

## Scope: In / Out

### In Scope
- `RecurrenceRuleParser` service class with two methods:
  - `getOccurrencesInRange(RecurrenceRule rule, LocalDate fromDateInclusive, int count)` -> `List<LocalDate>`
  - `getNextOccurrence(RecurrenceRule rule, LocalDate afterDateExclusive)` -> `LocalDate`
- Support for 4 recurrence patterns: DAILY, WEEKLY, MONTHLY, EVERY_N_DAYS
- Validation logic throwing `InvalidRecurrenceRuleException` for invalid rules
- Unit tests covering all 10 acceptance criteria plus edge cases (leap year, year boundary)

### Out of Scope
- Complex RRULE (exceptions, BYSETPOS, end date)
- Timezone handling (deferred to household timezone handling)
- Scheduler integration (ST-1003)
- UI rule builder (ST-1005)
- Integration tests (none needed - pure domain logic)

---

## Anchors (non-negotiables)
| Artifact | Path |
|----------|------|
| Story Spec | `docs/planning/epics/EP-010/stories/ST-1002-recurrence-parser.md` |
| Epic | `docs/planning/epics/EP-010/epic.md` |
| RecurrenceRule interface | `services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java` |
| DoD | `docs/_governance/dod.md` |

---

## Plan Steps

### Step 1: Create InvalidRecurrenceRuleException
**Description:** Create domain exception for invalid recurrence rules following existing exception patterns.

**Expected Result:** Exception class exists with message field, follows project conventions.

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/routines/domain/InvalidRecurrenceRuleException.java`

### Step 2: Create RecurrenceRuleParser service
**Description:** Implement the parser service with validation and occurrence calculation logic using Java sealed interface pattern matching.

**Expected Result:**
- Service class with `@Service` annotation
- `getOccurrencesInRange` returns `List<LocalDate>` from inclusive start
- `getNextOccurrence` returns first date strictly after the exclusive date
- Validation throws `InvalidRecurrenceRuleException` for:
  - WEEKLY without daysOfWeek or empty daysOfWeek
  - MONTHLY without dayOfMonth or dayOfMonth < 1 or > 31
  - EVERY_N_DAYS with interval <= 0

**Files touched:**
- CREATE: `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java`

### Step 3: Create unit tests for RecurrenceRuleParser
**Description:** Implement comprehensive unit tests covering all 10 ACs plus edge cases.

**Expected Result:**
- All tests pass
- Coverage for: DAILY, WEEKLY (single/multiple days), MONTHLY (normal + day > month length), EVERY_N_DAYS
- Validation tests for invalid rules
- Edge case tests for leap year and year boundary

**Files touched:**
- CREATE: `services/backend/src/test/java/com/hometusk/routines/service/RecurrenceRuleParserTest.java`

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `services/backend/src/main/java/com/hometusk/routines/domain/InvalidRecurrenceRuleException.java` | CREATE | Domain exception for invalid rules |
| `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java` | CREATE | Parser service with occurrence calculation |
| `services/backend/src/test/java/com/hometusk/routines/service/RecurrenceRuleParserTest.java` | CREATE | Unit tests for parser |

---

## Tests & Checks

### Required Test Updates/Creations
- CREATE `RecurrenceRuleParserTest` with test methods:
  - `daily_returnsConsecutiveDaysFromInclusiveStart` (AC-1)
  - `weekly_singleDay_findsNextOccurrenceExclusive` (AC-2)
  - `weekly_multipleDays_returnsCorrectSequenceFromInclusiveStart` (AC-3)
  - `monthly_normalDay_findsNextOccurrenceExclusive` (AC-4)
  - `monthly_dayExceedsMonthLength_clampsToLastDay` (AC-5)
  - `everyNDays_spacesCorrectlyFromInclusiveStart` (AC-6)
  - `validation_weeklyWithoutDaysOfWeek_throws` (AC-7)
  - `validation_monthlyWithoutDayOfMonth_throws` (AC-8)
  - `validation_everyNDaysWithZeroInterval_throws` (AC-9)
  - `getNextOccurrence_neverReturnsAfterDateItself` (AC-10)
  - `leapYear_february29Handled` (edge case)
  - `yearBoundary_crossesCorrectly` (edge case)

### Commands to Run
```bash
# Run all tests
cd services/backend && ./gradlew test

# Run specific test class
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.RecurrenceRuleParserTest"

# Verify build
cd services/backend && ./gradlew build

# Check formatting
cd services/backend && ./gradlew spotlessCheck

# Apply formatting if needed
cd services/backend && ./gradlew spotlessApply
```

### CI Gates Expected
- All tests pass (green)
- Build succeeds
- Spotless formatting passes
- No new compiler warnings

---

## Contract Impact
None. This is internal domain logic with no API changes.

---

## Docs Updates
- [ ] No index updates needed (no new ADR/contract/diagram)
- [ ] No ADR needed (pure domain logic)
- [ ] No diagrams needed (standard service pattern)

---

## Rollout / Rollback

### Rollout
- No feature flags needed (internal service, not exposed via API yet)
- No database migration (pure logic)
- Service will be consumed by ST-1003 (RoutineSchedulerService)

### Rollback Steps
- Revert the 3 new files if needed
- No data changes to revert
- Downstream story ST-1003 would need to wait

---

## Done Criteria

### Acceptance Criteria Mapping
| AC | Criteria | Test Method |
|----|----------|-------------|
| AC-1 | DAILY returns consecutive days from inclusive start | `daily_returnsConsecutiveDaysFromInclusiveStart` |
| AC-2 | WEEKLY single day, exclusive after | `weekly_singleDay_findsNextOccurrenceExclusive` |
| AC-3 | WEEKLY multiple days, inclusive start | `weekly_multipleDays_returnsCorrectSequenceFromInclusiveStart` |
| AC-4 | MONTHLY exclusive after | `monthly_normalDay_findsNextOccurrenceExclusive` |
| AC-5 | MONTHLY day > month length -> clamp to last day | `monthly_dayExceedsMonthLength_clampsToLastDay` |
| AC-6 | EVERY_N_DAYS inclusive start | `everyNDays_spacesCorrectlyFromInclusiveStart` |
| AC-7 | WEEKLY without daysOfWeek throws | `validation_weeklyWithoutDaysOfWeek_throws` |
| AC-8 | MONTHLY without dayOfMonth throws | `validation_monthlyWithoutDayOfMonth_throws` |
| AC-9 | EVERY_N_DAYS interval <= 0 throws | `validation_everyNDaysWithZeroInterval_throws` |
| AC-10 | getNextOccurrence never returns afterDate itself | `getNextOccurrence_neverReturnsAfterDateItself` |

### DoD Link
See `docs/_governance/dod.md`

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Off-by-one errors in date calculations | Incorrect task generation | Explicit test cases for boundary dates, use Java 21 date APIs |
| MONTHLY day clamping edge cases | Tasks on wrong day | Test Feb 28/29, months with 30/31 days |
| RecurrenceRule null fields | NPE in parser | Validation throws before calculation |

---

## Implementation Notes

### RecurrenceRuleParser Design
```java
@Service
public class RecurrenceRuleParser {

    public List<LocalDate> getOccurrencesInRange(RecurrenceRule rule, LocalDate fromDateInclusive, int count) {
        validate(rule);
        // Pattern match on sealed interface
        // Return up to count occurrences starting from fromDateInclusive
    }

    public LocalDate getNextOccurrence(RecurrenceRule rule, LocalDate afterDateExclusive) {
        validate(rule);
        // Return first occurrence AFTER afterDateExclusive (not including it)
    }

    private void validate(RecurrenceRule rule) {
        // Throw InvalidRecurrenceRuleException for invalid rules
    }
}
```

### Pattern Matching (Java 21 sealed interface)
```java
switch (rule) {
    case RecurrenceRule.Daily d -> handleDaily(fromDate, count);
    case RecurrenceRule.Weekly w -> handleWeekly(w, fromDate, count);
    case RecurrenceRule.Monthly m -> handleMonthly(m, fromDate, count);
    case RecurrenceRule.EveryNDays e -> handleEveryNDays(e, fromDate, count);
}
```

---

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-1002/prompt-plan.md`
- APPLY: `docs/planning/workpacks/ST-1002/prompt-apply.md`
