# Voice Command Chat

**Type**: Sequence
**Last Updated**: 2026-06-14
**Status**: current

## Purpose

Explain how a voice recording becomes an editable transcript draft and then a
normal HomeTusk command only after explicit user Send.

## Diagram

```mermaid
sequenceDiagram
    autonumber
    participant User as User
    participant Web as HomeTusk web Commands
    participant Voice as Voice ASR BFF
    participant AIASR as AI Platform ASR
    participant Commands as Commands API
    participant Pipeline as Command pipeline
    participant DB as PostgreSQL

    User->>Web: Tap mic
    Web->>Web: Request browser microphone permission
    Web->>Web: Record audio with MediaRecorder
    User->>Web: Stop recording
    Web->>Voice: POST /api/v1/voice/transcriptions multipart file
    Voice->>Voice: Authenticate, validate file, rate-limit
    Voice->>AIASR: POST /v1/asr/transcribe multipart file
    AIASR-->>Voice: Transcript + safe trace metadata
    Voice-->>Web: transcript draft, status=ok, traceId, latencyMs
    Web->>Web: Insert editable transcript into command input
    User->>Web: Edit and click Send
    Web->>Commands: POST /api/v1/commands source=voice asrTraceId
    Commands->>Pipeline: Validate, decide, guardrail, execute
    Pipeline->>DB: Store Command, DecisionLog, domain changes
    Pipeline-->>Commands: CommandResponse
    Commands-->>Web: executed / needs_input / rejected / degraded
    Web-->>User: Structured result card
```

## Notes

- ASR BFF never calls the Commands API.
- Browser never calls AI Platform directly.
- Raw audio is not persisted.
- Raw transcript is not logged by the ASR flow.
- `asrTraceId` is safe metadata for linking the draft to a command audit trail.
