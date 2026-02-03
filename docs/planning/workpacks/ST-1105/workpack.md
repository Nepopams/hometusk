# Workpack: ST-1105 — Observability (Metrics + Structured Logs)

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S14/sprint.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Add Micrometer metrics and structured logging to ASR proxy for production observability.

---

## Scope

### In Scope
- `asr_requests_total{status}` counter
- `asr_latency_ms{phase}` histogram
- `asr_failures_total{reason}` counter
- Structured logs with correlationId, userId, householdId, sizeBytes, durationMs
- Expose at `/actuator/prometheus`

### Out of Scope
- Alerting rules
- Grafana dashboards
- Distributed tracing spans

---

## Files to Change

| File | Action |
|------|--------|
| `AsrMetrics.java` | CREATE |
| `AsrMetricsTest.java` | CREATE |
| `AsrService.java` | MODIFY add metrics/logging |
| `AsrController.java` | MODIFY set MDC |
| `AsrClientImpl.java` | MODIFY add latency timer |
| `application.yml` | MODIFY enable prometheus |
| `AsrControllerIntegrationTest.java` | MODIFY add metrics tests |

---

## Implementation Plan

1. Create AsrMetrics component (follow DecisionMetrics pattern)
2. Instrument AsrService with counters
3. Add structured logging to AsrService
4. Add latency timers to AsrClientImpl
5. Add failure counter recording
6. Verify prometheus endpoint config
7. Write AsrMetricsTest unit tests
8. Add metrics integration test

---

## Metrics Labels (Bounded)
- status: success | error
- phase: create | poll
- reason: unavailable | timeout | invalid_format | rate_limited | file_too_large

---

## Verification Commands

```bash
./gradlew build
./gradlew test --tests "*AsrMetrics*"
./gradlew test --tests "*Asr*"
```

---

## AC Mapping

| AC | Description |
|----|-------------|
| AC-1 | Request counter incremented |
| AC-2 | Latency histogram recorded |
| AC-3 | Failure counter with reason |
| AC-4 | Structured log fields |
| AC-5 | No PII in logs |
| AC-6 | CorrelationId in all logs |
| AC-7 | Metrics at /actuator/prometheus |
