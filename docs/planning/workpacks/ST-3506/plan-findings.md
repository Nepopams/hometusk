# PLAN Findings: ST-3506 - Push Device Registration Backend Foundation

## Findings

1. `docs/contracts/http/mobile-devices.openapi.yaml` already defines register, update, and delete endpoints.
2. No backend package currently owns mobile device registrations.
3. Flyway migrations are under `services/backend/src/main/resources/db/migration`; next migration is `V032` after the merged voice-command trace migration.
4. Current exception mapping needs new `DEVICE_NOT_FOUND` and `DEVICE_TOKEN_CONFLICT` codes for contract alignment.
5. Integration tests can extend `IntegrationTestBase` and use mock JWT users.

## Approved Implementation Plan

1. Implement `mobile` backend package with entity, repository, service, DTOs, and controller.
2. Add Flyway migration with indexes and active-token uniqueness.
3. Add focused integration tests.
4. Update contract status, service catalog, and indexes.

## Gate C Recommendation

GO. The change is security-sensitive and data-impacting, but contract scope is additive and bounded.
