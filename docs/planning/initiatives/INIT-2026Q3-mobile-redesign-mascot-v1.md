# Initiative: INIT-2026Q3-mobile-redesign-mascot-v1 — Mobile Redesign + Mascot Integration v1

## Status

Draft (to be approved at Human Gate A)

## Initiative type

Mobile UX / Visual System / Mascot Integration / Product Polish / Contract-Safe Client Redesign

## Owner

HomeTusk product engineering team.

## Target milestone

MVP Closure / Mobile Track, after Mobile AI Command UX v1 smoke and before production rollout.

## Parent / Related initiatives

- Mobile AI Command UX v1: `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.md`
- Natural Command + Needs Confirmation Backend Contract: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- AI Platform 2.1 Contract Intake: `docs/planning/initiatives/INIT-2026Q3-ai-platform-2-1-contract-intake.md`
- Native Mobile MVP: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Mobile Client Refactor Foundation: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.md`
- Mascot design exploration: external Pencil/Figma board and approved static mascot direction
- Future candidate: mascot motion layer
- Future candidate: production rollout / feature-flag enablement
- Future candidate: read-only `answered` / status-query UX

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
- Mobile home feature: `clients/mobile/src/features/home/**`
- Mobile shared UI/styles: `clients/mobile/src/shared/ui/**`
- Mobile app shell: `clients/mobile/src/app/**`
- Mobile assets: `clients/mobile/assets/**`

### Backend contract dependency, read-only

- Commands OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- Backend contract initiative: `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- Mobile AI Command UX v1 execution: `docs/planning/initiatives/INIT-2026Q3-mobile-ai-command-ux-v1.execution.md`

### Design input

Approved design input is external to repo and should be treated as implementation guidance:

- Mobile / MVP Screens / Home v1
- Mobile / MVP Screens / Command v1
- Mobile / Components
- Handoff / Codex Specs / Mobile v1
- Mascot static direction: small warm household helper in orange hood/cloak with home symbol

Do not infer implementation details from blank overview boards. Use the approved Home v1, Command v1, and Handoff/Codex Specs board as the primary design source.

---

## 1. Problem / Opportunity

HomeTusk now has a working mobile AI-command flow:

- mobile sends typed text as `natural_command`;
- backend returns controlled outcomes;
- mobile handles `executed`, `executed_degraded`, `scheduled`, `needs_input`, `needs_confirmation`, and `rejected`;
- pending confirmations can be approved or cancelled through HomeTusk backend endpoints.

The current mobile client is functionally adequate but still feels like an engineering MVP:

- the Command tab does not yet feel like the central household command surface;
- outcome cards are technically correct but visually generic;
- confirmation state needs stronger trust signaling;
- recent commands look like technical history rather than household activity;
- Home does not yet feel like a calm household headquarters;
- mascot is not integrated as a product state explainer.

The product opportunity is to apply the approved visual system and mascot direction without changing business logic ownership:

```text
mobile becomes clearer, warmer, and more trustworthy
while backend remains the source of truth
and AI Platform remains external.
```

---

## 2. Outcome

The mobile app presents HomeTusk as a modern household command center:

- Command is the primary natural-language action surface;
- Home is a calm overview/routing surface;
- command outcomes are visually coherent and state-specific;
- confirmation clearly communicates that no action happened before explicit approval;
- mascot appears as a small state-bound helper, not as the main interaction;
- static mascot fallback works without motion;
- existing AI-command behavior remains unchanged.

This initiative does not introduce new backend semantics or new product flows.

---

## 3. Scope (Now / Next / Later)

### NOW — Static mobile redesign + mascot integration

#### 3.1 Visual tokens

Apply mobile visual tokens from approved handoff:

```text
background: warm cream
surfaces: warm white / elevated soft white
text: warm charcoal / secondary warm gray
primary action: soft teal
state success: soft green
state confirmation: amber
state needs_input: blue-gray
state rejected: muted red
state degraded: neutral amber-gray
radius: 24 hero cards, 16–22 cards, 999 pills/buttons
spacing: 14–20 screen padding, 8–16 internal gaps, compact rows
elevation: gentle shadow only on hero/priority cards
```

Codex may map these into existing React Native style objects rather than introducing a token engine if that is simpler.

#### 3.2 Mascot assets and component

Introduce a static mascot component with state mapping.

Required asset names:

```text
mascot_idle.png
mascot_hello.png
mascot_thinking.png
mascot_success.png
mascot_confirm.png
mascot_confused.png
mascot_reject.png
mascot_degraded.png
```

Expected repo location:

```text
clients/mobile/assets/mascot/
```

If final static assets are not present, Codex may:

- create a `Mascot` component with deterministic fallback UI;
- document expected filenames and sizes;
- keep layout implementation unblocked.

Do not redraw the mascot in code. Do not generate new mascot art in this initiative.

#### 3.3 Mascot state mapping

Use the mascot as a state explainer only:

```text
idle -> mascot_idle.png
home welcome -> mascot_hello.png
thinking / sending -> mascot_thinking.png
executed -> mascot_success.png
needs_input -> mascot_confused.png
needs_confirmation -> mascot_confirm.png
rejected -> mascot_reject.png
degraded -> mascot_degraded.png
```

Rules:

- mascot never acts as a button;
- mascot never replaces text explanation;
- mascot does not appear in every row;
- mascot placement must stay lightweight;
- state must be understandable without animation.

#### 3.4 Command v1 redesign

Redesign Command tab according to approved Command v1 board.

Required states:

- idle / default;
- thinking while command is being submitted;
- executed;
- needs_input;
- needs_confirmation;
- rejected;
- degraded;
- scheduled if returned.

Preserve behavior:

- command composer still sends `natural_command`;
- `needs_input` continuation remains separate from `needs_confirmation`;
- `needs_confirmation` approve/cancel still go through HomeTusk backend;
- no local execution of proposed actions;
- no raw provider payload displayed.

#### 3.5 Home v1 redesign

Redesign Home as overview/routing surface.

Home should show, where data is available:

- household title;
- today summary;
- tasks/zones/shopping counts;
- important household items;
- pending confirmation summary, if current in-session data exists;
- recent change / recent command hint;
- quick routes to Command / Tasks / Shopping.

Home is not a command composer in this initiative.

If durable pending confirmation read model is not available, do not fake it. Show in-session state only or document the gap.

#### 3.6 Outcome cards and recent commands

Redesign mobile cards:

- `CommandHeroCard` / state hero;
- `CommandOutcomeCard`;
- `CommandConfirmationCard`;
- `RecentCommandRow`;
- `StatusBanner`;
- `EmptyState`;
- `StatePill`;
- `EntityChip`;
- `SoftCard`;
- `PrimaryButton` / `SecondaryButton`.

User-facing language must be household/product language:

- no audit/confidence/inference/pipeline wording;
- no technical fallback/provider details;
- no raw provider payload;
- no internal stack traces.

#### 3.7 Empty states

Introduce empty-state UI for:

- empty household/home;
- empty tasks;
- empty shopping;
- empty command history;
- optional pending confirmation empty state.

Optional asset names:

```text
empty_tasks.png
empty_shopping.png
empty_commands.png
```

If assets are missing, use static mascot fallback or plain card copy.

#### 3.8 Documentation and smoke

Update:

- `clients/mobile/README.md`;
- mobile smoke checklist;
- initiative execution notes;
- roadmap only if this initiative is explicitly activated as NOW.

---

### NEXT — Optional static asset polish / smoke hardening

Potential follow-up after implementation:

- replace placeholder mascot assets with final exports;
- run Android/iOS smoke;
- verify reduced-motion behavior if animations are added later;
- improve durable pending confirmation restore only after backend read model exists.

### LATER — Motion layer

Optional later initiative:

- subtle idle animation;
- thinking pulse;
- success bounce;
- confirmation attention motion;
- reduced motion setting;
- animation must never carry meaning alone.

---

## 4. In Scope

- Mobile visual redesign.
- Command v1 redesign.
- Home v1 redesign.
- Static mascot component and state mapping.
- State-specific command cards.
- Recent command row redesign.
- Empty states.
- Shared visual tokens / style updates.
- Mobile README / smoke docs.
- Mobile-only typecheck verification.

---

## 5. Out of Scope

- Backend changes.
- OpenAPI changes.
- AI Platform changes.
- Direct mobile → AI Platform integration.
- New command lifecycle states.
- `answered` / status-query UX.
- New command execution business logic.
- Native mobile voice capture if not already present.
- Production rollout/config changes.
- Mascot redraw or new mascot concept generation.
- Motion-first implementation.
- Undo/rollback UX unless explicitly supported server-side.
- Mascot-driven interaction model.

---

## 6. Cross-repo Ownership

This initiative is HomeTusk-owned and committed only in the HomeTusk repository.

No AI Platform repository files may be modified.

Mobile consumes existing HomeTusk backend contracts only.

---

## 7. Assumptions

- Mobile AI Command UX v1 is merged and usable.
- Backend `natural_command + needs_confirmation` contract is available.
- Current smoke issues such as session persistence are separate bugs and should not be hidden inside this redesign unless they directly block mobile visual testing.
- Mascot direction is approved and should not be redrawn by Codex.
- Static assets are preferred before motion.
- HomeTusk remains household productivity software, not a game.

---

## 8. Success Metrics

This initiative succeeds when:

1. Command screen matches approved Command v1 direction.
2. Home screen matches approved Home v1 direction.
3. Mascot appears in approved placements and states.
4. `natural_command` submission still works.
5. `needs_confirmation` still requires explicit approve/cancel.
6. UI clearly says no action happened before approval.
7. `needs_input` remains visually and functionally distinct from `needs_confirmation`.
8. Rejected/degraded states are user-safe and non-technical.
9. Recent commands are readable household activity.
10. Tasks and Shopping surfaces remain usable.
11. Static fallback works without animation.
12. Mobile typecheck passes.
13. No backend/OpenAPI/AI Platform files are changed.

---

## 9. Constraints / Guardrails

- Preserve current mobile AI-command behavior.
- Do not reinterpret backend status locally.
- Do not execute proposed actions locally.
- Do not overload `needs_input` as confirmation.
- Do not call AI Platform directly from mobile.
- Do not expose raw provider payload, prompts, credentials, audit internals, confidence, pipeline details, or stack traces.
- Keep mascot supportive and state-bound.
- Keep Command UX primary.
- Keep Home as overview/routing.
- Prefer static assets over animation for v1.

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mascot overuse makes product feel childish | HIGH | Strict placement rules; no mascot in dense lists; no mascot-as-avatar pattern |
| Redesign breaks natural command flow | HIGH | Typecheck and smoke; preserve existing request builder and API calls |
| Confirmation trust copy becomes weaker | HIGH | Required no-action-yet copy and explicit approve/cancel UX |
| Codex rewrites backend/API to match design | HIGH | Backend/OpenAPI/provider files forbidden |
| Motion becomes required for meaning | MEDIUM | Static state must carry meaning; motion later only |
| Blank overview boards confuse implementation | MEDIUM | Use approved Home v1, Command v1, and Handoff board only |
| Asset gap blocks implementation | LOW | Mascot component supports fallback placeholders |
| Recent commands become decorative and less useful | MEDIUM | Preserve status, command text, and readable outcome labels |

---

## 11. Expected Files

Codex must inspect actual repo conventions first, but likely files include:

```text
docs/planning/initiatives/INIT-2026Q3-mobile-redesign-mascot-v1.md
docs/planning/initiatives/INIT-2026Q3-mobile-redesign-mascot-v1.execution.md

docs/planning/workpacks/INIT-2026Q3-MOBILE-REDESIGN-MASCOT-V1/**

clients/mobile/README.md
clients/mobile/docs/release-smoke-ai-command.md
clients/mobile/src/shared/ui/styles.ts
clients/mobile/src/shared/ui/Mascot.tsx
clients/mobile/src/shared/ui/EmptyState.tsx
clients/mobile/src/features/command/CommandSurface.tsx
clients/mobile/src/features/command/CommandComposer.tsx
clients/mobile/src/features/command/CommandOutcomeCard.tsx
clients/mobile/src/features/command/CommandConfirmationCard.tsx
clients/mobile/src/features/command/commandOutcomeFormatting.ts
clients/mobile/src/features/home/**
clients/mobile/src/app/surfaces.ts
clients/mobile/assets/mascot/**
```

Do not modify:

```text
services/backend/**
docs/contracts/http/commands.openapi.yaml
vr_ai_platform/**
```

If backend/API changes appear necessary, stop and create a HOLD recommendation instead of applying them.

---

## 12. Exit Criteria

The initiative is complete when:

1. All expected implementation or fallback artifacts are present.
2. Static mascot component and state mapping exist.
3. Command surface follows approved Command v1 direction.
4. Home surface follows approved Home v1 direction.
5. Confirmation card clearly states no action happened yet.
6. Approve/cancel still work through existing mobile handlers.
7. Empty states and recent commands are redesigned or documented if deferred.
8. Mobile README/smoke docs are updated.
9. `cd clients/mobile && npm run typecheck` passes.
10. Scope scan confirms no backend/OpenAPI/AI Platform changes.
11. Residual risks and next steps are recorded.

---

## 13. Flags

| Flag | Value | Notes |
|------|-------|-------|
| contract_impact | no | Consumes existing backend contract only |
| backend_impact | no | Explicitly forbidden |
| mobile_impact | yes | Main scope |
| ai_platform_impact | no | Explicitly forbidden |
| design_system_impact | yes | Visual tokens and shared UI primitives |
| asset_impact | yes | Mascot static assets/fallbacks |
| security_sensitive | medium | Confirmation UX must not misrepresent execution state |
| traceability_critical | medium | Command/confirmation ids should remain visible enough for support |
| adr_needed | no | No architecture boundary change expected |
| diagrams_needed | no | Design boards are source of visual mapping |
| cross_repo | no | HomeTusk only |

---

## 14. Anti-Scope-Creep

DO NOT:

- change backend runtime;
- change public OpenAPI;
- change AI Platform;
- add direct mobile-to-AI calls;
- implement `answered`;
- add production rollout/config;
- add new command lifecycle states;
- add local AI logic;
- execute proposed actions locally;
- make mascot a button or chatbot avatar;
- hide natural command text inside `create_task.title`;
- reduce confirmation safety copy;
- use animation as the only state indicator.

---

## 15. Next Step After Gate A

Codex should:

1. Read this initiative.
2. Read current mobile command/home/shared UI code.
3. Read approved design handoff board/specs.
4. Produce PLAN with:
   - current component map;
   - target component map;
   - exact files;
   - asset strategy;
   - state mapping;
   - verification commands;
   - risks and stop conditions.
5. Do not APPLY before Gate C.
