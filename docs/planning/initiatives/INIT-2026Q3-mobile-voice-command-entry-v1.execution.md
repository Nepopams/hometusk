# INIT-2026Q3 Mobile Voice Command Entry v1 Execution Notes

**Status:** Gate C GO; APPLY complete; Review Gate GO; Gate D GO / LIMITED-GO.
**Date:** 2026-06-18
**Initiative:** `docs/planning/initiatives/INIT-2026Q3-mobile-voice-command-entry-v1.md`
**Delegation:** The user delegated Human Gate decisions to Codex for this workstream, with results recorded here.

---

## Gate C Decision - GO

**Decision:** GO for mobile-only APPLY.

**Rationale:**

- The initiative is accepted by the user as the active delivery goal.
- Backend voice baseline is already accepted and implemented through `POST /api/v1/voice/transcriptions`.
- ADR-020 and the voice transcription OpenAPI contract require clients to use the HomeTusk BFF and then return to explicit command submission.
- Current mobile command request code already supports `inputMode=voice_transcript`, `source=voice`, and optional `asrTraceId`.
- No contract, backend, AI Platform, or production rollout changes are required for this APPLY.

**Approved scope:**

- Add a secondary microphone entry point inside the existing mobile Command screen.
- Add a short recording sheet with ready, recording, review, processing, permission/error states.
- Upload recorded audio to the existing HomeTusk voice transcription endpoint.
- Insert the returned transcript as an editable command draft.
- Send voice-originated drafts only after explicit user Send through the existing command flow.
- Preserve `needs_input` and `needs_confirmation` semantics.
- Update mobile smoke checklist and compatibility notes.

**Approved files / areas:**

- `clients/mobile/package.json`
- `clients/mobile/package-lock.json`
- `clients/mobile/app.json`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/app/AppShell.tsx`
- `clients/mobile/src/app/types.ts`
- `clients/mobile/src/features/command/**`
- `clients/mobile/src/shared/errors/apiErrorFormatting.ts`
- `clients/mobile/src/shared/ui/LabeledInput.tsx`
- `clients/mobile/src/shared/ui/styles.ts`
- `clients/mobile/docs/release-smoke-ai-command.md`
- `docs/planning/initiatives/INIT-2026Q3-mobile-voice-command-entry-v1.execution.md`

**Forbidden scope:**

- `services/backend/**`
- `docs/contracts/http/**`
- AI Platform files or direct mobile-to-AI-Platform calls
- command contract changes
- production rollout / feature flag enablement
- auto-send after ASR
- local AI/ASR logic in mobile
- separate voice assistant mode or navigation surface

**Required checks:**

- `cd clients/mobile && npm run typecheck`
- scope scan for forbidden backend/OpenAPI/AI Platform changes
- mobile source scan for direct AI Platform calls and auto-send behavior
- update `clients/mobile/docs/release-smoke-ai-command.md`

---

## APPLY Evidence

**Delivered in APPLY:**

- Added `expo-audio` as the mobile recording dependency and configured the Expo audio plugin with foreground microphone permission copy.
- Added a secondary voice entry action to the existing Command composer.
- Added `VoiceRecordingSheet` with ready, recording, review, processing, permission denied, recording failed, upload/transcription failed, unsupported media, rate limit, timeout, and unclear speech states.
- Voice recording uses Expo high-quality recording output; native `.m4a` / `audio/m4a` and web `audio/webm` are compatible with the accepted backend media allowlist.
- Added mobile API client support for authenticated multipart `POST /api/v1/voice/transcriptions` with `X-Correlation-ID`.
- Successful ASR returns only an editable transcript draft and safe `traceId`; the sheet inserts the draft into the existing command composer and closes.
- Existing Send remains the only command execution trigger.
- Voice-originated command drafts use the existing `natural_command` request path with `inputMode=voice_transcript`, `source=voice`, and optional `asrTraceId`.
- `needs_input` continuation and `needs_confirmation` approve/cancel handlers remain unchanged and separate.
- Recent command hints mark voice-originated commands as `Voice:` / `Голосом:` while reusing the existing recent-command UI.
- Mobile README and smoke checklist now document voice compatibility, multipart auth, manual Send, metadata, failure states, and direct-AI boundary checks.

**Held by design:**

- No backend runtime changes.
- No HTTP/OpenAPI contract changes.
- No AI Platform changes or direct mobile-to-AI-Platform calls.
- No local ASR/AI logic in mobile.
- No auto-send after ASR.
- No separate voice assistant mode or navigation surface.
- No production rollout / feature flag enablement.
- No raw audio persistence decision in mobile.

## Verification Evidence

Command run from `clients/mobile`:

```text
npm run typecheck
```

Result: **PASS** on 2026-06-18.

Dependency check from `clients/mobile`:

```text
npm ls expo-audio
```

Result: **PASS**, resolved `expo-audio@56.0.12`.

Scope scan from repository root:

```text
git diff --name-only -- services/backend docs/contracts/http docs/integration/ai-platform vr_ai_platform
```

Result: no output.

Mobile boundary scan:

```text
rg -n "AI Platform|ai-platform|/v1/asr|/v1/decide|direct mobile|direct.*AI|auto-send|auto send|autosend|automatically.*send|always-listening|wake word|assistant mode" clients/mobile/src clients/mobile/README.md clients/mobile/docs/release-smoke-ai-command.md docs/planning/initiatives/INIT-2026Q3-mobile-voice-command-entry-v1.execution.md
```

Result: only boundary documentation / forbidden-scope checklist hits; no direct mobile AI Platform call or auto-send implementation found.

Whitespace check from repository root:

```text
git diff --check -- clients/mobile docs/planning/initiatives/INIT-2026Q3-mobile-voice-command-entry-v1.execution.md
```

Result: **PASS**, with Windows line-ending warnings only.

## Review Gate Decision

**Decision:** GO for the mobile-only voice command entry implementation.

**Must-fix findings:** none.

**Evidence:**

- Typecheck passed.
- Dependency resolution confirms `expo-audio@56.0.12`.
- Forbidden backend/OpenAPI/AI Platform scope scan was empty.
- Voice transcription calls are routed through HomeTusk API client to `/voice/transcriptions`.
- Voice transcript insertion does not call `/commands`; command submission remains behind the existing Send button.
- `handleSubmitCommand` still sends `type=natural_command` and now only adds voice metadata when a safe ASR trace id is present.
- Confirmation approve/cancel and `needs_input` continuation code paths were preserved.
- Smoke checklist covers voice happy path, metadata, failure states, and no direct AI Platform calls.

**Residual risks:**

- Manual device/emulator microphone smoke was not run in this turn.
- Multipart upload behavior must still be validated against a live backend on Android/iOS hardware or a development build.
- iOS simulator microphone limitations may require physical device or EAS/dev build validation.

## Gate D Decision

**Decision:** GO for code completion and branch publication. LIMITED-GO for product readiness until live device voice smoke is executed.

**Rationale:**

- The approved mobile-only voice entry scope is implemented without backend, contract, AI Platform, or production rollout changes.
- Existing typed command flow, recent commands, `needs_input`, and `needs_confirmation` semantics are preserved.
- Verification checks passed for the available static/type/scope evidence.

**Next recommended action:**

Run the updated `clients/mobile/docs/release-smoke-ai-command.md` checklist on an Android/iOS-capable environment against a backend with `POST /api/v1/voice/transcriptions` enabled.
