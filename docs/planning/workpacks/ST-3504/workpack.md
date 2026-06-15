# Workpack: ST-3504 - Tasks and Shopping Mobile Mutations

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Epic: `docs/planning/epics/EP-035/epic.md`
- Story: `docs/planning/epics/EP-035/stories/ST-3504-tasks-shopping-mutations.md`
- REST contract: `docs/contracts/http/commands.openapi.yaml`
- Mobile app: `clients/mobile/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE / GATE D GO.** Gate B is delegated GO after ST-3503 Gate D. Artifact gate is GO with no contract change because existing command and shopping endpoints cover the required mutations.

## Outcome

Authenticated mobile users can create and complete tasks through the command pipeline, add shopping items, mark items purchased, delete items, and see item-to-task linkage where present.

## Acceptance Criteria

- [x] AC-1: User can complete an open task through the existing command pipeline.
- [x] AC-2: User can create a task through the existing command pipeline.
- [x] AC-3: User can add a shopping item to an existing shopping list.
- [x] AC-4: User can mark a shopping item purchased.
- [x] AC-5: User can delete a shopping item.
- [x] AC-6: User can see task-shopping linkage on shopping item rows.
- [x] AC-7: Boundary errors are shown without leaking cross-household data.

## Non-goals

- Full offline-first mutation sync.
- Bulk edit.
- New mobile-specific domain model.
- New task CRUD endpoint.
- New shopping aggregation endpoint.

## Files to change

- `clients/mobile/App.tsx` - mutation controls and refresh wiring.
- `clients/mobile/src/api/client.ts` - shopping mutation methods.
- `clients/mobile/src/api/types.ts` - shopping mutation DTOs.
- `clients/mobile/README.md` - mutation boundary notes.
- `docs/planning/epics/EP-035/stories/ST-3504-tasks-shopping-mutations.md`
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- `docs/planning/workpacks/ST-3504/**`

Forbidden:

- Direct task CRUD endpoint for create/complete.
- Direct mobile-to-AI-Platform code.
- Offline mutation queue.
- `docs/integration/ai-platform/v1/upstream/**`
- `infra/uat/nginx/Dockerfile`

## Implementation Plan

1. Add mobile shopping mutation DTOs and client methods.
2. Add task create form and task complete action using `executeCommand`.
3. Add shopping add/purchase/delete actions using existing shopping endpoints.
4. Refresh read models after successful mutation.
5. Keep errors generic and household-scoped.

## Contract Impact

No contract change. ST-3504 consumes:

- `POST /api/v1/commands` for `create_task` and `complete_task`
- `POST /api/v1/households/{householdId}/shopping-lists/{listId}/items`
- `PATCH /api/v1/households/{householdId}/shopping-items/{itemId}`
- `DELETE /api/v1/households/{householdId}/shopping-items/{itemId}`

## Verification Commands

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- Source review for command boundary and household-scoped shopping paths.

## Evidence

- Mobile typecheck: `cd clients/mobile && npm run typecheck` passed.
- Expo CLI smoke: `cd clients/mobile && npx expo start --help` passed.
- Source review: `rg -n "executeCommand|create_task|complete_task|addShoppingItem|updateShoppingItem|deleteShoppingItem|shopping-items|shopping-lists|generateClientUuid|Idempotency-Key|selectedHouseholdId" clients/mobile/src clients/mobile/App.tsx clients/mobile/README.md` reviewed.
- No backend or contract changes were required for ST-3504.

## Risks

- Command outcomes may return `needs_input` or `rejected`; mobile should show a compact status rather than assuming success.
- Shopping add requires an existing list; creating lists is not part of ST-3504.

## Rollback

- Revert ST-3504 mobile mutation controls/client methods and workpack status changes.
- ST-3503 read-only home remains usable.

## Prompt Pack

- PLAN: `docs/planning/workpacks/ST-3504/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3504/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3504/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3504/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3504/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3504/gate-d.md`
