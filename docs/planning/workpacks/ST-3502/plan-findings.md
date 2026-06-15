# PLAN Findings: ST-3502 - Mobile Auth and Secure Session Persistence

## Mode

Read-only PLAN completed on 2026-06-14 before APPLY.

## Findings

1. Existing `/api/v1/auth/login` and `/api/v1/auth/register` return `204` and HttpOnly cookies, not JSON tokens.
2. Existing `/api/v1/auth/refresh` requires a refresh cookie, so it is not a reliable native SecureStore refresh path.
3. `KeycloakAuthService` already returns `AuthTokens` internally for login/register/refresh and supports best-effort logout by refresh token.
4. Native mobile can safely use an additive JSON-token facade backed by the same `KeycloakAuthService` without adding a provider or changing web cookie behavior.
5. `clients/mobile/src/storage/secureSessionStore.ts` already provides a SecureStore boundary for `accessToken` and `refreshToken`.
6. `clients/mobile/src/storage/localAppMemory.ts` uses AsyncStorage only for non-sensitive state.

## Approved Implementation Plan

1. Add `/auth/mobile/login`, `/auth/mobile/register`, `/auth/mobile/refresh`, and `/auth/mobile/logout` to the contract.
2. Implement backend mobile endpoints using `KeycloakAuthService`.
3. Permit the mobile auth endpoints in Spring Security and skip cookie auth filter for them.
4. Add focused backend tests.
5. Add mobile auth methods, login/register UI, SecureStore bootstrap, refresh fallback, and logout.
6. Run contract/backend/mobile verification.

## Stop-The-Line Conditions

- Stop if implementation needs a new auth provider.
- Stop if mobile tokens would need AsyncStorage/plain storage.
- Stop if existing web cookie auth behavior changes.
- Stop if backend starts logging raw access/refresh tokens.
- Stop if native OIDC/social setup becomes necessary for ST-3502 acceptance.

## Gate C Recommendation

GO. The change is additive, security-sensitive but bounded, and directly advances the Native Mobile MVP session requirement.
