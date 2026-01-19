# Story: Validate Intent Recognition Accuracy

**ID:** ST-002
**Epic:** EP-001 (MVP Closure)
**Sprint:** S01
**Points:** 2
**Status:** ready

---

## Title

Validate intent recognition accuracy meets 80%+ target

---

## Description

As a product owner, I want documented evidence that the system correctly recognizes user intents at least 80% of the time, so that I can verify this MVP success metric is met.

**Context:**
MVP success metric requires "80%+ intent recognition accuracy". Current implementation is rule-based (not AI/NL), so for supported command types (create_task, complete_task), accuracy should be 100% for well-formed requests. This story validates and documents the actual accuracy.

**User Value:**
Confidence that the system interprets commands correctly.

---

## Acceptance Criteria

### AC1: Test dataset created
```
Given MVP supports create_task and complete_task commands
When test dataset is created
Then it contains at least 50 sample commands covering:
  - Valid create_task commands (various payloads)
  - Valid complete_task commands
  - Edge cases (missing optional fields, boundary values)
  - Invalid commands (unsupported types, malformed payloads)
```

### AC2: Accuracy measurement
```
Given test dataset with 50+ commands
When accuracy test is executed
Then system correctly identifies intent for >= 80% of commands
And results are logged with per-command outcomes
```

### AC3: Results documented
```
Given accuracy test completes
When results are documented
Then document includes:
  - Total commands tested
  - Correct intent recognitions
  - Accuracy percentage
  - List of any failures with root cause
```

### AC4: Failure analysis (if applicable)
```
Given accuracy is below 80%
When failures are analyzed
Then root causes are documented
And recommendations for Stage 2 improvement are provided
```

---

## Test Strategy

### Approach
1. Create test data file: `src/test/resources/intent-accuracy-dataset.json`
2. Create parameterized test: `IntentAccuracyValidationTest.java`
3. Run test and capture results
4. Document in `docs/planning/mvp-accuracy-validation.md`

### Test Data Structure
```json
{
  "commands": [
    {
      "input": { "type": "create_task", "payload": {...} },
      "expectedIntent": "create_task",
      "description": "Standard task creation"
    }
  ]
}
```

### What Counts as "Correct"
- Command type correctly parsed
- Payload validated without false rejections
- No false positives (invalid commands not accepted)

---

## Technical Notes

**Files to create:**
- `services/backend/src/test/resources/intent-accuracy-dataset.json`
- `services/backend/src/test/java/com/hometusk/validation/IntentAccuracyValidationTest.java`

**Files to create (docs):**
- `docs/planning/mvp-accuracy-validation.md`

**Note:** This is primarily a validation/documentation task, not a feature implementation.

---

## Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | No API changes |
| adr_needed | no | Validation only |
| diagrams_needed | no | No structural changes |
| security_sensitive | no | Test data only |
| traceability_critical | no | Validation task |

---

## Definition of Ready Checklist

- [x] Title is clear and user-centric
- [x] Description includes context and user value
- [x] Acceptance criteria are testable
- [x] Test strategy defined
- [x] Flags assessed
- [x] No blocking dependencies
