# Codex PLAN: ST-1207 — Client Telemetry Events

## Objective
Explore the codebase to gather implementation details for adding client-side voice telemetry.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `ls`, `find`, `cat`, `rg`, `grep`, `head`, `tail`, `git status`, `git diff`
- Forbidden: any writes, edits, package installs

## Questions to Answer

### Q1: useAudioRecorder structure
- Read `clients/web/src/hooks/useAudioRecorder.ts`
- Where is recording started? (function name, line)
- Where is recording stopped/cancelled?
- What state/error types exist?

### Q2: useAsrTranscription structure
- Read `clients/web/src/hooks/useAsrTranscription.ts`
- Where is upload initiated? (function name, line)
- Where is transcription result received?
- What error types exist?
- Is there an existing correlationId?

### Q3: CommandInput integration points
- Read `clients/web/src/components/commands/CommandInput.tsx`
- Where does voice transcript get set?
- Where is command submitted?
- How to detect if transcript was edited before submit?

### Q4: Existing lib/ patterns
- Run `ls clients/web/src/lib/`
- Are there existing telemetry/logging modules?
- What patterns are used for utility modules?

### Q5: localStorage usage patterns
- Run `rg "localStorage" clients/web/src/`
- Any existing localStorage wrappers?
- Key naming conventions?

## Expected Output

```
## Findings

### useAudioRecorder
- Start recording: [function:line]
- Stop/cancel: [function:line]
- Error types: [list]

### useAsrTranscription
- Upload: [function:line]
- Result: [function:line]
- Error types: [list]
- Existing correlationId: [yes/no, where]

### CommandInput
- Transcript set: [line]
- Command submit: [function:line]
- Edit detection approach: [description]

### lib/ patterns
- Existing modules: [list]
- Pattern for new module: [description]

### localStorage
- Existing usage: [yes/no]
- Key pattern: [description]

### Recommended Implementation
[Brief notes on approach based on findings]
```
