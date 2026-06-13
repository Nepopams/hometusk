# PLAN Findings: ST-3302 - Command Attribute Confirmation UI

## Result
**Gate C recommendation: GO.** ST-3302 is implementable as a frontend-only slice that consumes the ST-3301 contract/backend foundation.

## Current-State Findings
- The active route is `clients/web/src/routes/Commands.tsx`; it sends `payload: { title }` only.
- `clients/web/src/components/commands/CommandInput.tsx` already has a richer form but is not mounted by the router, so switching to it would increase scope.
- Existing API helpers and hooks already expose household-scoped members and zones.
- `clients/web/src/types/api.ts` still lacks the new optional command-level fields.
- Existing result states and command history are driven by `useCommand`, so the UI can preserve idempotency/history behavior by keeping the same hook path.

## Implementation Decision
Enhance the active `Commands.tsx` composer directly:
1. Add local state for due date, assignee, zone, and validation feedback.
2. Load members/zones with `useMembers(householdId)` and `useZones(householdId)`.
3. Keep `payload` title-only and put selected values into top-level `CommandRequest`.
4. Block past due dates client-side before calling `execute`.
5. Reset controls when clearing or starting a new command.

## File List For APPLY
- `clients/web/src/types/api.ts`
- `clients/web/src/routes/Commands.tsx`
- `clients/web/src/routes/Commands.css`
- `clients/web/src/i18n/translations.ts` only if existing labels are insufficient
- `docs/planning/workpacks/ST-3302/workpack.md`
- `docs/planning/workpacks/ST-3302/checklist.md`

## Tests
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- Browser verification at the active commands route if a dev server can be started.

## STOP-THE-LINE
- Backend changes become necessary.
- `scheduleAt` is required to complete the story.
- The active route cannot load household-scoped options.
