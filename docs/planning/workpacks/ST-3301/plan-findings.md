# PLAN Findings: ST-3301 - Command Structured Attributes Contract/Backend Foundation

## Result
**Gate C recommendation: GO.** The slice is implementable as a backward-compatible Commands API delta without touching AI Platform upstream snapshots, frontend code, or scheduling lifecycle.

## Current-State Findings
- `docs/contracts/http/commands.openapi.yaml` defines `CommandRequest` with `householdId`, `type`, `payload`, `source`, and `clientTimestamp`; new command-level attributes are absent.
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java` has no `dueDate`, `assigneeId`, or `zoneId`.
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java` stores only raw JSONB payload and has no first-class attribute columns.
- `CommandService.execute` creates the `Command` before schema/business validation, which is good for DecisionLog on rejected validation paths.
- `BusinessValidator.validateCreateTask` already enforces assignee membership, zone household scope, and future deadline when those values are present in `CreateTaskPayload`.
- `ManualDecisionProvider` and `ActionExecutor` already propagate `assigneeId`, `zoneId`, and `deadline` from the effective create-task payload into the created task.
- `CommandIdempotencyService` hashes the full `CommandRequest`, so new DTO fields will naturally participate in idempotency once added.
- Existing command integration tests cover payload-only happy path, invalid assignee/zone/deadline, and idempotency basics.

## Implementation Decision
Use a normalization layer in `CommandService`:
1. Persist the command with raw payload plus nullable explicit command attributes.
2. For `CREATE_TASK`, build an effective payload map before schema/business validation.
3. Merge top-level `assigneeId`, `zoneId`, and `dueDate` into effective payload fields `assigneeId`, `zoneId`, and `deadline`.
4. If a payload field and top-level field are both present with different values, reject with `BUSINESS_RULE_VIOLATION` and violation rule `COMMAND_ATTRIBUTE_CONFLICT`.
5. Pass the effective payload to schema validation, business validation, decision context, and continuation execution.

## File List For APPLY
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

## Migration Shape
- Add nullable `commands.due_date TIMESTAMP WITH TIME ZONE`.
- Add nullable `commands.assignee_id UUID`.
- Add nullable `commands.zone_id UUID`.
- Add comments. No indexes required for ST-3301 immediate execution.

## Tests
- Extend `CommandPipelineTest` with command-level attribute happy path and persisted command assertions.
- Keep existing payload-only full payload test as backward compatibility evidence.
- Add conflict rejection test.
- Add top-level invalid assignee, invalid zone, and past dueDate tests.
- Extend `CommandIdempotencyIntegrationTest` for same attributes replay and changed attribute conflict.

## Verification Commands
- `cd services/backend && ./gradlew test --tests "*Command*"`
- `./scripts/test.sh`

## Risks
- Continuation could lose explicit attributes if normalization only handles initial execute; APPLY must merge persisted attributes during `continueCommand`.
- Contract wording must avoid implying `scheduleAt` is supported in ST-3301.
- Conflict comparison must normalize UUID/date-time strings to avoid false conflicts.

## STOP-THE-LINE
- Runtime requires accepting `scheduleAt` to complete ST-3301.
- Attribute support cannot preserve existing payload-only behavior.
- DecisionLog is not written on validation/business rejection.
- Idempotency hash excludes new attributes.
