# Codex APPLY Prompt - ST-3504

Implement only the approved ST-3504 scope.

## Scope

- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/App.tsx`
- `clients/mobile/README.md`
- ST-3504 planning/workpack artifacts
- EP/execution status after successful review

## Requirements

- Task create/complete must use `POST /commands`.
- Shopping mutations must use selected household paths.
- Refresh read models after successful mutation.
- Render command outcomes and errors compactly.
- Keep offline mutation queue out of scope.

## Verification

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- Source review for task command boundary and household-scoped shopping paths.
