# Services Instructions

Runtime services live here. Follow root `AGENTS.md` invariants and keep changes
small.

## Backend rules

- The current active runtime is the unified backend in `services/backend/`.
- Enforce domain invariants in code, not prompts.
- Preserve command traceability: `Command`, `DecisionLog`, `TaskActivity`, and
  correlation ID propagation.
- Preserve degraded mode when AI Platform is unavailable.
- Do not add new production dependencies without explicit justification.
- If services, endpoints, schemas, or command pipeline behavior change, update
  contracts, service catalog, ADRs, diagrams, and indexes as required.

## Verification

Prefer the repository-level command when practical:

```bash
./scripts/test.sh
```

For backend-only checks, use existing Gradle tasks from `services/backend`.
