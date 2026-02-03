# Codex PLAN Prompt: ST-1205 — CommandInput Voice Integration

## Mission
You are in **PLAN mode** (read-only exploration). Understand CommandInput structure for voice integration.

**DO NOT** edit, create, or modify any files.

---

## Context
Integrating voice input into existing CommandInput component using:
- VoiceMicButton (ST-1201)
- useAudioRecorder (ST-1202)
- useAsrTranscription (ST-1203)
- VoiceRecordingStatus (ST-1204)

### References
- Story: `docs/planning/epics/EP-012/stories/ST-1205-command-input-integration.md`
- Workpack: `docs/planning/workpacks/ST-1205/workpack.md`

---

## Exploration Tasks

### 1. CommandInput Structure
```bash
# Read current CommandInput implementation
cat clients/web/src/components/commands/CommandInput.tsx

# Check CSS
cat clients/web/src/components/commands/CommandInput.css 2>/dev/null | head -50
```

### 2. CommandInput Props and State
```bash
# Check what props CommandInput receives
grep -A 20 "interface.*Props\|type.*Props" clients/web/src/components/commands/CommandInput.tsx

# Check existing state
grep "useState\|useReducer" clients/web/src/components/commands/CommandInput.tsx
```

### 3. Form/Input Structure
```bash
# Check how input is structured
grep -E "input|textarea|form|onSubmit|onChange" clients/web/src/components/commands/CommandInput.tsx | head -20
```

### 4. Current Loading/Disabled States
```bash
# Check if there's loading/submitting state
grep -E "isLoading|isSubmitting|disabled|loading" clients/web/src/components/commands/CommandInput.tsx
```

### 5. useAuth for householdId
```bash
# Verify useAuth provides householdId
grep -A 5 "useAuth" clients/web/src/hooks/useAuth.ts | head -15
```

### 6. Existing Imports
```bash
# Check current imports in CommandInput
head -30 clients/web/src/components/commands/CommandInput.tsx
```

---

## Expected Findings

1. **CommandInput structure**:
   - Props interface
   - Current state management
   - Form/input element structure
   - Submit handler

2. **Integration points**:
   - Where to add mic button
   - How to populate input value
   - Existing loading/disabled logic

3. **householdId access**:
   - How to get householdId for ASR upload

---

## Output Format

```
## PLAN Findings: ST-1205

### 1. CommandInput Structure
- Props: [describe]
- State: [describe]
- Input: [controlled/uncontrolled, field name]

### 2. Integration Points
- Mic button location: [describe]
- Input value control: [describe]
- Loading state: [describe]

### 3. householdId
- Source: [describe]

### 4. Blockers
- [list or "none"]

### 5. Ready for APPLY
- [Yes/No]
```
