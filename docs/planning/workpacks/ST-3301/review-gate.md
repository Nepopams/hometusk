# Review Gate: ST-3301 - Command Structured Attributes Contract/Backend Foundation

## Review Result: GO

Date: 2026-06-13

## Scope Reviewed
- Workpack: `docs/planning/workpacks/ST-3301/workpack.md`
- Story: `docs/planning/epics/EP-033/stories/ST-3301-command-structured-attributes-backend.md`
- Runtime diff: command request DTO, command entity, command service normalization, Flyway migration, command integration tests.
- Contract/docs diff: Commands OpenAPI, contracts index, service catalog, EP-033/ST-3301 planning artifacts.

## Must-fix
None.

## Should-fix
None.

## Evidence
- Command-level `dueDate`, `assigneeId`, and `zoneId` are persisted on `Command` and applied through an effective `create_task` payload before schema/business validation.
- Payload-only `create_task` requests remain supported.
- Top-level/payload conflicts reject with `BUSINESS_RULE_VIOLATION` and `COMMAND_ATTRIBUTE_CONFLICT` before task creation.
- Existing `BusinessValidator` enforces assignee membership, zone household scope, and future deadline on the normalized payload.
- DecisionLog failure paths remain active because rejected schema/business validation occurs after command creation and inside the existing validation-failure catch blocks.
- Idempotency covers new top-level fields through full `CommandRequest` hashing, with replay/conflict integration coverage.
- No files under `docs/integration/ai-platform/v1/upstream/**` were changed.
- Review found and fixed one planning-doc anchor drift before GO: new EP-033/ST-3301 docs now point at the canonical `INIT-2026Q3‑command‑attributes.md` path.

## Commands
- `cd services/backend && ./gradlew spotlessApply test --tests "*Command*"` - passed via Git Bash on 2026-06-13.
- `./scripts/test.sh` - passed via Git Bash on 2026-06-13.
- `git diff --check` - passed on 2026-06-13 with only the existing LF-to-CRLF warning for `docs/contracts/http/commands.openapi.yaml`.

## Recommendation
GO for delegated Human Gate D. ST-3301 can be considered implemented; continue the initiative with ST-3302 command attribute confirmation UI.
