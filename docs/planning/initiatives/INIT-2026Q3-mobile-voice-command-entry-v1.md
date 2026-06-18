# Initiative: INIT-2026Q3-mobile-voice-command-entry-v1 — Mobile Voice Command Entry v1

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Mobile UX / Voice Input / Command Surface Extension / Contract-Reuse Client Work / Product Trust

## Owner

HomeTusk product engineering team.

## Target milestone

MVP Closure / Mobile Track, after Mobile Redesign + Mascot Integration v1 and before production rollout.

## Parent / Related initiatives

- Mobile Redesign + Mascot Integration v1: `docs/planning/initiatives/INIT-2026Q3-mobile-redesign-mascot-v1.md`
- Mobile AI Command UX v1: `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- Voice Command Chat MVP (closed runtime baseline): `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- AI Command Artifact Gate: `docs/planning/initiatives/INIT-2026Q3-ai-command-artifact-gate.md`
- Native Mobile MVP: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- ADR-020 Voice Command Chat ASR BFF: `docs/adr/020-voice-command-chat-asr-bff.md`
- Voice transcription contract: `docs/contracts/http/voice-transcriptions.openapi.yaml`

---

## Sources of Truth

### Product / roadmap

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Workflow: `docs/CODEX-WORKFLOW.md`

### Mobile implementation

- Mobile README: `clients/mobile/README.md`
- Mobile source: `clients/mobile/src/**`
- Mobile command feature: `clients/mobile/src/features/command/**`
- Mobile shared UI/styles: `clients/mobile/src/shared/ui/**`
- Mobile app shell: `clients/mobile/src/app/**`
- Mobile assets: `clients/mobile/assets/**`

### Backend contract dependency, read-only

This initiative reuses the accepted voice backend/API baseline. It does not open a new backend delivery scope.

