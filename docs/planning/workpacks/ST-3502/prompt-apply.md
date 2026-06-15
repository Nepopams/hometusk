# Codex APPLY Prompt: ST-3502 - Mobile Auth and Secure Session Persistence

## Mode

APPLY. Implement only the approved ST-3502 scope.

## Objective

Add a secure mobile auth/session foundation using HomeTusk backend and SecureStore.

## Allowed Files

- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`
- `services/backend/src/main/java/com/hometusk/auth/**`
- `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java`
- `services/backend/src/test/java/com/hometusk/integration/AuthControllerIntegrationTest.java`
- `clients/mobile/**`
- `docs/planning/epics/EP-035/**`
- `docs/planning/initiatives/INIT-2026Q3-native-mobile-mvp.execution.md`
- `docs/planning/workpacks/ST-3502/**`

## Forbidden Files

- `docs/integration/ai-platform/v1/upstream/**`
- `infra/uat/nginx/Dockerfile`
- `clients/web/**` runtime files unless required to prove compatibility

## Invariants

- Existing web cookie auth remains backward-compatible.
- Mobile stores sensitive tokens only in SecureStore.
- No new auth provider.
- No direct mobile-to-AI-Platform calls.

## Verification Commands

- `npx --yes @redocly/cli lint docs/contracts/http/commands.openapi.yaml`
- `cd services/backend && ./gradlew test --tests "*AuthControllerIntegrationTest"`
- `cd clients/mobile && npm run typecheck`
- `cd clients/mobile && npx expo start --help`

## STOP-THE-LINE

Stop and report if the approved scope requires changing AI Platform upstream contracts, storing tokens in AsyncStorage, or breaking browser cookie auth.
