# Voice Command Chat Visual QA

**Date:** 2026-06-14
**Initiative:** INIT-2026Q3-voice-command-chat-mvp
**Status:** implementation QA in progress

## Sources

- Initiative: `docs/planning/initiatives/INIT-2026Q3-voice-command-chat-mvp.md`
- Implementation map: `docs/design/voice-command-chat/implementation-map.md`
- Component inventory: `docs/design/voice-command-chat/component-inventory.md`
- Responsive rules: `docs/design/voice-command-chat/responsive-rules.md`
- Frontend acceptance: `docs/design/voice-command-chat/frontend-acceptance-criteria.md`
- Pencil MCP source inspected: `hometusk.pen`
- PNG exports present in repo: `docs/design/voice-comand-chat/voice/*.png`
- Runtime route: `clients/web/src/routes/Commands.tsx`

## QA Notes

The implementation reuses the current Commands shell instead of introducing a
separate assistant surface. Voice controls are embedded into the composer:

- mic button uses the existing `VoiceMicButton` states;
- recording/uploading/transcribing uses `VoiceRecordingStatus`;
- ASR errors use `VoiceErrorMessage`;
- transcript draft remains the existing textarea;
- result rendering stays driven by `CommandResponse.status`;
- history rail and mobile history sheet remain unchanged.

## State Coverage

| State | Runtime evidence |
|-------|------------------|
| Empty / idle | Composer renders typed textarea and optional mic. |
| Recording | `voiceMode=recording`, mic pressed state, timer, cancel. |
| Uploading | `voiceMode=uploading`, processing mic, uploading label. |
| Transcribing | `voiceMode=transcribing`, processing mic, transcribing label. |
| Draft ready | ASR result fills textarea and shows draft badge. |
| Processing command | Existing `useCommand.isLoading` result skeleton. |
| Executed / scheduled / degraded | Existing structured result cards. |
| Needs input | Existing clarification card from command response. |
| Rejected | Existing rejection card with retry/new command. |
| Permission error | Recorder error maps to `VoiceErrorMessage`. |
| Timeout / rate limit | ASR hook maps 504/429 to controlled error states. |
| Unsupported media | ASR hook maps 415 to controlled error state. |

## Responsive Review

- Desktop keeps the two-column Commands layout and history rail.
- Tablet follows the current Commands breakpoint behavior.
- Mobile uses the existing single-column layout; voice status wraps inside the
  composer and touch targets remain at least 44px.
- The voice row uses `flex-wrap` so timer/status text does not force horizontal
  overflow.

## Verification Evidence

- `cd clients/web && npm run build`: PASS on 2026-06-14; Vite kept the existing
  >500 kB chunk warning.
- `cd clients/web && npm run lint`: PASS on 2026-06-14.
- `cd clients/web && npm test -- --run`: PASS on 2026-06-14; 29 tests across
  4 files.
- Browser visual smoke on `http://127.0.0.1:5175`: PASS for app shell and
  dev-login render with `VITE_AUTH_PROVIDER=dev`,
  `VITE_VOICE_COMMAND_ENABLED=true`. Full Commands visual pass requires a dev
  JWT and backend session.

## Risks And Follow-Ups

- The repo path for exported PNGs is `voice-comand-chat` with a historical typo;
  canonical markdown handoff lives in `voice-command-chat`.
- Full visual comparison against PNG exports remains a manual QA task unless a
  screenshot-diff harness is added later.
