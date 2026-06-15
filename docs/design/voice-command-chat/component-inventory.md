# Voice Command Chat Component Inventory

**Initiative:** INIT-2026Q3-voice-command-chat-mvp
**Date:** 2026-06-14

## Reused Components

| Component | File | Role |
|-----------|------|------|
| Commands page | `clients/web/src/routes/Commands.tsx` | Active command composer, result cards, history rail. |
| Voice mic button | `clients/web/src/components/commands/VoiceMicButton.tsx` | Mic entry point with idle/recording/processing/disabled states. |
| Recording status | `clients/web/src/components/commands/VoiceRecordingStatus.tsx` | Recording timer, uploading, transcribing, cancel action. |
| Voice error message | `clients/web/src/components/commands/VoiceErrorMessage.tsx` | Controlled permission, network, timeout, rate-limit, and ASR errors. |
| Audio recorder hook | `clients/web/src/hooks/useAudioRecorder.ts` | Browser MediaRecorder lifecycle and duration. |
| ASR hook | `clients/web/src/hooks/useAsrTranscription.ts` | Uploads audio to HomeTusk voice BFF and returns transcript metadata. |
| Command hook | `clients/web/src/hooks/useCommand.ts` | Existing command execution and idempotency behavior. |
| Button | `clients/web/src/components/ui/Button.tsx` | Existing primary/secondary actions. |

## New Or Updated Runtime Pieces

| Piece | Target | Reason |
|-------|--------|--------|
| Voice ASR BFF controller | `services/backend/src/main/java/com/hometusk/voice/api` | New sync contract for editable transcript drafts. |
| AI Platform ASR client | `services/backend/src/main/java/com/hometusk/voice/client` | Isolate upstream `/v1/asr/transcribe` integration. |
| Voice config | `services/backend/src/main/java/com/hometusk/voice/config` | `voice.enabled`, `voice.asr.enabled`, base URL, timeouts, limits. |
| Voice metrics | `services/backend/src/main/java/com/hometusk/voice/metrics` | MVP observability without raw audio/transcript data. |
| Command trace metadata | `commands.asr_trace_id`, `CommandRequest.asrTraceId` | Auditable linkage from ASR draft to command lifecycle. |
| Web composer voice state | `clients/web/src/routes/Commands.tsx` | Active page needs the mic and draft insertion flow. |

## Deliberately Not Added

- No separate chat service.
- No new top-level navigation item.
- No direct browser AI Platform client.
- No persistent voice transcript store.
- No new icon library; the current codebase uses inline SVG and existing UI primitives.
