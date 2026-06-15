# Voice Command Chat Implementation Map

**Initiative:** INIT-2026Q3-voice-command-chat-mvp
**Date:** 2026-06-14
**Status:** current for implementation

## Sources

- Initiative: `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Pencil source inspected through Pencil MCP: `hometusk.pen`
- Legacy exported design path present in repo: `docs/design/voice-comand-chat/`
- Required canonical handoff path for this initiative: `docs/design/voice-command-chat/`
- Active web route: `clients/web/src/routes/Commands.tsx`
- Reusable voice components: `clients/web/src/components/commands/Voice*.tsx`

The repo contains the design export folder as `voice-comand-chat` with a
historical typo. This map creates the canonical `voice-command-chat` markdown
handoff without moving or rewriting `.pen` files. `.pen` access remains through
Pencil MCP only.

## Frame To Runtime Mapping

| Design frame / artifact | Runtime target | Implementation notes |
|-------------------------|----------------|----------------------|
| Voice Command Chat MVP desktop/tablet/mobile frame group | `clients/web/src/routes/Commands.tsx` | Voice is embedded into the existing Commands page shell; no new navigation item. |
| `VoiceChat/CommandInputBar/*` | Commands textarea and composer actions | Keep the current command textarea as the editable transcript draft. |
| `VoiceChat/MicButton/*` and `Voice/MicButton-*` | `VoiceMicButton` | Reuse existing component and state classes: idle, recording, processing, disabled. |
| `VoiceChat/RecordingPanel/*` | `VoiceRecordingStatus` plus composer voice row | Recording/transcribing state stays near the input on desktop/tablet and fits the composer on mobile. |
| `VoiceChat/TranscriptionStatus/*` | `useAsrTranscription` state and `VoiceRecordingStatus` | Uploading/transcribing are distinct frontend states, both non-executing. |
| `VoiceChat/ErrorBanner/*` | `VoiceErrorMessage` | Controlled ASR errors preserve typed draft and offer retry or typed fallback. |
| `VoiceChat/ResultCard/*` | Existing Commands result cards | Results continue to be driven by `CommandResponse.status`, not free-form AI text. |
| `VoiceChat/ClarificationChips/*` | Existing `needs_input` card plus follow-up path | MVP renders clarification and suggestions where available; command continuation remains through existing API. |
| Command history rail | Existing local command history rail | Keep the current right rail on desktop and mobile sheet behavior. |

## State Mapping

| MVP state | UI state source | Backend/API boundary |
|-----------|-----------------|----------------------|
| `idle` | Empty composer, mic idle | No API call |
| `recording` | `useAudioRecorder.isRecording`, mic recording | Browser MediaRecorder only |
| `uploading` | audio blob captured, request in flight | `POST /api/v1/voice/transcriptions` |
| `transcribing` | ASR request in flight | AI Platform ASR behind HomeTusk BFF |
| `draft_ready` | transcript inserted into textarea | No command execution |
| `processing_command` | existing `useCommand.isLoading` | `POST /api/v1/commands` only after Send |
| `executed` | `CommandResponse.status=executed/executed_degraded` | Existing command pipeline |
| `needs_input` | `CommandResponse.status=needs_input` | Existing command continuation API |
| `rejected` | `CommandResponse.status=rejected` | Existing command rejection path |
| `asr_permission_error` | recorder error `permission_denied` | Browser permission only |
| `asr_timeout_or_rate_limit` | ASR error `timeout` or `rate_limited` | Controlled BFF error |
| `asr_unsupported_media` | ASR error `unsupported_media` | Controlled BFF error |

## Implementation Boundaries

- ASR BFF creates only an editable transcript draft.
- ASR BFF never calls `/api/v1/commands`.
- The browser never calls AI Platform directly.
- Raw audio is discarded after request handling and is not persisted.
- ASR logs and metrics must not include raw transcript text.
- Voice-originated command execution is represented by `source=voice` and optional `asrTraceId`.
