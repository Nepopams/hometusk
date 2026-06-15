# Initiative: INIT-2026Q3-mobile-client-refactor-foundation — Mobile Client Refactor Foundation

## Status

DONE - delegated Gate D GO on 2026-06-15 for branch
`codex/mobile-client-refactor-foundation`.

## Initiative type

Engineering Enablement / Mobile / Refactoring / Reliability / Future AI-command readiness

## Owner

HomeTusk product engineering team.

## Target milestone

Post Native Mobile MVP / Before Mobile AI Command Center

## Parent / Related initiatives

- Parent baseline: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Future candidate: Mobile AI Command Center / Natural command control layer
- Related architecture:
  - `clients/mobile/`
  - `clients/mobile/App.tsx`
  - `clients/mobile/AGENTS.md`
  - `docs/contracts/http/commands.openapi.yaml`
  - `docs/integration/ai-platform/**`

---

## Sources of Truth

- Product Goal: `docs/planning/strategy/product-goal.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-mobile-client-refactor-foundation.execution.md`
- Epic: `docs/planning/epics/EP-036/epic.md`
- Workpack: `docs/planning/workpacks/ST-3601/workpack.md`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Mobile README: `clients/mobile/README.md`
- Mobile AGENTS: `clients/mobile/AGENTS.md`
- REST Contracts: `docs/contracts/**`
- Service Catalog: `docs/architecture/service-catalog.md`
- Existing Mobile App: `clients/mobile/`
- Existing Command Pipeline: `POST /api/v1/commands`

---

## 1. Problem / Opportunity

Native Mobile MVP is delivered and the app works, but the implementation is intentionally MVP-shaped.

The current mobile client centralizes too much behavior in `clients/mobile/App.tsx`:

- auth/session bootstrap;
- household selection;
- read model loading;
- push/deep-link routing;
- task mutations;
- shopping mutations;
- surface/tab rendering;
- command chat state;
- command request construction;
- command outcome rendering;
- continuation parsing.

This is acceptable for first mobile validation, but it is not a sustainable foundation for the next product direction: mobile-first AI command control.

The risk is not theoretical. Current Command tab behavior is implemented as deterministic text parsing in the mobile client. Plain text becomes `create_task`; `done ...` becomes `complete_task`; continuation accepts generic text or `key=value` pairs. This makes the mobile command surface hard to evolve into a richer AI-command UX without creating fragile condition-heavy code.

This initiative creates a clean mobile feature/module structure before adding new command intelligence.

---

## 2. Outcome (what changes for user)

No intended product behavior change.

User-visible behavior should remain the same:

- login/logout still works;
- selected household persists;
- home/tasks/shopping/command tabs still work;
- task create/complete still works;
- shopping item add/mark/delete still works;
- command submit/continue still works;
- push/deep links still work;
- recent commands still work.

The outcome is internal:

- mobile code becomes easier to reason about;
- command feature becomes isolated;
- future AI-command work can be added without modifying a monolithic `App.tsx`;
- verification and review become smaller and safer.

---

## 3. Scope (Now / Next / Later)

### NOW — Behavior-preserving mobile refactor

#### App shell decomposition

Extract high-level application responsibilities out of `App.tsx` into focused modules.

Expected target shape may be adjusted by Codex after repository inspection, but should follow this direction:

