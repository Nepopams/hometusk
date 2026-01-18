# API Contracts

This directory contains all API contracts for HomeTusk project.

## Structure

```
contracts/
  http/              # OpenAPI specs for HTTP APIs
  schemas/           # JSON Schema definitions
  external/          # Contracts for external systems (read-only or adapter)
  events/            # AsyncAPI specs (future)
```

## Contract Types

| Type | Format | Purpose | Example |
|------|--------|---------|---------|
| HTTP API | OpenAPI 3.x | REST endpoints | `commands.openapi.yaml` |
| Schemas | JSON Schema | Data models | `task.schema.json` |
| External | OpenAPI 3.x | 3rd-party integrations | `ai-platform.decision.openapi.yaml` |
| Events | AsyncAPI 2.x | Event-driven comms | `task-events.asyncapi.yaml` |

## Contract Lifecycle

1. **Draft**: Design in progress, not implemented
2. **Stable**: Implemented, breaking changes require versioning
3. **Deprecated**: Scheduled for removal, migration path provided

## When to Create/Update

Run `contract-writer` agent when:
- Adding new endpoint or event type
- Changing request/response schema (breaking or non-breaking)
- Integrating with external system

## Contract Package

Each contract should include:
- **Spec file**: OpenAPI/JSON Schema/AsyncAPI
- **Examples**: Sample requests/responses
- **Compatibility notes**: Breaking changes, migration guide
- **Index update**: Add to `docs/_indexes/contracts-index.md`

## Upstream Contracts (External)

Contracts in `external/` may be:
- **Owned by HomeTusk**: We define the contract (provider)
- **Owned by external team**: We consume the contract (consumer)

**CRITICAL**: If contract is owned externally, treat it as **READ-ONLY** and adapt to changes.

---

See also:
- `docs/_indexes/contracts-index.md` — Master index of all contracts
- `docs/integration/ai-platform/v1/` — AI Platform integration package
