# API Contracts Index

This index tracks all API contracts (OpenAPI, JSON Schema, AsyncAPI) in the project.

## How to Use

- **Contract**: Name/identifier of the contract
- **Consumer**: Who calls this API
- **Provider**: Who implements this API
- **Version**: Current version (semver)
- **Status**: `draft` | `stable` | `deprecated`
- **Link**: Relative path from repo root

## Index

| Contract | Consumer | Provider | Version | Status | Link |
|----------|----------|----------|---------|--------|------|
| Commands API | Frontend (future) | HomeTusk Backend | v1 | stable | [Link](../contracts/http/commands.openapi.yaml) |
| Routines API | Web Frontend | HomeTusk Backend | v1 | draft | [Link](../contracts/http/routines.openapi.yaml) |
| ASR Proxy API | Web/Bot | HomeTusk Backend | v1 | draft | [Link](../contracts/http/asr-proxy.openapi.yaml) |
| Shopping Marketplaces API | Web Frontend | HomeTusk Backend | v1 | draft | [Link](../contracts/http/shopping-marketplaces.openapi.yaml) |
| AI Platform Decision API | HomeTusk Backend | AI Platform (external) | v1 | stable | [Link](../contracts/external/ai-platform.decision.openapi.yaml) |
| Task Schema | Commands API | Backend | v1 | stable | [Link](../contracts/schemas/task.schema.json) |
| Command Schema | Commands API | Backend | v1 | stable | [Link](../contracts/schemas/command.schema.json) |

## Contract Lifecycle

1. **Draft**: Contract under design, not implemented
2. **Stable**: Contract implemented, breaking changes require versioning
3. **Deprecated**: Contract scheduled for removal, migration path provided

## Upstream Contracts (Read-Only)

| Contract | Source | Local Path |
|----------|--------|------------|
| AI Platform Decision API (upstream) | AI Platform team | [Link](../integration/ai-platform/v1/upstream/) |
| ASR Service API (upstream) | ASR Service team | [Link](../contracts/external/asr-service/asr/openapi.yaml) |

**CRITICAL**: Upstream contracts are **READ-ONLY**. Changes require coordination with external team.

---

**Maintenance**: Update this index when using `hometusk-contract-governance` or manually adding contracts.

## Material Contract Notes

- 2026-06-13 — `commands.openapi.yaml` and `shopping-marketplaces.openapi.yaml`
  include the non-breaking INIT-2026Q3-shopping-categories contract delta:
  optional `category`/`source` metadata on shopping items, list filters,
  partial item update semantics, and shopping run/export propagation. Runtime
  implementation is tracked by `docs/planning/workpacks/ST-3101/workpack.md`.
- 2026-06-13 — `commands.openapi.yaml` includes the non-breaking
  INIT-2026Q3-shopping-manual-flow delta: manual shopping list creation,
  optional `linkedTaskId` on manual add/update item, and explicit null unlink
  semantics. Runtime implementation is tracked by
  `docs/planning/workpacks/ST-SHOP-001-005/workpack.md`.
- 2026-06-13 — `commands.openapi.yaml` includes the non-breaking
  INIT-2026Q3-command-attributes ST-3301 delta: optional command-level
  `dueDate`, `assigneeId`, and `zoneId` for immediate `create_task` commands.
  Existing payload-only clients remain compatible; conflicting top-level and
  payload attributes reject with `COMMAND_ATTRIBUTE_CONFLICT`. Runtime
  implementation is tracked by `docs/planning/workpacks/ST-3301/workpack.md`.
- 2026-06-13 — `commands.openapi.yaml` includes the non-breaking
  INIT-2026Q3-command-attributes ST-3303 delta: optional one-off `scheduleAt`
  for command submission and a `scheduled` response variant. Scheduled
  commands validate at submission and are revalidated at due-time execution.
  Runtime implementation is tracked by
  `docs/planning/workpacks/ST-3303/workpack.md`.
