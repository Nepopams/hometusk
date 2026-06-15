# Workpack: ST-3501 - Mobile Stack ADR and App Foundation

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-035/epic.md`
- Story: `docs/planning/epics/EP-035/stories/ST-3501-mobile-stack-app-foundation.md`
- ADR: `docs/adr/020-native-mobile-client-stack.md`
- Diagram: `docs/diagrams/sequence-mobile-push-deep-link.md`
- Existing web API client/types: `clients/web/src/lib/api.ts`, `clients/web/src/types/api.ts`
- Service catalog: `docs/architecture/service-catalog.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE - GATE D GO.** Gate A/B, artifact gate, PLAN findings, delegated Gate C, APPLY, verification, review gate, and delegated Gate D are complete as of 2026-06-14.

## Outcome

HomeTusk gets a first-class Expo TypeScript native mobile app foundation with documented stack decision, app shell, safe storage boundaries, and local runbook.

## Acceptance Criteria

- [x] AC-1: ADR-020 is accepted and linked from the ADR index.
- [x] AC-2: `clients/mobile` exists as an Expo TypeScript app with package scripts, README, and metadata.
- [x] AC-3: `npm run typecheck` passes in `clients/mobile`.
- [x] AC-4: Sensitive token storage is represented only by a secure-storage boundary; non-sensitive local memory is separate.
- [x] AC-5: Home, Tasks, Shopping, and Command surfaces are visible in the app shell.
- [x] AC-6: No Firebase/Supabase domain backend, direct AI Platform client, PWA, or Capacitor replacement is introduced.

## Implementation Evidence

- `clients/mobile` generated as an Expo SDK 56 TypeScript app.
- `clients/mobile/App.tsx` provides the first HomeTusk mobile shell with Home, Tasks, Shopping, and Command surfaces.
- `clients/mobile/src/config/env.ts` defines the API base URL boundary through `EXPO_PUBLIC_API_BASE_URL`.
- `clients/mobile/src/api/client.ts` defines a HomeTusk-only API client and command submission headers.
- `clients/mobile/src/api/ids.ts` avoids assuming `crypto.randomUUID()` exists on every React Native runtime.
- `clients/mobile/src/storage/secureSessionStore.ts` isolates sensitive session material behind Expo SecureStore.
- `clients/mobile/src/storage/localAppMemory.ts` limits plain local memory to selected household, command draft, and recent command hints.
- `clients/mobile/README.md` and `clients/mobile/AGENTS.md` document run commands and mobile guardrails.
- Expo dev server was started on `http://localhost:8081`; shell `Invoke-WebRequest` returned HTTP 200 with the `HomeTusk Mobile` Expo manifest.

## Non-goals

- Full auth/session flow.
- Backend push/device implementation.
- Real push delivery.
- Full task/shopping mutations.
- Production app store release.

## Files to change

- `docs/adr/020-native-mobile-client-stack.md` - stack, push, local persistence decision.
- `docs/_indexes/adr-index.md` - add ADR-020.
- `docs/diagrams/sequence-mobile-push-deep-link.md` - mobile push/deep-link flow.
- `docs/_indexes/diagrams-index.md` - add mobile diagram.
- `docs/contracts/http/mobile-devices.openapi.yaml` - draft device token contract for later backend work.
- `docs/_indexes/contracts-index.md` - add mobile device contract.
- `docs/architecture/service-catalog.md` - record mobile app as in-development and selected stack.
- `clients/mobile/**` - Expo app foundation.
- `docs/planning/workpacks/ST-3501/**` - PLAN/Gate/evidence updates.

Forbidden for ST-3501:

- `docs/integration/ai-platform/v1/upstream/**`
- Spring Boot runtime code.
- Backend migrations.
- `infra/uat/nginx/Dockerfile`

## Implementation plan

### Commit 1 - Planning, ADR, and contract artifacts

Steps:
1. Record roadmap/Gate A move and execution decomposition.
2. Add ADR-020 and indexes.
3. Add mobile push/deep-link diagram and index.
4. Add draft mobile device registration contract and contract index entry.
5. Update service catalog with mobile app status/stack.

Verification:
- Manual markdown/OpenAPI review.

### Commit 2 - Expo mobile foundation

Steps:
1. Generate `clients/mobile` with Expo TypeScript.
2. Remove generator noise that conflicts with HomeTusk instructions.
3. Add app shell with Home, Tasks, Shopping, and Command surfaces.
4. Add config, API, secure storage, and local memory boundaries.
5. Add README/runbook and mobile `AGENTS.md`.

Verification:
- `cd clients/mobile && npm install`
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

### Commit 3 - Evidence and gates

Steps:
1. Record PLAN findings.
2. Record delegated Gate C.
3. Update checklist/workpack evidence.
4. Run review gate and record Gate D if checks pass.

## Contract impact

ST-3501 adds a draft contract artifact for upcoming ST-3506 only. No runtime backend endpoint is implemented in this story.

## Docs updates

- [x] ADR index updated.
- [x] Diagram index updated.
- [x] Contract index updated.
- [x] Service catalog updated.
- [x] Mobile README/runbook added.

## Tests

- [x] Mobile TypeScript typecheck.
- [x] Expo CLI smoke/help command.
- [x] Source review for secure/local storage boundaries.

## Verification commands

- `cd clients/mobile && npm install`
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

## DoD checklist

- [x] Tests pass or blockers are documented.
- [x] No direct mobile-to-AI-Platform calls.
- [x] No non-HomeTusk domain backend dependency.
- [x] Sensitive token boundary uses secure storage.
- [x] Workpack contains evidence/commands.
- [x] Review gate completed before Gate D.

## Risks

- Expo SDK dependency drift can break generated defaults. Mitigation: use `create-expo-app` defaults and record package versions in lockfile.
- App shell can become decorative without backend boundaries. Mitigation: include API/config/storage boundaries in the first slice.
- Secure storage can be bypassed later. Mitigation: name and document the secure token boundary before auth work.

## Rollback

- Remove `clients/mobile`.
- Revert ADR/index/diagram/contract/service catalog docs.
- No database migration or backend runtime rollback is required for ST-3501.

## Prompt Pack

- PLAN: `docs/planning/workpacks/ST-3501/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3501/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3501/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3501/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3501/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3501/gate-d.md`
