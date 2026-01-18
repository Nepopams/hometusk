# Architecture Diagrams

This directory contains architecture and flow diagrams for HomeTusk project.

## Diagram Types

| Type | Purpose | Tool | Example |
|------|---------|------|---------|
| C4 Level 1 (Context) | System boundaries, external actors | Mermaid/PlantUML | System context |
| C4 Level 2 (Container) | Services, data stores | Mermaid/PlantUML | Container diagram |
| C4 Level 3 (Component) | Internal structure of a service | Mermaid/PlantUML | Backend components |
| Sequence | Request/response flow | Mermaid/PlantUML | Command processing |
| Flow | Business process flow | Mermaid/draw.io | Task lifecycle |
| ERD | Database schema | Mermaid/dbdiagram.io | Domain model |

## When to Create/Update

Run `diagram-steward` agent when:
- New service or component added
- Service boundaries change
- Integration points change
- Command/data flow changes

**Do NOT update diagrams for**:
- Internal refactoring (no boundary change)
- Bug fixes
- Code quality improvements

## Diagram Format

Prefer **text-based diagrams** (Mermaid, PlantUML) over binary formats:
- Version control friendly (git diff works)
- Easy to update
- Renderable in Markdown

## Naming Convention

```
{type}-{short-name}.md
```

Examples:
- `c4-system-context.md`
- `sequence-command-processing.md`
- `flow-task-lifecycle.md`

## Diagram Template

```markdown
# [Diagram Title]

**Type**: C4 Level 1 | Sequence | Flow | ERD
**Last Updated**: YYYY-MM-DD
**Status**: current | proposed | superseded

## Purpose
What does this diagram explain?

## Diagram

\`\`\`mermaid
graph TD
  A[User] --> B[Backend]
  B --> C[Database]
\`\`\`

## Notes
- Key decision 1
- Key decision 2
```

---

**Maintenance**:
- Run `diagram-steward` agent when creating/updating diagrams
- Update `docs/_indexes/diagrams-index.md` when adding new diagram

See also:
- `docs/architecture/decisions/mvp/` — Existing C4 diagrams (to be migrated to `docs/diagrams/`)
- `docs/_indexes/diagrams-index.md` — Master index
