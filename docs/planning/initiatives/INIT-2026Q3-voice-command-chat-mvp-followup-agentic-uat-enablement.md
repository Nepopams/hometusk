# INIT-2026Q3: Voice Command Chat MVP Follow-up — Agentic UAT Enablement

## Status

Proposed

## Initiative type

Corrective follow-up / UAT enablement / AI Platform integration / Command pipeline verification

## Parent initiative

`docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`

## Owner

HomeTusk product engineering team.

## Target milestone

MVP Closure / Voice Command Chat UAT readiness

## Context

The parent Voice Command Chat MVP initiative was intended to validate an end-to-end voice command scenario:

```text
voice -> ASR -> editable transcript -> user Send -> AI Platform decision/agents -> schema validation -> guardrails -> action execution -> persisted HomeTusk entities -> structured UI result
```

The current UAT implementation has completed the voice/ASR part:

```text
voice -> ASR -> editable transcript -> user Send -> existing command endpoint
```

However, confirmed voice commands are currently processed through the manual decision provider in UAT:

```dotenv
DECISION_PROVIDER=manual
```

This means the current UAT state validates voice transcription and editable transcript UX, but does not validate agentic command processing through AI Platform.

This is a scope gap. The parent initiative must not be considered complete until confirmed voice commands use AI Platform decisioning on the happy path in UAT.

## Problem

Voice Command Chat currently proves that a user can speak and get an editable command draft.

It does not yet prove that a voice-originated command can be interpreted and applied by the external AI Platform decision/agent pipeline.

This creates a product risk: the UI may look complete, but the actual differentiated value — natural language command handling via agents and safe domain action execution — is not validated in UAT.

## Goal

Complete the missing UAT path for Voice Command Chat:

```text
voice -> ASR -> editable transcript -> user Send -> /api/v1/commands -> AiPlatformDecisionProvider -> schema validation -> guardrails -> action executor -> persisted entities -> structured UI result
```

The ASR endpoint must remain transcription-only.

Agentic processing must happen only after explicit user confirmation through the existing command pipeline.

## Core boundary

Do not make the ASR endpoint call decisioning.

Correct:

```text
POST /api/v1/voice/transcriptions
  -> ASR only
  -> transcript draft

User reviews and presses Send

POST /api/v1/commands
  -> AI Platform Decision Provider
  -> validated decision
  -> guardrails
  -> action executor
```

Incorrect:

```text
POST /api/v1/voice/transcriptions
  -> ASR
  -> /v1/decide
  -> domain action
```

## Scope

### In scope

* Verify AI Platform decision endpoint path, auth, request/response contract and health.
* Verify HomeTusk `AiPlatformDecisionProvider` compatibility with current AI Platform decision contract.
* Configure UAT to use `DECISION_PROVIDER=aiplatform` for confirmed commands.
* Ensure voice-originated commands go through the same command pipeline as typed commands.
* Add/adjust backend tests or smoke tests proving AI Platform provider is used.
* Add/adjust frontend behavior only if current UI does not correctly render AI Platform outcomes.
* Add UAT checklist proving end-to-end agentic command processing.
* Add observability/log markers that show provider source: manual vs aiplatform vs fallback.
* Update parent initiative DoD so ASR-only cannot pass.
* Update docs/changelog/service catalog if needed.

### Out of scope

* No LLM/agent logic inside HomeTusk.
* No ASR endpoint decisioning.
* No auto-send after ASR.
* No direct frontend call to AI Platform.
* No upstream AI Platform contract redesign.
* No generic assistant chat.
* No streaming ASR.
* No TTS.
* No wake word.
* No major redesign of Voice Command Chat UI.
* No new microservice.

## Current known state

### Completed by parent initiative

* Voice recording UI.
* ASR request to HomeTusk backend.
* HomeTusk ASR integration with AI Platform ASR.
* Editable transcript draft.
* Manual Send action into Commands flow.
* Basic UI states for ASR success/error and command result display.

### Gap

* UAT uses manual provider.
* Confirmed voice commands are not processed by AI Platform decisioning on the happy path.
* No UAT smoke proving `voice -> ASR -> Send -> AI Platform decision -> persisted domain action`.
* No hard DoD gate preventing ASR-only completion from being marked as full Voice Command Chat MVP.

