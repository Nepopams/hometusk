# Story: ST-1204 — VoiceRecordingStates UI

## Status: Ready

## Description
Create UI components for voice recording states with timer and cancel button.

## In Scope
- `clients/web/src/components/commands/VoiceRecordingStatus.tsx`
- States: recording (timer + cancel), uploading, transcribing
- Timer in mm:ss format
- Accessible: aria-live for state changes

## Acceptance Criteria
- AC-1: Recording state shows timer and cancel button
- AC-2: Timer formats correctly (75s → 01:15)
- AC-3: Uploading state shows spinner
- AC-4: Transcribing state shows spinner
- AC-5: Cancel button calls handler
- AC-6: Screen reader announces state changes

## Points: 2

## Flags
- contract_impact: no
