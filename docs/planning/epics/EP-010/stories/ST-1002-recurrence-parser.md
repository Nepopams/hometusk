# Story: ST-1002 — Recurrence Rule Parser

## Sources of Truth
- Epic: `docs/planning/epics/EP-010/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q3-recurring-tasks-scheduling.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Awaiting Human Gate approval

## User Value
> "Указал 'каждую субботу' или 'каждые 3 дня' — система понимает когда следующие даты задач."

---

## Description
Implement RRULE-lite parser that converts recurrence rule JSON to a sequence of dates:
- `RecurrenceRuleParser` service
- Support patterns: DAILY, WEEKLY, MONTHLY, EVERY_N_DAYS
- Method: `getNextOccurrences(rule, startDate, count)` -> List<LocalDate>
- Method: `getNextOccurrence(rule, afterDate)` -> LocalDate

---

## In Scope
- `RecurrenceRule` value object with typed fields
- `RecurrenceRuleParser` service
- DAILY pattern: every day
- WEEKLY pattern: specific days of week
- MONTHLY pattern: specific day of month (handles month length)
- EVERY_N_DAYS pattern: every N days from start
- Validation of rule structure

## Out of Scope
- Complex RRULE (exceptions, BYSETPOS, end date)
- Timezone handling (use household timezone)
- Scheduler integration (ST-1003)
- UI rule builder (ST-1005)

---

## Acceptance Criteria

### AC-1: DAILY pattern
```
Given rule = { "type": "DAILY" }
And startDate = 2026-01-28
When getNextOccurrences(rule, startDate, 3)
Then returns [2026-01-28, 2026-01-29, 2026-01-30]
```

### AC-2: WEEKLY pattern (single day)
```
Given rule = { "type": "WEEKLY", "daysOfWeek": ["SATURDAY"] }
And startDate = 2026-01-28 (Wednesday)
When getNextOccurrence(rule, startDate)
Then returns 2026-01-31 (next Saturday)
```

### AC-3: WEEKLY pattern (multiple days)
```
Given rule = { "type": "WEEKLY", "daysOfWeek": ["MONDAY", "FRIDAY"] }
And startDate = 2026-01-28 (Wednesday)
When getNextOccurrences(rule, startDate, 4)
Then returns [2026-01-30 (Fri), 2026-02-02 (Mon), 2026-02-06 (Fri), 2026-02-09 (Mon)]
```

### AC-4: MONTHLY pattern (valid day)
```
Given rule = { "type": "MONTHLY", "dayOfMonth": 15 }
And startDate = 2026-01-28
When getNextOccurrence(rule, startDate)
Then returns 2026-02-15
```

### AC-5: MONTHLY pattern (day > month length)
```
Given rule = { "type": "MONTHLY", "dayOfMonth": 31 }
And startDate = 2026-02-01 (Feb has 28 days in 2026)
When getNextOccurrence(rule, startDate)
Then returns 2026-02-28 (last day of Feb)
```

### AC-6: EVERY_N_DAYS pattern
```
Given rule = { "type": "EVERY_N_DAYS", "interval": 3 }
And startDate = 2026-01-28
When getNextOccurrences(rule, startDate, 4)
Then returns [2026-01-28, 2026-01-31, 2026-02-03, 2026-02-06]
```

### AC-7: Validation - WEEKLY requires daysOfWeek
```
Given rule = { "type": "WEEKLY" } (no daysOfWeek)
When parsing rule
Then throws InvalidRecurrenceRuleException
```

### AC-8: Validation - MONTHLY requires dayOfMonth
```
Given rule = { "type": "MONTHLY" } (no dayOfMonth)
When parsing rule
Then throws InvalidRecurrenceRuleException
```

### AC-9: Validation - EVERY_N_DAYS requires interval > 0
```
Given rule = { "type": "EVERY_N_DAYS", "interval": 0 }
When parsing rule
Then throws InvalidRecurrenceRuleException
```

### AC-10: getNextOccurrence skips current date if already passed
```
Given rule = { "type": "DAILY" }
And afterDate = 2026-01-28
When getNextOccurrence(rule, afterDate)
Then returns 2026-01-29 (not 2026-01-28)
```

---

## Test Strategy

### Unit Tests (primary focus)
- `RecurrenceRuleParserTest`:
  - `daily_returnsConsecutiveDays`
  - `weekly_singleDay_findsNextOccurrence`
  - `weekly_multipleDays_alternatesCorrectly`
  - `monthly_normalDay_works`
  - `monthly_dayExceedsMonthLength_usesLastDay`
  - `everyNDays_spacesCorrectly`
  - `validation_weeklyWithoutDays_throws`
  - `validation_monthlyWithoutDay_throws`
  - `validation_everyNWithZero_throws`
- Edge cases:
  - Leap year Feb 29
  - Year boundary crossing
  - Multiple year span

### Integration Tests
- None (pure domain logic, no external dependencies)

---

## Points
**3 points**

## Dependencies
- ST-1001 (RecurrenceRule stored in Routine entity)

## Flags
- contract_impact: no (internal logic)
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no
