# ADR Instructions

This directory is the canonical location for new Architecture Decision Records.

## Rules

- Create ADRs only for architecture-significant decisions: service boundaries,
  integration points, data model, hard-to-reverse dependencies, NFRs, security,
  operations, or contract posture.
- Do not create ADRs for routine implementation details.
- Accepted ADRs are immutable. Supersede them with a new ADR instead of editing
  the decision after acceptance.
- Update `docs/_indexes/adr-index.md` when adding or superseding an ADR.
- Legacy ADRs under `docs/architecture/decisions/**` remain valid references
  until migrated.
