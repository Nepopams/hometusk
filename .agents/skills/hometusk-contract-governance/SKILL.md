---
name: hometusk-contract-governance
description: Use when a HomeTusk change affects HTTP APIs, DTOs, JSON Schemas, events, external integrations, AI Platform adapter contracts, or compatibility notes.
---

# HomeTusk Contract Governance

## Purpose

Keep integration boundaries contract-first and prevent drift between public
behavior, schemas, examples, and implementation plans.

## Sources

- `docs/contracts/**`
- `docs/_indexes/contracts-index.md`
- `docs/integration/**`
- `docs/integration/ai-platform/v1/upstream/**` as read-only upstream truth
- related ADRs under `docs/adr/**` or legacy `docs/architecture/decisions/**`
- relevant planning artifacts under `docs/planning/**`

## Workflow

1. Identify provider, consumer, protocol, version, and artifact path.
2. Describe the contract delta:
   - endpoints;
   - fields and validation;
   - error semantics;
   - compatibility posture.
3. Decide whether the change is breaking.
4. Add examples for happy path and at least one edge or error case when adding a
   new surface.
5. Update `docs/_indexes/contracts-index.md` when a contract is added or
   materially changed.
6. Require a human artifact gate before implementation for external behavior
   changes.

## Allowed scope

- Create or update HomeTusk-owned contract docs under `docs/contracts/**`.
- Update contract indexes and compatibility notes.
- Update HomeTusk-owned integration mapping outside upstream snapshots.

## Forbidden scope

- Do not edit `docs/integration/ai-platform/v1/upstream/**`.
- Do not implement controllers, DTOs, services, migrations, or tests.
- Do not create breaking changes without versioning or an approved migration plan.

## Output

Respond in Russian with contract surfaces, compatibility decision, files changed,
required gates, and implementation handoff notes.
