# Checklist: ST-1105 — Observability

## Implementation
- [ ] AsrMetrics class created
- [ ] asr_requests_total counter
- [ ] asr_latency_ms histogram
- [ ] asr_failures_total counter
- [ ] Labels are bounded
- [ ] Structured logging added
- [ ] MDC set in controller
- [ ] Prometheus endpoint enabled

## Tests
- [ ] AsrMetricsTest passes
- [ ] Metrics integration test
- [ ] Log verification test
- [ ] All ASR tests pass

## Security
- [ ] No PII in logs
- [ ] No API key in logs

## AC Verification
- [ ] AC-1: Request counter
- [ ] AC-2: Latency histogram
- [ ] AC-3: Failure counter
- [ ] AC-4: Log fields
- [ ] AC-5: No PII
- [ ] AC-6: CorrelationId
- [ ] AC-7: Prometheus endpoint

## Build
- [ ] `./gradlew build` passes
- [ ] `./gradlew spotlessCheck` passes
