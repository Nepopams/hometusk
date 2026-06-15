# Story: ST-3506 - Push Device Registration Backend Foundation

## Status: DONE

**Epic:** EP-035 | **Priority:** P0 | **Points:** 5

## Description

Add the backend contract and implementation needed for authenticated mobile clients to register, update, and delete push device tokens.

## Acceptance Criteria

1. `POST /api/v1/mobile/devices` registers or refreshes a current user's mobile device token.
2. `PATCH /api/v1/mobile/devices/{deviceId}` rotates token/status metadata safely.
3. `DELETE /api/v1/mobile/devices/{deviceId}` deactivates the user's device registration.
4. Device rows are scoped to the authenticated user and cannot be accessed cross-user.
5. Token values are not logged.
6. Contract, migration, service catalog, and tests are updated.

## Out of Scope

- Notification preferences.
- Rich notification actions.
- Production APNs/FCM credential rollout beyond the selected MVP provider path.

## Flags

- contract_impact: yes.
- data_impact: yes.
- security_sensitive: high.
- traceability_critical: medium.

## Planning

- Workpack: `docs/planning/workpacks/ST-3506/`
- Gate C: delegated GO on 2026-06-14.
- Gate D: delegated GO on 2026-06-14.

## Evidence

- Review gate: `docs/planning/workpacks/ST-3506/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3506/gate-d.md`
- Contract: `docs/contracts/http/mobile-devices.openapi.yaml`
- Migration: `services/backend/src/main/resources/db/migration/V032__create_mobile_devices.sql`
- Focused test: `MobileDeviceControllerIntegrationTest`
