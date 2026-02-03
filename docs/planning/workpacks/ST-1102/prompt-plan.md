# Codex PLAN Prompt: ST-1102 — AsrClient HTTP Adapter

## Instructions

You are in PLAN mode. Your task is to explore the codebase and gather information needed to implement ST-1102.

**CRITICAL CONSTRAINTS:**
- NO file modifications (no Edit, no Write)
- NO code generation
- ONLY read-only operations allowed
- Output findings in structured format for APPLY phase

**Allowed commands:**
- `ls`, `find` — list files
- `cat`, `head`, `tail` — read files
- `rg`, `grep` — search patterns
- `git status`, `git diff` — inspect state

**Forbidden:**
- Any file edits/writes
- Network access
- Package installs
- git commit/push

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1102/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1102-asr-client-adapter.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- External ASR Contract: `docs/contracts/external/asr-service/asr/openapi.yaml`
- External ASR Errors: `docs/contracts/external/asr-service/asr/errors.md`
- DoD: `docs/_governance/dod.md`

---

## Story Summary

Implement HTTP client adapter for asr-service with:
- `AsrClient` interface (createTranscription, getTranscription)
- `AsrClientImpl` using Spring RestClient
- Exception hierarchy mapped from asr-service errors
- Retry logic (1 retry on 503/timeout)
- Timeout configuration (connect: 5s, read: 30s)
- X-Request-Id header propagation (correlationId)
- WireMock unit tests

---

## Exploration Tasks

### 1. Find existing HTTP client patterns

Search for `AiPlatformClient` as reference pattern:
```bash
find services/backend/src -name "*AiPlatformClient*" -o -name "*AiPlatform*Client*"
rg -l "AiPlatformClient" services/backend/src/main/java
cat services/backend/src/main/java/com/hometusk/ai/client/AiPlatformClient.java
cat services/backend/src/main/java/com/hometusk/ai/client/AiPlatformClientImpl.java
```

### 2. Find resilience configuration patterns

Search for RetryRegistry/Resilience4j usage:
```bash
rg -l "RetryRegistry" services/backend/src
rg -l "Resilience" services/backend/src
cat services/backend/src/main/java/com/hometusk/ai/client/AiPlatformResilienceConfig.java
```

### 3. Find existing exception patterns

Search for exception hierarchies:
```bash
find services/backend/src/main/java -path "*exception*" -name "*.java" | head -20
cat services/backend/src/main/java/com/hometusk/ai/exception/AiPlatformException.java
```

### 4. Check application.yml structure

Find where external service configs are defined:
```bash
cat services/backend/src/main/resources/application.yml | grep -A20 "ai-platform"
```

### 5. Find WireMock test patterns

Search for WireMock test examples:
```bash
rg -l "WireMock" services/backend/src/test
cat services/backend/src/test/java/com/hometusk/ai/client/AiPlatformClientImplTest.java
```

### 6. Examine external ASR contract

Read the asr-service contract and error codes:
```bash
cat docs/contracts/external/asr-service/asr/openapi.yaml
cat docs/contracts/external/asr-service/asr/errors.md
```

### 7. Check package structure

Verify where asr package should be created:
```bash
ls -la services/backend/src/main/java/com/hometusk/
```

### 8. Find existing DTO patterns

Check if records are used for DTOs:
```bash
rg "public record" services/backend/src/main/java --type java | head -10
```

### 9. Check RestClient usage

Find how RestClient is configured:
```bash
rg "RestClient" services/backend/src/main/java --type java -l
rg "RestClient.builder" services/backend/src/main/java --type java -C3
```

### 10. Find latest migration version

Check migration numbering:
```bash
ls services/backend/src/main/resources/db/migration/ | tail -5
```

---

## Expected Output Format

After exploration, provide findings in this format:

```
## PLAN Findings

### 1. Reference Patterns Found
- AiPlatformClient location: [path]
- AiPlatformClientImpl location: [path]
- Resilience config location: [path]
- Exception base class: [path]

### 2. Package Structure
- New asr package should be at: [path]
- Existing subpackages to mirror: [list]

### 3. Configuration Structure
- application.yml section example: [snippet]
- Property class pattern: [class name]

### 4. Test Patterns
- WireMock base class: [path]
- Test setup pattern: [description]

### 5. DTO Patterns
- Record usage: [yes/no]
- Example DTO: [path]

### 6. Latest Migration
- Latest version: V0XX
- Next version for asr: V0XX

### 7. External ASR Contract Summary
- POST endpoint: [path]
- GET endpoint: [path]
- Error codes: [list]

### 8. Risks/Blockers Found
- [Any issues discovered]
```

---

## Anti-Scope-Creep

DO NOT explore or report on:
- Controller patterns (ST-1103)
- Rate limiting (ST-1104)
- Metrics/observability (ST-1105)
- Security tests (ST-1106)
- Frontend/web code
- Unrelated services

Focus ONLY on: HTTP client implementation, resilience patterns, exception mapping, WireMock testing.
