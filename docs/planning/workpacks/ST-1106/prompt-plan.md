# Codex PLAN Prompt: ST-1106 — Security Boundaries + Integration Tests

## Instructions

You are in PLAN mode. Explore the codebase to gather information for implementing ST-1106.

**CRITICAL CONSTRAINTS:**
- NO file modifications
- ONLY read-only operations
- Output findings in structured format

**Allowed commands:** `ls`, `find`, `cat`, `head`, `tail`, `rg`, `grep`

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1106/workpack.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## Story Summary

Create comprehensive security-focused integration tests for ASR proxy:
- JWT required tests (401)
- Membership required tests (403)
- IDOR prevention tests (404 for wrong household)
- API key non-exposure tests
- Upstream error mapping tests (503, 429, timeout, 401)

---

## Exploration Tasks

### 1. Review existing ASR controller tests for patterns
```bash
cat services/backend/src/test/java/com/hometusk/asr/controller/AsrControllerIntegrationTest.java
```

### 2. Find existing security test patterns in other modules
```bash
find services/backend/src/test -name "*Security*Test*" -type f
rg "returns401|returns403|noAuth|notMember" services/backend/src/test/java --type java -l
```

### 3. Review IntegrationTestBase for JWT helpers
```bash
cat services/backend/src/test/java/com/hometusk/integration/IntegrationTestBase.java
```

### 4. Review ASR controller for security enforcement points
```bash
cat services/backend/src/main/java/com/hometusk/asr/controller/AsrController.java
```

### 5. Review ASR service for IDOR prevention (AsrTranscriptionRef lookup)
```bash
cat services/backend/src/main/java/com/hometusk/asr/service/AsrService.java
```

### 6. Check existing ASR exceptions for error mapping
```bash
ls services/backend/src/main/java/com/hometusk/asr/exception/
cat services/backend/src/main/java/com/hometusk/asr/exception/AsrException.java
```

### 7. Review GlobalExceptionHandler for ASR error mapping
```bash
rg "AsrException|AsrUnavailable|AsrRateLimited" services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java -C5
```

### 8. Review ASR client for API key handling (ensure non-exposure)
```bash
cat services/backend/src/main/java/com/hometusk/asr/client/AsrClientImpl.java | head -100
```

### 9. Check WireMock usage patterns in existing tests
```bash
rg "stubFor|WireMockServer" services/backend/src/test/java/com/hometusk/asr --type java -C3
```

### 10. Review contract for expected error codes
```bash
cat docs/contracts/http/asr-proxy.openapi.yaml | head -200
```

---

## Expected Output Format

```
## PLAN Findings

### 1. Existing Test Patterns
- Auth test pattern: [description]
- Membership test pattern: [description]
- WireMock stub pattern: [description]

### 2. IntegrationTestBase Helpers
- JWT helper method: [name]
- JWT for other user: [method if exists]
- Test household/user setup: [description]

### 3. Security Enforcement Points
- AsrController auth: [how enforced]
- Membership check: [method/service]
- IDOR prevention: [AsrTranscriptionRef lookup details]

### 4. Error Mapping
- 503 upstream → [mapped code]
- 429 upstream → [mapped code]
- timeout → [mapped code]
- 401 upstream → [mapped code]

### 5. API Key Handling
- Key injection point: [where]
- Exposure risk check: [description]

### 6. Existing Coverage Gaps
- [list tests NOT yet covered]

### 7. Test File Location
- Recommended: [path]

### 8. Risks/Blockers
- [list any issues]
```

---

## Anti-Scope-Creep

DO NOT explore:
- Rate limiting implementation (ST-1104)
- Observability/metrics (ST-1105)
- Frontend/web code
- Non-ASR security tests