```text
clients/mobile/src/app/
  AppShell.tsx
  surfaces.ts
  types.ts

clients/mobile/src/features/auth/
  AuthScreen.tsx
  authSessionController.ts

clients/mobile/src/features/households/
  HouseholdSwitcher.tsx
  selectedHouseholdStore.ts

clients/mobile/src/features/home/
  HomeSurface.tsx

clients/mobile/src/features/tasks/
  TasksSurface.tsx
  taskMutations.ts

clients/mobile/src/features/shopping/
  ShoppingSurface.tsx
  shoppingMutations.ts

clients/mobile/src/features/command/
  CommandSurface.tsx
  CommandComposer.tsx
  CommandOutcomeCard.tsx
  CommandContinuationCard.tsx
  commandRequestBuilder.ts
  commandHistoryStore.ts
  commandTypes.ts

clients/mobile/src/features/notifications/
  notificationRouting.ts
  pushRegistrationController.ts

clients/mobile/src/shared/ui/
  LabeledInput.tsx
  StatusBanner.tsx
  MutationFeedback.tsx
  DataSurface.tsx
  SectionList.tsx
  InfoRow.tsx
  LoadingPanel.tsx
  ErrorPanel.tsx

clients/mobile/src/shared/format/
  dates.ts
  ids.ts
  labels.ts

clients/mobile/src/shared/errors/
  apiErrorFormatting.ts
  Codex may choose a smaller split if justified, but must extract the Command feature at minimum.

Command feature isolation

Move all command-specific logic out of App.tsx:

CommandSurface;
command composer/input;
command outcome card;
continuation UI;
buildCommandRequestFromText;
parseContinuationInput;
recent command hint storage wrapper;
command-specific error handling if separable.

The refactor must preserve current command behavior exactly unless a bug is found and explicitly documented.

UI component extraction

Extract repeated UI primitives into shared/ui:

labeled input;
status banner;
mutation feedback;
data panel/surface;
section list;
info row;
loading panel;
error panel.

No visual redesign.

State and side-effect cleanup

Separate pure functions from side effects:

command request building should be pure and unit-testable;
formatting helpers should be pure;
API side effects should stay in feature controllers/hooks/helpers;
storage access should be isolated behind small functions.
Verification

Add or preserve smoke-level verification for:

TypeScript compilation;
command request builder;
continuation parser;
command outcome formatting;
app boot path where practical.

If test infra is not present, Codex must at least add pure-function tests only if lightweight and supported by current stack; otherwise document manual verification and keep npm run typecheck as blocking.

NEXT — Mobile AI-command UX foundation

Not in this initiative.

Potential future work:

first-class natural_command contract;
richer structured result cards;
guided needs_input chips/forms;
command confirmation cards;
mobile voice draft using existing ASR BFF;
decision source/degraded trace display;
command timeline.
LATER — Agent capability and AI Platform review

Before implementing advanced AI control, run a separate discovery/spike initiative for AI Platform agent capabilities.

The current concern is that platform agents may be functionally weak for the target product loop. This must be assessed before HomeTusk commits to a richer AI-command UX.

Potential future discovery topics:

supported household intents;
multi-action planning quality;
clarification quality;
task ↔ shopping linkage quality;
scheduling/deadline normalization;
member/zone/list grounding;
guardrails compatibility;
explainability and trace payloads;
confidence semantics;
regression test harness for agent decisions.
4. In Scope (explicit)
Mobile refactor only.
TypeScript/module restructuring.
Extract command feature from App.tsx.
Extract reusable UI components.
Extract pure command request/continuation helpers.
Preserve current API calls and contracts.
Preserve current UX behavior.
Update mobile README if module structure or local commands change.
Update clients/mobile/AGENTS.md if new mobile boundaries or conventions are introduced.
Add lightweight tests only where they fit current tooling.
5. Out of Scope (explicit)
No new backend endpoints.
No REST contract changes.
No AI Platform contract changes.
No new command semantics.
No natural_command implementation.
No mobile voice input.
No generic assistant chat.
No direct mobile call to AI Platform.
No Firebase/Supabase/domain backend addition.
No navigation library migration unless Codex proves it is necessary for the refactor.
No visual redesign.
No push provider change.
No offline mutation sync.
No large dependency introduction without ADR or explicit justification.
6. Assumptions
Existing mobile behavior is the baseline.
HomeTusk backend remains the source of truth.
Mobile remains a native React Native + Expo client.
Sensitive session data remains in SecureStore.
AsyncStorage remains allowed only for non-sensitive app memory.
Command execution remains through HomeTusk backend /commands.
The refactor should reduce risk before adding AI-command capabilities.
Current AI Platform/agent functionality is not assumed to be sufficient for the next product step.
7. Success Metrics
Engineering
clients/mobile/App.tsx is substantially smaller and mostly acts as app composition/root orchestration.
Command feature has its own module/folder.
UI primitives are reused instead of being embedded in App.tsx.
Command request building is isolated and testable.
TypeScript passes.
Product safety
No intended user-visible behavior regressions.
Command tab still supports:
plain text create task;
done/complete ...;
controlled outcomes;
needs_input continuation.
Push/deep-link handoff still works.
Secure session behavior is unchanged.
Future readiness
A future AI-command initiative can modify src/features/command/** without touching unrelated auth, shopping, push, or household code.
Future agent capability review can be done independently of mobile refactor.
8. Constraints / Guardrails
Behavior-preserving refactor: any behavior change must be explicitly documented as a bug fix.
Contract-first: do not modify API contracts in this initiative.
No direct AI Platform calls from mobile.
No LLM/agent logic in mobile.
No new backend-as-a-service.
No sensitive tokens in AsyncStorage/plain storage.
Keep dependency changes minimal.
Keep commits small and reviewable.
Prefer pure helper extraction before UI behavior changes.
Avoid premature architecture: no over-engineered state framework unless necessary.
Do not convert the app into a multi-navigation rewrite unless separately approved.
9. Dependencies
Internal
clients/mobile/App.tsx
clients/mobile/src/api/client.ts
clients/mobile/src/api/types.ts
clients/mobile/src/storage/localAppMemory.ts
clients/mobile/src/storage/secureSessionStore.ts
clients/mobile/src/notifications/pushNotifications.ts
clients/mobile/README.md
clients/mobile/AGENTS.md
External
Current Expo / React Native / TypeScript toolchain.
Existing npm run typecheck.

No backend dependency expected.

10. Risks & Mitigations
Risk	Impact	Mitigation
Refactor accidentally changes mobile behavior	HIGH	Behavior-preserving scope, typecheck, manual smoke checklist
Codex over-splits into excessive architecture	MEDIUM	Explicit target: focused feature folders, no new framework
Command behavior regresses	HIGH	Extract command builder as pure function and verify existing cases
Push/deep-link routing breaks	HIGH	Keep notification routing extraction minimal and smoke-test deep links
Auth/session behavior breaks	HIGH	Avoid changing secure session semantics
Refactor becomes hidden AI-command implementation	HIGH	AI-command semantics explicitly out of scope
New dependencies add maintenance burden	MEDIUM	No new production dependency without justification
11. Codex Decomposition Policy

After Human Gate A approval, Codex must create downstream execution artifacts itself:

execution index;
epic/story decomposition if needed;
workpacks;
checklists;
PLAN/APPLY/REVIEW prompts.

Human-written decomposition is intentionally not included here.

Codex must begin in Plan Mode and inspect the current repository before implementation.

12. Exit Criteria

This initiative is complete when:

App.tsx is reduced to app-level orchestration/composition.
Command feature is extracted under clients/mobile/src/features/command/.
Auth, household, home, tasks, shopping, notifications/push responsibilities are no longer all embedded directly in App.tsx.
Shared UI primitives are extracted where practical.
Command request builder is isolated from UI rendering.
Continuation parser is isolated from UI rendering.
Existing command behavior is preserved.
Existing task/shopping mutations still work.
Existing push/deep-link behavior still works.
Secure session behavior is unchanged.
AsyncStorage remains non-sensitive only.
npm run typecheck passes.
Mobile README or AGENTS is updated if structure changes.
No backend/API/AI Platform contract changes are made.
No new AI-command semantics are introduced.
13. Flags
Flag	Value	Notes
contract_impact	no	Behavior-preserving mobile refactor
adr_needed	maybe	Only if Codex introduces a new mobile architecture convention or dependency
diagrams_needed	no	Optional lightweight module map only
security_sensitive	medium	Must not alter token/session storage semantics
traceability_critical	medium	Command flow must preserve idempotency/correlation behavior
backend_impact	no	Backend changes out of scope
ai_platform_impact	no	AI Platform changes out of scope
14. Anti-Scope-Creep

DO NOT:

implement natural_command;
redesign command UX;
add mobile voice input;
call AI Platform from mobile;
change /commands contract;
change backend command pipeline;
add generic assistant behavior;
add new backend service;
add Firebase/Supabase;
migrate to a new navigation/state framework without explicit justification;
add offline mutation sync;
change push provider;
change auth/session strategy;
hide behavior changes inside refactor.
15. Next Step After Gate A

Codex should receive a planning prompt:

read this initiative;
inspect clients/mobile/App.tsx and related mobile modules;
propose a behavior-preserving refactor plan;
identify exact files to create/move/modify;
define verification steps;
produce PLAN only;
do not modify files until Human Gate C.
