# Gate C - ST-3502 Mobile Auth and Secure Session Persistence

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- PLAN findings recorded in `docs/planning/workpacks/ST-3502/plan-findings.md`.
- Existing web cookie auth behavior is preserved as a non-goal.
- The mobile path reuses `KeycloakAuthService` and does not add a new provider.
- SecureStore boundary already exists from ST-3501.

## Approved APPLY Scope

- Contract/docs update for additive mobile auth endpoints.
- Backend DTO/controller/security/test updates.
- Mobile login/register/bootstrap/refresh/logout implementation.
- Workpack/checklist/review/Gate D evidence updates.

## Conditions

- Stop if sensitive tokens must be stored outside SecureStore.
- Stop if existing web auth endpoints become breaking.
- Stop if AI Platform or upstream snapshots enter the change.
