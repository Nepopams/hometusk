# Workpack: ST-3303 - Scheduled Command Execution with `scheduleAt`

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-033/epic.md`
- Story: `docs/planning/epics/EP-033/stories/ST-3303-scheduled-command-execution.md`
- ADR: `docs/adr/016-scheduled-command-execution.md`
- Diagram: `docs/diagrams/sequence-scheduled-command-execution.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Runtime command pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- Active web route: `clients/web/src/routes/Commands.tsx`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**DONE - Gate D GO.** Artifact gate, read-only PLAN, delegated Gate C, APPLY, review gate, and delegated Gate D completed on 2026-06-13.

## Outcome
HomeTusk accepts one-off scheduled commands via `scheduleAt`, persists them as auditable command lifecycle entities, and executes due commands later through the existing command pipeline.

## Acceptance Criteria
- [x] AC-1: Future `scheduleAt` returns `scheduled`, persists command, creates no task, and writes DecisionLog.
- [x] AC-2: Past `scheduleAt` rejects without task creation and records validation failure when applicable.
- [x] AC-3: Scheduler executes due scheduled commands through the existing pipeline and marks them executed.
- [x] AC-4: Scheduler revalidates mutable business invariants at due time and rejects invalid commands safely.
- [x] AC-5: Idempotency includes `scheduleAt`.
- [x] AC-6: Active web composer can send future `scheduleAt` and blocks past schedule time.

## Non-goals
- Recurrence.
- Reminder notifications.
- Priority.
- Calendar integrations.
- New AI Platform upstream snapshots.
- Quartz or external scheduling dependencies.

## Files to change
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandScheduledResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java`
- `services/backend/src/main/java/com/hometusk/commands/repository/CommandRepository.java`
- `services/backend/src/main/java/com/hometusk/commands/scheduler/CommandSchedulerJob.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandSchedulerService.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/resources/application.yml`
- `services/backend/src/main/resources/db/migration/V028__add_scheduled_commands.sql`
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`
- `services/backend/src/test/java/com/hometusk/integration/CommandIdempotencyIntegrationTest.java`
- `clients/web/src/components/commands/CommandHistoryEntry.tsx`
- `clients/web/src/types/api.ts`
- `clients/web/src/routes/Commands.tsx`
- `clients/web/src/routes/Commands.css`
- `clients/web/src/i18n/translations.ts`
- ST-3303 workpack/checklist evidence files.

## Implementation plan
1. Update contract/docs for optional `scheduleAt`, `scheduled` response, and scheduler lifecycle notes.
2. Add backend persistence/status/response support.
3. Refactor command processing just enough for scheduled submission and due-time execution to reuse the existing pipeline.
4. Add command scheduler service/job behind `hometusk.command-scheduler.enabled=false` by default.
5. Add backend integration tests for AC-1 through AC-5.
6. Add active web schedule-at control and request mapping/validation for AC-6.
7. Run focused backend command tests, web build/lint, browser verification, and full test script if focused checks pass.

## Contract impact
- Additive optional request field: `CommandRequest.scheduleAt`.
- Additive response variant: `CommandScheduledResponse` with `status=scheduled`.
- Backward compatible for clients that do not send `scheduleAt`.

## Docs updates
- [x] ADR-016 accepted.
- [x] Scheduled command sequence diagram added.
- [x] ADR and diagram indexes updated.
- [x] OpenAPI updated.
- [x] Contract index updated.
- [x] Service catalog updated.

## Tests
- [x] Backend integration: scheduled submission creates no task and writes scheduled log.
- [x] Backend integration: past schedule rejects.
- [x] Backend integration: due scheduler creates task and marks command executed.
- [x] Backend integration: due-time invalid assignee rejects safely.
- [x] Backend integration: idempotency replay/conflict includes `scheduleAt`.
- [x] Web build/lint.
- [x] Browser desktop/mobile verification.

## Verification Evidence
- `cd services/backend && ./gradlew spotlessApply test --tests "*Command*"`: PASS on 2026-06-13.
- `cd clients/web && npm run build`: PASS on 2026-06-13; Vite reported the existing >500 kB chunk warning.
- `cd clients/web && npm run lint`: PASS on 2026-06-13.
- Browser verification via Playwright + system Edge on `http://127.0.0.1:5177`: PASS on 2026-06-13.
  - Captured POST body used top-level `scheduleAt`, `dueDate`, `assigneeId`, and `zoneId`.
  - Payload remained `{ "title": "Clean scheduled task" }`.
  - Past schedule time was blocked client-side with no second POST.
  - Desktop layout used 2 columns and mobile layout used 1 column with no horizontal overflow.
- `./scripts/test.sh`: PASS on 2026-06-13.
- `git diff --check`: PASS on 2026-06-13; only LF-to-CRLF working-copy warnings.

## DoD checklist
- [x] Tests pass or blocker documented.
- [x] Immediate command path unchanged.
- [x] Scheduled command path writes DecisionLog at scheduling and due-time outcome.
- [x] Scheduler disabled by default.
- [x] No upstream AI Platform snapshots changed.
- [x] Workpack contains evidence/commands.
- [x] Review gate completed before Gate D.

## Risks
- Scheduled commands can become invalid later; revalidation at due time is required.
- Multiple DecisionLog rows per command may surprise consumers; document this as lifecycle trace.
- Scheduler execution can be retried after errors; keep status `scheduled` until final status transition.
- Web schedule control can crowd mobile layout; verify responsive layout.

## Rollback
- Disable `hometusk.command-scheduler.enabled`.
- Revert runtime/UI/contract changes.
- If migration has been applied, reject or execute pending scheduled commands before dropping nullable `schedule_at` and removing `scheduled` status from the check constraint.

## Prompt Pack
- PLAN findings: `docs/planning/workpacks/ST-3303/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3303/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3303/prompt-apply.md`
- Review Gate: `docs/planning/workpacks/ST-3303/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3303/gate-d.md`
