# INIT-2026Q3: Voice Command Chat MVP

## Status

In Progress

## Initiative type

Product / Backend / Frontend / AI Platform Integration / Design-to-Code

## Owner

HomeTusk product engineering team.

## Target milestone

MVP Closure / Iteration 2

## Context

HomeTusk уже имеет базовый command-driven pipeline: пользовательская команда превращается в решение, проходит валидацию, guardrails и приводит к созданию или изменению доменных сущностей.

Следующий продуктовый шаг — дать пользователю возможность закрывать бытовые сценарии голосом: сказать команду, получить расшифровку, проверить её, отправить и увидеть результат в виде созданных задач, покупок, связей task ↔ shopping или уточняющего вопроса.

Это не отдельный AI-chat и не generic assistant. Это **Voice Command Chat** внутри существующего раздела Commands.

HomeTusk остаётся product-сервисом. ASR и LLM/agent decisioning выполняются во внешней AI Platform. HomeTusk не содержит LLM-логики, не вызывает модели напрямую и не принимает невалидированный AI output к исполнению.

## Goal

Реализовать end-to-end сценарий Voice Command Chat:

1. Пользователь открывает Commands.
2. Пользователь нажимает кнопку микрофона.
3. Пользователь записывает голосовую команду.
4. Frontend отправляет аудио в HomeTusk ASR BFF.
5. HomeTusk ASR BFF вызывает AI Platform ASR.
6. ASR возвращает transcript draft.
7. Пользователь проверяет и редактирует transcript.
8. Пользователь вручную отправляет команду.
9. HomeTusk запускает существующий command pipeline.
10. UI показывает результат:

    * executed;
    * needs_input;
    * rejected;
    * controlled ASR error.

## Core principle

ASR only creates an editable draft.

ASR must not execute a command.

Command execution happens only after explicit user action: Send.

## Scope

### Backend

Implement HomeTusk ASR BFF endpoint:

```http
POST /api/v1/voice/transcriptions
Content-Type: multipart/form-data
```

Expected request field:

```text
file: binary audio file
```

Expected successful response:

```json
{
  "transcript": "add trash bags and radish to the Auchan shopping list",
  "status": "ok",
  "traceId": "trace-asr-...",
  "latencyMs": 1234
}
```

The endpoint must:

* require authenticated user;
* validate multipart request;
* validate that exactly one audio file is provided;
* validate supported media type;
* validate max file size;
* call AI Platform ASR endpoint;
* map AI Platform success response;
* map AI Platform controlled errors;
* return safe metadata;
* never execute `/api/v1/commands`;
* never store raw audio;
* never log raw audio;
* never log raw transcript.

### AI Platform integration

HomeTusk consumes the AI Platform ASR contract:

```http
POST /v1/asr/transcribe
Content-Type: multipart/form-data
```

AI Platform ASR returns transcript text and metadata.

ASR endpoint does not call `/v1/decide`, RouterV2, Assist Mode, or agent decisioning.

### Command pipeline integration

Frontend sends the confirmed transcript through the existing command flow.

Primary endpoint:

```http
POST /api/v1/commands
```

If the existing command contract supports metadata, frontend/backend may include:

```json
{
  "source": "voice",
  "asrTraceId": "trace-asr-..."
}
```

If the existing contract does not support this yet, the implementation must not invent incompatible fields. It must either:

* add backward-compatible optional metadata fields; or
* document the gap and use the existing command request shape.

### Frontend

Implement Voice Command Chat inside the existing Commands area.

The feature must include:

* microphone entry point;
* browser recording state;
* cancel recording;
* uploading/transcribing state;
* editable transcript draft;
* manual Send action;
* command processing state;
* structured result rendering;
* needs_input clarification UI;
* rejected state with recovery;
* ASR permission error;
* ASR timeout/rate-limit error;
* responsive desktop/tablet/mobile layouts.

### Design-to-code

Use existing Pencil design artifacts as input.

Primary design source:

```text
docs/design/voice-command-chat/hometusk.pen
```