## Required UAT configuration

UAT must be configured to use AI Platform decisioning on the happy path.

Expected environment/config shape:

```dotenv
DECISION_PROVIDER=aiplatform
DECISION_FALLBACK_ENABLED=true
AI_PLATFORM_URL=http://10.0.0.5
AI_PLATFORM_DECISION_PATH=/v1/decide
AI_PLATFORM_API_KEY=
```

If the application uses YAML/config properties instead of these exact env names, Codex must map these logical settings to the actual existing HomeTusk configuration names.

Manual decision provider is allowed only for:

* local development;
* explicit fallback tests;
* intentionally configured fallback mode.

Manual decision provider must not be the happy path for UAT Voice Command Chat completion.

### Implementation note — 2026-06-14

The UAT AI Platform decision route is `POST /v1/decide` behind the private
network URL `http://10.0.0.5`. HomeTusk now sends the upstream snake_case
decision request envelope and validates/maps the upstream response envelope.
The ASR endpoint remains transcription-only.

## Functional requirements

### FR-01: Decision provider selection

HomeTusk must use `AiPlatformDecisionProvider` for confirmed commands in UAT.

The provider source must be visible in logs, metrics or decision trace.

Expected outcome for happy path:

```text
provider = aiplatform
```

Not acceptable for happy path:

```text
provider = manual
```

### FR-02: Voice-originated command path

A voice-originated command must follow the same execution pipeline as typed commands after user confirmation:

```text
CommandController -> CommandService -> ContextBuilder -> DecisionProviderSelector -> AiPlatformDecisionProvider -> schema validation -> guardrails -> ActionExecutor
```

Exact class names may differ. Codex must inspect current code and use actual implementation names.

### FR-03: ASR remains transcription-only

The ASR BFF endpoint must not call:

* AI Platform decision endpoint;
* RouterV2;
* Assist Mode;
* domain action executor;
* command service.

### FR-04: Contract compatibility check

Codex must compare:

* current HomeTusk AI Platform integration docs;
* current upstream AI Platform decision contract snapshot;
* current `AiPlatformDecisionProvider` request/response mapper;
* current UAT platform endpoint behavior if available through existing tests/mocks/config.

If a mismatch is found, Codex must document it and propose a minimal adapter/config fix.

### FR-05: Optional command metadata

If current command contract supports optional metadata, confirmed voice commands may include:

```json
{
  "source": "voice",
  "asrTraceId": "trace-asr-..."
}
```

If the contract does not support this, Codex must not silently invent incompatible fields.

Allowed options:

1. Add backward-compatible optional metadata fields.
2. Keep current command shape and document the traceability gap.

### FR-06: AI Platform outcomes in UI

The frontend must correctly render outcomes produced by AI Platform decisions:

* executed;
* needs_input;
* rejected;
* degraded/fallback if represented by backend.

If current UI already renders these via command response status, no UI changes are needed except tests/verification.

### FR-07: Safe fallback

If AI Platform decisioning is unavailable and fallback is enabled, HomeTusk must degrade according to existing fallback policy.

Fallback behavior must be explicit and observable.

Acceptable fallback examples:

* manual provider fallback;
* controlled reject;
* controlled needs_input;
* configured fallback response.

Silent success without traceability is not acceptable.

## Backend requirements

### B-01: Provider traceability

Decision logs, application logs or response metadata must allow UAT verification of provider source.

Minimum useful fields:

```text
commandId
correlationId
decisionProvider
decisionSource
externalDecisionId
fallbackUsed
aiPlatformLatencyMs
aiPlatformStatus
```

Use existing fields where available. Do not introduce unnecessary new persistence if current `DecisionLog` already covers this.

### B-02: Config validation

At startup or first AI Platform call, misconfiguration should fail safely.

Examples:

* missing base URL;
* missing/invalid decision path;
* missing API key if required;
* timeout config invalid.

### B-03: Health/smoke support

If existing platform health endpoint is available, UAT runbook should include it.

If not, UAT smoke can use a controlled command decision request.

### B-04: Contract validation

AI Platform response must be schema-validated before mapping/execution.

Invalid AI Platform response must not cause NPE or partial execution.

### B-05: Guardrails unchanged

