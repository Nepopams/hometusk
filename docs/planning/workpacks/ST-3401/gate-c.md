# Gate C: ST-3401 - Minimal Household Dashboard Home

## Decision
GO.

## Date
2026-06-13

## Approver
Delegated to Codex by the active user goal.

## Basis
- PLAN findings are recorded in `docs/planning/workpacks/ST-3401/plan-findings.md`.
- The NOW slice avoids new backend contracts and uses existing household-scoped endpoints.
- Allowed runtime files are limited to dashboard route/styles, sidebar navigation, and translations.
- Security boundary risk is bounded by existing current-household hooks and `ProtectedRoute requireHousehold`.

## Conditions
- Do not add a new dashboard endpoint.
- Do not edit OpenAPI, upstream snapshots, ADRs, diagrams, or service catalog.
- Stop if implementation requires backend behavior changes or cross-household aggregation.
