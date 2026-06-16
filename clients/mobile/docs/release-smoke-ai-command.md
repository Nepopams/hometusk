# Mobile AI Command UX v1 Smoke Checklist

## Scope

Manual smoke for `INIT-2026Q3-mobile-ai-command-ux-v1`.

## Preconditions

- Backend points at a build that has the accepted `natural_command + needs_confirmation` contract and approve/cancel lifecycle.
- Mobile uses `EXPO_PUBLIC_API_BASE_URL` for that backend.
- Tester signs in and selects a household.
- Test household has at least one member, zone, task, and shopping list when scenario fixtures need them.

## Checks

- [ ] Typecheck passes: `cd clients/mobile && npm run typecheck`.
- [ ] Command tab accepts typed natural text and sends `type=natural_command`.
- [ ] Request includes `payload.text`, `inputMode=text`, `locale`, `timezone`, `referenceInstant`, `source=mobile`, and `clientTimestamp`.
- [ ] `executed` response renders as a controlled success outcome.
- [ ] `executed_degraded` response renders degraded reason or fallback strategy without crashing.
- [ ] `needs_input` response renders clarify UI and can continue through `/commands/{commandId}/continue`.
- [ ] `rejected` response renders `errorCode` / safe reason as controlled outcome.
- [ ] `scheduled` response renders schedule date when backend returns it.
- [ ] `needs_confirmation` response renders summary, reasons, risk labels, proposed actions, expiry, command id, and confirmation id.
- [ ] Confirmation card states that no action has happened yet.
- [ ] Approve calls `/commands/{commandId}/confirmations/{confirmationId}/approve`.
- [ ] Successful approve shows terminal state and refreshes household read models.
- [ ] Repeated approve does not allow duplicate local taps while in flight.
- [ ] Cancel calls `/commands/{commandId}/confirmations/{confirmationId}/cancel`.
- [ ] Successful cancel shows terminal state and does not refresh as a domain mutation.
- [ ] Forbidden/not found/conflict/expired confirmation failures render user-safe error copy.
- [ ] Recent commands distinguish executed, degraded, clarify, rejected, scheduled, and confirmation outcomes.
- [ ] App refresh limitation is understood: v1 does not restore pending confirmations from a durable backend read model.
- [ ] No mobile request goes directly to AI Platform.
- [ ] No raw provider payload, provider prompt, credential, stack trace, or raw audio is shown.

## Follow-up Candidates

- Durable pending confirmation read model / history.
- Native mobile ASR transcript capture and explicit submit flow.
- Production rollout / feature-flag enablement.
- Read-only `answered` / status-query UX after separate backend/provider gate.
