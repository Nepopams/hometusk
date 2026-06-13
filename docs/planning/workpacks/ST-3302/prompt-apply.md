# Codex APPLY Prompt: ST-3302 - Command Attribute Confirmation UI

Implement only the approved ST-3302 scope.

## Scope
- Add optional due date, assignee, and zone controls to the active `Commands.tsx` composer.
- Load options from current household members/zones.
- Submit selected values as top-level `CommandRequest.dueDate`, `assigneeId`, and `zoneId`.
- Preserve payload-only compatibility when controls are blank.
- Add client-side past due-date validation.

## Do Not Change
- Backend code or migrations.
- OpenAPI contracts.
- `scheduleAt` behavior.
- AI Platform upstream snapshots.
- Voice command flow.

## Verification
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- Browser desktop/mobile verification.
