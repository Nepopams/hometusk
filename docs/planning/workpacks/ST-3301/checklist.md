# DoD Checklist: ST-3301 - Command Structured Attributes Contract/Backend Foundation

## Readiness
- [x] Active initiative identified from roadmap.
- [x] Story has testable acceptance criteria.
- [x] Impact flags recorded.
- [x] Artifact gate identified and delegated GO for non-breaking contract delta.
- [x] Codex PLAN prompt generated.
- [x] Codex PLAN findings recorded.
- [x] Gate C delegated approval recorded after PLAN.

## Contract
- [x] `CommandRequest.dueDate` documented as optional date-time.
- [x] `CommandRequest.assigneeId` documented as optional UUID.
- [x] `CommandRequest.zoneId` documented as optional UUID.
- [x] Existing payload-only examples remain valid.
- [x] Attribute happy-path example added.
- [x] Conflict/error semantics documented.
- [x] Contract index material note updated.

## Backend
- [x] `CommandRequest.java` includes nullable fields.
- [x] `Command.java` persists nullable explicit attributes.
- [x] Flyway migration adds nullable columns.
- [x] Effective create_task payload uses command-level attributes.
- [x] Conflicting top-level vs payload values reject before action execution.
- [x] Command-level `dueDate` maps to task `deadline`.
- [x] Existing payload-only path remains unchanged.
- [x] DecisionLog is written for success and rejected validation paths.

## Security / Boundaries
- [x] Top-level assignee must be a household member.
- [x] Top-level zone must exist in household.
- [x] Past dueDate rejects.
- [x] Error responses do not leak cross-household details.

## Idempotency
- [x] Same key + same request with attributes replays stored response.
- [x] Same key + changed attribute returns 409.

## Verification
- [x] `cd services/backend && ./gradlew test --tests "*Command*"` passes.
- [x] `./scripts/test.sh` passes or blocker documented.
- [x] Review gate completed with GO before Gate D.

## Final
- [x] Workpack evidence updated after APPLY.
- [x] Gate D decision recorded.
