# Gate C - ST-3507 Push Receive, Deep Links, and Release Smoke Path

## Decision

- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-14

## Evidence

- ST-3506 Gate D GO completed and stable `/mobile/devices` contract exists.
- Expo docs reviewed for notification setup, incoming notification listeners, and deep-link handling.
- PLAN findings recorded in `docs/planning/workpacks/ST-3507/plan-findings.md`.

## Approved Scope

- Mobile-only push permission/token registration.
- Backend registration through ST-3506 contract.
- Deep-link and notification response routing.
- Invite accept through existing backend invite contract.
- Release smoke documentation.

## Constraints

- No direct AI Platform calls from mobile.
- No new backend contract unless verification proves a blocking mismatch.
- Push token values must not be logged or stored in AsyncStorage.
