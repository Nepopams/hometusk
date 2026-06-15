# Workpack: ST-3506 - Push Device Registration Backend Foundation

## Sources of Truth

- Story: `docs/planning/epics/EP-035/stories/ST-3506-push-device-registration.md`
- Contract: `docs/contracts/http/mobile-devices.openapi.yaml`
- ADR: `docs/adr/020-native-mobile-client-stack.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Backend: `services/backend/`
- DoR/DoD: `docs/_governance/dor.md`, `docs/_governance/dod.md`

## Status

**DONE.** Delegated Gate D GO recorded on 2026-06-14.

## Outcome

Authenticated mobile clients can register, update, and deactivate Expo push device registrations without exposing tokens in responses or cross-user access.

## Acceptance Criteria

- [x] AC-1: `POST /api/v1/mobile/devices` registers or refreshes current user's device token.
- [x] AC-2: `PATCH /api/v1/mobile/devices/{deviceId}` rotates token/status metadata safely.
- [x] AC-3: `DELETE /api/v1/mobile/devices/{deviceId}` deactivates current user's device registration.
- [x] AC-4: Device rows are scoped to authenticated user.
- [x] AC-5: Token values are not logged or returned.
- [x] AC-6: Contract, migration, service catalog, and tests are updated.

## Implementation Plan

1. Add migration for `mobile_devices`.
2. Add mobile device domain/repository/service/DTO/controller.
3. Add error codes for device not found and token conflict.
4. Add integration tests for register, refresh, patch, delete, and cross-user boundaries.
5. Validate contract and focused backend tests.

## Contract Impact

Uses `docs/contracts/http/mobile-devices.openapi.yaml`, now marked stable for the additive ST-3506 surface.

## Verification Commands

- `npx --yes @redocly/cli lint docs/contracts/http/mobile-devices.openapi.yaml`
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*MobileDeviceControllerIntegrationTest"`

## Prompt Pack

- PLAN findings: `docs/planning/workpacks/ST-3506/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3506/gate-c.md`
- Review gate: `docs/planning/workpacks/ST-3506/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3506/gate-d.md`
