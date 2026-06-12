# Architecture Decision Records (ADR)

This directory contains Architecture Decision Records for HomeTusk project.

## What is an ADR?

An ADR documents a **significant architectural decision**, including:
- Context (why we need to decide)
- Decision (what we chose)
- Consequences (trade-offs)
- Alternatives considered (what we rejected and why)

## When to Create an ADR

Create an ADR when the decision:
- Affects system structure (new service, component boundary)
- Affects integration points (new external system, API change)
- Affects non-functional requirements (performance, security, scalability)
- Is **hard to reverse** (tech stack, data model, authentication mechanism)

**Do NOT create ADR for**:
- Tactical code choices (which library for parsing, variable naming)
- Temporary scaffolding or prototypes
- Decisions already documented in contracts or diagrams

## Naming Convention

```
{number}-{short-kebab-case-title}.md
```

Examples:
- `001-mvp-voice-task-scenario.md`
- `004-stage2-ai-platform-integration.md`
- `009-mvp-commands-vs-crud-boundary.md`

## ADR Template

```markdown
# ADR-XXX: [Title]

**Status**: proposed | accepted | superseded | rejected
**Date**: YYYY-MM-DD
**Supersedes**: ADR-YYY (if applicable)

## Context
What problem are we solving? What constraints exist?

## Decision
What did we decide to do?

## Consequences
### Positive
- Benefit 1
- Benefit 2

### Negative
- Trade-off 1
- Technical debt introduced

## Alternatives Considered
### Option A: [Name]
- Pros: ...
- Cons: ...
- Why rejected: ...

### Option B: [Name]
- Pros: ...
- Cons: ...
- Why rejected: ...
```

## ADR Lifecycle

1. **Proposed**: Under review, not yet implemented
2. **Accepted**: Approved and implemented
3. **Superseded**: Replaced by newer ADR (link to successor)
4. **Rejected**: Decided not to proceed (document why)

---

**Maintenance**:
- Use the `hometusk-adr-diagram-governance` skill when creating/updating ADRs
- Update `docs/_indexes/adr-index.md` when adding new ADR

See also:
- `docs/architecture/decisions/` — Existing ADRs (to be migrated to `docs/adr/`)
- `docs/_indexes/adr-index.md` — Master index
