# Story: ST-1202 — Audio Recording with MediaRecorder

## Status: Ready

## Description
Create `useAudioRecorder` hook for browser audio recording using MediaRecorder API.
Outputs WebM/Opus format compatible with ASR proxy.

## In Scope
- `clients/web/src/hooks/useAudioRecorder.ts`
- Start/stop recording methods
- Auto-stop at 60 seconds max duration
- Timer callback for UI updates
- Error handling for getUserMedia failures

## Out of Scope
- ASR upload (ST-1203)
- UI components (ST-1201, ST-1204)

## Acceptance Criteria
- AC-1: Hook returns { start, stop, isRecording, duration, audioBlob, error }
- AC-2: Start requests microphone permission
- AC-3: Recording produces WebM blob
- AC-4: Duration updates during recording
- AC-5: Auto-stop at 60 seconds
- AC-6: Error on permission denied
- AC-7: Stop cleans up resources

## Test Strategy
- Unit tests with mocked MediaRecorder
- Manual testing in Chrome, Firefox, Safari

## Points: 3

## Flags
- contract_impact: no
- security_sensitive: yes (microphone access)
