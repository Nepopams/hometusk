# Story: ST-1207 — Client Telemetry Events

## Status: Ready

## Description
Client-side telemetry for voice input flow (console + localStorage in MVP).

## In Scope
- `clients/web/src/lib/voiceTelemetry.ts`
- Events: voice_start, voice_cancel, voice_upload_ok/fail, asr_ok/fail, voice_transcript_edited, voice_command_submitted
- Storage: console.log + localStorage
- No PII in events

## Acceptance Criteria
- AC-1: voice_start logged when recording starts
- AC-2: voice_cancel logged when cancelled
- AC-3: upload events logged with correlationId
- AC-4: ASR events logged with duration
- AC-5: voice_transcript_edited logged if modified
- AC-6: voice_command_submitted logged on submit
- AC-7: Events stored in localStorage (max 100)
- AC-8: No PII in events

## Points: 2

## Flags
- security_sensitive: yes (ensure no PII)
