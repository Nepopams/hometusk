# Codex PLAN Prompt: ST-1104 — Guardrails (Validation + Rate Limiting)

## Instructions

You are in PLAN mode. Explore the codebase to gather information for implementing ST-1104.

**CRITICAL CONSTRAINTS:**
- NO file modifications
- ONLY read-only operations
- Output findings in structured format

**Allowed commands:** `ls`, `find`, `cat`, `head`, `tail`, `rg`, `grep`

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1104/workpack.md`
- Story: `docs/planning/epics/EP-011/stories/ST-1104-asr-guardrails.md`
- Epic: `docs/planning/epics/EP-011/epic.md`

---

## Story Summary

Implement validation, rate limiting, and idempotency:
- File size check (max 10MB)
- Format validation (audio/ogg, audio/mpeg, audio/wav, audio/mp4, audio/webm)
- Rate limiting: POST 5/min, GET 30/min per user per household
- Idempotency via AsrIdempotencyRecord with SHA-256 digest

---

## Exploration Tasks

### 1. Check if Bucket4j is already a dependency
```bash
grep -i "bucket4j" services/backend/build.gradle.kts
cat services/backend/build.gradle.kts | grep -A5 "dependencies"
```

### 2. Find existing validation patterns
```bash
rg "validation" services/backend/src/main/java --type java -l
rg "MultipartFile" services/backend/src/main/java --type java -C3
```

### 3. Find existing rate limiting patterns
```bash
rg -i "rate" services/backend/src/main/java --type java -l
rg -i "bucket" services/backend/src/main/java --type java -l
```

### 4. Find CommandIdempotency pattern
```bash
cat services/backend/src/main/java/com/hometusk/commands/idempotency/CommandIdempotencyService.java
find services/backend/src -name "*Idempotency*" -type f
```

### 5. Check latest migration version
```bash
ls services/backend/src/main/resources/db/migration/ | tail -5
```

### 6. Review current AsrController
```bash
cat services/backend/src/main/java/com/hometusk/asr/controller/AsrController.java
```

### 7. Review current AsrService
```bash
cat services/backend/src/main/java/com/hometusk/asr/service/AsrService.java
```

### 8. Review AsrProperties
```bash
cat services/backend/src/main/java/com/hometusk/asr/client/AsrProperties.java
```

### 9. Check existing exception hierarchy
```bash
ls services/backend/src/main/java/com/hometusk/asr/exception/
```

### 10. Review GlobalExceptionHandler
```bash
cat services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java | head -100
```

### 11. Check application.yml for asr config
```bash
grep -A20 "^asr:" services/backend/src/main/resources/application.yml
```

---

## Expected Output Format

```
## PLAN Findings

### 1. Bucket4j Dependency
- Already present: [yes/no]
- Version: [if present]

### 2. Existing Patterns
- Validation pattern: [description]
- Rate limiting pattern: [found/not found]
- Idempotency pattern: [path]

### 3. Migration Version
- Latest: V0XX
- Next: V024

### 4. Current ASR Code Review
- AsrController handles: [list]
- AsrService handles: [list]
- AsrProperties fields: [list]

### 5. Exceptions Exist
- [list existing asr exceptions]

### 6. GlobalExceptionHandler
- Existing ASR handlers: [yes/no]
- Pattern for adding: [description]

### 7. Risks/Blockers
- [list any issues]
```

---

## Anti-Scope-Creep

DO NOT explore:
- Metrics/observability (ST-1105)
- Security tests (ST-1106)
- Web/frontend code
