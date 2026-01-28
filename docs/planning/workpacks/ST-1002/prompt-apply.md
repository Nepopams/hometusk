# Codex APPLY Prompt: ST-1002 — Recurrence Rule Parser

## Mode: APPLY (Implementation)

You may create/edit files and run commands. Follow the plan strictly.

---

## From PLAN Phase Verification

**Findings confirmed:**
- RecurrenceRule is a sealed interface with 4 record subtypes (Daily, Weekly, Monthly, EveryNDays)
- Exception pattern: extend RuntimeException (simpler, no ErrorCode needed)
- Service pattern: @Service annotation in com.hometusk.routines.service
- Test pattern: JUnit 5 + AssertJ, method naming like `methodName_scenario_expectedResult`
- Validation overlap: keep parser validation independent of RoutineService validation

**Decisions:**
- `InvalidRecurrenceRuleException extends RuntimeException` with message constructor
- Parser validates independently (will be used by scheduler ST-1003)
- Use Java 21 sealed interface pattern matching (switch expressions)

---

## Anchors (read before implementing)

```
docs/planning/workpacks/ST-1002/workpack.md
docs/planning/epics/EP-010/stories/ST-1002-recurrence-parser.md
services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java
services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java (for patterns)
services/backend/src/test/java/com/hometusk/routines/service/RoutineServiceTest.java (for test patterns)
```

---

## Files to Create

### 1. InvalidRecurrenceRuleException.java

**Path:** `services/backend/src/main/java/com/hometusk/routines/domain/InvalidRecurrenceRuleException.java`

```java
package com.hometusk.routines.domain;

/**
 * Exception thrown when a RecurrenceRule is invalid for parsing.
 */
public class InvalidRecurrenceRuleException extends RuntimeException {

    public InvalidRecurrenceRuleException(String message) {
        super(message);
    }
}
```

### 2. RecurrenceRuleParser.java

**Path:** `services/backend/src/main/java/com/hometusk/routines/service/RecurrenceRuleParser.java`

**Key requirements:**
- `@Service` annotation
- Two public methods with exact signatures:
  - `List<LocalDate> getOccurrencesInRange(RecurrenceRule rule, LocalDate fromDateInclusive, int count)`
  - `LocalDate getNextOccurrence(RecurrenceRule rule, LocalDate afterDateExclusive)`
- Validation method that throws `InvalidRecurrenceRuleException`
- Use Java 21 switch pattern matching on sealed interface

**Validation rules:**
- WEEKLY: `daysOfWeek` must not be null or empty
- MONTHLY: `dayOfMonth` must be 1-31 (validation only, clamping happens in calculation)
- EVERY_N_DAYS: `interval` must be > 0

**Key semantics:**
- `fromDateInclusive`: first returned date CAN be this date if it matches rule
- `afterDateExclusive`: first returned date is STRICTLY AFTER this date (never equals it)
- MONTHLY day clamping: if dayOfMonth > month length, use last day of month

### 3. RecurrenceRuleParserTest.java

**Path:** `services/backend/src/test/java/com/hometusk/routines/service/RecurrenceRuleParserTest.java`

**Required test methods (map to ACs):**

| Test Method | AC | Description |
|-------------|-----|-------------|
| `daily_returnsConsecutiveDaysFromInclusiveStart` | AC-1 | DAILY from 2026-01-28, count=3 → [28,29,30] |
| `weekly_singleDay_findsNextOccurrenceExclusive` | AC-2 | WEEKLY [SAT], after Wed 2026-01-28 → Sat 2026-02-01 |
| `weekly_multipleDays_returnsCorrectSequenceFromInclusiveStart` | AC-3 | WEEKLY [MON,FRI], from Wed 2026-01-28, count=4 → [Fri 31, Mon Feb2, Fri Feb6, Mon Feb9] |
| `monthly_normalDay_findsNextOccurrenceExclusive` | AC-4 | MONTHLY day=15, after 2026-01-28 → 2026-02-15 |
| `monthly_dayExceedsMonthLength_clampsToLastDay` | AC-5 | MONTHLY day=31, after 2026-02-01 → 2026-02-28 |
| `everyNDays_spacesCorrectlyFromInclusiveStart` | AC-6 | EVERY_N_DAYS interval=3, from 2026-01-28, count=4 → [28,31,Feb3,Feb6] |
| `validation_weeklyWithoutDaysOfWeek_throws` | AC-7 | WEEKLY with null/empty daysOfWeek throws |
| `validation_monthlyWithoutDayOfMonth_throws` | AC-8 | MONTHLY with dayOfMonth < 1 or > 31 throws |
| `validation_everyNDaysWithZeroInterval_throws` | AC-9 | EVERY_N_DAYS with interval ≤ 0 throws |
| `getNextOccurrence_neverReturnsAfterDateItself` | AC-10 | DAILY, after 2026-01-28 → 2026-01-29 (not 28) |
| `leapYear_february29Handled` | edge | MONTHLY day=29, year 2024 (leap) handled |
| `yearBoundary_crossesCorrectly` | edge | DAILY from Dec 30, count=5 crosses year |

**Test patterns:**
```java
@Test
void daily_returnsConsecutiveDaysFromInclusiveStart() {
    RecurrenceRule rule = new RecurrenceRule.Daily();
    LocalDate from = LocalDate.of(2026, 1, 28);

    List<LocalDate> result = parser.getOccurrencesInRange(rule, from, 3);

    assertThat(result).containsExactly(
        LocalDate.of(2026, 1, 28),
        LocalDate.of(2026, 1, 29),
        LocalDate.of(2026, 1, 30)
    );
}
```

---

## Forbidden Paths (DO NOT MODIFY)

- `services/backend/src/main/java/com/hometusk/routines/domain/RecurrenceRule.java` — existing interface
- `services/backend/src/main/java/com/hometusk/routines/service/RoutineService.java` — existing service
- Any files outside `services/backend/src/main/java/com/hometusk/routines/` and test equivalent
- Any API controllers
- Any database migrations

---

## Implementation Order

1. Create `InvalidRecurrenceRuleException.java`
2. Create `RecurrenceRuleParser.java` with full implementation
3. Create `RecurrenceRuleParserTest.java` with all 12 test methods
4. Run tests: `./gradlew test --tests "com.hometusk.routines.service.RecurrenceRuleParserTest"`
5. Run formatting: `./gradlew spotlessApply`
6. Verify full build: `./gradlew build`

---

## Verification Commands

```bash
# Run specific tests
cd services/backend && ./gradlew test --tests "com.hometusk.routines.service.RecurrenceRuleParserTest"

# Run all tests
cd services/backend && ./gradlew test

# Apply formatting
cd services/backend && ./gradlew spotlessApply

# Full build
cd services/backend && ./gradlew build
```

**Expected:** All tests pass, build succeeds, no warnings.

---

## STOP-THE-LINE Rule

If ANY of the following occurs, STOP immediately and report:
- Tests fail and you cannot determine why
- Need to modify forbidden files
- Unexpected codebase state (missing dependencies, etc.)
- Unclear requirement interpretation

Do NOT guess or improvise. Report and wait for clarification.

---

## Output Checklist

After implementation, confirm:
- [ ] `InvalidRecurrenceRuleException.java` created
- [ ] `RecurrenceRuleParser.java` created with @Service annotation
- [ ] `RecurrenceRuleParserTest.java` created with 12 test methods
- [ ] All tests pass
- [ ] `./gradlew spotlessApply` applied
- [ ] `./gradlew build` succeeds
- [ ] No changes to forbidden files
