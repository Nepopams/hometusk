# Codex PLAN Prompt: ST-1103 — ASR Proxy Endpoints (Controller)

## Instructions

You are in PLAN mode. Your task is to explore the codebase and gather information needed to implement ST-1103.

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

- Workpack: `docs/planning/workpacks/ST-1103/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1103-asr-proxy-endpoints.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- HomeTusk Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Story Summary

Implement REST controller for ASR proxy with:
- `AsrController` with POST/GET endpoints
- JWT authentication (existing Spring Security)
- Household membership check (existing MembershipService)
- `AsrTranscriptionRef` entity + repository (IDOR prevention)
- Correlation ID handling (generate if missing)
- Exception to HTTP response mapping
- `pollAfterMs` in GET response for queued/processing
- Integration tests with WireMock

---

## Exploration Tasks

### 1. Find existing controller patterns

Search for controller examples:
```bash
find services/backend/src/main/java -name "*Controller.java" | head -10
cat services/backend/src/main/java/com/hometusk/tasks/controller/TaskController.java
```

### 2. Find MembershipService for authorization

```bash
rg -l "MembershipService" services/backend/src/main/java
cat services/backend/src/main/java/com/hometusk/household/service/MembershipService.java
```

### 3. Find GlobalExceptionHandler for error mapping

```bash
find services/backend/src -name "*ExceptionHandler*" -o -name "*ControllerAdvice*"
cat services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java
```

### 4. Find correlation ID handling patterns

```bash
rg "correlationId" services/backend/src/main/java --type java -l
rg "X-Correlation-ID" services/backend/src/main/java --type java -C3
```

### 5. Find existing entity patterns

```bash
find services/backend/src/main/java -name "*.java" -path "*domain*" | head -10
cat services/backend/src/main/java/com/hometusk/tasks/domain/Task.java | head -50
```

### 6. Find existing repository patterns

```bash
find services/backend/src/main/java -name "*Repository.java" | head -10
cat services/backend/src/main/java/com/hometusk/tasks/repository/TaskRepository.java
```

### 7. Find latest migration version

```bash
ls services/backend/src/main/resources/db/migration/ | tail -5
```

### 8. Find integration test patterns

```bash
find services/backend/src/test -name "*IntegrationTest.java" -o -name "*ControllerTest.java" | head -10
cat services/backend/src/test/java/com/hometusk/integration/tasks/TaskControllerIntegrationTest.java | head -100
```

### 9. Find WireMock integration test base

```bash
rg -l "WireMockServer" services/backend/src/test
cat services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java
```

### 10. Check AsrClient interface (just created in ST-1102)

```bash
cat services/backend/src/main/java/com/hometusk/asr/client/AsrClient.java
cat services/backend/src/main/java/com/hometusk/asr/dto/AsrJobCreated.java
cat services/backend/src/main/java/com/hometusk/asr/dto/AsrJobResult.java
```

### 11. Find multipart file handling patterns

```bash
rg "MultipartFile" services/backend/src/main/java --type java -l
rg "@RequestPart" services/backend/src/main/java --type java -C3
```

### 12. Check proxy contract for response shapes

```bash
cat docs/contracts/http/asr-proxy.openapi.yaml | head -100
```

### 13. Find ErrorCode enum or error response patterns

```bash
rg "enum.*Error" services/backend/src/main/java --type java -l
cat services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java
```

### 14. Check authentication/principal extraction

```bash
rg "@AuthenticationPrincipal" services/backend/src/main/java --type java -C3
rg "getCurrentUser" services/backend/src/main/java --type java -l
```

---

## Expected Output Format

After exploration, provide findings in this format:

```
## PLAN Findings

### 1. Controller Patterns Found
- Example controller: [path]
- Endpoint annotation style: [description]
- Response entity pattern: [description]

### 2. Authorization Patterns
- MembershipService location: [path]
- Method to check membership: [signature]
- How to get current user: [pattern]

### 3. Exception Handling
- GlobalExceptionHandler location: [path]
- Existing ASR exception handlers: [yes/no, if yes list them]
- Error response format: [class name]

### 4. Correlation ID Handling
- Filter/interceptor location: [path or "not found"]
- How to access correlationId: [pattern]
- How to generate if missing: [pattern]

### 5. Entity Patterns
- Base entity class: [path or "none"]
- JPA annotations used: [list]
- UUID generation: [strategy]

### 6. Repository Patterns
- Base repository interface: [path]
- Custom query examples: [description]

### 7. Latest Migration
- Latest version: V0XX
- Next version for asr: V023

### 8. Integration Test Patterns
- Test base class: [path]
- Auth setup: [description]
- WireMock setup: [description]

### 9. AsrClient Interface
- createTranscription signature: [signature]
- getTranscription signature: [signature]

### 10. Multipart Handling
- Example endpoint: [path]
- Annotation used: [description]

### 11. Risks/Blockers Found
- [Any issues discovered]
```

---

## Anti-Scope-Creep

DO NOT explore or report on:
- Rate limiting implementation (ST-1104)
- Input validation (size/format) (ST-1104)
- Metrics/observability (ST-1105)
- Security edge case tests (ST-1106)
- Idempotency record implementation (ST-1104)
- Frontend/web code

Focus ONLY on: Controller, entity, repository, service patterns for ASR proxy endpoints.
