# Diagram Instructions

This directory is the canonical location for new architecture and flow diagrams.

## Rules

- Update diagrams only for structural, flow, deployment, or integration boundary
  changes.
- Prefer text-based diagrams such as PlantUML or Mermaid.
- Keep the diagram level minimal and useful; avoid deep C4 levels unless they
  reduce real implementation or review risk.
- Update `docs/_indexes/diagrams-index.md` when adding or replacing diagrams.
- Legacy diagrams under `docs/architecture/diagrams/**` and
  `docs/architecture/decisions/mvp/**` remain valid references until migrated.