PNG exports:

```text
docs/design/voice-command-chat/voice/voice-chat-clickable-prototype-map.png
docs/design/voice-command-chat/voice/voice-chat-desktop-asr-permission-error.png
docs/design/voice-command-chat/voice/voice-chat-desktop-asr-timeout-rate-limit.png
docs/design/voice-command-chat/voice/voice-chat-desktop-draft-ready.png
docs/design/voice-command-chat/voice/voice-chat-desktop-empty.png
docs/design/voice-command-chat/voice/voice-chat-desktop-executed-shopping-items.png
docs/design/voice-command-chat/voice/voice-chat-desktop-executed-task-shopping-link.png
docs/design/voice-command-chat/voice/voice-chat-desktop-needs-input.png
docs/design/voice-command-chat/voice/voice-chat-desktop-processing.png
docs/design/voice-command-chat/voice/voice-chat-desktop-recording.png
docs/design/voice-command-chat/voice/voice-chat-desktop-rejected.png
docs/design/voice-command-chat/voice/voice-chat-desktop-transcribing.png
docs/design/voice-command-chat/voice/voice-chat-mobile-asr-permission-error.png
docs/design/voice-command-chat/voice/voice-chat-mobile-draft-ready.png
docs/design/voice-command-chat/voice/voice-chat-mobile-empty.png
docs/design/voice-command-chat/voice/voice-chat-mobile-executed-shopping-items.png
docs/design/voice-command-chat/voice/voice-chat-mobile-needs-input.png
docs/design/voice-command-chat/voice/voice-chat-mobile-processing.png
docs/design/voice-command-chat/voice/voice-chat-mobile-recording-bottom-sheet.png
docs/design/voice-command-chat/voice/voice-chat-mobile-transcribing.png
docs/design/voice-command-chat/voice/voice-chat-tablet-empty.png
docs/design/voice-command-chat/voice/voice-command-chat-components.png
docs/design/voice-command-chat/voice/voice-command-chat-mvp-handoff-qa.png
docs/design/voice-command-chat/voice/voice-command-chat-mvp.png
```

Codex must use Pencil MCP if available.

If Pencil MCP is unavailable, Codex must use PNG exports and generated markdown handoff as fallback.

The implementation must not create a new visual direction. It must reuse the existing HomeTusk design system: colors, typography, spacing, cards, inputs, buttons, badges, navigation patterns and responsive conventions.

## Non-goals

* No generic assistant chat.
* No separate Voice or AI navigation item.
* No auto-send after ASR.
* No streaming ASR in MVP.
* No wake word.
* No TTS.
* No persistent raw audio storage.
* No raw transcript logging.
* No direct browser call to AI Platform.
* No new microservice.
* No LLM/agent logic inside HomeTusk.
* No changes to canonical AI Platform decision contracts unless explicitly approved in a separate initiative.

## User scenarios

### Scenario 1: Add shopping items by voice

User says:

```text
add trash bags and radish to the Auchan shopping list
```

Expected behavior:

* recording completes;
* ASR returns transcript;
* transcript appears in editable input;
* user presses Send;
* existing command pipeline runs;
* shopping items are added;
* UI renders structured result card.

Expected UI result:

```text
Added 2 items to the Auchan shopping list:
- trash bags
- radish
```

### Scenario 2: Create task and link shopping items

User says:

```text
buy milk and chicken for dinner
```

Expected behavior:

* transcript appears as draft;
* user sends;
* command pipeline creates task and shopping items when supported by decision;
* guardrails validate result;
* UI renders task-shopping linked result.

Expected UI result:

```text
Created task:
Buy dinner groceries

Linked shopping items:
- milk
- chicken
```

### Scenario 3: Needs input

User says:

```text
clean the kitchen tomorrow evening
```

Expected behavior:

* transcript appears as draft;
* user sends;
* command pipeline returns `needs_input`;
* UI presents clarification card;
* user can choose a chip or type an answer;
* `needs_input` is not shown as error.

