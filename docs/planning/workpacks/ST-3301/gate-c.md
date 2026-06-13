# Gate C - ST-3301 Command Structured Attributes Contract/Backend Foundation

## Decision
- Decision: GO
- Decider: Codex, under delegated human-gate authority from user goal
- Date: 2026-06-13

## Evidence
- PLAN findings recorded in `docs/planning/workpacks/ST-3301/plan-findings.md`.
- Scope is backward-compatible and does not require upstream AI Platform changes.
- `scheduleAt` and scheduling lifecycle are explicitly out of scope for ST-3301.
- Household boundary and DecisionLog invariants are testable in existing command integration tests.

## Approved APPLY Scope
- Contract/docs changes for optional `dueDate`, `assigneeId`, and `zoneId`.
- Backend DTO/domain/migration support.
- Effective create_task payload normalization and conflict rejection.
- Focused command/idempotency tests.
- Workpack/checklist evidence updates.

## Conditions
- Stop if `scheduleAt` implementation is needed.
- Stop if upstream AI Platform snapshots would need edits.
- Stop if idempotency or DecisionLog behavior cannot be preserved.
