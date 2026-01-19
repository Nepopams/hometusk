# Work Package: ST-002 — Intent Accuracy Validation

**Story:** ST-002
**Epic:** EP-001 (MVP Closure)
**Sprint:** S01
**Target:** Codex implementation

---

## Summary

Create test dataset and validation test to verify intent recognition accuracy meets 80%+ MVP target. Document results.

---

## Anchors (Source of Truth)

| Anchor | Path | Relevance |
|--------|------|-----------|
| MVP Scope | `docs/planning/mvp.md` | Success metric: "80%+ intent recognition accuracy" |
| Story Spec | `docs/planning/epics/EP-001/stories/ST-002-intent-accuracy-validation.md` | Full AC |
| CommandType | `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java` | Supported intents |
| SchemaValidator | `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java` | Validation logic |

---

## Implementation Steps

### Step 1: Create test dataset

**File:** `services/backend/src/test/resources/intent-accuracy-dataset.json`

**Action:** Create JSON file with 50+ test commands.

```json
{
  "description": "Intent accuracy validation dataset for MVP",
  "version": "1.0",
  "commands": [
    {
      "id": "TC001",
      "category": "create_task_valid",
      "input": {
        "householdId": "{{HOUSEHOLD_ID}}",
        "type": "create_task",
        "payload": {
          "title": "Clean the kitchen"
        },
        "source": "web"
      },
      "expectedIntent": "create_task",
      "expectedOutcome": "success",
      "description": "Minimal valid create_task"
    },
    {
      "id": "TC002",
      "category": "create_task_valid",
      "input": {
        "householdId": "{{HOUSEHOLD_ID}}",
        "type": "create_task",
        "payload": {
          "title": "Buy groceries",
          "description": "Milk, bread, eggs",
          "zoneId": "{{ZONE_ID}}",
          "deadline": "{{FUTURE_DATE}}"
        },
        "source": "mobile"
      },
      "expectedIntent": "create_task",
      "expectedOutcome": "success",
      "description": "Full create_task with all optional fields"
    },
    // ... 48+ more test cases covering:
    // - Valid create_task variations (20 cases)
    // - Valid complete_task variations (15 cases)
    // - Invalid commands - unknown type (5 cases)
    // - Invalid commands - schema violations (10 cases)
  ]
}
```

**Categories to cover:**
1. `create_task_valid` - 20 cases
2. `complete_task_valid` - 15 cases
3. `invalid_type` - 5 cases (unknown command type)
4. `invalid_schema` - 10 cases (missing required fields, wrong types)

**Verification:** JSON is valid and parseable.

---

### Step 2: Create validation test class

**File:** `services/backend/src/test/java/com/hometusk/validation/IntentAccuracyValidationTest.java`

**Action:** Create parameterized test that runs all dataset commands.

```java
@SpringBootTest
@AutoConfigureMockMvc
class IntentAccuracyValidationTest extends IntegrationTestBase {

    @Value("classpath:intent-accuracy-dataset.json")
    private Resource datasetResource;

    @Test
    void validateIntentAccuracy() throws Exception {
        // 1. Load dataset
        IntentDataset dataset = objectMapper.readValue(
            datasetResource.getInputStream(), IntentDataset.class);

        // 2. Setup test household and zone
        UUID householdId = createTestHousehold();
        UUID zoneId = createTestZone(householdId);
        UUID taskId = createTestTask(householdId); // for complete_task tests

        // 3. Run each command and track results
        List<TestResult> results = new ArrayList<>();
        for (IntentTestCase tc : dataset.commands()) {
            TestResult result = executeAndVerify(tc, householdId, zoneId, taskId);
            results.add(result);
        }

        // 4. Calculate accuracy
        long correct = results.stream().filter(TestResult::passed).count();
        double accuracy = (double) correct / results.size() * 100;

        // 5. Log results
        log.info("Intent Accuracy: {}/{} = {:.2f}%", correct, results.size(), accuracy);
        results.stream()
            .filter(r -> !r.passed())
            .forEach(r -> log.warn("FAILED: {} - {}", r.testCaseId(), r.reason()));

        // 6. Assert target met
        assertThat(accuracy)
            .as("Intent recognition accuracy should be >= 80%")
            .isGreaterThanOrEqualTo(80.0);

        // 7. Write results to file for documentation
        writeResultsReport(results, accuracy);
    }

    private void writeResultsReport(List<TestResult> results, double accuracy) {
        // Write to target/intent-accuracy-results.json
    }
}
```

