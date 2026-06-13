# DoD Checklist: ST-3302 - Command Attribute Confirmation UI

## Readiness
- [x] ST-3301 backend foundation is Gate D GO.
- [x] Active route identified.
- [x] Existing household members/zones hooks identified.
- [x] Codex PLAN findings recorded.
- [x] Gate C delegated approval recorded.

## Frontend
- [x] `CommandRequest` type includes optional `dueDate`, `assigneeId`, and `zoneId`.
- [x] Commands route has due date control.
- [x] Commands route has assignee selector.
- [x] Commands route has zone selector.
- [x] Selected values are submitted as top-level command fields.
- [x] Blank controls are omitted from the request.
- [x] Clear/new command resets attribute controls.

## Security / Boundaries
- [x] Assignee options come from current household members.
- [x] Zone options come from current household zones.
- [x] Backend remains the final validator for invalid/cross-household values.

## Verification
- [x] `npm run build` passes in `clients/web`.
- [x] `npm run lint` passes in `clients/web`.
- [x] Browser desktop check passes.
- [x] Browser mobile check passes.
- [x] Review gate completed with GO before Gate D.

## Final
- [x] Workpack evidence updated after APPLY.
- [x] Gate D decision recorded.
