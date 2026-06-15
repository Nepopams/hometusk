# Codex APPLY Prompt - ST-3505

Implement only the approved ST-3505 scope.

## Requirements

- Submit commands only to HomeTusk `/commands`.
- Continue `needs_input` only through `/commands/{commandId}/continue`.
- Persist only non-sensitive recent command hints.
- Render controlled outcomes: executed, scheduled, needs_input, rejected, executed_degraded.
- No direct AI Platform calls.

## Verification

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- Source review for command boundary and storage.
