# Review Gate: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Decision
**GO.**

Date: 2026-06-13

## Scope Reviewed
- Optional `CommandRequest.scheduleAt` contract and `scheduled` response variant.
- `commands.schedule_at` persistence and `scheduled` lifecycle status.
- Feature-flagged `CommandSchedulerJob` / `CommandSchedulerService`.
- Due-time execution through `CommandService` with row lock and validation reuse.
- Active web Commands route schedule-at control and scheduled-result display.
- Workpack, service catalog, and contract index updates.

## Must-Fix Findings
None.

## Should-Fix Findings
None.

## Evidence
- Focused command suite passed: `cd services/backend && ./gradlew spotlessApply test --tests "*Command*"`.
- Full repo test script passed: `./scripts/test.sh`.
- Web build and lint passed: `cd clients/web && npm run build`; `cd clients/web && npm run lint`.
- Browser verification passed for desktop and mobile via Playwright + system Edge.
- `git diff --check` passed with LF-to-CRLF warnings only.

## Invariant Check
- AI remains a decision engine, not source of truth.
- Scheduled commands validate at submission and revalidate mutable business invariants at due time.
- DecisionLog is written at scheduling and at due-time execution or rejection.
- Scheduler is disabled by default.
- Upstream AI Platform snapshots were not edited.

## Residual Risk
- Operational environments must explicitly enable `hometusk.command-scheduler.enabled=true` when scheduled execution should run.
