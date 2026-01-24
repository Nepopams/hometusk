# Story: ST-706 — Observability Hooks

## Sources of Truth
- Epic: `docs/planning/epics/EP-008/epic.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — Sprint S07 stretch

## User Value
> "Как SRE, хочу видеть метрики использования аналитики и время ответа для мониторинга."

---

## Description
Add observability for analytics endpoint:
- Structured logging
- Metrics (request count, latency)
- No PII in logs

---

## Acceptance Criteria

### AC-1: Request logging
```
Given analytics request
When processed
Then log entry contains:
  - householdId (UUID)
  - period
  - latency_ms
  - status (200/403/etc)
And NO user email or name in logs
```

### AC-2: Metrics exposed
```
Given Micrometer configured
Then metrics available:
  - http_server_requests_seconds{uri="/api/v1/households/{householdId}/analytics"}
Or custom:
  - analytics_requests_total{period, status}
```

### AC-3: No PII in logs
```
Given log output
Then contains only:
  - UUIDs (householdId, userId)
  - Technical data (latency, status)
And NOT:
  - User email
  - User display name
  - Task titles
```

---

## Implementation Notes

```java
@GetMapping
public ResponseEntity<AnalyticsSummary> getAnalytics(...) {
    long start = System.currentTimeMillis();
    try {
        // ... logic
        return ResponseEntity.ok(response);
    } finally {
        long latency = System.currentTimeMillis() - start;
        log.info("analytics_request householdId={} period={} latency_ms={}",
                 householdId, period, latency);
    }
}
```

---

## Points
**1 point**

---

## Flags
- observability: yes
