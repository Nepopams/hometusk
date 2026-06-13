# Workpack: ST-3302 - Command Attribute Confirmation UI

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-033/epic.md`
- Story: `docs/planning/epics/EP-033/stories/ST-3302-command-attribute-confirmation-ui.md`
- ST-3301 backend foundation: `docs/planning/workpacks/ST-3301/gate-d.md`
- Commands route: `clients/web/src/routes/Commands.tsx`
- Commands styles: `clients/web/src/routes/Commands.css`
- Web API types: `clients/web/src/types/api.ts`
- Existing household option hooks: `clients/web/src/hooks/useMembers.ts`, `clients/web/src/hooks/useZones.ts`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**DONE - GATE D GO.** Planning, read-only PLAN, delegated Gate C, frontend APPLY, verification, review gate, and delegated Gate D completed on 2026-06-13.

## Outcome
The active web command composer lets users set optional due date, assignee, and zone for immediate create-task commands and submits them as command-level fields supported by ST-3301.

## Acceptance Criteria
- [x] AC-1: The active Commands page exposes optional due date, assignee, and zone controls.
- [x] AC-2: Selected values are sent as top-level `CommandRequest` fields while `payload` remains title-only.
- [x] AC-3: Leaving controls blank keeps the previous payload-only create-task request compatible.
- [x] AC-4: Assignee and zone options come from current household members/zones.
- [x] AC-5: A past due date is blocked client-side.

## Implementation Evidence
- `clients/web/src/types/api.ts` now includes optional `CommandRequest.dueDate`, `assigneeId`, and `zoneId`.
- `clients/web/src/routes/Commands.tsx` loads current household members/zones, renders optional controls, blocks past due dates, and maps selected values to top-level request fields.
- `clients/web/src/routes/Commands.css` adds responsive composer field styling: three columns on desktop, one column on mobile.
- Browser submit evidence showed `payload` with only `title` plus top-level `dueDate`, `assigneeId`, and `zoneId`.
- Browser validation evidence showed past due-date submission is blocked before POST.
- Desktop and mobile viewport checks showed no overlap between controls, actions, and result content.

## Non-goals
- `scheduleAt` UI or scheduled command execution.
- Backend, migration, OpenAPI, or upstream AI Platform changes.
- Full command confirmation modal.
- Voice command flow refactor.

## Files to change
- `clients/web/src/types/api.ts` - add optional command-level request fields.
- `clients/web/src/routes/Commands.tsx` - add state, option loading, validation, and request mapping.
- `clients/web/src/routes/Commands.css` - responsive controls layout and validation styling.
- `clients/web/src/i18n/translations.ts` - add small labels only if existing translations are insufficient.
- `docs/planning/workpacks/ST-3302/workpack.md` and `checklist.md` - evidence after APPLY.
- `docs/planning/epics/EP-033/epic.md` and ST-3302 story - status updates after review.

## Implementation plan
1. Extend `CommandRequest` with optional `dueDate`, `assigneeId`, and `zoneId`.
2. In `Commands.tsx`, load members/zones using existing hooks for the current household.
3. Add due date, assignee, and zone composer state plus client-side future-date validation.
4. Submit selected attributes as top-level command fields and keep payload limited to title.
5. Clear attribute state on clear/new command.
6. Add compact responsive CSS for the composer controls without changing result/history behavior.
7. Run web build/lint and browser verification.

## Contract impact
No new contract change. This consumes the ST-3301 additive Commands API fields already documented in `docs/contracts/http/commands.openapi.yaml`.

## Docs updates
- [x] Story/workpack evidence updated after APPLY.
- [x] No OpenAPI update expected.
- [x] No ADR/diagram expected.

## Tests
- [x] `npm run build` in `clients/web`.
- [x] `npm run lint` in `clients/web`.
- [x] Browser verification for desktop and mobile composer layout.

## DoD checklist
- [x] Existing command execution path preserved.
- [x] Client-side past due-date validation present.
- [x] Current-household options used for assignee/zone.
- [x] TypeScript build passes.
- [x] Review gate completed before Gate D.

## Risks
- The active route and unused structured `CommandInput` component can drift further; keep this slice focused on the active route.
- Adding controls can crowd mobile layout; verify responsive wrapping.
- Frontend validation is advisory only; backend ST-3301 remains source of truth.

## Rollback
- Revert the web route/type/style changes and ST-3302 planning artifacts. ST-3301 backend remains backward-compatible.

## Prompt Pack
- PLAN findings: `docs/planning/workpacks/ST-3302/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3302/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3302/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3302/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3302/gate-d.md`
