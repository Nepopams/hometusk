# Codex PLAN Prompt: ST-1203 — ASR Upload + Polling Hook

## Mission
You are in **PLAN mode** (read-only exploration). Understand the codebase for creating `useAsrTranscription` hook.

**DO NOT** edit, create, or modify any files.

---

## Context
Creating a React hook for ASR transcription: upload audio → poll for result.

### References
- Story: `docs/planning/epics/EP-012/stories/ST-1203-asr-upload-polling.md`
- Workpack: `docs/planning/workpacks/ST-1203/workpack.md`
- Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## Exploration Tasks

### 1. Existing Hooks Structure
```bash
ls -la clients/web/src/hooks/
cat clients/web/src/hooks/index.ts
```

### 2. Auth Token Access
```bash
# How is auth token stored/accessed?
grep -r "auth_token\|localStorage\|getToken" clients/web/src --include="*.ts" | head -10

# Check if there's an auth context
find clients/web/src -name "*auth*" -o -name "*Auth*" | head -10
```

### 3. API Call Patterns
```bash
# How are API calls made? fetch? axios?
grep -r "fetch\|axios" clients/web/src --include="*.ts" --include="*.tsx" | head -10

# Check for API base URL config
grep -r "api/v1\|baseUrl\|API_URL" clients/web/src --include="*.ts" | head -10
```

### 4. Household Context
```bash
# How is householdId accessed?
grep -r "householdId\|household" clients/web/src --include="*.ts" --include="*.tsx" | head -10
```

### 5. Existing useAsrTranscription (if any)
```bash
grep -r "useAsr\|transcription" clients/web/src --include="*.ts" | head -5
```

---

## Expected Findings

1. **Auth pattern**: How to get JWT token
2. **API pattern**: fetch vs axios, base URL
3. **Household access**: How to get current householdId
4. **Existing patterns**: Any similar hooks for reference

---

## Output Format

```
## PLAN Findings: ST-1203

### 1. Auth Pattern
- Token storage: [describe]
- Access method: [describe]

### 2. API Pattern
- HTTP client: [fetch/axios]
- Base URL: [hardcoded/env]

### 3. Household Access
- [describe how householdId is accessed]

### 4. Blockers
- [list or "none"]

### 5. Ready for APPLY
- [Yes/No]
```
