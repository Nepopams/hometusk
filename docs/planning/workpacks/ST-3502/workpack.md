# Workpack: ST-3502 - Mobile Auth and Secure Session Persistence

## Sources of Truth

- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.md`
- Execution index: `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- Epic: `docs/planning/epics/EP-035/epic.md`
- Story: `docs/planning/epics/EP-035/stories/ST-3502-mobile-auth-session.md`
- ADR: `docs/adr/021-native-mobile-client-stack.md`
- Auth contract: `docs/contracts/http/commands.openapi.yaml`
- Backend auth controller: `services/backend/src/main/java/com/hometusk/auth/api/AuthController.java`
- Backend auth service: `services/backend/src/main/java/com/hometusk/auth/keycloak/KeycloakAuthService.java`
- Mobile app: `clients/mobile/`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status

**DONE / GATE D GO.** Gate B is delegated GO for ST-3502 after ST-3501 Gate D. Artifact gate, Gate C, review gate, and Gate D are recorded in this workpack.

## Outcome

Mobile users can login/register through the existing HomeTusk auth strategy, keep the sensitive session in SecureStore, reload `/api/v1/users/me`, recover from app restart, refresh expired sessions, and logout by clearing sensitive state.

## Acceptance Criteria

- [x] AC-1: Mobile login returns access/refresh session tokens through an approved mobile auth path.
- [x] AC-2: Mobile register returns access/refresh session tokens through the same auth strategy.
- [x] AC-3: Mobile app stores tokens only in SecureStore.
- [x] AC-4: Mobile app bootstraps an existing SecureStore session and calls `/api/v1/users/me`.
- [x] AC-5: Invalid/expired access token attempts refresh; failed refresh clears session and returns to unauthenticated state.
- [x] AC-6: Logout best-effort invalidates the refresh token through backend and clears SecureStore.
- [x] AC-7: Web cookie auth endpoints remain backward-compatible.

## Non-goals

- New auth provider.
- Social provider OAuth exchange inside HomeTusk backend.
- Native OIDC browser flow / `hometusk-mobile` Keycloak public client setup.
- Biometric app lock.
- Push registration.

## Files to change

- `docs/contracts/http/commands.openapi.yaml` - add mobile auth endpoints and token response schemas.
- `docs/_indexes/contracts-index.md` - add material non-breaking auth note.
- `docs/architecture/service-catalog.md` - record mobile token auth surface.
- `services/backend/src/main/java/com/hometusk/auth/api/AuthController.java` - add mobile JSON-token auth endpoints.
- `services/backend/src/main/java/com/hometusk/auth/dto/MobileAuthResponse.java` - token response DTO.
- `services/backend/src/main/java/com/hometusk/auth/dto/MobileRefreshRequest.java` - refresh request DTO.
- `services/backend/src/main/java/com/hometusk/auth/dto/MobileLogoutRequest.java` - logout request DTO.
- `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java` - permit mobile auth endpoints.
- `services/backend/src/main/java/com/hometusk/auth/filter/JwtCookieAuthFilter.java` - skip mobile auth endpoints.
- `services/backend/src/test/java/com/hometusk/integration/AuthControllerIntegrationTest.java` - mobile auth endpoint coverage.
- `clients/mobile/App.tsx` - auth/session UI states.
- `clients/mobile/src/api/client.ts` - mobile auth API methods.
- `clients/mobile/src/api/types.ts` - auth DTOs.
- `clients/mobile/src/storage/secureSessionStore.ts` - session helpers.
- `clients/mobile/README.md` - auth/session runbook notes.
- `docs/planning/workpacks/ST-3502/**` - evidence and gates.

Forbidden:

- `docs/integration/ai-platform/v1/upstream/**`
- Direct mobile-to-AI-Platform code
- Firebase/Supabase domain backend
- `infra/uat/nginx/Dockerfile`

## Implementation plan

### Commit 1 - Contract and docs

1. Add non-breaking mobile auth endpoints under `/auth/mobile/*` to the HomeTusk auth contract.
2. Add `MobileAuthResponse`, `MobileRefreshRequest`, and `MobileLogoutRequest` schemas.
3. Update contract index and service catalog.

Verification:
- `npx --yes @redocly/cli lint docs/contracts/http/commands.openapi.yaml`

### Commit 2 - Backend mobile auth facade

1. Add DTOs.
2. Add AuthController mobile login/register/refresh/logout methods backed by `KeycloakAuthService`.
3. Permit `/api/v1/auth/mobile/**`.
4. Add focused controller tests.

Verification:
- `cd services/backend && ./gradlew test --tests "*AuthControllerIntegrationTest"`

### Commit 3 - Mobile secure session UI

1. Add mobile auth methods to API client.
2. Implement login/register form and authenticated shell bootstrap.
3. Store access/refresh tokens only in SecureStore.
4. Use `/users/me` after login/bootstrap and clear session on failed refresh.

Verification:
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

## Contract impact

- Provider: HomeTusk Backend.
- Consumer: Native mobile app.
- Protocol/version: HTTP OpenAPI v1, additive endpoints.
- Compatibility: non-breaking; existing browser cookie endpoints keep their 204 + Set-Cookie behavior.
- Security posture: mobile token response is only for native clients that store tokens in platform secure storage.

## Docs updates

- [x] Auth contract updated.
- [x] Contract index updated.
- [x] Service catalog updated.
- [x] Mobile README updated.

## Tests

- [x] Backend controller tests for mobile login/register/refresh/logout.
- [x] Mobile typecheck.
- [x] Source review for SecureStore-only sensitive session storage.

## Verification commands

- `npx --yes @redocly/cli lint docs/contracts/http/commands.openapi.yaml`
- `cd services/backend && ./gradlew test --tests "*AuthControllerIntegrationTest"`
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

## DoD checklist

- [x] Tests pass or blockers documented.
- [x] No sensitive tokens in AsyncStorage/plain storage.
- [x] Web auth compatibility preserved.
- [x] Contract/docs updated before runtime completion.
- [x] Workpack contains evidence/commands.
- [x] Review gate completed before Gate D.

## Evidence

- Contract lint: `npx --yes @redocly/cli lint docs/contracts/http/commands.openapi.yaml` passed with warnings only for missing `info.license` and logout operations without 4XX responses.
- Backend focused test: `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*AuthControllerIntegrationTest"` passed.
- Mobile typecheck: `cd clients/mobile && npm run typecheck` passed.
- Expo CLI smoke: `cd clients/mobile && npx expo start --help` passed.
- Secure storage review: `rg -n "accessToken|refreshToken|SecureStore|AsyncStorage" clients/mobile/src clients/mobile/App.tsx clients/mobile/README.md clients/mobile/AGENTS.md` shows token material only in API/session code and SecureStore; AsyncStorage remains limited to non-sensitive local app memory.

## Risks

- Returning refresh tokens in JSON increases native app responsibility. Mitigation: mobile stores tokens only in SecureStore and clears on logout/failed refresh.
- Password login via backend is not the final social/OIDC native UX. Mitigation: keep this as MVP secure-session foundation; native OIDC/social client setup remains out of ST-3502.
- Auth endpoints are security-sensitive. Mitigation: reuse existing `KeycloakAuthService`, do not log tokens, and add controller tests.

## Rollback

- Revert mobile auth endpoints and DTOs.
- Revert mobile auth UI/client changes.
- Existing browser cookie auth remains unaffected.

## Prompt Pack

- PLAN: `docs/planning/workpacks/ST-3502/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3502/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3502/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3502/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3502/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3502/gate-d.md`
