# Codex PLAN: ST-1208 — Cross-Browser Polish + Accessibility

## Objective
Explore current voice components to assess accessibility and cross-browser readiness.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `ls`, `find`, `cat`, `rg`, `grep`, `head`, `tail`
- Forbidden: any writes, edits

## Questions to Answer

### Q1: VoiceMicButton accessibility
- Read `clients/web/src/components/commands/VoiceMicButton.tsx`
- Does it have `aria-label`?
- Does it have `aria-pressed` or similar toggle indicator?
- Is it a `<button>` element (inherently keyboard accessible)?

### Q2: VoiceRecordingStatus accessibility
- Read `clients/web/src/components/commands/VoiceRecordingStatus.tsx`
- Does it have `aria-live` region?
- Is the state ("Recording", "Uploading", "Transcribing") announced?

### Q3: VoiceErrorMessage accessibility
- Read `clients/web/src/components/commands/VoiceErrorMessage.tsx`
- Does it have `role="alert"`?
- Is `aria-live` set?

### Q4: MediaRecorder support check
- Read `clients/web/src/hooks/useAudioRecorder.ts`
- How is MediaRecorder support detected?
- What MIME types are checked?
- Is there a Safari-specific fallback?

### Q5: Focus management
- Read `clients/web/src/components/commands/CommandInput.tsx`
- When transcript arrives, is focus moved to input?
- Where does this happen (line numbers)?

### Q6: Keyboard handlers
- Search for `onKeyDown` in voice components
- Is Escape handled to cancel recording?

### Q7: CSS focus styles
- Read `clients/web/src/components/commands/VoiceMicButton.css`
- Are there `:focus` or `:focus-visible` styles?

## Expected Output

```
## Findings

### VoiceMicButton
- Element type: [button/div/other]
- aria-label: [yes/no, value]
- aria-pressed: [yes/no]
- Keyboard accessible: [yes/no]

### VoiceRecordingStatus
- aria-live: [yes/no, value]
- State announced: [yes/no]

### VoiceErrorMessage
- role: [alert/other/none]
- aria-live: [yes/no, value]

### MediaRecorder support
- Detection: [description]
- MIME types: [list]
- Safari handling: [yes/no, how]

### Focus management
- Transcript focus: [yes/no, line]

### Keyboard handlers
- Escape handling: [yes/no]
- onKeyDown locations: [list]

### CSS focus styles
- :focus-visible: [yes/no]

### Gaps to fix
- [List of missing accessibility features]
```
