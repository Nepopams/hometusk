# Review Result: GO

## Must-fix

None.

## Should-fix

- Push delivery and receipt are intentionally deferred to ST-3507; ST-3506 only stores current-user device registrations.
- Token conflict handling is conservative: an active provider token can belong to only one active registration across users.

## Evidence

- `V032__create_mobile_devices.sql` creates `mobile_devices` with current-user foreign key, platform/provider/status checks, and an active-token unique index.
- `MobileDeviceController` exposes `POST`, `PATCH`, and `DELETE` under `/api/v1/mobile/devices`.
- `MobileDeviceService` scopes reads by authenticated user id, rejects cross-user token conflicts, and deactivates instead of hard-deleting.
- `MobileDeviceDto` excludes `pushToken`; focused tests assert the token is not returned.
- `GlobalExceptionHandler` maps `DEVICE_NOT_FOUND` to 404 and `DEVICE_TOKEN_CONFLICT` to 409.

## Commands

- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*MobileDeviceControllerIntegrationTest"` - passed after Docker Desktop daemon was started.
- `npx --yes @redocly/cli lint docs/contracts/http/mobile-devices.openapi.yaml` - passed with warning-only output.
- `rg -n "pushToken|push_token|DEVICE_TOKEN|MobileDeviceDto|log\\." services/backend/src/main/java/com/hometusk/mobile services/backend/src/main/java/com/hometusk/shared/exception services/backend/src/test/java/com/hometusk/integration/MobileDeviceControllerIntegrationTest.java docs/contracts/http/mobile-devices.openapi.yaml` - reviewed token exposure/logging surface.

## Recommendation

GO for delegated Gate D on ST-3506. Proceed to ST-3507 push receive, deep links, and release smoke path.