**Verification:** Test compiles and runs.

---

### Step 3: Define test case model

**File:** `services/backend/src/test/java/com/hometusk/validation/IntentAccuracyValidationTest.java` (inner classes)

```java
record IntentDataset(String description, String version, List<IntentTestCase> commands) {}

record IntentTestCase(
    String id,
    String category,
    Map<String, Object> input,
    String expectedIntent,
    String expectedOutcome,
    String description
) {}

record TestResult(
    String testCaseId,
    String category,
    boolean passed,
    String actualIntent,
    String actualOutcome,
    String reason
) {}
```

---

### Step 4: Implement test execution logic

**File:** Same test class.

**Logic:**
```java
private TestResult executeAndVerify(IntentTestCase tc, UUID householdId, UUID zoneId, UUID taskId) {
    // Replace placeholders
    String requestBody = prepareRequest(tc.input(), householdId, zoneId, taskId);

    try {
        MvcResult result = mockMvc.perform(post("/api/v1/commands")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();

        // Determine actual outcome
        String actualOutcome = status == 200 ? "success" : "error";
        String actualIntent = extractIntent(body, tc.input());

        // Compare
        boolean intentCorrect = tc.expectedIntent().equals(actualIntent);
        boolean outcomeCorrect = tc.expectedOutcome().equals(actualOutcome);
        boolean passed = intentCorrect && outcomeCorrect;

        return new TestResult(tc.id(), tc.category(), passed,
            actualIntent, actualOutcome,
            passed ? "OK" : "Expected " + tc.expectedOutcome() + "/" + tc.expectedIntent());

    } catch (Exception e) {
        return new TestResult(tc.id(), tc.category(), false,
            "error", "exception", e.getMessage());
    }
}
```

---

### Step 5: Run validation and capture results

**Command:** `./gradlew :services:backend:test --tests "*IntentAccuracyValidationTest*"`

**Output:** Results in `target/intent-accuracy-results.json`

---

### Step 6: Create documentation report

**File:** `docs/planning/mvp-accuracy-validation.md`

**Template:**
```markdown
# MVP Intent Recognition Accuracy Validation

**Date:** YYYY-MM-DD
**Test Run:** IntentAccuracyValidationTest

## Summary

| Metric | Value |
|--------|-------|
| Total test cases | 50 |
| Passed | XX |
| Failed | XX |
| **Accuracy** | **XX.X%** |
| Target | 80% |
| **Status** | **PASS/FAIL** |

## Test Categories

| Category | Cases | Passed | Accuracy |
|----------|-------|--------|----------|
| create_task_valid | 20 | XX | XX% |
| complete_task_valid | 15 | XX | XX% |
| invalid_type | 5 | XX | XX% |
| invalid_schema | 10 | XX | XX% |

## Failed Cases (if any)

| ID | Category | Expected | Actual | Reason |
|----|----------|----------|--------|--------|
| TCXXX | ... | ... | ... | ... |

## Conclusion

Intent recognition accuracy is XX.X%, which [MEETS/DOES NOT MEET] the 80% MVP target.
```

**Action:** Codex fills in actual results after running test.

---

## Files Modified/Created

| File | Action |
|------|--------|
| `src/test/resources/intent-accuracy-dataset.json` | Create (50+ test cases) |
| `IntentAccuracyValidationTest.java` | Create |
| `docs/planning/mvp-accuracy-validation.md` | Create (filled with results) |

---

## Forbidden Changes

- DO NOT modify production code
- DO NOT change existing tests
- DO NOT modify contracts

---

## Invariants to Preserve

1. All existing tests must still pass
2. Test data must not pollute production database

---

## Test Commands

```bash
# Run accuracy validation test
./gradlew :services:backend:test --tests "*IntentAccuracyValidationTest*"

# Check results
cat services/backend/build/test-results/test/*.xml | grep -A5 "IntentAccuracy"
```

---

## Done Criteria

- [ ] Dataset with 50+ test cases created
- [ ] Validation test implemented and passing
- [ ] Accuracy >= 80% verified
- [ ] Results documented in mvp-accuracy-validation.md
