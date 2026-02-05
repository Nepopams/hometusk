# Workpack: ST-1207 — Client Telemetry Events

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-012/epic.md`
- Story: `docs/planning/epics/EP-012/stories/ST-1207-client-telemetry.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready**

---

## Goal
Add client-side telemetry for voice input flow to track usage patterns and debug issues. Events stored in console + localStorage (MVP), no PII.

---

## Scope

### In Scope
- New `voiceTelemetry.ts` module with event logging
- Events: voice_start, voice_cancel, voice_upload_ok/fail, asr_ok/fail, voice_transcript_edited, voice_command_submitted
- localStorage ring buffer (max 100 events)
- Integration points in useAudioRecorder, useAsrTranscription, CommandInput
- correlationId linking related events
- Duration tracking for ASR

### Out of Scope
- Server-side telemetry
- Analytics dashboard
- Remote event collection
- PII data (usernames, transcript content)

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/voiceTelemetry.ts` | CREATE | Telemetry module with logVoiceEvent + storage |
| `clients/web/src/hooks/useAudioRecorder.ts` | MODIFY | Add voice_start/voice_cancel events |
| `clients/web/src/hooks/useAsrTranscription.ts` | MODIFY | Add upload/asr events with correlationId |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY | Add transcript_edited/command_submitted events |

---

## Implementation Plan

### Step 1: Create voiceTelemetry module

**File:** `clients/web/src/lib/voiceTelemetry.ts`

Types:
```typescript
export type VoiceEventType =
  | 'voice_start'
  | 'voice_cancel'
  | 'voice_upload_ok'
  | 'voice_upload_fail'
  | 'voice_asr_ok'
  | 'voice_asr_fail'
  | 'voice_transcript_edited'
  | 'voice_command_submitted';

export interface VoiceEvent {
  type: VoiceEventType;
  timestamp: number;
  correlationId?: string;
  durationMs?: number;
  errorType?: string;
  metadata?: Record<string, string | number | boolean>;
}
```

Functions:
- `logVoiceEvent(event: Omit<VoiceEvent, 'timestamp'>): void` — log to console + localStorage
- `getVoiceEvents(): VoiceEvent[]` — retrieve from localStorage
- `clearVoiceEvents(): void` — clear localStorage

Storage:
- Key: `hometusk_voice_telemetry`
- Max 100 events (FIFO ring buffer)
- JSON serialized array

### Step 2: Integrate with useAudioRecorder

Add events:
- `voice_start` — when recording starts successfully
- `voice_cancel` — when user cancels recording

Generate correlationId on start, pass to downstream hooks.

### Step 3: Integrate with useAsrTranscription

Accept correlationId parameter.

Add events:
- `voice_upload_ok` — upload succeeded, include correlationId
- `voice_upload_fail` — upload failed, include errorType
- `voice_asr_ok` — transcription completed, include durationMs
- `voice_asr_fail` — transcription failed, include errorType

### Step 4: Integrate with CommandInput

Track:
- `voice_transcript_edited` — if user modifies transcript before submit
- `voice_command_submitted` — when command submitted after voice input

Logic:
- Compare original transcript vs submitted text
- If different and non-empty, log edited event

### Step 5: Ensure no PII

Review all logged data:
- NO transcript text
- NO user identifiers
- NO household IDs (unless hashed)
- OK: correlationId, timestamps, durations, error types, booleans

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Type check
npm run build

# Lint
npm run lint

# Manual testing
npm run dev
# 1. Record voice → check console for voice_start
# 2. Cancel → check console for voice_cancel
# 3. Complete flow → check localStorage for events
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | voice_start logged when recording starts | Console + manual test |
| AC-2 | voice_cancel logged when cancelled | Console + manual test |
| AC-3 | upload events logged with correlationId | Console check |
| AC-4 | ASR events logged with duration | Console check |
| AC-5 | voice_transcript_edited logged if modified | Manual test |
| AC-6 | voice_command_submitted logged on submit | Manual test |
| AC-7 | Events stored in localStorage (max 100) | DevTools check |
| AC-8 | No PII in events | Code review |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| localStorage quota | Low | 100 event limit, small payloads |
| PII leak | High | Code review, no transcript/user data |
| Performance impact | Low | Async writes, minimal data |

---

## Rollback

- Remove telemetry calls from hooks/components
- Delete voiceTelemetry.ts
- No backend changes

---

## References

- Voice flow entry: `CommandInput.tsx`
- Recording hook: `useAudioRecorder.ts`
- ASR hook: `useAsrTranscription.ts`
