# Codex PLAN Prompt: ST-3502 - Mobile Auth and Secure Session Persistence

## Mode

Read-only PLAN. Do not edit, create, delete, move, format, or generate tracked files.

## Objective

Produce a decision-complete implementation plan for mobile auth/session persistence.

## Sources to Read

- `docs/planning/workpacks/ST-3502/workpack.md`
- `docs/planning/epics/EP-035/stories/ST-3502-mobile-auth-session.md`
- `docs/adr/020-native-mobile-client-stack.md`
- `docs/contracts/http/commands.openapi.yaml`
- `services/backend/src/main/java/com/hometusk/auth/api/AuthController.java`
- `services/backend/src/main/java/com/hometusk/auth/keycloak/KeycloakAuthService.java`
- `services/backend/src/main/java/com/hometusk/auth/service/AuthTokens.java`
- `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java`
- `services/backend/src/test/java/com/hometusk/integration/AuthControllerIntegrationTest.java`
- `clients/mobile/App.tsx`
- `clients/mobile/src/api/client.ts`
- `clients/mobile/src/storage/secureSessionStore.ts`
- `clients/mobile/src/storage/localAppMemory.ts`

## Constraints

- No new auth provider.
- Do not edit AI Platform upstream snapshots.
- Do not add Firebase/Supabase.
- Sensitive session tokens must use SecureStore only.
- Preserve existing web cookie auth behavior.
- Keep native OIDC/social setup out of ST-3502.

## Required Output

- Files to change.
- Exact implementation steps.
- Risks and stop-the-line conditions.
- Verification commands.
- Gate C recommendation.
