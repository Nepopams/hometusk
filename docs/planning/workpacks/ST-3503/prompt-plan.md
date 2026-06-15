# Codex PLAN Prompt - ST-3503

Read-only PLAN for household home read models.

## Scope

- Inspect `clients/mobile`, `clients/web` read hooks/API, `docs/contracts/http/commands.openapi.yaml`, and backend controllers for household read endpoints.
- Confirm whether ST-3503 requires any contract/backend changes.
- Identify mobile files to update and verification commands.

## Stop Conditions

- Stop if a new backend aggregation endpoint is required.
- Stop if sensitive token storage outside SecureStore is needed.
- Stop if mobile would need direct AI Platform access.

## Expected Output

- Findings with paths.
- Approved APPLY scope.
- Risks and verification.
