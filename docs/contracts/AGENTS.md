# Contract Instructions

This directory owns HomeTusk-owned HTTP, schema, event, and external adapter
contracts.

## Rules

- Contract changes must be designed before implementation.
- Update `docs/_indexes/contracts-index.md` when adding or materially changing a
  contract.
- Include compatibility notes for breaking and non-breaking changes.
- Include examples for happy path and at least one edge or error case when a
  new contract surface is introduced.
- Do not edit externally owned upstream snapshots in
  `docs/integration/ai-platform/v1/upstream/**`.
- Runtime code must adapt to external upstream contracts, not the reverse.
