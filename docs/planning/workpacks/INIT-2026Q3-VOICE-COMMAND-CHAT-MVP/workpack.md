# Workpack: INIT-2026Q3-VOICE-COMMAND-CHAT-MVP

## Sources of Truth

- Initiative: `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Product goal: `docs/planning/strategy/product-goal.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Voice transcription contract: `docs/contracts/http/voice-transcriptions.openapi.yaml`
- AI Platform mapping: `docs/integration/ai-platform/v1/mapping/hometusk-asr-bff.md`
- ADR: `docs/adr/020-voice-command-chat-asr-bff.md`
- Diagram: `docs/diagrams/sequence-voice-command-chat.md`
- Design handoff: `docs/design/voice-command-chat/*.md`
- Backend runtime: `services/backend/src/main/java/com/hometusk/**`
- Web runtime: `clients/web/src/**`

## Status

**IN_PROGRESS.** Human gates are delegated to Codex by the user for this goal.
Gate C and artifact gate are self-approved when the listed artifacts are
created and implementation remains within this workpack.

## Objective

Deliver an MVP voice-to-command path inside the existing Commands page:
record audio, transcribe through HomeTusk ASR BFF, insert an editable draft,
and execute only after explicit user Send through the existing command pipeline.

## Scope

- Add sync `POST /api/v1/voice/transcriptions`.
- Add AI Platform ASR client for `/v1/asr/transcribe`.
- Add controlled ASR validation/error mapping, feature flags, rate limit, and metrics.
- Add optional command metadata `source=voice` and `asrTraceId`.
- Integrate existing web Commands composer with mic recording and editable transcript.
- Preserve typed command behavior and old async household ASR proxy.
- Update contracts, ADR, diagram, service catalog, API coverage, changelog, and rollout/QA notes.

## Non-goals

- No auto-send after ASR.
- No generic assistant chat.
- No direct browser AI Platform call.
- No local LLM or model code.
- No persistent raw audio or transcript storage in ASR flow.
- No upstream snapshot edits under `docs/integration/ai-platform/v1/upstream/**`.

## Files Expected To Change

- `docs/design/voice-command-chat/*.md`
- `docs/contracts/http/voice-transcriptions.openapi.yaml`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/adr/020-voice-command-chat-asr-bff.md`
- `docs/_indexes/adr-index.md`
- `docs/diagrams/sequence-voice-command-chat.md`
- `docs/_indexes/diagrams-index.md`
- `docs/integration/ai-platform/v1/mapping/hometusk-asr-bff.md`
- `docs/architecture/service-catalog.md`
- `docs/mvp/api-coverage.md`
- `CHANGELOG.md`
- `services/backend/src/main/java/com/hometusk/voice/**`
- `services/backend/src/main/java/com/hometusk/commands/**`
- `services/backend/src/main/resources/application.yml`
- `services/backend/src/main/resources/db/migration/V031__add_voice_command_trace.sql`
- `services/backend/src/test/java/com/hometusk/voice/**`
- `clients/web/src/routes/Commands.tsx`
- `clients/web/src/routes/Commands.css`
- `clients/web/src/hooks/useAsrTranscription.ts`
- `clients/web/src/types/api.ts`
- `clients/web/src/i18n/translations.ts`

## Acceptance Criteria

- [ ] ASR BFF authenticates requests and validates exactly one audio file.
- [ ] ASR BFF maps controlled errors to stable status/code pairs.
- [ ] ASR BFF never creates or executes a command.
- [ ] ASR flow stores no raw audio and logs no raw transcript.
- [ ] Voice metrics exist for requests, errors, latency, file size, and command outcome.
- [ ] Web voice success inserts editable draft and waits for explicit Send.
- [ ] Web typed command flow remains unchanged.
- [ ] Voice command Send uses `source=voice` and `asrTraceId` when available.
- [ ] Desktop/tablet/mobile layouts remain usable without horizontal overflow.
- [ ] Backend and frontend focused checks pass or blockers are documented.

## Implementation Plan

1. Finish contract, ADR, diagram, design handoff, and workpack docs.
2. Add backend voice config, validation, rate limiting, ASR client, controller, metrics, and tests.
3. Add command trace metadata migration and runtime support.
4. Update frontend ASR hook to the sync BFF contract.
5. Integrate voice controls into the active Commands route.
6. Update docs/service catalog/API coverage/changelog and visual QA.
7. Run focused backend tests, frontend build/lint, and browser checks if the app can start.

## Risks

- ASR upstream contract shape may drift; map tolerant success fields and controlled error codes.
- Existing Russian translations contain legacy encoding artifacts; keep new UI keys minimal.
- Full integration tests depend on local Docker/Testcontainers.

## Verification Evidence

- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain testClasses --console=plain`: PASS on 2026-06-14.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.voice.service.VoiceRateLimitServiceTest" --rerun-tasks --console=plain`: PASS on 2026-06-14.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.voice.*" --console=plain`: BLOCKED locally because Testcontainers could not find a Docker environment; integration tests compiled and are present for Docker-enabled CI/local runs.
- `cd clients/web && npm test -- --run`: PASS on 2026-06-14; 29 tests across 4 files.
- `cd clients/web && npm run build`: PASS on 2026-06-14; existing Vite >500 kB chunk warning remains.
- `cd clients/web && npm run lint`: PASS on 2026-06-14.
- Browser smoke: PASS for app shell/dev login on `http://127.0.0.1:5175`; full Commands screen visual requires dev JWT/backend session.

## Rollback

- Disable `voice.enabled=false` or `voice.asr.enabled=false`.
- Hide frontend with `VITE_VOICE_COMMAND_ENABLED=false`.
- Revert additive endpoint/UI changes.
- If migration was applied, keep nullable `commands.asr_trace_id` until a safe DB cleanup window.
