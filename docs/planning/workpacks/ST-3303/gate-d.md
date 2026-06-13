# Gate D: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Decision
**GO.**

Date: 2026-06-13

## Delegation
Human Gate D was delegated by the active user goal on 2026-06-13. Codex evaluated implementation evidence and Review Gate result.

## Release Readiness
- Contract, backend, frontend, tests, and planning evidence are complete.
- Scheduler is feature-flagged and disabled by default.
- Scheduled command traceability is preserved through DecisionLog.
- No upstream AI Platform contract snapshots were changed.

## Verification
- `cd services/backend && ./gradlew spotlessApply test --tests "*Command*"`: PASS.
- `cd clients/web && npm run build`: PASS.
- `cd clients/web && npm run lint`: PASS.
- Browser desktop/mobile verification: PASS.
- `./scripts/test.sh`: PASS.
- `git diff --check`: PASS with LF-to-CRLF warnings only.

## Rollback Note
Disable `hometusk.command-scheduler.enabled` first. If reverting runtime support after migration, resolve any pending `scheduled` commands before removing the nullable `schedule_at` column and `scheduled` check-constraint value.
