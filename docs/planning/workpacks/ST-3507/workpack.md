# Workpack: ST-3507 - Push Receive, Deep Links, and Release Smoke Path

## Sources of Truth

- Story: `docs/planning/epics/EP-035/stories/ST-3507-push-deeplinks-release-smoke.md`
- Initiative execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Contract: `docs/contracts/http/mobile-devices.openapi.yaml`
- Command/auth contract: `docs/contracts/http/commands.openapi.yaml`
- ADR: `docs/adr/020-native-mobile-client-stack.md`
- Diagram: `docs/diagrams/sequence-mobile-push-deep-link.md`
- Mobile app: `clients/mobile/`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`
- Expo docs checked on 2026-06-14:
  - `https://docs.expo.dev/push-notifications/push-notifications-setup/`
  - `https://docs.expo.dev/push-notifications/receiving-notifications/`
  - `https://docs.expo.dev/linking/into-your-app/`

## Status

**DONE.** Delegated Gate D GO recorded on 2026-06-14.

## Outcome

The native mobile client can request push permission, obtain an Expo push token when a valid EAS project id is available, register the token with HomeTusk backend, route notification/deep-link handoffs into safe app surfaces, and document the internal release smoke path.

## Acceptance Criteria

- [x] AC-1: Mobile requests push permission and obtains a push token through Expo Push Service.
- [x] AC-2: Mobile registers the token through `POST /api/v1/mobile/devices`.
- [x] AC-3: Test push path is documented and can be smoke-tested on a dev build.
- [x] AC-4: Push/deep-link handoff routes task, command chat, invite accept, and notification fallback targets.
- [x] AC-5: Target loading relies on backend authorization after auth.
- [x] AC-6: Android dev build path is documented and smoke-verified where local tooling allows.
- [x] AC-7: iOS dev/TestFlight-equivalent path is documented and credential limits are explicit.

## In Scope

- Install Expo notification/linking dependencies needed by the selected stack.
- Add mobile notification permission/token registration and backend registration.
- Add local non-sensitive device registration state for logout/deactivation handoff.
- Add deep-link and notification response parsing/routing.
- Add invite acceptance through existing backend invite contract.
- Update mobile README and planning artifacts.

## Out of Scope

- Production push provider delivery adapter beyond Expo Push Service registration.
- Notification preferences.
- Rich notification actions.
- Store launch polish.
- New backend contract beyond ST-3506.

## Implementation Plan

1. Add Expo notification/linking dependencies and config plugin.
2. Add mobile API types/client methods for mobile devices and invite accept.
3. Add notification/deep-link utility module and non-sensitive local device registration storage.
4. Wire signed-in push registration, logout deactivation, notification listeners, and deep-link routing in `App.tsx`.
5. Add README release smoke instructions and close Gate D after verification.

## Verification Commands

- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`
- `cd clients/mobile && npm ls expo-notifications expo-constants expo-linking`

## Risks And Rollback

- Push tokens require a development build with Expo/EAS project id; Expo Go is not a sufficient production-like push path.
- iOS push verification depends on Apple credentials and registered device access.
- Rollback: remove mobile notification dependencies/config, remove ST-3507 mobile wiring, and keep ST-3506 backend registration as dormant additive surface.