Existing guardrails remain mandatory for AI Platform decisions.

AI Platform output must not bypass:

* schema validation;
* membership checks;
* deadline sanity;
* workload/task limits;
* shopping item validation;
* task-shopping linkage safeguards.

## Frontend requirements

### FE-01: No auto-send

Frontend must continue requiring explicit user Send after ASR transcript draft.

### FE-02: Result rendering

Frontend must render command response based on structured backend status and domain entities, not raw AI text.

### FE-03: Needs input

If AI Platform returns `needs_input`, UI must show clarification state, not error.

### FE-04: Rejected

If AI Platform returns rejected/guardrails rejection, UI must show controlled rejection and allow draft editing/resend.

### FE-05: Provider internals

Frontend must not expose AI Platform model, schema, raw prompt, raw response or guardrail internals as primary UI.

A debug/details affordance is acceptable only if consistent with existing internal/UAT tooling and not shown as product-facing content.

## UAT smoke scenarios

### Smoke 1: Voice shopping command

Input voice command:

```text
add trash bags and radish to the Auchan shopping list
```

Expected:

* ASR returns editable transcript.
* User presses Send.
* `/api/v1/commands` uses `AiPlatformDecisionProvider`.
* Shopping items are persisted.
* UI shows structured shopping result card.
* Logs/decision trace prove provider is `aiplatform`.

### Smoke 2: Voice task command

Input voice command:

```text
clean the kitchen tomorrow evening
```

Expected:

* ASR returns editable transcript.
* User presses Send.
* AI Platform decision is used.
* Task is created or `needs_input` is returned if assignment/context is insufficient.
* If task is created, guardrails have passed.
* Logs/decision trace prove provider is `aiplatform`.

### Smoke 3: Voice task + shopping linkage

Input voice command:

```text
buy milk and chicken for dinner
```

Expected:

* ASR returns editable transcript.
* User presses Send.
* AI Platform decision is used.
* Task and shopping items are created where supported by decision contract.
* Linked entities are persisted where supported.
* UI renders task-shopping linked result.
* Logs/decision trace prove provider is `aiplatform`.

### Smoke 4: Needs input from AI Platform

Input command should trigger ambiguity.

Expected:

* AI Platform decision returns `needs_input` or equivalent mapped status.
* HomeTusk does not execute partial unsafe actions.
* UI renders clarification state.
* Provider source is `aiplatform`.

### Smoke 5: Rejected command

Input command:

```text
clean the kitchen yesterday evening
```

Expected:

* AI Platform decision or HomeTusk guardrails reject the command.
* No invalid task is persisted.
* UI renders controlled rejection.
* Provider source is visible.

### Smoke 6: AI Platform outage fallback

Temporarily point AI Platform URL/path to unavailable endpoint or use existing test mechanism.

Expected:

* fallback policy is used if enabled;
* fallback source is visible;
* no crash;
* no silent fake AI success;
* UI renders controlled fallback outcome.

## Tests

### Required backend tests

* `AiPlatformDecisionProvider` success path maps valid decision.
* Invalid AI Platform response is rejected safely.
* AI Platform timeout uses configured fallback behavior.
* Provider selector uses AI Platform when configured.
* Provider selector does not use manual provider as happy path when configured for AI Platform.
* Voice-originated command after Send enters normal command pipeline.
* ASR endpoint does not invoke command service.
* Guardrails are invoked for AI Platform decisions.

### Required integration/smoke tests

* voice transcript -> Send -> AI Platform decision -> shopping items persisted;
* voice transcript -> Send -> AI Platform decision -> task created or needs_input;
* voice transcript -> Send -> AI Platform decision -> task-shopping linked result where supported;
* AI Platform needs_input -> UI/API response maps to clarification;
* AI Platform rejected -> UI/API response maps to controlled rejection;
* AI Platform unavailable -> fallback behavior.

### Optional frontend tests

Add only if current frontend implementation lacks coverage:

* Send after ASR calls Commands API.
* Result rendering supports AI Platform executed outcome.
* Result rendering supports AI Platform needs_input.
* Result rendering supports AI Platform rejected.
* ASR success does not auto-send.

## Documentation updates

### Update parent initiative

Update:

```text
docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md
```

