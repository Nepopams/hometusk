# INIT-2026Q3-02: ASR BFF and AI Platform Integration

## Status

Proposed

## Owner

HomeTusk backend team.

## Problem

The AI Platform provides ASR, but HomeTusk should not expose platform credentials or raw platform contracts directly to the browser/mobile client. HomeTusk also needs product-level authentication, authorization, rate limits, privacy controls, and trace correlation.

## Goal

Add a HomeTusk backend proxy/BFF endpoint for ASR transcription.

Target endpoint:

```http
POST /api/v1/voice/transcriptions
Content-Type: multipart/form-data
```

Request field:

```text
file: binary audio file
```

Successful response:

```json
{
  "transcript": "add trash bags and radish to the Auchan shopping list",
  "status": "ok",
  "traceId": "trace-asr-...",
  "latencyMs": 1234
}
```

## Platform contract

HomeTusk calls AI Platform:

```http
POST /v1/asr/transcribe
Content-Type: multipart/form-data
```

The platform endpoint returns transcript text and safe metadata. It does not call decisioning or agents.

## Non-goals

- No direct browser call to AI Platform.
- No command execution in the ASR endpoint.
- No transcript normalization in HomeTusk beyond trimming/safe validation.
- No voice storage.
- No streaming ASR in MVP.
- No provider-specific UI details.

## Backend requirements

### Controller

Create `VoiceController` or equivalent.

Responsibilities:

- require JWT;
- accept one multipart audio file;
- validate file presence;
- validate size;
- validate allowed media type;
- call ASR client;
- map platform response to HomeTusk response;
- map controlled errors;
- emit safe metrics.

### Client

Create `AiPlatformAsrClient` or equivalent.

Responsibilities:

- read ASR config;
- call AI Platform endpoint;
- set timeout;
- handle provider errors;
- return structured result.

### Config

Proposed config:

```yaml
voice:
  enabled: true
  asr:
    enabled: true
    max-file-size-mb: 25
    timeout-ms: 30000
    allowed-media-types:
      - audio/mpeg
      - audio/mp3
      - audio/mp4
      - audio/m4a
      - audio/wav
      - audio/x-wav
      - audio/webm
      - audio/ogg
      - audio/flac

aiplatform:
  asr:
    base-url: ${AI_PLATFORM_ASR_BASE_URL}
    transcribe-path: ${AI_PLATFORM_ASR_TRANSCRIBE_PATH:/v1/asr/transcribe}
    api-key: ${AI_PLATFORM_ASR_API_KEY}
```

### Privacy

Do not store:

- raw audio;
- raw transcript in logs;
- upstream raw response body;
- prompt/user text in logs.

Allowed logs/metrics:

- request id;
- trace id;
- provider status;
- latency;
- media type;
- file size bucket;
- error type.

### Error mapping

| Platform/Error | HomeTusk HTTP | UI state |
|---|---:|---|
| `invalid_multipart` | 400 | ASR error |
| `missing_audio_file` | 400 | ASR error |
| `file_too_large` | 413 | ASR error |
| `unsupported_media` | 415 | ASR error |
| `auth_error` | 502 | ASR error |
| `bad_upstream_response` | 502 | ASR error |
| `upstream_unavailable` | 502 | ASR error |
| `timeout` | 504 | ASR timeout |
| local rate limit | 429 | ASR rate limit |

## Command integration

ASR endpoint does not call `/api/v1/commands`.

The frontend sends command text separately after user confirmation.

If existing command request supports metadata, include:

```json
{
  "source": "voice",
  "asrTraceId": "trace-asr-..."
}
```

If not supported, keep the existing command contract and document the gap.

## Tests

### Backend tests

- success with supported audio type;
- missing file;
- unsupported media type;
- file too large;
- platform timeout;
- platform bad response;
- platform auth error;
- no raw transcript/audio in logs if log capture exists.

### Integration tests

- ASR success returns transcript draft;
- ASR error does not create command;
- ASR timeout maps to controlled error.

## Observability

Metrics:

- `hometusk.voice.asr.requests`;
- `hometusk.voice.asr.errors`;
- `hometusk.voice.asr.latency`;
- `hometusk.voice.asr.file_size_bucket`;
- `hometusk.voice.asr.transcript_length_bucket`.

## Rollout

- Add feature flags.
- Enable locally.
- Enable for internal household.
- Enable for UAT users.
- Monitor error rate and latency.
- Only then enable broadly.

## Risks

### Risk: platform ASR contract changes

Mitigation: keep ASR path configurable and isolate platform mapping in one client.

### Risk: transcript logs leak private household information

Mitigation: safe logging only; add tests or log review.

### Risk: browser audio format mismatch

Mitigation: support `audio/webm` and other MVP allowlist types; expose clear UI errors.
