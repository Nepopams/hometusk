# Architecture Decision Records (ADR) Index

This index tracks all ADRs in the project. Update this file when creating/superseding ADRs.

## How to Use

- **Status**: `proposed` → `accepted` → `superseded` (or `rejected`)
- **Link**: Relative path from repo root
- **Date**: Date of acceptance (YYYY-MM-DD)

## Index

| ADR ID | Title | Status | Date | Link |
|--------|-------|--------|------|------|
| ADR-001 | Voice task scenario (future) | accepted | 2024-01-XX | [Link](../architecture/decisions/001-mvp-voice-task-scenario.md) |
| ADR-002 | Text MVP scenario | accepted | 2024-01-XX | [Link](../architecture/decisions/002-mvp-text-command-scenario.md) |
| ADR-003 | Stage 1 Commands API | accepted | 2024-01-XX | [Link](../architecture/decisions/003-stage1-commands-api.md) |
| ADR-004 | Stage 2 AI Platform Integration | accepted | 2024-01-XX | [Link](../architecture/decisions/004-stage2-ai-platform-integration.md) |
| ADR-009 | Commands vs CRUD Boundary | accepted | 2024-01-XX | [Link](../architecture/decisions/009-mvp-commands-vs-crud-boundary.md) |

## Template

When creating a new ADR, use this template:

```markdown
# ADR-XXX: [Title]

**Status**: proposed | accepted | superseded | rejected
**Date**: YYYY-MM-DD
**Supersedes**: ADR-YYY (if applicable)

## Context
What is the problem we're solving?

## Decision
What did we decide to do?

## Consequences
What are the trade-offs (positive and negative)?

## Alternatives Considered
What other options did we evaluate and why were they rejected?
```

---

**Maintenance**: Update this index when running adr-designer agent or manually creating ADRs.