Add explicit note:

```text
ASR-only is not sufficient for initiative completion.
The parent initiative is complete only when confirmed voice commands are processed by AI Platform Decision Provider on the UAT happy path.
```

### Update UAT checklist

Create or update:

```text
docs/uat/voice-command-chat-uat-checklist.md
```

Minimum sections:

* environment configuration;
* AI Platform health/decision endpoint verification;
* voice ASR smoke;
* voice -> AI decision smoke;
* needs_input smoke;
* rejected smoke;
* fallback smoke;
* log/trace verification.

### Update changelog

Create or update:

```text
docs/mvp/CHANGELOG-voice-command-agentic-uat.md
```

### Update service catalog

Update if service/component status changes:

```text
docs/architecture/service-catalog.md
```

### Update ADR if needed

If the implementation changes boundaries, create/update ADR.

Expected ADR topic if needed:

```text
ADR: Voice Command Chat ASR vs Decision Boundary
```

## Acceptance criteria

### Product acceptance

* User can speak a command and get editable transcript.
* User manually sends transcript.
* Confirmed command is processed by AI Platform decisioning in UAT.
* UI shows executed/needs_input/rejected outcome.
* ASR-only flow is not considered complete.

### Backend acceptance

* UAT uses `AiPlatformDecisionProvider` on happy path.
* Manual provider is not used as happy path in UAT.
* AI Platform decision response is schema-validated.
* Guardrails run before action execution.
* Decision provider source is traceable.
* Fallback behavior is explicit and tested.

### Frontend acceptance

* No auto-send after ASR.
* User can edit transcript before Send.
* Command result UI supports AI Platform outcomes.
* Needs input is clarification, not error.
* Rejected is controlled outcome with recovery path.

### UAT acceptance

* UAT config is documented.
* Smoke tests prove AI Platform provider usage.
* Persisted entities are verified in DB/API.
* Logs/decision trace prove provider source.
* Fallback smoke is executed.

## Definition of Done

* Gap analysis document completed.
* AI Platform decision endpoint/path/auth verified.
* UAT config documented and applied.
* Happy-path voice command uses `AiPlatformDecisionProvider`.
* Manual provider is not used for UAT happy path.
* At least three end-to-end voice command smokes pass:

  * shopping items;
  * task or needs_input;
  * task-shopping linkage where supported.
* Needs input smoke passes.
* Rejected smoke passes.
* Fallback smoke passes.
* Logs/decision traces prove provider source.
* Parent initiative DoD updated.
* UAT checklist created/updated.
* Changelog created.
* Tests added or explicit manual UAT verification documented.
* Known limitations documented.

## Rollout plan

### Step 1: Local compatibility check

* Run backend tests.
* Verify `AiPlatformDecisionProvider` against mocked AI Platform.
* Verify fallback behavior.

### Step 2: UAT config enablement

Set UAT to AI Platform decisioning:

```dotenv
DECISION_PROVIDER=aiplatform
DECISION_FALLBACK_ENABLED=true
AI_PLATFORM_URL=http://10.0.0.5
AI_PLATFORM_DECISION_PATH=/v1/decide
AI_PLATFORM_API_KEY=
```

### Step 3: UAT smoke

Run all smoke scenarios in this initiative.

### Step 4: UAT acceptance

Accept only if:

* ASR works;
* transcript is editable;
* confirmed command uses AI Platform;
* domain entities are persisted;
* UI renders structured outcome;
* fallback is controlled.

### Step 5: Parent initiative status update

Update parent initiative from partial to complete only after this follow-up passes.

## Risks

### Risk: AI Platform decision endpoint contract mismatch

Mitigation:

* compare current upstream contract with HomeTusk integration package;
* keep adapter changes minimal;
* add schema validation tests.

### Risk: UAT environment points to wrong path

Mitigation:

* make decision path explicit;
* document health/smoke command;
* log effective provider/path at safe level without secrets.

### Risk: fallback hides AI Platform failure

Mitigation:

* include `fallbackUsed` or provider source in trace/logs;
* UAT happy path must explicitly prove non-fallback AI Platform decision.

### Risk: frontend appears complete while backend is manual

Mitigation:

* add UAT gate requiring provider source verification;
* update parent initiative DoD.