- ADR-020 Voice Command Chat ASR BFF: `docs/adr/020-voice-command-chat-asr-bff.md`
- Voice transcription contract: `docs/contracts/http/voice-transcriptions.openapi.yaml`
- Voice Command Chat MVP initiative (closed baseline): `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- AI Platform ASR mapping: `docs/integration/ai-platform/v1/mapping/hometusk-asr-bff.md`

### Design input

Approved design input is external to repo and should be treated as implementation guidance:

- `09 Discovery / Voice Command Entry`
- `10 Mobile / MVP Screens / Voice v1`
- `11 Handoff / Codex Specs / Voice v1`
- Approved mobile visual system from Command v1 / Home v1 / Empty States v1

Do not infer behavior from raw discovery variants once `Voice v1` mocks are approved. Use the approved `Voice v1` screens and handoff board as the primary design source.

---

## 1. Problem / Opportunity

HomeTusk mobile already has a command-first UX and a controlled backend command lifecycle, but command entry remains text-first in the client. This creates friction in common household situations:

- user is moving around the house;
- user has busy hands;
- user wants to capture a short household command faster than typing it.

The product opportunity is to add a microphone entry point without breaking the existing command model:

```text
voice becomes another way to enter a command
while backend remains the source of truth
and result handling stays inside the existing Command flow.
```

This initiative must not turn HomeTusk into a separate voice assistant or generic chat UI.

---

## 2. Outcome

The mobile app supports a bounded voice entry flow inside Commands:

- user sees a secondary mic entry point on the Command screen;
- mic opens a short recording sheet;
- user can start, stop, cancel, retry, or send the recording;
- audio is sent to the existing HomeTusk voice transcription backend path;
- backend returns an editable transcript draft;
- user explicitly sends the command through the existing command flow;
- the UI returns into normal command outcomes:
  - executed;
  - needs_input;
  - needs_confirmation;
  - rejected;
  - degraded.

This initiative does not introduce new backend semantics or a new voice-specific command lifecycle.

---

## 3. Scope (Now / Next / Later)

### NOW — Mobile Voice Command Entry v1

#### 3.1 Command-screen entry point

Introduce a microphone entry point on the Command screen.

Rules:

- mic is visible as a secondary action;
- typed input remains the primary command path;
- mic must not compete with the main `Отправить` action;
- the command screen remains the main natural-language surface.

Preferred pattern from discovery:

```text
mic entry from Command -> recording sheet -> review before send -> processing -> existing Command flow
```

Do not use press-and-hold as the primary MVP pattern.

#### 3.2 Recording sheet / modal

Introduce a lightweight recording surface opened from the mic action.

Required states:

- ready to record;
- active recording;
- review before send;
- processing;
- permission denied;
- recording failed / upload failed / unclear speech.

Rules:

- no assistant personality layer;
- no live waveform editor for MVP;
- no streaming transcript;
- no separate navigation item;
- no always-listening behavior.

#### 3.3 Return to existing Command flow

After the transcript draft is created, the product must return to the standard command path.

Preserve behavior:

- ASR only creates a draft;
- command execution happens only after explicit user Send;
- `needs_confirmation` still requires explicit approve/cancel;
- no auto-send after transcription;
- no separate voice-result branch.

#### 3.4 Voice-specific failure and trust states

Required mobile handling:

- microphone permission denied;
- recording failed;
- upload / transcription failed;
- could not parse safely;
- fallback to text from each failure point.

User-facing language must stay product/household-oriented:

- no audit/confidence/inference/pipeline wording;
- no provider details;
- no raw transcript logging language;
- no AI Platform naming in UI.

#### 3.5 Asset and component work

This initiative may introduce light UI components only where needed for the approved voice flow.

Expected candidates:

- `VoiceEntryButton`
- `VoiceRecordingSheet`
- `VoiceReviewCard`
- `VoiceErrorState`
- reuse of `CommandOutcomeCard`, `CommandConfirmationCard`, `StatusBanner`, `PrimaryButton`, `SecondaryButton`

If mascot placement is present in approved voice mocks, it must remain lightweight and supportive only.

### NEXT — Optional improvements after v1

- inline mic affordance refinement after MVP learnings;
- transcript edit affordance polishing;
- better rate-limit / timeout messaging polish;
- optional reduced-motion refinement.

### LATER — Explicitly not in this initiative

- streaming ASR;
- wake word / always-listening;
- TTS / spoken responses;
- voice entry on all screens;
- long-form voice notes;
- live conversational voice mode;
- direct mobile -> AI Platform integration;
- local AI/ASR logic in mobile.

---

## 4. Backend dependency posture

Backend work is treated as an accepted dependency, not as new delivery scope in this initiative.

The mobile client must reuse:

```http
POST /api/v1/voice/transcriptions
```

and then continue through:

```http
POST /api/v1/commands
```

Required validation before implementation starts:

- React Native recorder/export format matches the backend media allowlist;
- mobile multipart auth works for the voice endpoint;
- mobile can carry safe voice metadata into the command flow where supported (`source=voice`, `asrTraceId`);
- error mapping is sufficient for the approved mobile failure states.

If gaps are found, open a narrow compatibility work item rather than a new broad backend initiative.

---

## 5. Non-goals

- No new backend voice initiative.
- No changes to ADR-020 unless a separate initiative approves them.
- No direct mobile -> AI Platform calls.
- No auto-send after ASR.
- No auto-execution from transcript draft.
- No voice-specific domain semantics.
- No generic AI assistant or voice assistant mode.
- No persistent raw audio storage decisions inside this initiative.
- No raw transcript logging in client UX.
- No production rollout / flag enablement work.

---

## 6. Delivery shape

### Deliverable A — Approved voice mocks

Approved mobile boards:

- `10 Mobile / MVP Screens / Voice v1`
- `11 Handoff / Codex Specs / Voice v1`

### Deliverable B — Mobile implementation

Expected implementation areas:

- `clients/mobile/src/features/command/**`
- `clients/mobile/src/shared/ui/**`
- `clients/mobile/assets/**`
- `clients/mobile/docs/**`

### Deliverable C — Validation notes

Short compatibility note/checklist covering:

- supported recorder format;
- max size / duration assumptions;
- multipart auth path;
- command metadata reuse;
- mobile handling of `400/413/415/429/502/504` voice endpoint errors.

---

## 7. Risks

### Risk 1 — Voice becomes a separate product mode

This would split the command system and create a second UX branch.

**Mitigation:** enforce the rule that voice ends in the same command/result surfaces as text.

### Risk 2 — Text command UX gets weakened

Mic could visually dominate the composer.

**Mitigation:** keep mic secondary and preserve typed input as the primary safety path.

### Risk 3 — Permission/privacy confusion

Users may not understand when recording starts and what happens to it.

**Mitigation:** explicit start/stop/send/cancel model and clear fallback to text.

### Risk 4 — Mobile/backend incompatibility

Recorder format or multipart behavior may not align with the accepted backend contract.

**Mitigation:** run a compatibility validation checklist before coding full integration.

### Risk 5 — Scope creep into streaming or assistant behavior

**Mitigation:** strict out-of-scope list and no new runtime behavior beyond approved mocks.

---

## 8. Acceptance criteria

This initiative is complete when:

- approved `Voice v1` mobile mocks exist;
- approved `Voice v1` Codex handoff board exists;
- mobile has a mic entry point on the Command screen;
- mobile supports ready / recording / review / processing / failure states;
- text fallback exists from every failure path;
- voice returns into the standard command result flow;
- no direct mobile -> AI Platform call exists;
- no separate voice assistant mode is introduced;
- a compatibility validation note exists for recorder/media/auth/error mapping;
- static mobile QA / smoke notes are updated.

---

## 9. Human Gate A checklist

Approve only if:

- approved Pencil `Voice v1` mocks are available;
- team confirms reuse of accepted backend voice contract;
- team agrees that voice is an input modality, not a new assistant mode;
- mobile implementation will not start before compatibility validation;
- out-of-scope list is accepted as binding.

---

## 10. Recommended next steps

1. Finalize `10 Mobile / MVP Screens / Voice v1`
2. Create `11 Handoff / Codex Specs / Voice v1`
3. Add a short compatibility validation note for mobile/backend voice path
4. Start mobile implementation against the accepted backend contract
5. Run smoke verification and keep text command flow as fallback
