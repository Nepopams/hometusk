# Gate C - ST-3501 Mobile Stack ADR and App Foundation

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- PLAN findings recorded in `docs/planning/workpacks/ST-3501/plan-findings.md`.
- ADR-020 records the mobile stack, push strategy, local persistence, and rollback posture.
- Draft device-token contract exists for later ST-3506 work but no backend runtime endpoint is approved for ST-3501.
- `clients/mobile` does not currently exist, so scaffold APPLY will not overwrite an existing mobile client.
- Expo generator is available through `npx create-expo-app@latest`.

## Approved APPLY Scope

- Create `clients/mobile` Expo TypeScript app.
- Add HomeTusk-specific app shell and storage/API boundaries.
- Add mobile README/runbook and mobile `AGENTS.md`.
- Run mobile install/typecheck/Expo CLI smoke.
- Update ST-3501 workpack/checklist evidence after verification.

## Conditions

- Stop if backend runtime, migrations, or AI Platform upstream edits become necessary.
- Stop if app scaffold introduces Firebase/Supabase as a domain backend.
- Stop if app scaffold uses PWA/Capacitor as the mobile solution.
- Stop if sensitive token storage cannot stay behind secure storage.
