# Gate C: Natural Command & Confirmation Contract Spike

## Decision

GO for docs-only APPLY.

## Decider

Codex, under delegated human-gate authority from the user goal.

## Date

2026-06-16

## Evidence

- Initiative scope is docs-only and explicitly forbids runtime/backend/mobile/API/provider implementation.
- PLAN findings name exact files and forbidden files.
- Current accepted Commands API and runtime models were inspected read-only.
- AI Platform `2.1.0` provider evidence is available as read-only input.
- Provider repo status is clean before APPLY.

## Approved Scope

- Roadmap and initiative status update.
- Execution notes.
- Workpack/checklist/PLAN/APPLY/review/Gate D planning artifacts.
- Draft contract spike package under `docs/research/ai-command-capabilities/natural-command-contract-spike/**`.
- Draft-only contract index entry.

## Forbidden Scope

- Accepted public OpenAPI change.
- Runtime/backend/migration/test implementation.
- Mobile/web implementation.
- AI Platform repo writes.
- Production rollout/config change.
- `answered` contract/runtime.

## Rationale

The PLAN is decision-complete and bounded to draft artifacts. Contract governance can proceed as draft-only without changing external behavior.

## Risks Accepted

- Provider intent quality has non-blocker mismatch buckets.
- HomeTusk still lacks pending confirmation runtime state.
- Future implementation may need an ADR/diagram if confirmation ownership changes service boundaries.
