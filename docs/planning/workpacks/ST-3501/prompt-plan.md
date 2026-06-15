# Codex PLAN Prompt: ST-3501 - Mobile Stack ADR and App Foundation

## Mode

Read-only PLAN. Do not edit, create, delete, move, format, or generate tracked files.

## Objective

Produce a decision-complete implementation plan for ST-3501, the Native Mobile MVP app foundation.

## Sources to Read

- `docs/planning/workpacks/ST-3501/workpack.md`
- `docs/planning/epics/EP-035/epic.md`
- `docs/planning/epics/EP-035/stories/ST-3501-mobile-stack-app-foundation.md`
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- `docs/adr/021-native-mobile-client-stack.md`
- `docs/diagrams/sequence-mobile-push-deep-link.md`
- `docs/contracts/http/mobile-devices.openapi.yaml`
- `docs/architecture/service-catalog.md`
- `clients/web/src/lib/api.ts`
- `clients/web/src/types/api.ts`
- `clients/web/package.json`

## Constraints

- Do not implement backend endpoints in ST-3501.
- Do not edit AI Platform upstream snapshots.
- Do not introduce Firebase/Supabase as a domain backend.
- Do not introduce PWA/Capacitor as the primary mobile solution.
- Keep sensitive token handling behind secure storage.
- Keep local app memory non-sensitive.
- Prepare command surfaces to use HomeTusk backend command pipeline only.

## Required Output

- Files to change.
- Exact implementation steps.
- Risks and stop-the-line conditions.
- Verification commands.
- Gate C recommendation.
