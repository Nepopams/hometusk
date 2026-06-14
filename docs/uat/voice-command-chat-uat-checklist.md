# Voice Command Chat UAT Checklist

Date: 2026-06-14

## Required Configuration

`deploy-uat.yml` builds `.env.runtime` from `UAT_ENV_FILE` plus the optional
`UAT_VOICE_ENV_FILE` overlay. If `UAT_VOICE_ENV_FILE` already exists in GitHub,
recreate it with its existing values plus the decision settings below; do not
replace it with only the decision block.

Safe `UAT_VOICE_ENV_FILE` template for the voice + agentic UAT path:

```dotenv
HOMETUSK_VOICE_ENABLED=true
HOMETUSK_VOICE_ASR_ENABLED=true
DECISION_PROVIDER=aiplatform
DECISION_FALLBACK_ENABLED=true
AI_PLATFORM_URL=http://10.0.0.5
AI_PLATFORM_ASR_TRANSCRIBE_PATH=/v1/asr/transcribe
AI_PLATFORM_DECISION_PATH=/v1/decide
AI_PLATFORM_API_KEY=
AI_PLATFORM_ASR_CONNECT_TIMEOUT_MS=5000
AI_PLATFORM_ASR_READ_TIMEOUT_MS=30000
HOMETUSK_VOICE_ASR_MAX_SIZE_BYTES=10485760
HOMETUSK_VOICE_ASR_REQUESTS_PER_MINUTE=5

# Keep these here only if the current UAT setup already stores Yandex IdP
# values in UAT_VOICE_ENV_FILE instead of UAT_ENV_FILE.
HOMETUSK_IDP_YANDEX_ALIAS=yandex
HOMETUSK_IDP_YANDEX_CLIENT_ID=<existing-uat-yandex-client-id>
HOMETUSK_IDP_YANDEX_CLIENT_SECRET=<existing-uat-yandex-client-secret>
HOMETUSK_IDP_YANDEX_DEFAULT_SCOPE=login:info login:email login:avatar
HOMETUSK_IDP_YANDEX_FORCE_CONFIRM=false
HOMETUSK_IDP_YANDEX_HOSTED_DOMAIN=
```

`AI_PLATFORM_API_KEY` is blank for this UAT network setup unless the HomeTusk ->
AI Platform gateway starts requiring a bearer token. The Cloud.ru ASR upstream
key remains inside the AI Platform service as `ASR_API_KEY`; it is not the
HomeTusk `AI_PLATFORM_API_KEY`.

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
