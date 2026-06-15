# ADR-021: Native Mobile Client Stack

**Status:** Accepted
**Date:** 2026-06-14
**Initiative:** INIT-2026Q3-native-mobile-mvp

## Context

HomeTusk is moving Native Mobile Client MVP into NOW as a parallel product
validation track. The mobile client must support Android and iOS, reuse the
HomeTusk backend as source of truth, preserve command traceability, support
secure session persistence, and provide a realistic push/deep-link path.

The decision is hard to reverse because it determines:

- build and release tooling;
- auth/session storage primitives;
- push provider integration;
- app navigation and deep-link handling;
- API client and type-sharing strategy;
- how much implementation can reuse existing TypeScript client knowledge.

The initiative explicitly forbids using PWA or Capacitor as the primary mobile
solution, adding a Firebase/Supabase domain backend, calling AI Platform
directly from mobile, or creating a mobile-specific domain model.

## Decision

HomeTusk will build `clients/mobile` with React Native + Expo + TypeScript.

### 1. App Stack

The mobile app is an Expo-managed React Native project in `clients/mobile`.
TypeScript is mandatory. Android and iOS dev build paths are documented in the
mobile README.

The app remains a HomeTusk client only:

- HomeTusk backend remains the source of truth.
- Mobile calls HomeTusk REST APIs and command pipeline endpoints.
- Mobile never calls AI Platform directly.
- Mobile does not introduce a second domain backend.

### 2. Push Strategy

For MVP/internal testing, mobile push uses Expo Notifications and the Expo Push
Service as the delivery provider path. HomeTusk backend stores the authenticated
user's device registration and sends safe push payloads through the selected
provider adapter.

Direct FCM/APNs integration is deferred until Expo push is insufficient for
production constraints.

### 3. Auth And Storage

Sensitive session/token material must use platform secure storage. Non-sensitive
local app memory may use plain local storage for:

- selected household;
- command draft input;
- recent command/chat history;
- read-only cache metadata;
- notification/deep-link handoff state.

Plain local storage must not contain access tokens, refresh tokens, provider
tokens, secrets, or raw push credentials.

### 4. API Client Posture

The first mobile slice may use a hand-written typed API boundary aligned to the
existing web client and OpenAPI contracts. Generated client adoption remains
preferred when practical, but is not required for the first scaffold slice.

If the manual API boundary grows beyond the initial mobile MVP, Codex must
revisit generated client adoption in a follow-up ADR or implementation note.

### 5. Navigation And Deep Links

Mobile navigation must support safe fallback routes. Deep links are routing
hints only; backend authorization remains mandatory when loading tasks,
invites, command chat, notifications, or any household-scoped entity.

## Consequences

### Positive

- Shares TypeScript ergonomics with the existing web client.
- Expo gives a faster Android/iOS dev build and internal testing path.
- Expo SecureStore, Notifications, and Linking cover key MVP primitives without
  custom native modules in the first slice.
- A single mobile app codebase reduces duplicated Kotlin/Swift product logic.
- Push delivery can be validated before committing to direct FCM/APNs.

### Negative

- Expo adds a platform/tooling dependency and may need EAS for reliable device
  builds.
- Some native capabilities may require config plugins or moving to prebuild.
- Expo Push Service is another external dependency and may need replacement or
  direct provider integration for production policy/performance reasons.
- Type sharing with backend contracts is still manual until generated client
  adoption is introduced.

### Risks And Mitigations

| Risk | Mitigation |
|------|------------|
| Expo dependency blocks a native capability | Keep Expo-managed path for MVP, allow prebuild/config plugins only through ADR/update if needed. |
| Push payload leaks sensitive household data | Payload contract allows only safe identifiers/title snippets; app fetches detail from backend after auth. |
| Secure storage is bypassed | Create a named secure token boundary in ST-3501 and verify it in auth work. |
| Manual API types drift from contracts | Keep API boundary small, link to OpenAPI, and revisit generated client if drift appears. |
| Mobile command chat becomes generic assistant | Command chat sends only HomeTusk `POST /commands` requests and renders controlled outcomes. |

## Alternatives Considered

### Flutter

Flutter is strong for cross-platform UI, but it would introduce Dart as a new
language/runtime into a repository whose client code and API semantics already
use TypeScript. It would also duplicate more DTO and validation mapping work for
the first MVP.

### Native Kotlin + Swift

Fully native apps provide maximum platform fidelity, but they would double the
client implementation effort, testing surface, and API-client maintenance for
the MVP. This is too slow for the current validation goal.

### React Native Without Expo

Bare React Native gives more immediate native control, but increases setup,
release, push, and device-build overhead before the product loop is validated.
Expo can still move toward prebuild/native customization later if needed.

### PWA Or Capacitor

Rejected by initiative guardrails. PWA/mobile-width web already exists as a
supporting surface and does not satisfy native push/session/deep-link goals.

## Migration And Rollback

ST-3501 is additive. Runtime rollback can remove `clients/mobile` and leave the
backend/web unchanged. Contract and ADR rollback is a docs-only revert until
device-token backend implementation ships.

If Expo becomes unsuitable after MVP validation, a later ADR can supersede this
decision and migrate the client while preserving the HomeTusk backend contract.

## Related

- Initiative: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Epic: `docs/planning/epics/EP-035/epic.md`
- Mobile device contract: `docs/contracts/http/mobile-devices.openapi.yaml`
- Diagram: `docs/diagrams/sequence-mobile-push-deep-link.md`
