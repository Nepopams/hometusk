# Voice Command Chat UAT Checklist

Date: 2026-06-14

## Required Configuration

Use these values in the UAT runtime env:

```dotenv
DECISION_PROVIDER=aiplatform
DECISION_FALLBACK_ENABLED=true
AI_PLATFORM_URL=http://10.0.0.5
AI_PLATFORM_DECISION_PATH=/v1/decide
AI_PLATFORM_API_KEY=
```

ASR remains configured separately:

```dotenv
HOMETUSK_VOICE_ENABLED=true
HOMETUSK_VOICE_ASR_ENABLED=true
AI_PLATFORM_ASR_TRANSCRIBE_PATH=/v1/asr/transcribe
```

## Preflight

- [ ] HomeTusk backend logs show `AI Platform client initialized` with `decisionPath=/v1/decide`.
- [ ] HomeTusk backend logs show `DecisionProviderSelector` uses `aiplatform` on the happy path.
- [ ] AI Platform responds locally from HomeTusk UAT:
  `curl -v --max-time 10 http://10.0.0.5/v1/asr/transcribe` returns `405 Method Not Allowed` for GET.
- [ ] AI Platform `/v1/decide` is reachable from HomeTusk UAT with a valid POST smoke payload.

## Smoke Scenarios

- [ ] Voice command: "купи молоко" -> ASR transcript -> Send -> shopping item is created.
- [ ] Voice command: "убраться на кухне" -> ASR transcript -> Send -> task is created.
- [ ] Ambiguous voice command -> ASR transcript -> Send -> `needs_input` result.
- [ ] Logs or decision log prove `source=AI_PLATFORM`, not `MANUAL`, for the happy path.
- [ ] Fallback is exercised separately by making AI Platform unavailable and expecting `executed_degraded`.

## Evidence To Capture

- [ ] Browser screenshot of successful command result.
- [ ] Backend log line for AI Platform request/response.
- [ ] Decision log row with `source=AI_PLATFORM` and raw upstream decision payload.
- [ ] AI Platform decision log line for matching `trace_id`.
