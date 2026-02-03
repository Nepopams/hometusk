# Story: ST-1205 — Integration with CommandInput

## Status: Ready

## Description
Integrate voice recording into existing CommandInput component.
Transcript populates text field for editing before submit.

## In Scope
- Modify `clients/web/src/components/commands/CommandInput.tsx`
- Add VoiceMicButton next to input
- Wire useAudioRecorder and useAsrTranscription
- Transcript populates title field
- State management: voice flow vs text flow

## Dependencies
- ST-1201, ST-1202, ST-1203, ST-1204

## Acceptance Criteria
- AC-1: Mic button visible in CommandInput
- AC-2: Click mic starts recording
- AC-3: Click again stops recording
- AC-4: Transcript populates input field (editable)
- AC-5: Submit command works normally
- AC-6: Cancel discards audio and returns to idle
- AC-7: Mic disabled during command execution

## Points: 5

## Flags
- diagrams_needed: lite (state diagram)
