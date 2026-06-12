# AI Platform Integration Instructions

This package documents how HomeTusk consumes the external AI Platform.

## Non-negotiable upstream rule

`docs/integration/ai-platform/v1/upstream/**` contains externally owned upstream
contract snapshots. Treat that subtree as read-only:

- do not edit schemas, examples, README files, VERSION files, or any other file
  under `upstream/**`;
- adapt HomeTusk-owned mapping, wrapper contracts, runtime code, and docs outside
  `upstream/**`;
- coordinate upstream contract changes through ADR/contract governance before
  implementation.

Unsupported upstream response or action types must degrade safely to Clarify or
Reject; do not throw unexpected runtime exceptions for contract drift.
