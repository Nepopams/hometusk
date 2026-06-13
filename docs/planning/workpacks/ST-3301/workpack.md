# Workpack: ST-3301 - Command Structured Attributes Contract/Backend Foundation

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-033/epic.md`
- Story: `docs/planning/epics/EP-033/stories/ST-3301-command-structured-attributes-backend.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Runtime command DTO/pipeline: `services/backend/src/main/java/com/hometusk/commands/**`
- Command schemas: `services/backend/src/main/resources/schemas/create-task.schema.json`, `docs/contracts/schemas/create-task.schema.json`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**DONE - GATE D GO.** Planning, artifact gate, read-only PLAN, Gate C, runtime APPLY, formatting, focused command tests, full test script verification, review gate, and delegated Gate D completed on 2026-06-13.

## Outcome
`POST /api/v1/commands` can accept explicit command-level `dueDate`, `assigneeId`, and `zoneId` for immediate `create_task` commands. The backend persists those attributes, validates them through existing domain invariants, applies them to created tasks, and keeps old payload-only clients working.

## Acceptance Criteria
- [x] AC-1: Command-level `dueDate`, `assigneeId`, and `zoneId` create a task with deadline, assignee, and zone applied.
- [x] AC-2: Existing payload-only clients still work unchanged.
- [x] AC-3: Conflicting top-level and payload attribute values are rejected and create no task.
- [x] AC-4: Invalid assignee or zone is rejected without cross-household leakage.
- [x] AC-5: Past `dueDate` is rejected.
- [x] AC-6: Idempotency includes top-level attributes.
- [x] AC-7: OpenAPI and compatibility notes document the non-breaking contract delta.

## Implementation Evidence
- Contract/docs updated:
  - `docs/contracts/http/commands.openapi.yaml`
  - `docs/_indexes/contracts-index.md`
  - `docs/architecture/service-catalog.md`
- Backend updated:
  - `CommandRequest` accepts nullable `dueDate`, `assigneeId`, and `zoneId`.
  - `Command` persists nullable explicit attributes through migration `V027__add_command_structured_attributes.sql`.
  - `CommandService` builds an effective create_task payload, maps `dueDate` to `deadline`, rejects conflicting top-level/payload attributes with `COMMAND_ATTRIBUTE_CONFLICT`, and reuses existing schema/business validation plus decision execution.
  - Continuation context re-applies persisted command attributes when a NEEDS_INPUT command resumes.
- Tests updated:
  - `CommandPipelineTest` covers command-level attributes, persistence, conflict rejection, invalid assignee/zone, and past dueDate.
  - `CommandIdempotencyIntegrationTest` covers replay/conflict behavior with command-level attributes.
- Verification:
  - `cd services/backend && ./gradlew spotlessApply test --tests "*Command*"` passed via Git Bash on 2026-06-13.
  - `./scripts/test.sh` passed via Git Bash on 2026-06-13.

## Non-goals
- `scheduleAt` acceptance or scheduled command execution.
- Frontend confirmation UI.
- Recurrence, reminders, priority, or bulk commands.
- AI Platform upstream snapshot edits.
- Contract breaking changes or API version bump.

## Files to change
Candidate files for APPLY after PLAN confirms exact signatures:
- `docs/contracts/http/commands.openapi.yaml` - add optional command-level attributes and examples.
- `docs/_indexes/contracts-index.md` - record material non-breaking Commands API delta.
- `docs/architecture/service-catalog.md` - update command pipeline/data catalog notes if columns or behavior change.
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java` - add nullable attributes.
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java` - persist nullable explicit attributes.
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java` - normalize/validate effective create_task payload and persist attributes.
- `services/backend/src/main/resources/db/migration/V027__add_command_structured_attributes.sql` - nullable columns and indexes if needed.
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java` or new `CommandStructuredAttributesIntegrationTest.java` - command attribute coverage.
- `services/backend/src/test/java/com/hometusk/integration/CommandIdempotencyIntegrationTest.java` - idempotency coverage.

