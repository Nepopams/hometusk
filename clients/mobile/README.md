# HomeTusk Mobile

Native mobile client for the HomeTusk Android/iOS MVP.

## Stack

- React Native + Expo + TypeScript.
- Expo SecureStore for sensitive session material.
- AsyncStorage only for non-sensitive app memory.
- HomeTusk backend remains the source of truth.

## Local Setup

```bash
cd clients/mobile
npm install
npm run typecheck
npm start
```

Use `EXPO_PUBLIC_API_BASE_URL` to point the app at a backend:

```bash
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1 npm start
```

On Android emulators, use the host address that can reach the backend from the emulator environment.

## Source Layout

The mobile client keeps `App.tsx` as the Expo entrypoint and places app behavior under `src/`:

- `src/app/` - app shell orchestration, surface metadata, shared app types, and read-model defaults.
- `src/features/auth/` - auth screen and secure session bootstrap/login/logout controller.
- `src/features/households/` - household switcher and selected-household persistence.
- `src/features/home/`, `src/features/tasks/`, `src/features/shopping/` - household surfaces and mutation helpers.
- `src/features/command/` - command composer, hero/outcome/confirmation/continuation cards, request builder, continuation parser, outcome formatting, and recent-command history wrapper.
- `src/features/notifications/` - push registration and notification/deep-link routing helpers.
- `src/shared/ui/`, `src/shared/format/`, `src/shared/errors/` - reusable mobile UI primitives, mascot fallback renderer, pure formatters, and API error formatting.

Future command/voice work should prefer `src/features/command/**` first and avoid coupling command changes to auth, shopping, push, or household modules.

## Device Builds

```bash
npm run android
npm run ios
```

iOS simulator/device builds require macOS or an Expo/EAS path with Apple credentials. The MVP release path targets internal testing/dev builds first, not production store launch.

## Push And Deep Links

The app uses Expo Push Service for the MVP provider path. Push registration runs after sign-in:

1. The app checks for an Expo/EAS project id from the development build config.
2. It requests notification permission on iOS/Android.
3. It obtains an Expo push token.
4. It registers the token through `POST /api/v1/mobile/devices`.

The push token is not stored in AsyncStorage and is not shown in the UI. The app stores only the non-sensitive backend device registration id so logout can deactivate the registration best effort.

Supported handoff targets:

- `hometusk://task/{taskId}?householdId={householdId}`
- `hometusk://command?householdId={householdId}`
- `hometusk://invite?token={inviteToken}`
- `hometusk://notification/{notificationId}?householdId={householdId}`

Push notification data can use the same fields through either `deepLink`/`url` or safe target keys such as `route`, `taskId`, `commandId`, `inviteToken`, `notificationId`, and `householdId`. Target data is only a handoff hint; the app reloads data through the authenticated backend session.

## Internal Release Smoke

Android dev build smoke:

```bash
npx expo install expo-notifications expo-constants expo-linking
npx expo install expo-audio expo-file-system
npx expo start
npx uri-scheme open "hometusk://command" --android
```

For a push smoke, use an Android physical device or emulator with Google Play services, an Expo/EAS development build with push credentials, and the Expo push notifications tool. After sign-in, the app should show "Push registration is ready for this device"; send a test notification with `data.deepLink` such as `hometusk://command`.

iOS dev/TestFlight-equivalent smoke:

- Requires macOS or EAS Build plus Apple Developer credentials.
- Register the test device before the first push-capable development build.
- Build/install the development build, sign in, confirm push registration, then send a test notification through the Expo push notifications tool.
- When Apple credentials are unavailable, treat iOS push receipt as credential-blocked but still verify TypeScript, app config, deep-link parsing, and Android path.

Expo references checked on 2026-06-14 and refreshed for audio on 2026-06-18:

- `https://docs.expo.dev/push-notifications/push-notifications-setup/`
- `https://docs.expo.dev/push-notifications/receiving-notifications/`
- `https://docs.expo.dev/linking/into-your-app/`
- `https://docs.expo.dev/versions/latest/sdk/audio/`
- `https://docs.expo.dev/versions/latest/sdk/filesystem/`

## Boundaries

- Do not call AI Platform directly from mobile.
- Do not add Firebase/Supabase as a HomeTusk domain backend.
- Do not store access/refresh tokens in AsyncStorage.
- Use `/api/v1/auth/mobile/login`, `/api/v1/auth/mobile/register`, `/api/v1/auth/mobile/refresh`, and `/api/v1/auth/mobile/logout` for native session flow.
- Store `accessToken`, `refreshToken`, and token expiry metadata only through Expo SecureStore.
- Keep push payloads small and safe; load deep-link target data from the backend after auth.
- Keep offline mutation sync out of NOW; read-only cache and drafts are allowed.