### Scenario 4: Rejected command

User says:

```text
clean the kitchen yesterday evening
```

Expected behavior:

* transcript appears as draft;
* user sends;
* guardrails or business validation reject the command;
* UI shows controlled rejection reason;
* user can edit and resend.

### Scenario 5: ASR permission error

User taps mic but browser denies access.

Expected behavior:

* UI shows microphone permission error;
* user can retry or close;
* typed draft is preserved.

### Scenario 6: ASR timeout or rate limit

ASR call fails with timeout or rate limit.

Expected behavior:

* UI shows controlled error;
* user can retry voice;
* existing draft is preserved when available.

## Functional requirements

### FR-01: ASR BFF endpoint

HomeTusk must expose:

```http
POST /api/v1/voice/transcriptions
```

The endpoint must return transcript draft and safe metadata.

### FR-02: Feature flags

Add feature flags:

```text
voice.enabled
voice.asr.enabled
```

Optional frontend flag:

```text
VITE_VOICE_COMMAND_ENABLED
```

or equivalent based on current frontend stack.

### FR-03: Browser recording

Frontend must support recording audio from microphone.

The implementation must:

* request microphone permission;
* start recording;
* show recording state;
* allow stop;
* allow cancel;
* send audio only after stop;
* discard audio after successful upload or cancel.

### FR-04: Editable transcript

ASR transcript must be inserted into command input as editable text.

No command is sent automatically.

### FR-05: Command send

User manually sends transcript through the existing command API.

The implementation must preserve existing typed command behavior.

### FR-06: Structured result cards

UI must render known command outcomes as structured cards:

* shopping items added;
* task created;
* task-shopping linked;
* needs input;
* rejected.

### FR-07: Clarification UI

For `needs_input`, UI must support:

* clarification text;
* suggested chips where available;
* free-text answer where needed;
* continued command flow.

### FR-08: Error states

UI must support controlled ASR errors:

* microphone permission denied;
* timeout;
* rate limit;
* unsupported audio;
* generic upstream unavailable.

### FR-09: Responsive behavior

Desktop:

* use existing Commands page shell;
* main chat/content column;
* optional context/history rail only if it matches existing design.

Tablet:

* readable single-column layout;
* no dense side rail by default.

Mobile:

* full-screen Commands chat;
* sticky bottom input;
* recording as bottom sheet or expanded bottom panel;
* touch targets at least 44px.

## Backend requirements

### B-01: ASR client

Create an AI Platform ASR client with configurable:

```text
baseUrl
transcribePath
apiKey
timeoutMs
maxFileSizeMb
allowedMediaTypes
```

### B-02: Supported media types

Use the current AI Platform ASR allowlist:

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

### B-03: Error mapping

Map controlled AI Platform errors into HomeTusk controlled API errors.

Minimum mapping:

```text
invalid_multipart       -> 400
missing_audio_file      -> 400
file_too_large          -> 413
unsupported_media       -> 415
asr_config_error        -> 500
auth_error              -> 502
bad_upstream_response   -> 502
upstream_unavailable    -> 502
timeout                 -> 504
local_rate_limit        -> 429
```

### B-04: Privacy

The backend must not store:

* raw audio;
* raw transcript in logs;
* raw upstream body;
* prompt text;
* command text in ASR logs.

Allowed logs/metrics:

* request id;
* user id hash or internal id if current logging policy allows;
* trace id;
* provider;
* status;
* latency;
* file size bucket;
* media type;
* error type.

### B-05: Metrics

Add metrics for:

```text
voice.asr.requests
voice.asr.errors
voice.asr.latency
voice.asr.file_size_bucket
voice.command.source.voice.count
voice.command.source.voice.outcome
```

Metric names may follow existing HomeTusk naming conventions.

## Frontend requirements

### FE-01: Component tree

Implement or reuse components:

```text
VoiceCommandChat
CommandInputBar
MicButton
RecordingPanel
TranscriptionStatus
ChatMessage
ResultCard
ClarificationChips
ErrorBanner
```

