# Gate C - ST-3506 Push Device Registration Backend Foundation

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- PLAN findings recorded in `docs/planning/workpacks/ST-3506/plan-findings.md`.
- Draft contract exists and is additive.
- ST-3505 reached Gate D GO.

## Approved APPLY Scope

- Backend mobile device registration persistence and endpoints.
- Flyway migration.
- Error code mapping.
- Integration tests.
- Contract/service catalog/index/story/workpack evidence.

## Conditions

- Do not log push token values.
- Do not return push token values in API responses.
- Keep rows scoped to current authenticated user.
