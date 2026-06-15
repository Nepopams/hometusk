# Story: ST-3501 - Mobile Stack ADR and App Foundation

## Status: DONE

**Epic:** EP-035 | **Priority:** P0 | **Points:** 5

Completed on 2026-06-14. Gate A/B, artifact gate, Gate C, APPLY, review gate, and Gate D were delegated by the user goal and recorded in the ST-3501 workpack.

## Description

Create the first-class `clients/mobile` native app foundation after documenting the mobile stack decision. The slice must prove that the selected stack can run as an Android/iOS-capable app, has a clear runbook, and starts with safe boundaries for API access, secure storage, local app memory, and command traceability.

## User Value

HomeTusk gains a concrete native mobile client path instead of keeping mobile as an abstract roadmap item.

## In Scope

- Accept ADR-020 for React Native + Expo + TypeScript.
- Create `clients/mobile` as an Expo TypeScript app.
- Add a mobile README/runbook with local, Android, iOS, and internal testing notes.
- Add mobile-specific `AGENTS.md`.
- Add initial app shell with Home, Tasks, Shopping, and Command surfaces.
- Add API/config boundary placeholders aligned to HomeTusk backend.
- Add secure-storage/local-memory utility boundaries without storing sensitive tokens in plain storage.
- Add verification commands for typecheck/start path.

## Out of Scope

- Full login/logout implementation.
- Real device token backend persistence.
- Push delivery.
- Full task/shopping mutations.
- Direct AI Platform calls.
- PWA/Capacitor wrapper.
- Production store release.

## Acceptance Criteria

### AC-1: Stack Decision Is Recorded
Given the story starts
When implementation begins
Then ADR-020 is accepted and linked from the ADR index.

### AC-2: Mobile App Is First-Class
Given the repository is checked out
When a developer opens `clients/mobile`
Then they see an Expo TypeScript app with its own package scripts, README, and project metadata.

### AC-3: App Shell Runs Through Typecheck
Given dependencies are installed
When `npm run typecheck` runs in `clients/mobile`
Then the TypeScript app shell passes without errors.

### AC-4: Boundaries Are Safe
Given the app foundation is created
When code is inspected
Then sensitive token storage is represented only by a secure-storage boundary
And plain local app memory is restricted to non-sensitive state.

### AC-5: Product Surfaces Are Visible
Given the app starts
When the root screen renders
Then Home, Tasks, Shopping, and Command surfaces are visible as first-slice navigation targets or panels.

### AC-6: No Architecture Escape Hatch
Given the app foundation is created
When dependencies and source are reviewed
Then there is no Firebase/Supabase domain backend, no direct AI Platform client, and no PWA/Capacitor replacement.

## Test Strategy

- `npm install` in `clients/mobile`.
- `npm run typecheck` in `clients/mobile`.
- `npx expo start --help` or equivalent smoke command to verify Expo CLI wiring without requiring a simulator.
- Manual source review for secure storage/local memory boundaries.

## Flags

- contract_impact: no for this story runtime; mobile device contract is prepared separately for ST-3506.
- data_impact: no.
- adr_needed: yes, ADR-020.
- diagrams_needed: yes, mobile push/deep-link sequence diagram.
- security_sensitive: medium, because app foundation defines token/local storage boundaries.
- traceability_critical: medium, because command shell must be prepared to preserve backend correlation/idempotency later.

## Dependencies

- Delegated Gate A/B approval.
- ADR-020 accepted.
- Node/npm available for Expo project setup.

## Gate Notes

- Artifact gate: GO for ADR and diagram before scaffold APPLY.
- Gate C: GO, recorded in `docs/planning/workpacks/ST-3501/gate-c.md`.
- Gate D: GO, recorded in `docs/planning/workpacks/ST-3501/gate-d.md`.
