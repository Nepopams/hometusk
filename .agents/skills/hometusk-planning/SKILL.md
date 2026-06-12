---
name: hometusk-planning
description: Use for HomeTusk PI planning, sprint planning, epic decomposition, story readiness, Gate A or Gate B preparation, and planning artifact updates under docs/planning.
---

# HomeTusk Planning

## Purpose

Create and maintain planning artifacts that are ready for human gates and later
workpack generation.

## Sources

Read the relevant subset:

- `docs/planning/strategy/product-goal.md`
- `docs/planning/strategy/roadmap.md`
- `docs/planning/releases/MVP.md`
- `docs/planning/initiatives/**`
- `docs/planning/pi/**`
- `docs/planning/epics/**`
- `docs/_governance/dor.md`
- `docs/_governance/dod.md`
- `docs/planning/_templates/**`

## Workflow

1. Choose planning level:
   - PI for 8-12 week or multi-epic work;
   - sprint for a committed timebox;
   - epic/story for decomposition;
   - discovery when readiness is missing.
2. Keep all artifacts source-backed.
3. Enforce DoR before committing story scope.
4. Add impact flags for contract, ADR, diagram, security, data, and traceability
   changes.
5. Keep committed scope small and demonstrable.
6. State out-of-scope explicitly.
7. Prepare Gate A or Gate B decision text when applicable.

## Allowed scope

- Create or update planning docs under `docs/planning/**`.
- Use templates from `docs/planning/_templates/**`.
- Link to contracts, ADRs, diagrams, and service catalog.

## Forbidden scope

- Do not write implementation code.
- Do not change API contracts as part of planning.
- Do not mark blocked work as Ready.
- Do not use `docs/planning/mvp.md` as a new source of truth; use
  `docs/planning/releases/MVP.md`.

## Output

Respond in Russian with changed planning artifacts, readiness status, gates,
risks, and next workflow.
