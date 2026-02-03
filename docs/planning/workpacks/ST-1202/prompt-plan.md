# Codex PLAN Prompt: ST-1202 — Audio Recording Hook

## Mission
You are in **PLAN mode** (read-only exploration). Understand the codebase structure for creating `useAudioRecorder` hook.

**DO NOT** edit, create, or modify any files. Only read and analyze.

---

## Context
Creating a `useAudioRecorder` React hook for browser audio recording using MediaRecorder API.

### Story Reference
- Story: `docs/planning/epics/EP-012/stories/ST-1202-audio-recording.md`
- Workpack: `docs/planning/workpacks/ST-1202/workpack.md`

---

## Exploration Tasks

### 1. Hooks Structure
```bash
# Check if hooks folder exists
ls -la clients/web/src/hooks/ 2>/dev/null || echo "hooks folder does not exist"

# List existing hooks
find clients/web/src -name "use*.ts" -o -name "use*.tsx" | head -20

# Check for barrel export
cat clients/web/src/hooks/index.ts 2>/dev/null || echo "No hooks index.ts"
```

### 2. Existing Hook Patterns
```bash
# Find any existing custom hook
find clients/web/src -name "use*.ts" -type f | head -5 | xargs cat 2>/dev/null | head -80

# Or check for hooks in components
grep -r "function use" clients/web/src --include="*.ts" --include="*.tsx" | head -10
```

### 3. TypeScript Patterns
```bash
# Check how types are exported
grep -r "export type" clients/web/src --include="*.ts" | head -10

# Check interface patterns
grep -r "export interface" clients/web/src --include="*.ts" | head -10
```

### 4. Import Patterns
```bash
# Check React imports pattern
grep "from 'react'" clients/web/src/**/*.ts* | head -5
```

---

## Expected Findings to Report

1. **File locations**:
   - Does `clients/web/src/hooks/` exist?
   - Is there a barrel export `hooks/index.ts`?

2. **Patterns to follow**:
   - Hook naming convention
   - Type export pattern
   - React import style

3. **Blockers or clarifications**:
   - Any existing audio/media hooks?
   - Conflicts?

---

## Constraints (PLAN mode)

**ALLOWED commands:** `ls`, `find`, `cat`, `head`, `tail`, `grep`, `rg`

**FORBIDDEN:** Any file edits/writes, npm commands

---

## Output Format

```
## PLAN Findings: ST-1202

### 1. File Locations
- Hooks folder: [exists/needs creation]
- Barrel export: [yes/no]

### 2. Patterns Observed
- Hook naming: [describe]
- Type exports: [describe]

### 3. Blockers
- [list or "none"]

### 4. Ready for APPLY
- [Yes/No]
```