### FE-02: State machine

Minimum states:

```text
idle
recording
uploading
transcribing
draft_ready
processing_command
executed
needs_input
rejected
asr_permission_error
asr_timeout_or_rate_limit
asr_unsupported_media
```

### FE-03: Design source mapping

Before implementation, Codex must create:

```text
docs/design/voice-command-chat/implementation-map.md
docs/design/voice-command-chat/component-inventory.md
docs/design/voice-command-chat/responsive-rules.md
docs/design/voice-command-chat/frontend-acceptance-criteria.md
```

These docs must map Pencil frames to frontend components and implementation states.

### FE-04: Visual consistency

Use existing HomeTusk:

* tokens;
* buttons;
* cards;
* inputs;
* badges;
* typography;
* layout shells.

Do not introduce:

* neon AI assistant visuals;
* generic chatbot styling;
* unrelated icon pack unless already used;
* new navigation model.

### FE-05: Result rendering

Result rendering must be driven by command response status and domain entities.

The UI should not rely on free-form AI text for critical actions.

## Design artifacts

### Primary source

```text
docs/design/voice-command-chat/hometusk.pen
```

### Visual QA references

Use these exact files:

```text
docs/design/voice-command-chat/voice/voice-chat-desktop-empty.png
docs/design/voice-command-chat/voice/voice-chat-desktop-recording.png
docs/design/voice-command-chat/voice/voice-chat-desktop-transcribing.png
docs/design/voice-command-chat/voice/voice-chat-desktop-draft-ready.png
docs/design/voice-command-chat/voice/voice-chat-desktop-processing.png
docs/design/voice-command-chat/voice/voice-chat-desktop-executed-shopping-items.png
docs/design/voice-command-chat/voice/voice-chat-desktop-executed-task-shopping-link.png
docs/design/voice-command-chat/voice/voice-chat-desktop-needs-input.png
docs/design/voice-command-chat/voice/voice-chat-desktop-rejected.png
docs/design/voice-command-chat/voice/voice-chat-desktop-asr-permission-error.png
docs/design/voice-command-chat/voice/voice-chat-desktop-asr-timeout-rate-limit.png
docs/design/voice-command-chat/voice/voice-chat-mobile-empty.png
docs/design/voice-command-chat/voice/voice-chat-mobile-recording-bottom-sheet.png
docs/design/voice-command-chat/voice/voice-chat-mobile-transcribing.png
docs/design/voice-command-chat/voice/voice-chat-mobile-draft-ready.png
docs/design/voice-command-chat/voice/voice-chat-mobile-processing.png
docs/design/voice-command-chat/voice/voice-chat-mobile-executed-shopping-items.png
docs/design/voice-command-chat/voice/voice-chat-mobile-needs-input.png
docs/design/voice-command-chat/voice/voice-chat-mobile-asr-permission-error.png
docs/design/voice-command-chat/voice/voice-chat-tablet-empty.png
docs/design/voice-command-chat/voice/voice-command-chat-components.png
docs/design/voice-command-chat/voice/voice-command-chat-mvp-handoff-qa.png
docs/design/voice-command-chat/voice/voice-command-chat-mvp.png
docs/design/voice-command-chat/voice/voice-chat-clickable-prototype-map.png
```

## Implementation phases

### Phase 0: Design ingestion and implementation map

Codex must:

* verify Pencil MCP availability;
* inspect `hometusk.pen` if MCP is available;
* inspect PNG exports;
* inspect existing frontend design system;
* create implementation mapping docs;
* identify reusable components;
* identify design gaps;
* stop before product code unless this phase is part of approved apply scope.

Deliverables:

```text
docs/design/voice-command-chat/implementation-map.md
docs/design/voice-command-chat/component-inventory.md
docs/design/voice-command-chat/responsive-rules.md
docs/design/voice-command-chat/frontend-acceptance-criteria.md
```

### Phase 1: Backend ASR BFF

Implement:

