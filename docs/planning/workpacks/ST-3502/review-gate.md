# Review Result: GO

## Must-fix

None.

## Should-fix

- Redocly still reports repository-wide warnings for missing `info.license` and logout operations without 4XX responses. These warnings are not introduced as blocking errors by ST-3502; the auth contract validates successfully.
- Local backend verification on Windows cannot use `./gradlew` directly because the repository has a POSIX-only wrapper and no `sh`/`bash` in this environment. The same Gradle wrapper was invoked through `GradleWrapperMain` instead.

## Evidence

- `docs/contracts/http/commands.openapi.yaml` documents additive `/auth/mobile/login`, `/auth/mobile/register`, `/auth/mobile/refresh`, and `/auth/mobile/logout`.
- `services/backend/src/main/java/com/hometusk/auth/api/AuthController.java` exposes the mobile JSON-token facade backed by `KeycloakAuthService`.
- `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java` permits `/api/v1/auth/mobile/**`.
- `services/backend/src/main/java/com/hometusk/auth/filter/JwtCookieAuthFilter.java` skips mobile auth endpoints and preserves browser cookie auth behavior.
- `clients/mobile/App.tsx` supports login/register, session bootstrap, refresh fallback, `/users/me` profile loading, and logout.
- `clients/mobile/src/storage/secureSessionStore.ts` keeps token material in Expo SecureStore and computes token expiry metadata.
- `clients/mobile/src/storage/localAppMemory.ts` continues to use AsyncStorage only for non-sensitive selected household, command draft, and recent command hints.

## Commands

- `npx --yes @redocly/cli lint docs/contracts/http/commands.openapi.yaml` - passed with three warnings.
- `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*AuthControllerIntegrationTest"` - passed.
- `cd clients/mobile && npm run typecheck` - passed.
- `cd clients/mobile && npx expo start --help` - passed.
- `rg -n "nullable: true" docs/contracts/http/commands.openapi.yaml docs/contracts/http/mobile-devices.openapi.yaml` - no matches.
- `rg -n "accessToken|refreshToken|SecureStore|AsyncStorage" clients/mobile/src clients/mobile/App.tsx clients/mobile/README.md clients/mobile/AGENTS.md` - reviewed; no sensitive token storage in AsyncStorage/plain local storage.

## Recommendation

GO for delegated Gate D on ST-3502. Proceed to ST-3503 household read models after preserving the Redocly warnings as known non-blocking cleanup.
