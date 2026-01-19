# Checklist: ST-002 — Intent Accuracy Validation

## Acceptance Criteria Verification

### AC1: Test dataset created
- [ ] File exists: `src/test/resources/intent-accuracy-dataset.json`
- [ ] Contains 50+ test commands
- [ ] Covers create_task valid cases (20+)
- [ ] Covers complete_task valid cases (15+)
- [ ] Covers invalid type cases (5+)
- [ ] Covers invalid schema cases (10+)

### AC2: Accuracy measurement
- [ ] Test class exists: `IntentAccuracyValidationTest.java`
- [ ] Test runs successfully
- [ ] Accuracy calculated correctly
- [ ] Accuracy >= 80% (or failures documented)

### AC3: Results documented
- [ ] File exists: `docs/planning/mvp-accuracy-validation.md`
- [ ] Contains total commands tested
- [ ] Contains pass/fail counts
- [ ] Contains accuracy percentage
- [ ] Contains category breakdown

### AC4: Failure analysis (if applicable)
- [ ] If accuracy < 80%, failures listed
- [ ] Root causes documented
- [ ] Stage 2 recommendations provided

---

## DoD Verification

### Code Quality
- [ ] Test code follows project conventions
- [ ] Spotless formatting applied
- [ ] No compiler warnings

### Tests Required
- [ ] IntentAccuracyValidationTest passes
- [ ] All other tests still pass: `./scripts/test.sh`

### Documentation Updates
- [ ] mvp-accuracy-validation.md created and complete

---

## Final Verification Commands

```bash
# 1. Run accuracy validation
./gradlew :services:backend:test --tests "*IntentAccuracyValidationTest*"

# 2. Verify all tests pass
./scripts/test.sh

# 3. Check documentation exists
cat docs/planning/mvp-accuracy-validation.md
```

---

## Sign-off

| Role | Approved | Date |
|------|----------|------|
| Codex (Implementation) | [ ] | |
| Claude (Review) | [ ] | |
| Human (Final Gate) | [ ] | |