* ASR controller;
* ASR request validation;
* AI Platform ASR client;
* config;
* error mapping;
* safe logging;
* metrics;
* backend tests.

Deliverables:

```text
POST /api/v1/voice/transcriptions
```

### Phase 2: Frontend static Voice Command Chat

Implement static UI states without real audio recording.

States:

* empty;
* recording;
* transcribing;
* draft ready;
* processing;
* executed shopping items;
* executed task-shopping link;
* needs input;
* rejected;
* ASR permission error;
* ASR timeout/rate limit.

### Phase 3: Browser recording and ASR integration

Implement:

* microphone permission handling;
* recording start/stop/cancel;
* audio upload to HomeTusk ASR BFF;
* transcript insertion into editable command input;
* retry and recovery behavior.

### Phase 4: Commands API integration

Implement:

* manual command send;
* command processing state;
* rendering of command response;
* needs_input continuation;
* rejected recovery;
* command history in UI state.

Do not create persistent chat storage unless already supported by the current backend.

### Phase 5: Visual QA against Pencil

Create:

```text
docs/reviews/voice-command-chat-visual-qa.md
```

The review must compare implementation against:

* `.pen` source if available;
* PNG exports;
* responsive rules;
* component inventory;
* accessibility requirements.

### Phase 6: Documentation and rollout

Update:

```text
docs/architecture/service-catalog.md
docs/contracts/http/*.yaml
docs/adr or docs/architecture/decisions
docs/mvp/api-coverage.md
CHANGELOG.md
```

Add rollout notes:

* local;
* UAT;
* feature flag enablement;
* metrics to monitor;
* known limitations.

## Test strategy

### Backend tests

Required:

* successful ASR transcription;
* missing file;
* unsupported media type;
* file too large;
* AI Platform timeout;
* AI Platform bad response;
* AI Platform auth error;
* ASR endpoint does not execute command;
* safe logging does not include transcript/audio where testable.

### Frontend tests

Required:

* renders empty state;
* recording state;
* cancel recording;
* permission denied;
* ASR success inserts editable transcript;
* ASR timeout preserves draft where applicable;
* Send invokes command API only after user action;
* needs_input renders clarification;
* rejected renders recovery path;
* mobile input remains accessible.

### Integration tests

Required:

* voice transcript -> edit -> send -> executed shopping items;
* voice transcript -> send -> task-shopping linked result;
* voice transcript -> send -> needs_input;
* voice transcript -> send -> rejected;
* ASR failure -> no command created.

### Visual QA

Required:

* desktop empty;
* desktop recording;
* desktop draft ready;
* desktop executed shopping items;
* desktop needs input;
* desktop rejected;
* mobile empty;
* mobile recording bottom sheet;
* mobile draft ready;
* mobile executed shopping items;
* handoff QA frame.

## Security and privacy

* ASR BFF requires authentication.
* Browser must not call AI Platform directly.
* Raw audio must not be persisted.
* Raw audio must not be logged.
* Raw transcript must not be logged by ASR flow.
* Command text follows existing HomeTusk command logging policy.
* Platform API key must be backend-only.
* ASR endpoint must enforce file size limit.
* ASR endpoint must enforce media type allowlist.
* Errors must be controlled and user-safe.

## Acceptance criteria

### Product acceptance

* User can complete a shopping command by voice.
* User can complete a task + shopping command by voice.
* UAT proves the full chain: ASR transcript -> manual Send -> `/api/v1/commands` -> `AiPlatformDecisionProvider`.
* User can handle needs_input from voice-originated command.
* User can recover from rejected command.
* User can recover from ASR permission error.
* User can recover from ASR timeout/rate limit.
* No command is sent automatically after ASR.

### Backend acceptance

* `/api/v1/voice/transcriptions` is implemented.
* AI Platform ASR client is isolated.
* Confirmed transcript is processed by the existing command pipeline with `decision.provider=aiplatform`.
* Config is environment-driven.
* Controlled errors are mapped.
* Tests exist for success and failure paths.
* Logs do not contain raw audio/transcript.

