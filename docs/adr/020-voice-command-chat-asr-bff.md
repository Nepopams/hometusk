# ADR-020: Voice Command Chat ASR BFF

**Status:** Accepted
**Date:** 2026-06-14
**Initiative:** INIT-2026Q3-voice-command-chat-mvp

## Context

HomeTusk is command-driven: users submit intent, the backend validates and
executes domain actions, and every command remains traceable. Voice Command
Chat adds a microphone path to the existing Commands page, but the voice step
must not become a hidden execution path.

The repo already has an older household-scoped async ASR proxy for foundation
work. The MVP needs a simpler synchronous BFF that creates an editable draft
inside the active Commands UI while keeping the existing async proxy compatible.

Non-negotiable constraints:

- AI Platform is external and HomeTusk does not implement local LLM/ASR models.
- ASR output is a suggestion, not source of truth.
- Command execution happens only through `/api/v1/commands`.
- Raw audio and raw ASR transcript must not be logged or persisted by the ASR flow.

## Decision

HomeTusk will add a synchronous Voice ASR BFF endpoint:

```http
POST /api/v1/voice/transcriptions
```

The endpoint:

- requires authentication;
- validates exactly one multipart `file`;
- enforces the AI Platform ASR media allowlist and max file size;
- applies a local per-user rate limit;
- calls AI Platform ASR `POST /v1/asr/transcribe`;
- returns `{ transcript, status, traceId, latencyMs }`;
- maps controlled errors to stable HomeTusk error codes;
- never calls `/api/v1/commands`;
- stores no raw audio and logs no raw transcript.

Voice-originated command execution remains a normal command submission after
explicit user Send. The Commands API accepts additive metadata:

```json
{
  "source": "voice",
  "asrTraceId": "trace-asr-..."
}
```

`asrTraceId` is persisted on `commands` for audit linkage and may be included in
DecisionLog context snapshots as safe metadata.

Feature flags:

- backend: `voice.enabled`, `voice.asr.enabled`;
- frontend: `VITE_VOICE_COMMAND_ENABLED`.

## Consequences

### Positive

- Voice UX is delivered without changing the command execution boundary.
- Existing typed Commands behavior remains the fallback and the primary safety path.
- AI Platform secrets stay server-side.
- Audit can link a voice draft to a command via `asrTraceId` without storing audio.
- The older async ASR proxy remains available for existing consumers.

### Negative

- HomeTusk temporarily has two ASR-facing APIs: legacy async proxy and MVP sync BFF.
- Synchronous ASR can make the web user wait for the upstream response.
- ASR upstream response drift needs tolerant adapter code and tests.

## Alternatives Considered

### Reuse The Legacy Async Household ASR Proxy

Rejected for the MVP flow. It requires upload plus polling and is scoped around
household transcription jobs, while Voice Command Chat needs a short-lived draft
inside the active composer.

### Call AI Platform Directly From Browser

Rejected. It would expose provider details/secrets and bypass HomeTusk
guardrails, error mapping, observability, and rate limiting.

### Auto-Send Transcript After ASR

Rejected. ASR quality is not a safe execution trigger. The transcript must be
editable and manually sent.

### Add Local ASR Or LLM Code

Rejected. HomeTusk consumes the external AI Platform and keeps business rules in
application code.

## Migration And Rollback

The change is additive.

Rollback options:

1. set `voice.enabled=false` or `voice.asr.enabled=false`;
2. set `VITE_VOICE_COMMAND_ENABLED=false`;
3. keep nullable `commands.asr_trace_id` until a planned cleanup migration;
4. continue serving the legacy async ASR proxy unchanged.

## Related

- Initiative: `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- Contract: `docs/contracts/http/voice-transcriptions.openapi.yaml`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Mapping: `docs/integration/ai-platform/v1/mapping/hometusk-asr-bff.md`
- Diagram: `docs/diagrams/sequence-voice-command-chat.md`
