# Story: Validate Response Time Performance

**ID:** ST-003
**Epic:** EP-001 (MVP Closure)
**Sprint:** S02
**Points:** 2
**Status:** ready

---

## Title

Validate response time performance meets < 2s p95 target

---

## Description

As a product owner, I want documented evidence that the system responds within 2 seconds for 95% of requests, so that I can verify this MVP success metric is met.

**Context:**
MVP success metric requires "< 2s p95 response time for command processing". This story creates a performance test, runs it, and documents the results.

**User Value:**
Confidence that the system is responsive enough for real-world use.

---

## Acceptance Criteria

### AC1: Performance test created
```
Given MVP command processing pipeline exists
When performance test is created
Then test:
  - Sends at least 100 create_task commands
  - Uses realistic payload sizes
  - Runs against test database (Testcontainers)
  - Measures response time for each request
```

### AC2: P95 calculation
```
Given performance test completes with 100+ requests
When p95 is calculated
Then p95 response time is computed correctly
And value is compared against 2s target
```

### AC3: Results documented
```
Given performance test completes
When results are documented
Then document includes:
  - Number of requests
  - p50, p95, p99, max response times
  - Pass/Fail against 2s p95 target
  - Test environment description
```

### AC4: Bottleneck analysis (if applicable)
```
Given p95 exceeds 2s target
When bottlenecks are analyzed
Then slow components identified
And recommendations for optimization provided
```

---

## Test Strategy

### Approach
1. Create performance test: `CommandPerformanceTest.java`
2. Use Spring's `StopWatch` or similar for timing
3. Run 100+ sequential requests (no concurrent load for MVP)
4. Calculate percentiles
5. Document in `docs/planning/mvp-performance-validation.md`

### Test Scenario
```java
@Test
void performanceTest_createTask_p95Under2Seconds() {
    List<Long> responseTimes = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        long start = System.currentTimeMillis();
        // POST /api/v1/commands with create_task
        long elapsed = System.currentTimeMillis() - start;
        responseTimes.add(elapsed);
    }
    long p95 = calculateP95(responseTimes);
    assertThat(p95).isLessThan(2000);
}
```

### Environment
- Testcontainers PostgreSQL
- Manual decision provider (no AI Platform latency)
- Single-threaded execution

---

## Technical Notes

**Files to create:**
- `services/backend/src/test/java/com/hometusk/validation/CommandPerformanceTest.java`

**Files to create (docs):**
- `docs/planning/mvp-performance-validation.md`

**Considerations:**
- Test measures application latency, not network
- AI Platform mode would have different characteristics (tested separately)
- For MVP, manual/fallback mode is primary concern

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
