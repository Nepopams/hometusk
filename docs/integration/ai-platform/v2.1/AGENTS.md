# AI Platform Integration v2.1 Instructions

This package documents how HomeTusk consumes AI Platform provider contract
`2.1.0`.

## Upstream Snapshot Rule

`upstream/**` is a HomeTusk-local snapshot of externally owned AI Platform
contract inputs. Treat it as read-only after import:

- do not hand-edit schemas under `upstream/**`;
- update mappings, compatibility notes, adapter code, and tests outside
  `upstream/**`;
- import a new provider snapshot only through contract governance and record
  provider revision/source metadata.

## Runtime Boundary

HomeTusk remains execution authority. Provider `confirm` must never execute in
this initiative.
