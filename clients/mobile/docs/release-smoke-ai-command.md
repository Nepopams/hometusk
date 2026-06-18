# Mobile AI Command UX v1 Smoke Checklist

## Scope

Manual smoke for `INIT-2026Q3-mobile-ai-command-ux-v1`, the mobile-only visual layer from `INIT-2026Q3-mobile-redesign-mascot-v1`, and Mobile Voice Command Entry v1.

## Preconditions

- Backend points at a build that has the accepted `natural_command + needs_confirmation` contract and approve/cancel lifecycle.
- Backend has the accepted `POST /api/v1/voice/transcriptions` BFF enabled for the test user.
- Mobile uses `EXPO_PUBLIC_API_BASE_URL` for that backend.
- Tester signs in and selects a household.
- Test household has at least one member, zone, task, and shopping list when scenario fixtures need them.
- Device/emulator can grant microphone permission; if not, run the permission-denied smoke path.

## Checks

- [ ] Typecheck passes: `cd clients/mobile && npm run typecheck`.
- [ ] Home uses warm cream background, warm white cards, teal primary route action, and readable bottom navigation.
- [ ] Home shows summary counts, recent command/change hint, empty state when data is empty, and no command composer.
- [ ] Home pending confirmation summary appears only for current in-session `needs_confirmation` and states that no action has happened yet.
- [ ] Command tab shows redesigned state hero with static mascot placeholder or approved static asset.
- [ ] Command tab accepts typed natural text and sends `type=natural_command`.
- [ ] Request includes `payload.text`, `inputMode=text`, `locale`, `timezone`, `referenceInstant`, `source=mobile`, and `clientTimestamp`.
- [ ] Mic is visible as a secondary action and does not replace typed command entry.
- [ ] Mic opens a short recording sheet with ready state and explicit close/start controls.
- [ ] Recording state shows a timer and explicit stop/cancel controls.
- [ ] Stopping recording shows review-before-upload state; no command is sent at this point.
- [ ] Sending the recording calls `POST /api/v1/voice/transcriptions` as authenticated multipart `file`.
- [ ] Backend logs contain `POST /api/v1/voice/transcriptions` for the upload attempt; if not, verify mobile backend URL/device reachability and native upload behavior before debugging ASR.
- [ ] Native recording format is accepted by backend allowlist (`.m4a` / `audio/m4a` expected for Expo native high-quality preset).
- [ ] Successful voice transcription inserts an editable transcript into the normal command composer.
- [ ] Voice transcript is not auto-sent; user must press `Отправить команду`.
- [ ] Voice-originated Send uses `type=natural_command`, `payload.inputMode=voice_transcript`, `source=voice`, and safe `asrTraceId` metadata.
- [ ] Editing the voice transcript before Send keeps the standard command flow and does not create a separate voice result branch.
- [ ] Permission denied, recording failed, upload failed, unsupported media, rate limit, timeout, and unclear speech states offer typed fallback.
- [ ] `executed` response renders as a controlled success outcome.
- [ ] `executed_degraded` response renders a non-technical limited/safe outcome without crashing.
- [ ] `needs_input` response renders blue/gray clarify UI and can continue through `/commands/{commandId}/continue`.
- [ ] `rejected` response renders `errorCode` / safe reason as controlled outcome.
- [ ] `scheduled` response renders schedule date when backend returns it.
- [ ] `rejected` uses muted red visual treatment and does not look like an app crash.
- [ ] `needs_confirmation` response renders amber confirmation card with summary, reasons, risk labels, proposed actions, expiry, command id, and confirmation id.
- [ ] Confirmation card states that no action has happened yet.
- [ ] Approve calls `/commands/{commandId}/confirmations/{confirmationId}/approve`.
- [ ] Successful approve shows terminal state and refreshes household read models.
- [ ] Repeated approve does not allow duplicate local taps while in flight.
- [ ] Cancel calls `/commands/{commandId}/confirmations/{confirmationId}/cancel`.
- [ ] Successful cancel shows terminal state and does not refresh as a domain mutation.
- [ ] Forbidden/not found/conflict/expired confirmation failures render user-safe error copy.
- [ ] Recent commands distinguish executed, degraded, clarify, rejected, scheduled, and confirmation outcomes.
- [ ] Empty command history, empty tasks, and empty shopping states render as soft cards with static mascot fallback when final assets are absent.
- [ ] Mascot is not a button, does not replace explanatory text, and is limited to Home/Command/empty state support.
- [ ] App refresh limitation is understood: v1 does not restore pending confirmations from a durable backend read model.
- [ ] No mobile request goes directly to AI Platform.
- [ ] Voice upload goes only to HomeTusk `/api/v1/voice/transcriptions`, then command execution goes only to `/api/v1/commands`.
- [ ] No raw provider payload, provider prompt, credential, stack trace, or raw audio is shown.

## Follow-up Candidates

- Durable pending confirmation read model / history.
- Native mobile ASR transcript capture refinements after v1 smoke data.
- Production rollout / feature-flag enablement.
- Read-only `answered` / status-query UX after separate backend/provider gate.
