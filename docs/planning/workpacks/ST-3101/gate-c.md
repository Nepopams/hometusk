# Gate C — ST-3101 Codex PLAN Approval

## Sources of Truth
- Initiative: `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`
- Epic: `docs/planning/epics/EP-031/epic.md`
- Story: `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`
- Workpack: `docs/planning/workpacks/ST-3101/workpack.md`
- PLAN findings: `docs/planning/workpacks/ST-3101/plan-findings.md`
- Checklist: `docs/planning/workpacks/ST-3101/checklist.md`
- Contracts: `docs/contracts/http/commands.openapi.yaml`
- Contracts: `docs/contracts/http/shopping-marketplaces.openapi.yaml`

## Decision
- Decision: GO
- Decider: Human
- Date: 2026-06-13

## Scope Recap

### Approved for APPLY after GO
- Add nullable category/source metadata to shopping items.
- Add category/source snapshots to shopping run items.
- Extend DTOs, REST requests/responses, filters, partial PATCH, command-created item path, validation policy, run snapshots, exports, and service catalog.
- Add migration and backend tests listed in the workpack.

### Out of Scope
- Web UI controls, grouping, filters, or badges.
- Household source presets.
- Price/budget tracking.
- AI auto-categorisation.
- Multi-store runs or delegation.
- AI Platform upstream contract changes.

## Required Evidence Before Gate D
- `cd services/backend && ./gradlew test --tests "*ShoppingControllerTest*"`
- `cd services/backend && ./gradlew test --tests "*ShoppingIntegrationTest*"` if command pipeline code changes
- `cd services/backend && ./gradlew test --tests "*ShoppingRunEndpointIntegrationTest*"`
- `cd services/backend && ./gradlew test --tests "*ShoppingExportIntegrationTest*"`
- `cd services/backend && ./gradlew test --tests "*ShoppingExportServiceTest*"`
- `cd services/backend && ./gradlew spotlessCheck`
- `./scripts/test.sh` before review gate if runtime and environment allow it.

## Risks Accepted by Gate C GO
- Draft export CSV header intentionally changes to include `category` and `source`.
- Category taxonomy is intentionally fixed for NOW: `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other`.
- Existing items remain uncategorised until edited or recreated.
- AI Platform upstream snapshots stay read-only; HomeTusk adapts locally and degrades safely for unsupported shapes.

## Approval Notes
Approved by human message: "Утверждаю Gate C для ST-3101".
