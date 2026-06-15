# HomeTusk Voice ASR BFF To AI Platform ASR Mapping

**Status:** current
**Date:** 2026-06-14
**Initiative:** INIT-2026Q3-voice-command-chat-mvp

## Boundary

HomeTusk exposes `POST /api/v1/voice/transcriptions` to authenticated clients.
The backend calls AI Platform ASR at `POST /v1/asr/transcribe`.

This mapping is HomeTusk-owned adapter documentation. It does not modify the
read-only upstream snapshots under `docs/integration/ai-platform/v1/upstream/**`.

## Request Mapping

| HomeTusk field/header | AI Platform ASR field/header | Notes |
|-----------------------|------------------------------|-------|
| multipart `file` | multipart `file` | Exactly one file. Raw bytes are streamed/forwarded and not persisted. |
| `X-Correlation-ID` | `X-Request-Id` and `X-Correlation-ID` | Safe trace propagation. |
| backend config `voice.asr.apiKey` | `X-API-Key` | Backend-only secret. |

Supported media types:

```text
audio/mpeg
audio/mp3
audio/mp4
audio/m4a
audio/wav
audio/x-wav
audio/webm
audio/ogg
audio/flac
```

## Success Mapping

AI Platform ASR may return `transcript` or `text` depending on upstream
revision. HomeTusk maps either field to:

```json
{
  "transcript": "...",
  "status": "ok",
  "traceId": "trace-asr-...",
  "latencyMs": 1234
}
```

If the upstream response is missing a usable transcript field, HomeTusk returns
`bad_upstream_response` with HTTP 502.

## Error Mapping

| Upstream/local condition | HomeTusk code | HTTP |
|--------------------------|---------------|------|
| Invalid multipart request | `invalid_multipart` | 400 |
| Missing audio file | `missing_audio_file` | 400 |
| File too large | `file_too_large` | 413 |
| Unsupported media | `unsupported_media` | 415 |
| Local ASR config disabled/missing | `asr_config_error` | 500 |
| Upstream auth failure | `auth_error` | 502 |
| Upstream malformed response | `bad_upstream_response` | 502 |
| Upstream 5xx/network failure | `upstream_unavailable` | 502 |
| Upstream timeout | `timeout` | 504 |
| Local user rate limit | `local_rate_limit` | 429 |

## Privacy Rules

- Do not log raw audio.
- Do not log raw transcript in the ASR flow.
- Do not store raw audio or transcript in ASR tables.
- Metrics may include status, error code, media type, latency, and file size bucket.
- Command text follows the existing command pipeline logging policy only after user Send.
