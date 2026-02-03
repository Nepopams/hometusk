# Workpack: ST-1205 — Integration with CommandInput

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/epics/EP-012/epic.md`
- Story spec: `docs/planning/epics/EP-012/stories/ST-1205-command-input-integration.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status: Ready

## Outcome
Voice input integrated into CommandInput component: mic button, recording flow, transcript populates input field.

## Acceptance Criteria (from story)
- AC-1: Mic button visible in CommandInput
- AC-2: Click mic starts recording
- AC-3: Click again stops recording
- AC-4: Transcript populates input field (editable)
- AC-5: Submit command works normally
- AC-6: Cancel discards audio and returns to idle
- AC-7: Mic disabled during command execution

---

## Dependencies
- ST-1201: VoiceMicButton component ✅
- ST-1202: useAudioRecorder hook ✅
- ST-1203: useAsrTranscription hook ✅
- ST-1204: VoiceRecordingStatus component ✅

---

## Files to Change

### Modified Files
| Path | Change |
|------|--------|
| `clients/web/src/components/commands/CommandInput.tsx` | Add voice input integration |
| `clients/web/src/components/commands/CommandInput.css` | Styles for voice UI layout |

---

## Voice Input State Machine

```
idle ──[click mic]──> recording
recording ──[click mic]──> uploading
recording ──[cancel]──> idle
uploading ──[success]──> transcribing
uploading ──[error]──> idle (with error)
transcribing ──[success]──> ready (transcript in input)
transcribing ──[error]──> idle (with error)
ready ──[submit]──> (normal command flow)
ready ──[edit]──> (user edits transcript)
```

---

## Implementation Plan

### Key Integration Points

1. **Add voice state management**:
   - `voiceMode`: 'idle' | 'recording' | 'uploading' | 'transcribing'
   - Wire useAudioRecorder + useAsrTranscription

2. **Add VoiceMicButton** next to input:
   - State derived from voiceMode + audioRecorder.isRecording
   - Click toggles recording

3. **Show VoiceRecordingStatus** when recording/uploading/transcribing:
   - Replaces or overlays input during voice flow

4. **Populate transcript**:
   - When transcription completes, set input value
   - User can edit before submit

5. **Disable mic during command execution**:
   - Check existing isLoading/isSubmitting state

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npx tsc --noEmit
```

---

## Manual Testing

1. Open CommandInput
2. Click mic → should start recording (button turns red, pulsing)
3. Click mic again → should stop, show "Uploading..."
4. Wait → should show "Transcribing..."
5. Wait → transcript appears in input
6. Edit transcript if needed
7. Submit → command executes normally
8. Test cancel → returns to idle

---

## DoD Checklist Reference
See `checklist.md` for full DoD verification.

---

## Risks

| Risk | Mitigation |
|------|------------|
| Complex state management | Clear state machine, derived states |
| Race conditions | AbortController in hooks |
| UI flicker | Proper loading states |

---

## Rollback
Revert changes to CommandInput.tsx/css. Voice components remain (additive).
