# Codex PLAN Prompt - ST-3301 Command Structured Attributes Contract/Backend Foundation

## Mode
PLAN only. Read-only.

Do not edit, create, delete, move, format, or generate tracked files during PLAN.
Do not implement runtime code.
Do not generate `prompt-apply.md`.

## Objective
Produce a decision-complete implementation plan for ST-3301:

`POST /api/v1/commands` accepts optional command-level `dueDate`, `assigneeId`, and `zoneId` for immediate `create_task` commands, persists those explicit attributes, validates them through existing domain invariants, applies them to created tasks, and preserves existing payload-only/idempotency behavior.

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT-2026Q3‑command‑attributes.md`
- Epic: `docs/planning/epics/EP-033/epic.md`
- Story: `docs/planning/epics/EP-033/stories/ST-3301-command-structured-attributes-backend.md`
- Workpack: `docs/planning/workpacks/ST-3301/workpack.md`
- Checklist: `docs/planning/workpacks/ST-3301/checklist.md`
- Commands contract: `docs/contracts/http/commands.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- Service catalog: `docs/architecture/service-catalog.md`
- Backend command DTO: `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- Backend command domain: `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`
- Backend command service: `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- Business validation: `services/backend/src/main/java/com/hometusk/commands/pipeline/BusinessValidator.java`
- Manual decision provider: `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/ManualDecisionProvider.java`
- Action executor: `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- Idempotency service: `services/backend/src/main/java/com/hometusk/commands/idempotency/CommandIdempotencyService.java`
- Command integration tests: `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`, `services/backend/src/test/java/com/hometusk/integration/CommandIdempotencyIntegrationTest.java`

## Acceptance Criteria
- AC-1: Command-level `dueDate`, `assigneeId`, and `zoneId` create a task with deadline, assignee, and zone applied.
- AC-2: Existing payload-only clients still work unchanged.
- AC-3: Conflicting top-level and payload attribute values are rejected and create no task.
- AC-4: Invalid assignee or zone is rejected without cross-household leakage.
- AC-5: Past `dueDate` is rejected.
- AC-6: Idempotency includes top-level attributes.
- AC-7: OpenAPI and compatibility notes document the non-breaking contract delta.

## In Scope
- Contract docs and compatibility note.
- Backend DTO/domain/migration changes.
- Effective create_task payload normalization.
- Conflict rejection.
- Focused backend integration tests.
- Workpack/checklist evidence updates after APPLY.

## Out of Scope
- `scheduleAt`.
- Frontend UI.
- Recurrence/reminders/priority.
- AI Platform upstream snapshots.
- Any breaking contract versioning.

## Required PLAN Output
Record findings in the response or `plan-findings.md` after PLAN only if later approved, with:
- current-state findings with exact files/classes/methods;
- final implementation file list;
- migration shape;
- conflict semantics;
- test plan;
- verification commands;
- risks and rollback;
- STOP-THE-LINE conditions;
- Gate C recommendation.

## Invariants
- New command fields are optional.
- Existing payload-only `create_task` behavior must remain valid.
- `dueDate` maps to task `deadline`.
- Domain invariants stay in code.
- Invalid attributes are rejected; do not silently auto-fix.
- Every command path writes DecisionLog.
- Idempotency hash must include new request fields.
- Do not edit upstream AI Platform snapshots.
- Do not implement scheduling in ST-3301.

## Verification Candidates
- `cd services/backend && ./gradlew test --tests "*Command*"`
- `./scripts/test.sh`

## STOP-THE-LINE
- Contract delta cannot be made backward-compatible.
- Attribute validation requires changing AI Platform upstream contracts.
- Command lifecycle or scheduling is needed to satisfy a supposed ST-3301 requirement.
- Idempotency cannot distinguish changed top-level attributes.
- Household boundary validation cannot be preserved with the proposed implementation.