Do not change:
- `docs/integration/ai-platform/v1/upstream/**`
- frontend files
- scheduling/routine runtime files

## Implementation plan (commit-sized)

### Commit 1 - Contract and docs
Steps:
1. Add optional `dueDate`, `assigneeId`, and `zoneId` to `CommandRequest`.
2. Add examples for command-level attributes and an attribute conflict/error note.
3. Record non-breaking compatibility in `docs/_indexes/contracts-index.md`.
4. Update service catalog only for externally visible command behavior/data notes.

Files:
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`

Verification:
- Manual OpenAPI review for valid YAML and optional-only compatibility.

### Commit 2 - Backend persistence and normalization
Steps:
1. Add nullable command attributes to `CommandRequest`.
2. Add nullable command columns and JPA fields.
3. Normalize effective `create_task` payload before schema/business validation and decision context.
4. Reject conflicting top-level vs payload attribute values.
5. Persist explicit attributes on `Command`.

Files:
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/resources/db/migration/V027__add_command_structured_attributes.sql`

Verification:
- `cd services/backend && ./gradlew test --tests "*Command*"`

### Commit 3 - Tests and evidence
Steps:
1. Add/extend integration tests for AC-1 through AC-6.
2. Run focused command tests.
3. Run broader backend tests if focused tests pass.
4. Update this workpack/checklist with evidence.

Files:
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java` or new `CommandStructuredAttributesIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/integration/CommandIdempotencyIntegrationTest.java`
- `docs/planning/workpacks/ST-3301/workpack.md`
- `docs/planning/workpacks/ST-3301/checklist.md`

Verification:
- `cd services/backend && ./gradlew test --tests "*Command*"`
- `./scripts/test.sh`

## Contract impact
- Provider: HomeTusk Backend.
- Consumers: Web frontend, API clients.
- Protocol/version: HTTP OpenAPI v1, non-breaking additive optional fields.
- Compatibility: backward-compatible; old payload-only clients continue to work.
- Error semantics: conflicts and invalid attributes reject before action execution and write DecisionLog failure evidence.

## Docs updates
- [x] `docs/contracts/http/commands.openapi.yaml`
- [x] `docs/_indexes/contracts-index.md`
- [x] `docs/architecture/service-catalog.md` if runtime/data notes change.
- [x] No ADR expected for ST-3301.
- [x] No diagram expected for ST-3301.

## Tests
- [x] Integration: command-level attributes happy path.
- [x] Integration: payload-only compatibility.
- [x] Integration: conflict rejection.
- [x] Integration: invalid assignee/zone/past dueDate.
- [x] Integration: idempotency replay/conflict with attributes.

## Verification commands
- `cd services/backend && ./gradlew test --tests "*Command*"` - focused command suite passes.
- `./scripts/test.sh` - full project checks pass or any environment blocker is documented.

## DoD checklist
- [x] Tests pass or blocker documented.
- [x] No cross-household attribute leaks.
- [x] DecisionLog exists for success and rejected validation paths.
- [x] Idempotency semantics preserved.
- [x] Contract docs updated before runtime completion.
- [x] Workpack contains evidence/commands.
- [x] Review gate completed before Gate D.

## Risks
- Top-level and payload attributes can drift - define conflict rejection and cover it in tests.
- New columns may be redundant with JSONB payload - keep them nullable and audit-oriented for explicit user attributes.
- Date naming mismatch (`dueDate` vs existing `deadline`) can confuse clients - document `dueDate` as command-level alias that maps to task `deadline`.
- Idempotency hash could accidentally ignore new fields - use existing `CommandRequest` serialization and test changed attributes.

## Rollback
- Revert backend/contract commits.
- If migration has been applied, rollback by dropping nullable columns added in `V027`; no required data migration because old payload-only behavior remains intact.

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-3301/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3301/plan-findings.md`
- Gate C: `docs/planning/workpacks/ST-3301/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3301/prompt-apply.md`
- Review gate: `docs/planning/workpacks/ST-3301/review-gate.md`
- Gate D: `docs/planning/workpacks/ST-3301/gate-d.md`
