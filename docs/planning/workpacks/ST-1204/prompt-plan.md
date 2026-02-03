# Codex PLAN Prompt: ST-1204 — VoiceRecordingStates UI

## Mission
You are in **PLAN mode** (read-only exploration). Understand existing patterns for creating VoiceRecordingStatus component.

**DO NOT** edit, create, or modify any files.

---

## Context
Creating a status display component for voice recording states.

### References
- Story: `docs/planning/epics/EP-012/stories/ST-1204-recording-states-ui.md`
- Workpack: `docs/planning/workpacks/ST-1204/workpack.md`

---

## Exploration Tasks

### 1. Commands Folder Structure
```bash
ls -la clients/web/src/components/commands/
cat clients/web/src/components/commands/index.ts
```

### 2. Existing Status Components
```bash
# Check for similar status/state display patterns
find clients/web/src/components -name "*Status*" -o -name "*State*" | head -10
cat clients/web/src/components/commands/StatusBadge.tsx 2>/dev/null | head -50
```

### 3. CSS Tokens
```bash
# Verify available CSS tokens
cat clients/web/src/styles/tokens.css | head -60
```

### 4. sr-only Pattern
```bash
# Check if sr-only class exists
grep -r "sr-only\|visually-hidden" clients/web/src --include="*.css" | head -5
```

---

## Expected Findings

1. **Commands folder**: Confirmed structure
2. **Status patterns**: Any existing status components
3. **CSS tokens**: Available spacing/color tokens
4. **sr-only**: Whether class exists or needs creation

---

## Output Format

```
## PLAN Findings: ST-1204

### 1. Commands Folder
- [structure confirmed]

### 2. Status Patterns
- [existing patterns if any]

### 3. CSS Tokens
- [available tokens]

### 4. sr-only Class
- [exists/needs creation]

### 5. Ready for APPLY
- [Yes/No]
```
