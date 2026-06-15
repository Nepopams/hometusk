# Codex APPLY Prompt - ST-3503

Implement only the approved ST-3503 scope.

## Scope

- `clients/mobile/src/api/types.ts`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/App.tsx`
- `clients/mobile/README.md`
- ST-3503 planning/workpack artifacts
- EP/execution status after successful review

## Requirements

- Use existing HomeTusk backend endpoints only.
- Persist selected household as non-sensitive local app memory.
- Validate persisted selected household against the current profile.
- Render loading, error, empty, and populated states.
- Keep mutations out of scope.

## Verification

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- Source review for household scoping and storage boundaries.