### Risk: voice metadata changes command contract incompatibly

Mitigation:

* metadata must be optional and backward-compatible;
* if unsupported, document traceability gap instead of breaking contract.

## Open questions

1. What is the real UAT AI Platform decision endpoint path?
2. Does UAT require API key or service-to-service auth for decisioning?
3. Does current `AiPlatformDecisionProvider` already support the latest upstream decision contract?
4. Does command response include provider source, or is it only in logs/DecisionLog?
5. Does current frontend need changes to render AI Platform `needs_input` and `rejected`, or is backend response already normalized?
6. Should `source=voice` and `asrTraceId` be persisted in Command/DecisionLog in this follow-up?
7. Is fallback to manual provider acceptable for production, or only for UAT/local?

## Codex Plan Mode prompt

Use this prompt when picking up this initiative:

```text
Start in Plan Mode.

Roadmap initiative:
docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp-followup-agentic-uat-enablement.md

We have a scope gap in Voice Command Chat MVP.

Current state:
- ASR/voice transcript flow is implemented.
- User can record voice and get editable transcript.
- User can manually send transcript into Commands.
- UAT still uses DECISION_PROVIDER=manual.
- Therefore confirmed voice commands are not processed through AI Platform agents/decisioning.

Expected product outcome:
voice -> ASR -> editable transcript -> user Send -> /api/v1/commands -> AI Platform Decision Provider -> schema validation -> guardrails -> action execution -> persisted HomeTusk entities -> structured UI result.

Important boundary:
Do NOT make /api/v1/voice/transcriptions call /v1/decide.
ASR endpoint must remain transcription-only.
Agentic processing must happen after user confirms transcript and sends command through /api/v1/commands.

Tasks:
1. Review current Voice Command Chat implementation.
2. Review current DecisionProvider configuration and AiPlatformDecisionProvider.
3. Review current AI Platform integration docs/contracts.
4. Identify what prevents UAT from using AI Platform decisioning.
5. Produce a corrective implementation plan to complete agentic voice command processing on UAT.

Plan must cover:
- required env/config changes;
- decision endpoint path and auth verification;
- contract compatibility check;
- backend changes if optional metadata source=voice/asrTraceId is needed;
- frontend changes only if current UI does not display AI Platform outcomes correctly;
- smoke tests for voice -> ASR -> Send -> AI decision -> persisted entities;
- fallback tests;
- logging/observability checks proving AI provider vs manual provider;
- docs updates to parent initiative, UAT checklist, service catalog/changelog.

Acceptance:
- ASR-only is not considered done.
- UAT happy path must use AiPlatformDecisionProvider.
- ManualDecisionProvider is allowed only for local dev or explicit fallback.
- No LLM/agent logic inside HomeTusk.
- No direct frontend call to AI Platform.
- No auto-send after ASR.

Do not implement yet.
Return:
- gap analysis;
- implementation plan;
- files to inspect/change;
- test plan;
- UAT verification checklist;
- risks;
- open questions.
```

## Apply prompt

Use after plan review and approval:

```text
APPLY the approved corrective plan for Voice Command Chat agentic UAT enablement.

Goal:
Complete the missing part of Voice Command Chat MVP:
voice -> ASR -> editable transcript -> user Send -> AI Platform Decision Provider -> guardrails -> action execution -> persisted entities -> structured result UI.

Non-negotiable:
- Do not make ASR endpoint execute commands.
- Do not auto-send transcript.
- Do not add LLM/agent logic inside HomeTusk.
- Do not change canonical AI Platform contracts unless explicitly required and documented.
- UAT happy path must use AiPlatformDecisionProvider, not ManualDecisionProvider.
- Manual provider is allowed only for local dev or explicit fallback.

Implementation:
1. Fix/configure DecisionProvider selection for UAT.
2. Verify AI Platform decision path/auth/config.
3. Add/adjust tests proving AI Platform provider is used.
4. Add voice-originated command smoke tests.
5. Ensure frontend renders AI Platform outcomes: executed, needs_input, rejected.
6. Add observability/log markers for provider source.
7. Update parent initiative and UAT checklist so ASR-only cannot pass DoD.
8. Provide changelog, verification commands, risk notes.

Start implementation now.
```

:::
