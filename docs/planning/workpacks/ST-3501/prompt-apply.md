# Codex APPLY Prompt: ST-3501 - Mobile Stack ADR and App Foundation

## Mode

APPLY. Implement only the approved ST-3501 scope with minimal diff.

## Objective

Create the first-class native mobile app foundation for HomeTusk using React Native + Expo + TypeScript.

## Sources

- `docs/planning/workpacks/ST-3501/workpack.md`
- `docs/planning/workpacks/ST-3501/plan-findings.md`
- `docs/planning/workpacks/ST-3501/gate-c.md`
- `docs/planning/epics/EP-035/stories/ST-3501-mobile-stack-app-foundation.md`
- `docs/adr/020-native-mobile-client-stack.md`

## Allowed Files

- `clients/mobile/**`
- `docs/planning/workpacks/ST-3501/checklist.md`
- `docs/planning/workpacks/ST-3501/workpack.md`
- `docs/planning/workpacks/ST-3501/review-gate.md`
- `docs/planning/workpacks/ST-3501/gate-d.md`

## Forbidden Files

- `docs/integration/ai-platform/v1/upstream/**`
- `services/backend/**`
- `infra/uat/nginx/Dockerfile`
- Existing `clients/web/**` runtime files

## Invariants

- HomeTusk backend remains source of truth.
- Mobile must not call AI Platform directly.
- No Firebase/Supabase domain backend.
- No PWA/Capacitor primary mobile solution.
- Sensitive tokens must use secure storage only.
- Plain local app memory is only for non-sensitive state.

## Implementation Steps

1. Generate `clients/mobile` with Expo blank TypeScript.
2. Add a HomeTusk app shell with Home, Tasks, Shopping, and Command surfaces.
3. Add API/config/storage boundaries.
4. Add mobile README/runbook and mobile `AGENTS.md`.
5. Run verification commands.
6. Update workpack/checklist evidence.

## Verification Commands

- `cd clients/mobile && npm install`
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

## STOP-THE-LINE

Stop and report if implementation needs backend runtime changes, changes to AI Platform upstream snapshots, a non-HomeTusk domain backend, or unsafe token storage.
