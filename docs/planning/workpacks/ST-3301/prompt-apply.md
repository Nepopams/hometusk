# Codex APPLY Prompt - ST-3301 Command Structured Attributes Contract/Backend Foundation

## Mode
APPLY. Implement only the approved ST-3301 scope.

## Objective
Implement backward-compatible command-level `dueDate`, `assigneeId`, and `zoneId` support for immediate `create_task` commands.

## Approved Files
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/architecture/service-catalog.md`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/resources/db/migration/V027__add_command_structured_attributes.sql`
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`
- `services/backend/src/test/java/com/hometusk/integration/CommandIdempotencyIntegrationTest.java`
- `docs/planning/workpacks/ST-3301/workpack.md`
- `docs/planning/workpacks/ST-3301/checklist.md`

## Forbidden Files
- `docs/integration/ai-platform/v1/upstream/**`
- frontend files
- scheduling/routine runtime files

## Required Behavior
- New fields are optional.
- Existing payload-only `create_task` behavior remains valid.
- `dueDate` maps to task `deadline`.
- Top-level attribute values are validated by existing domain invariants.
- Conflicting top-level and payload values are rejected before action execution.
- Command stores explicit attributes in nullable columns.
- DecisionLog is written for success and rejected validation/business paths.
- Idempotency includes the new fields.

## Acceptance Criteria
- AC-1 through AC-7 in `docs/planning/workpacks/ST-3301/workpack.md`.

## Verification
- `cd services/backend && ./gradlew test --tests "*Command*"`
- `./scripts/test.sh`

## STOP-THE-LINE
- Any need to implement `scheduleAt`.
- Any need to edit upstream AI Platform snapshots.
- Any breaking contract change.
- Any loss of existing payload-only behavior.
