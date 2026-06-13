# Gate C: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Decision
**GO for Codex APPLY.**

Date: 2026-06-13

## Delegation
Human Gate C was delegated by the active user goal on 2026-06-13. Codex evaluated ADR-016, the scheduled command diagram, and read-only PLAN findings.

## Approved Scope
- One-off scheduled commands with optional `scheduleAt`.
- Additive Commands API request/response changes.
- Nullable command storage and `scheduled` lifecycle status.
- Feature-flagged command scheduler disabled by default.
- Due-time execution through the existing command pipeline.
- Active web composer schedule-at control.

## Required Verification
- `cd services/backend && ./gradlew test --tests "*Command*"`
- `cd clients/web && npm run build`
- `cd clients/web && npm run lint`
- Browser desktop/mobile verification.
- `./scripts/test.sh` if focused checks pass.

## Stop Conditions
- Recurrence, reminders, priority, or calendar integration becomes required.
- Runtime requires editing upstream AI Platform snapshots.
- Scheduled execution would bypass DecisionLog, guardrails, or domain validation.
