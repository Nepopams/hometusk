# DoD Checklist: ST-1202 — Audio Recording with MediaRecorder

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1202/workpack.md`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

- [ ] AC-1: Hook returns { start, stop, isRecording, duration, audioBlob, error }
- [ ] AC-2: Start requests microphone permission
- [ ] AC-3: Recording produces WebM blob
- [ ] AC-4: Duration updates during recording
- [ ] AC-5: Auto-stop at 60 seconds
- [ ] AC-6: Error on permission denied
- [ ] AC-7: Stop cleans up resources

---

## Code Quality

- [ ] Code follows project conventions (React hooks, TypeScript)
- [ ] ESLint/Prettier formatting applied
- [ ] No TypeScript errors (`npx tsc --noEmit`)
- [ ] No compiler warnings introduced
- [ ] Proper cleanup in useEffect

---

## Build Verification

- [ ] Web client builds successfully: `cd clients/web && npm run build`
- [ ] Hook exported from hooks/index.ts (if exists)

---

## Resource Management

- [ ] MediaStream tracks stopped on cleanup
- [ ] Timers cleared on cleanup
- [ ] No memory leaks on unmount

---

## Error Handling

- [ ] 'permission_denied' on NotAllowedError
- [ ] 'not_supported' if MediaRecorder unavailable
- [ ] 'recording_failed' on generic errors
- [ ] 'no_audio_data' if no chunks collected

---

## Browser Compatibility

- [ ] Works in Chrome (webm/opus)
- [ ] Works in Firefox (webm)
- [ ] Works in Safari (with fallback)

---

## Final Verification

- [ ] All checklist items complete
- [ ] Ready for integration with ASR hook (ST-1203)
