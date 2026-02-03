# Story: ST-1105 — Observability (Metrics + Structured Logs)

## Sources of Truth
- Epic: `docs/planning/epics/EP-011/epic.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-asr-integration-foundation.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Draft** — Blocked by ST-1103 (endpoints must exist)

## User Value
> "Ops может видеть метрики и логи по ASR: сколько запросов, какие ошибки, какая латентность."

---

## Description
Implement observability for ASR proxy:
- Micrometer metrics
- Structured logging (JSON)
- CorrelationId in all logs

---

## In Scope
- Metrics (Micrometer):
  - `asr_requests_total{status}` — counter
  - `asr_latency_ms{phase=create|poll}` — histogram
  - `asr_failures_total{reason}` — counter
- Structured logs:
  - correlationId, userId, householdId
  - sizeBytes, status, durationMs
- Log sanitization (no PII, no API key)

## Out of Scope
- Alerting rules (ops concern)
- Dashboard (Grafana config)
- Distributed tracing (spans) — v0 uses correlationId only

---

## Acceptance Criteria

### AC-1: Request counter metric
```
Given ASR request completed
Then asr_requests_total{status="success"} incremented
Or asr_requests_total{status="error"} incremented
```

### AC-2: Latency histogram metric
```
Given create transcription call
When call completes
Then asr_latency_ms{phase="create"} records duration
```

### AC-3: Failure counter with reason
```
Given asr-service returns 503
Then asr_failures_total{reason="unavailable"} incremented
```

### AC-4: Structured log on request
```
Given POST /asr/transcriptions
When request processed
Then log entry contains:
  - level: INFO
  - correlationId
  - userId
  - householdId
  - sizeBytes
  - status (success/error)
  - durationMs
```

### AC-5: No PII in logs
```
Given any log output
Then no file content logged
And no user email/name logged
And no API key logged
```

### AC-6: CorrelationId in all logs
```
Given request with correlationId
Then all related log entries include same correlationId
```

### AC-7: Metrics endpoint exposes ASR metrics
```
Given /actuator/prometheus endpoint
When scraped
Then asr_* metrics present
```

---

## Test Strategy

### Unit Tests
- `AsrMetricsTest`:
  - `requestCounter_incremented`
  - `latencyHistogram_recorded`
  - `failureCounter_incrementedWithReason`

### Integration Tests
- Verify metrics via TestRestTemplate to /actuator/prometheus
- Verify structured log output (capture log appender)

---

## Points
**3 points**

## Dependencies
- ST-1103 (endpoints must exist)

## Flags
- contract_impact: no
- adr_needed: no
- diagrams_needed: no
- security_sensitive: no
