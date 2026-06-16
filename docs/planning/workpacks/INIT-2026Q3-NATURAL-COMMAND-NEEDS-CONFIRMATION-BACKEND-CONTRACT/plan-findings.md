# PLAN Findings - INIT-2026Q3 Natural Command + Needs Confirmation Backend Contract

**Date:** 2026-06-16
**Mode:** Read-only exploration completed before Gate C.

## Files Read

- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/releases/MVP.md`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-and-confirmation-contract-spike.execution.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/implementation-readiness-decision.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/needs-confirmation-contract-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/confirmation-lifecycle-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/provider-confirm-mapping-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/decisionlog-traceability-v0.md`
- `docs/research/ai-command-capabilities/natural-command-contract-spike/command-response-outcomes-v0.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/_indexes/adr-index.md`
- `docs/_indexes/diagrams-index.md`
- `docs/architecture/service-catalog.md`
- `services/backend/src/main/java/com/hometusk/commands/api/CommandController.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/Command.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/DecisionLog.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/BusinessValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/DecisionLogWriter.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionContext.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionProviderSelector.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/ManualDecisionProvider.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/AiPlatformDecisionProvider.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionRequest.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/GuardrailResult.java`
- `services/backend/src/main/java/com/hometusk/commands/repository/CommandRepository.java`
- `services/backend/src/main/java/com/hometusk/commands/repository/DecisionLogRepository.java`
- `services/backend/src/main/java/com/hometusk/shared/exception/ErrorCode.java`
- `services/backend/src/main/java/com/hometusk/shared/persistence/LowercaseEnumConverter.java`
- `services/backend/src/main/java/com/hometusk/shared/persistence/CommandTypeConverter.java`
- `services/backend/src/main/java/com/hometusk/shared/persistence/CommandStatusConverter.java`
- `services/backend/src/main/resources/db/migration/V007__create_commands.sql`
- `services/backend/src/main/resources/db/migration/V008__create_decision_logs.sql`
- `services/backend/src/main/resources/db/migration/V010__add_ai_platform_fields.sql`
- `services/backend/src/main/resources/db/migration/V016__update_enum_check_constraints.sql`
- `services/backend/src/main/resources/db/migration/V027__add_command_structured_attributes.sql`
- `services/backend/src/main/resources/db/migration/V028__add_scheduled_commands.sql`
- `services/backend/src/main/resources/schemas/create-task.schema.json`
- `services/backend/src/main/resources/schemas/complete-task.schema.json`
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`
- `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java`

## Current-State Findings

- `POST /api/v1/commands` is the correct compatibility boundary; no separate AI
  endpoint is needed.
- `CommandRequest.getCommandType()` currently accepts only `create_task` and
  `complete_task`.
- `CommandType` currently contains only `CREATE_TASK` and `COMPLETE_TASK`.
- `CommandStatus` currently lacks `NEEDS_CONFIRMATION`.
- `CommandResponse` variants are `executed`, `scheduled`, `needs_input`,
  `rejected`, and `executed_degraded`; there is no `needs_confirmation`.
- `SchemaValidator` has schemas only for create/complete task.
- `ManualDecisionProvider` cannot safely execute `natural_command`; degraded mode
  for natural command must be non-mutating.
- `AiDecisionRequest.extractText()` already prefers `payload.text`, so
  `natural_command` can fit the existing provider request shape after schema/type
  support.
- `AiDecisionResponseMapper` maps provider `confirm` to
  `AI_CONFIRMATION_UNSUPPORTED`; this is the exact behavior this initiative
  replaces for supported confirm payloads.
- `DecisionLog` can store raw provider payload and external decision id, but it
  is not a workflow state store.
- Commands type/status persistence uses lowercase enum converters, so migration
  must update check constraints for `natural_command` and `needs_confirmation`.
- Existing AI Platform tests already assert provider `confirm` is non-mutating;
  these tests should be updated to expect `needs_confirmation`.

## Recommended APPLY Slice

**Gate C recommendation:** GO for a limited backend contract foundation.

Implement now:

1. Accepted additive Commands API docs for:
   - `type=natural_command`;
   - natural command payload;
   - `needs_confirmation` response;
   - pending confirmation creation semantics;
   - approve/cancel held as future endpoints.
2. ADR-022 for HomeTusk-owned pending confirmation state.
3. Sequence diagram for provider confirm to pending confirmation.
4. Backend support for `CommandType.NATURAL_COMMAND`.
5. Natural command JSON schema and explicit validation.
6. Safe non-mutating fallback for natural command when AI Platform is unavailable.
7. `DecisionResult.Confirm` and `CommandNeedsConfirmationResponse`.
8. `CommandConfirmation` persistent source of truth with `PENDING_CONFIRMATION`.
9. Provider `confirm` mapping to `needs_confirmation` for supported proposed actions.
10. Guardrail pre-check as proposal before persisting pending confirmation.
11. DecisionLog entry recording mapped `needs_confirmation`, provider raw payload,
    and HomeTusk confirmation id.
12. Tests proving no mutation before approval and compatibility with existing flows.

Hold for follow-up:

- approve/cancel endpoint implementation;
- approval idempotency;
- confirmation execution after approval;
- expiry scheduler;
- mobile/web confirmation UI;
- `answered`.

## Artifact Gate Decisions

### Contract Gate

**GO** for additive accepted Commands API contract changes listed above.

**HOLD** for approve/cancel endpoints and mobile/web client contracts until the
approval lifecycle/idempotency workpack is produced.

### ADR Gate

**GO** for a new ADR because the APPLY introduces a persistent pending
confirmation source of truth and lifecycle boundary.

Recommended artifact:

```text
docs/adr/022-pending-command-confirmation-state.md
```

### Diagram Gate

**GO** for a minimal sequence diagram because the provider confirm flow changes
command pipeline behavior and no-mutation review risk is high.

Recommended artifact:

```text
docs/diagrams/sequence-natural-command-needs-confirmation.md
```

### Security / Traceability Gate

**GO** with mandatory tests:

- provider confirm does not create tasks or shopping items;
- pending confirmation is household/requester scoped;
- raw provider payload is preserved in DecisionLog;
- public response does not expose raw provider payload;
- natural command degraded fallback is non-mutating.

## Allowed Files For APPLY

- `docs/planning/strategy/roadmap.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.md`
- `docs/planning/initiatives/INIT-2026Q3-natural-command-needs-confirmation-backend-contract.execution.md`
- `docs/planning/workpacks/INIT-2026Q3-NATURAL-COMMAND-NEEDS-CONFIRMATION-BACKEND-CONTRACT/**`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/_indexes/contracts-index.md`
- `docs/_indexes/adr-index.md`
- `docs/_indexes/diagrams-index.md`
- `docs/architecture/service-catalog.md`
- `docs/adr/022-pending-command-confirmation-state.md`
- `docs/diagrams/sequence-natural-command-needs-confirmation.md`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandType.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandStatus.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandConfirmation.java`
- `services/backend/src/main/java/com/hometusk/commands/domain/CommandConfirmationStatus.java`
- `services/backend/src/main/java/com/hometusk/commands/repository/CommandConfirmationRepository.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/CommandNeedsConfirmationResponse.java`
- `services/backend/src/main/java/com/hometusk/commands/dto/NaturalCommandPayload.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/SchemaValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/BusinessValidator.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionResult.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/ManualDecisionProvider.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/DecisionProviderSelector.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/decision/client/AiDecisionResponseMapper.java`
- `services/backend/src/main/java/com/hometusk/commands/service/CommandService.java`
- `services/backend/src/main/resources/schemas/natural-command.schema.json`
- `services/backend/src/main/resources/db/migration/V033__add_natural_command_confirmations.sql`
- `services/backend/src/test/java/com/hometusk/commands/pipeline/decision/client/AiPlatformDecisionAdapterTest.java`
- `services/backend/src/test/java/com/hometusk/integration/CommandPipelineTest.java`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/integration/aiplatform/AiPlatformIntegrationTestBase.java`

## Forbidden Files For APPLY

- `clients/**`
- `docs/integration/ai-platform/v1/upstream/**`
- `C:/Users/user/Documents/projects/VR_AI_Platform/**`
- production deployment/config files

## STOP Conditions

Stop and record HOLD if:

- pending confirmation cannot be persisted without reworking command ownership;
- provider `confirm` would need to execute actions before approval;
- tests require changing mobile/web clients;
- public contract change becomes breaking for existing structured clients;
- approval/cancel becomes necessary to keep first slice coherent.

## Verification Commands

- `cd services/backend && ./gradlew test --tests "com.hometusk.commands.pipeline.decision.client.AiPlatformDecisionAdapterTest"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.CommandPipelineTest"`
- `cd services/backend && ./gradlew test --tests "com.hometusk.integration.aiplatform.AiPlatformIntegrationTest"`
- `./scripts/test.sh` if targeted tests pass and runtime is available.

## Residual PLAN Risk

This APPLY intentionally returns `needs_confirmation` but does not implement
approval/cancel execution. That is a LIMITED-GO backend foundation, not full
confirmation product readiness. Gate D must recommend a follow-up approval/cancel
lifecycle initiative or workpack.
