# DoD Checklist: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Readiness
- [x] ST-3301 and ST-3302 are Gate D GO.
- [x] ADR-016 accepted.
- [x] Scheduled command sequence diagram added.
- [x] Codex PLAN findings recorded.
- [x] Gate C delegated approval recorded.

## Contract / Docs
- [x] `CommandRequest.scheduleAt` documented.
- [x] `CommandScheduledResponse` documented.
- [x] `scheduled` status documented.
- [x] Contract index updated.
- [x] Service catalog updated.

## Backend
- [x] Command stores nullable `scheduleAt`.
- [x] Command status supports `scheduled`.
- [x] Future `scheduleAt` returns scheduled response.
- [x] Scheduled submission creates no task immediately.
- [x] Past `scheduleAt` rejects.
- [x] Scheduler finds due scheduled commands.
- [x] Scheduler locks command row before execution.
- [x] Due execution reuses existing command pipeline.
- [x] Due-time invalid business state rejects without action.
- [x] Scheduler disabled by default.

## Frontend
- [x] Web API types include `scheduleAt`.
- [x] Active Commands route has schedule-at control.
- [x] Future schedule time is sent top-level.
- [x] Past schedule time is blocked client-side.
- [x] Responsive layout verified.

## Verification
- [x] `cd services/backend && ./gradlew test --tests "*Command*"` passes.
- [x] `cd clients/web && npm run build` passes.
- [x] `cd clients/web && npm run lint` passes.
- [x] Browser desktop check passes.
- [x] Browser mobile check passes.
- [x] `./scripts/test.sh` passes or blocker documented.
- [x] Review gate completed with GO before Gate D.

## Final
- [x] Workpack evidence updated after APPLY.
- [x] Gate D decision recorded.