## Auth Session Flow

The app opens a saved SecureStore session on boot, calls `/api/v1/users/me`, and attempts `/auth/mobile/refresh` once when the access token is stale or rejected. Invalid or expired refresh tokens clear SecureStore and return to the unauthenticated screen. Transient restore failures keep SecureStore intact and ask the user to retry. Logout calls `/auth/mobile/logout` best effort, then clears SecureStore on the device.

## Household Read Models

After sign-in, the app validates the stored selected household ID against `/api/v1/users/me`, persists the active household as non-sensitive AsyncStorage state, and loads members, zones, tasks, shopping lists/items, and notifications through existing household-scoped REST endpoints. ST-3503 intentionally does not add a backend aggregation endpoint or mobile write flows.

## Task And Shopping Mutations

Task create and complete actions use `POST /api/v1/commands` with mobile idempotency and correlation headers. Shopping item add, mark-purchased, and delete actions use the existing selected-household shopping endpoints. The mobile client refreshes read models after successful writes and does not implement an offline mutation queue.

## Command Chat

The Command tab is the native Mobile AI Command UX v1 surface over HomeTusk command contracts. Typed text is sent to `POST /api/v1/commands` as `type=natural_command` with `payload.text`, `inputMode=text`, locale, timezone, and a reference instant. HomeTusk backend remains the source of truth for command state, guardrails, execution, confirmation, and audit.

Mobile Voice Command Entry v1 adds voice only as a secondary command input. The mic opens a short recording sheet, records through `expo-audio`, sends one multipart `file` to `POST /api/v1/voice/transcriptions`, and inserts the returned transcript into the same editable command field. The app does not send the command automatically after ASR; the user must press the existing Send action.

Voice compatibility notes:

- `expo-audio` `RecordingPresets.HIGH_QUALITY` records `.m4a` on native platforms and `audio/webm` on web; both are accepted by the HomeTusk voice transcription media allowlist.
- The mobile client sends voice uploads through Expo FileSystem native multipart upload only to the authenticated HomeTusk BFF with bearer auth and `X-Correlation-ID`; it does not call AI Platform directly.
- If backend logs show normal mobile auth/read traffic but no `POST /api/v1/voice/transcriptions` for a voice attempt, debug mobile backend reachability or native upload before debugging ASR.
- Voice-originated command drafts are submitted through the existing `natural_command` path with `inputMode=voice_transcript`, `source=voice`, and safe `asrTraceId` metadata.
- Controlled voice failures map to permission denied, recording failed, upload/transcription failed, unsupported media, rate limit, timeout, or unclear speech states, each with typed command fallback.

The mobile visual system uses the approved Mobile Redesign + Mascot v1 direction:

- warm cream app background;
- warm white elevated cards;
- teal primary actions;
- amber confirmation cards;
- muted red rejection cards;
- blue/gray clarify cards;
- static mascot state support through `src/shared/ui/Mascot.tsx`;
- redesigned Home and Command surfaces with recent-command and empty states.

Final mascot PNGs are expected under `clients/mobile/assets/mascot/`:

- `mascot_idle.png`
- `mascot_hello.png`
- `mascot_thinking.png`
- `mascot_success.png`
- `mascot_confirm.png`
- `mascot_confused.png`
- `mascot_reject.png`
- `mascot_degraded.png`

Until final exports are present, the app renders deterministic labeled placeholders. Mascot artwork must come from the approved illustration source; do not redraw it in code.

The mobile client renders backend-controlled outcomes:

- `executed`
- `executed_degraded`
- `needs_input`
- `needs_confirmation`
- `rejected`
- `scheduled`

`needs_input` uses `/api/v1/commands/{commandId}/continue`. `needs_confirmation` shows a dedicated confirmation card with summary, reasons, risk labels, proposed actions, expiry, and explicit approve/cancel controls. Approve and cancel call the HomeTusk backend confirmation endpoints; mobile never executes or simulates proposed actions locally.

Recent command hints are stored only as non-sensitive AsyncStorage app memory. Pending confirmations are v1 submit-result-driven: after an app refresh, mobile does not reconstruct a durable pending confirmation card unless a future backend read model is separately gated.

The mobile client never calls AI Platform directly and never displays raw provider payloads, prompts, credentials, or stack traces.

## Verification

```bash
npm run typecheck
npx expo start --help
```

AI command manual smoke checklist:

- `clients/mobile/docs/release-smoke-ai-command.md`
