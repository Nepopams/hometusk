# Story: ST-3502 - Mobile Auth and Secure Session Persistence

## Status: DONE

**Epic:** EP-035 | **Priority:** P0 | **Points:** 5

## Description

Implement mobile login/logout and session persistence through the current HomeTusk auth strategy while keeping sensitive tokens in secure storage only.

Gate B, artifact gate, and Gate C are delegated GO on 2026-06-14. The approved mobile auth path is an additive JSON-token facade under `/api/v1/auth/mobile/*`, backed by the existing `KeycloakAuthService`, with tokens stored only in SecureStore on the native client.

Gate D delegated GO was recorded on 2026-06-14 after contract, backend, and mobile verification.

## Acceptance Criteria

1. User can login through the approved mobile auth path.
2. User can logout and sensitive session state is cleared.
3. App session survives app restart.
4. Expired/invalid session returns the app to a safe unauthenticated state.
5. No sensitive token is stored in AsyncStorage/plain local storage.
6. `/api/v1/users/me` loads after login and creates/resolves the HomeTusk user through backend identity rules.

## Out of Scope

- New auth provider.
- Mobile-owned identity source of truth.
- Social provider OAuth exchange inside HomeTusk backend.

## Flags

- contract_impact: yes, additive mobile auth endpoints.
- security_sensitive: high.
- traceability_critical: low.

## Evidence

- Workpack: `docs/planning/workpacks/ST-3502/`
- Review gate: `docs/planning/workpacks/ST-3502/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3502/gate-d.md`
