# AI Platform Integration

HomeTusk consumes the external AI Platform as a decision provider. HomeTusk is
the product-service, execution authority, guardrail authority, and audit source
of truth.

## Packages

| Package | Status | Purpose |
| --- | --- | --- |
| `v2.1/` | active intake | HomeTusk-owned intake package for AI Platform provider contract `2.1.0`. |
| `v1/` | superseded for new work | Historical package and migration context. Its `upstream/**` subtree remains read-only. |

## Non-Negotiable Boundaries

- Mobile and web clients must not call AI Platform directly.
- Provider tests are necessary but not sufficient for HomeTusk acceptance.
- AI output must be schema-validated before mapping.
- HomeTusk domain invariants remain enforced in HomeTusk code.
- Unsupported or unknown provider output must reject safely or degrade safely.
- `confirm` is non-executing until HomeTusk has a first-class
  `needs_confirmation` contract.
- `answer` remains blocked.
