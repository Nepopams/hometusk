# Codex PLAN Prompt: ST-1105 — Observability (Metrics + Structured Logs)

## Instructions

You are in PLAN mode. Explore the codebase to gather information for implementing ST-1105.

**CRITICAL CONSTRAINTS:**
- NO file modifications
- ONLY read-only operations
- Output findings in structured format

**Allowed commands:** `ls`, `find`, `cat`, `head`, `tail`, `rg`, `grep`

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1105/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1105-asr-observability.md`
- Epic: `docs/planning/epics/EP-011/epic.md`

---

## Story Summary

Add Micrometer metrics and structured logging to ASR proxy:
- `asr_requests_total{status}` counter
- `asr_latency_ms{phase}` histogram
- `asr_failures_total{reason}` counter
- Structured logs with correlationId, userId, householdId, sizeBytes, durationMs
- Expose metrics at `/actuator/prometheus`

---

## Exploration Tasks

### 1. Check if Micrometer is already a dependency
```bash
grep -i "micrometer" services/backend/build.gradle.kts
cat services/backend/build.gradle.kts | grep -A10 "dependencies"
```

### 2. Find existing metrics patterns (DecisionMetrics or similar)
```bash
rg "MeterRegistry" services/backend/src/main/java --type java -l
rg "Counter\." services/backend/src/main/java --type java -C3
rg "Timer\." services/backend/src/main/java --type java -C3
find services/backend/src -name "*Metrics*" -type f
```

### 3. Find existing structured logging patterns
```bash
rg "MDC\." services/backend/src/main/java --type java -l
rg "log\." services/backend/src/main/java/com/hometusk/asr --type java -C2
```

### 4. Review current AsrService for instrumentation points
```bash
cat services/backend/src/main/java/com/hometusk/asr/service/AsrService.java
```

### 5. Review current AsrController for MDC setup
```bash
cat services/backend/src/main/java/com/hometusk/asr/controller/AsrController.java
```

### 6. Review current AsrClientImpl for latency timer placement
```bash
cat services/backend/src/main/java/com/hometusk/asr/client/AsrClientImpl.java
```

### 7. Check prometheus actuator config in application.yml
```bash
grep -A20 "management:" services/backend/src/main/resources/application.yml
grep -i "prometheus" services/backend/src/main/resources/application.yml
```

### 8. Check existing logging config
```bash
grep -A10 "logging:" services/backend/src/main/resources/application.yml
```

### 9. Find existing metrics tests patterns
```bash
find services/backend/src/test -name "*Metrics*Test*" -type f
rg "MeterRegistry" services/backend/src/test/java --type java -l
```

### 10. Check ASR exception classes (for failure reason labels)
```bash
ls services/backend/src/main/java/com/hometusk/asr/exception/
```

---

## Expected Output Format

```
## PLAN Findings

### 1. Micrometer Dependency
- Already present: [yes/no]
- Version: [if present]
- Spring Boot Actuator: [present/missing]

### 2. Existing Metrics Patterns
- DecisionMetrics or similar: [path if found]
- MeterRegistry injection pattern: [description]
- Counter/Timer usage: [description]

### 3. Structured Logging Patterns
- MDC usage: [found/not found]
- Existing fields: [list]
- Logging library: [SLF4J/Logback/etc]

### 4. ASR Service Analysis
- Methods to instrument: [list]
- Suggested metric points: [list]

### 5. ASR Controller Analysis
- MDC setup location: [description]
- Fields to set: [list]

### 6. ASR Client Analysis
- Latency timer placement: [description]
- Existing timing: [yes/no]

### 7. Prometheus Config
- Actuator endpoint enabled: [yes/no]
- prometheus in includes: [yes/no]

### 8. ASR Exceptions (for failure reason labels)
- [list exception classes and suggested label mapping]

### 9. Risks/Blockers
- [list any issues]
```

---

## Anti-Scope-Creep

DO NOT explore:
- Security tests (ST-1106)
- Alerting/Grafana dashboards
- Distributed tracing
- Web/frontend code
