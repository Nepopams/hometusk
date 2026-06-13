# ST-SHOP-001-005 — Manual Shopping Flow without AI

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT-2026Q3-shopping-manual-flow.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`
- Contracts/OpenAPI: `docs/contracts/http/commands.openapi.yaml`
- API coverage: `docs/mvp/api-coverage.md`
- ADR boundary: `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`
- Task-shopping linkage ADR: `docs/architecture/decisions/008-stage5-task-shopping-linkage.md`
- Service catalog: `docs/architecture/service-catalog.md`

## Outcome
Users can complete the core shopping journey without AI: create a shopping list from an empty state, add manual items with quantity/unit/category/source, link/unlink items to household tasks, see linked purchases in task detail, and add linked purchases from task detail.

## Acceptance Criteria
- [x] Member can create a shopping list via `POST /api/v1/households/{householdId}/shopping-lists`.
- [x] Non-member cannot create a list in a household.
- [x] Shopping empty state exposes a create-list CTA and redirects to the new list.
- [x] Manual add item supports quantity, unit, category, source, and optional `linkedTaskId`.
- [x] Manual update supports category/source and `linkedTaskId`, with explicit `null` unlink.
- [x] Manual create/update rejects cross-household `linkedTaskId` without silently unlinking.
- [x] Task detail shows linked shopping items and supports adding a linked item.
- [x] API docs and coverage matrix describe the new/manual behavior.
- [x] Existing command/AI add shopping item behavior remains compatible and keeps safe unlinked fallback.

## Non-goals
- Custom category CRUD.
- Source/store presets.
- Price, budgeting, marketplace cart creation, or multi-store run redesign.
- AI category suggestions or upstream AI Platform contract changes.
- Rewriting command pipeline or task creation flow.

## Files to change
- `services/backend/src/main/java/com/hometusk/shopping/dto/CreateShoppingListRequest.java` — create-list DTO.
- `services/backend/src/main/java/com/hometusk/shopping/dto/AddShoppingItemRequest.java` — add `linkedTaskId`.
- `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateShoppingItemRequest.java` — add link/unlink presence tracking.
- `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java` — expose list create and pass link fields.
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java` — create list and strict manual task-link validation.
- `services/backend/src/test/java/com/hometusk/integration/ShoppingControllerTest.java` — backend happy/negative coverage.
- `clients/web/src/types/api.ts`, `clients/web/src/lib/api.ts`, `clients/web/src/hooks/useShoppingLists.ts`, `clients/web/src/hooks/useShoppingItems.ts`, `clients/web/src/lib/shoppingMetadata.ts` — client API and local state.
- `clients/web/src/routes/ShoppingLists.tsx`, `clients/web/src/routes/ShoppingLists.css` — create-list UI.
- `clients/web/src/routes/ShoppingDetail.tsx`, `clients/web/src/routes/ShoppingDetail.css` — quantity/unit and task selector.
- `clients/web/src/routes/TaskDetail.tsx`, `clients/web/src/routes/TaskDetail.css` — linked shopping block and add-from-task flow.
- `clients/web/src/i18n/translations.ts` — UI labels.
- `docs/contracts/http/commands.openapi.yaml`, `docs/mvp/api-coverage.md`, `docs/architecture/service-catalog.md`, `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`, `docs/_indexes/contracts-index.md` — contract/docs delta.
- `docs/planning/initiatives/INIT-2026Q3-shopping-manual-flow.md`, `docs/planning/strategy/roadmap.md` — closure status.

## Implementation plan
### Commit 1 — Backend manual shopping API
Steps:
1. Add create-list endpoint and DTO validation.
2. Add strict manual task link validation for item create/update.
3. Add integration tests for create list, same-household link, cross-household reject, and unlink.
Verification:
- `cd services/backend && ./gradlew test --tests com.hometusk.integration.ShoppingControllerTest`

### Commit 2 — Web manual shopping flow
Steps:
1. Add create-list empty/header flow.
2. Add quantity/unit/category/source/task link controls in shopping detail.
3. Add task-detail shopping block actions and add-linked-item modal.
Verification:
- `cd clients/web && npm run build`
- `cd clients/web && npm test -- --run`

### Commit 3 — Docs and closure
Steps:
1. Update OpenAPI/API coverage/service catalog/ADR note/contract index.
2. Mark initiative and roadmap closed if checks pass.
Verification:
- `./scripts/test.sh` when local infra/runtime allows.

## Contract impact
- Provider: HomeTusk backend.
- Consumer: Web frontend and future API clients.
- Protocol/version: HTTP API v1, non-breaking additive changes.
- Delta: `POST /households/{householdId}/shopping-lists`; optional `linkedTaskId` on add/update item; null unlink semantics.
- Compatibility: existing clients remain valid because new request fields are optional and existing response shape is unchanged except previously documented `linkedTaskId`.

## Docs updates
- [x] `docs/contracts/http/commands.openapi.yaml`
- [x] `docs/mvp/api-coverage.md`
- [x] `docs/architecture/service-catalog.md`
- [x] `docs/architecture/decisions/009-mvp-commands-vs-crud-boundary.md`
- [x] `docs/_indexes/contracts-index.md`

## Tests
- [x] Backend integration tests for list creation.
- [x] Backend integration tests for manual linked item create/update/unlink.
- [x] Backend negative test for cross-household linked task.
- [x] Frontend build/typecheck.

## Verification Evidence
- `cd clients/web && npm run build` — pass.
- `cd clients/web && npm test -- --run` — pass.
- `cd clients/web && npm run lint` — pass.
- `cd services/backend && ./gradlew compileJava compileTestJava --no-daemon` — pass.
- `cd services/backend && ./gradlew test --tests com.hometusk.integration.ShoppingControllerTest --no-daemon --info --stacktrace` — pass.
- `cd services/backend && ./gradlew test --info --no-daemon` — pass.
- `cd services/backend && ./gradlew spotlessCheck --no-daemon` — pass.
- `git diff --check` — pass; Git reports expected CRLF conversion warnings only.
- `./scripts/test.sh` — not runnable in this Windows session because `/bin/bash`/WSL is unavailable; covered by equivalent Gradle backend test plus web checks above.

## Verification commands
- `cd services/backend && ./gradlew test --tests com.hometusk.integration.ShoppingControllerTest`
- `cd clients/web && npm run build`
- `cd clients/web && npm test -- --run`
- `./scripts/test.sh`

## DoD checklist
- [x] Tests pass.
- [x] Household boundaries verified for list/item/task linkage.
- [x] Docs/contracts updated.
- [x] No upstream AI Platform snapshots changed.
- [x] Existing command-driven `add_shopping_item` remains compatible.

## Risks
- Manual and command shopping paths diverge -> keep shared `ShoppingService` validation helpers while preserving strict-vs-safe semantics.
- Cross-household task leak -> use household-scoped task lookup and 404 for invalid links.
- UI scope creep -> keep one modal and existing compact page structure.

## Rollback
- Revert the commit(s). No schema migration is required for this workpack.

## Prompt Pack
- PLAN: completed inline in this workpack and Codex thread.
- APPLY: delegated by user; Gate C self-approved for the files above.
- REVIEW: separate read-only review gate after implementation.