### Frontend acceptance

* Commands screen contains voice entry point.
* Transcript is editable.
* Send is manual.
* Desktop, tablet, and mobile layouts are implemented.
* UI uses existing design system.
* All major states have implementation and tests or stories.

### Design acceptance

* `.pen` is used as primary design source where MCP is available.
* PNG exports are used for visual QA.
* Implementation map exists.
* Component inventory exists.
* Responsive rules exist.
* Visual QA review exists.

### Architecture acceptance

* HomeTusk does not contain LLM/agent logic.
* ASR endpoint does not execute command.
* Existing command pipeline remains the only execution path.
* AI Platform integration is behind explicit client/adapter.
* No upstream decision contract changes are made in this initiative.

## Definition of Done

* Initiative implementation plan approved.
* Phase-scoped commits pushed.
* Backend ASR BFF implemented.
* Frontend Voice Command Chat implemented.
* Existing typed Commands flow still works.
* Voice flow works locally.
* UAT smoke proves an ASR-originated command creates or updates domain state through AI Platform decisioning.
* Backend tests added.
* Frontend tests or stories added.
* Visual QA document created.
* API docs updated.
* Service catalog updated.
* ADR created or updated.
* Changelog created.
* Rollout notes documented.
* Known limitations documented.

## Risks

### Risk: ASR quality is not enough for direct execution

Mitigation:

* no auto-send;
* editable transcript;
* visible review state.

### Risk: design drift

Mitigation:

* use `.pen` via Pencil MCP;
* use PNG exports for visual QA;
* require implementation map before coding.

### Risk: frontend becomes generic chatbot

Mitigation:

* keep feature inside Commands;
* render domain result cards;
* do not add separate AI navigation item.

### Risk: privacy regression

Mitigation:

* no raw audio storage;
* no raw transcript logging in ASR flow;
* safe metrics only;
* tests or review checklist.

### Risk: command continuation for needs_input is underspecified

Mitigation:

* Codex must inspect current command API;
* if continuation is unsupported, implement only display and document backend gap;
* do not invent incompatible contract silently.

### Risk: ASR platform outage blocks voice UX

Mitigation:

* controlled ASR error;
* typed command input remains available;
* feature flag can disable voice.

## Rollout plan

### Step 1: Local

Enable:

```text
voice.enabled=true
voice.asr.enabled=true
```

Verify:

* ASR BFF;
* frontend recording;
* transcript draft;
* command send;
* all UI states.

### Step 2: Internal UAT

Enable for internal household/users only.

Monitor:

* ASR latency;
* ASR error rate;
* command success rate;
* needs_input rate;
* rejected rate.

### Step 3: Limited beta

Enable for small user group.

Collect:

* transcript edit rate;
* retry rate;
* voice usage share;
* qualitative UX issues.

### Step 4: MVP enablement

Enable as part of Commands UX if metrics are acceptable.

## Open questions for implementation planning

1. Does current `/api/v1/commands` support optional metadata such as `source` and `asrTraceId`?
2. Does current command API support continuation for `needs_input`?
3. Does frontend already have a global audio/permission abstraction?
4. Does frontend have Storybook or equivalent component sandbox?
5. Should ASR BFF support local rate limiting in this initiative or in a follow-up?
6. Should command history be persisted or only local in MVP?
7. What exact frontend breakpoint values are already used in HomeTusk?
8. Should visual QA be manual markdown only or screenshot-diff based?

## Codex planning instruction

When this initiative is picked up, Codex must start in Plan Mode.

Codex must read:

```text
docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md
docs/design/voice-command-chat/hometusk.pen
docs/design/voice-command-chat/voice/*.png
existing frontend code
existing backend command code
existing AI Platform integration docs
existing service catalog
existing ADRs
```

Codex must use Pencil MCP if available.

If Pencil MCP is not available, Codex must continue using PNG exports and document fallback.

Codex must not implement until the plan is reviewed and approved.
