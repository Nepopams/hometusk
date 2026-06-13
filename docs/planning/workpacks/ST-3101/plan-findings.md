# PLAN Findings: ST-3101 — Shopping Category/Source Backend + Contract Foundation

## Status
Read-only PLAN completed on 2026-06-13.

No runtime code was changed during PLAN.

## Sources Read
- `AGENTS.md`
- `docs/CODEX-WORKFLOW.md`
- `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`
- `docs/planning/epics/EP-031/epic.md`
- `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`
- `docs/planning/workpacks/ST-3101/workpack.md`
- `docs/planning/workpacks/ST-3101/checklist.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- `docs/_governance/dod.md`
- `services/backend/build.gradle.kts`
- `services/backend/src/main/java/com/hometusk/shopping/**`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/ShoppingItemValidationPolicy.java`
- relevant shopping integration and unit tests under `services/backend/src/test/java/**`

## Current-State Findings
- `ShoppingItem` currently stores `name`, `quantity`, `unit`, purchase state, creator, command/task links, idempotency key, and timestamps. It has no category/source metadata.
- Direct REST shopping item creation goes through `ShoppingController.addItem(...)` and `ShoppingService.addItemDirect(...)`.
- Command-created shopping items go through `ActionExecutor.executeAddShoppingItemFromAction(...)` and `ShoppingService.addItem(AddItemRequest)`. If HomeTusk accepts category/source action parameters locally, this path must validate and store them; while upstream omits them, tests should prove explicit null preservation.
- `ShoppingItemValidationPolicy` validates only item name today. If HomeTusk action parameters accept category/source, category validation must be added either in this policy or in `ShoppingService` with safe rejection.
- `ShoppingRunItem.fromShoppingItem(...)` snapshots name/quantity/unit only. It must copy category/source to satisfy AC-7.
- `ShoppingExportService.exportAsCsv(...)` currently emits `name,quantity,unit,purchased`. ST-3101 intentionally changes the draft export surface to include `category,source`.
- Existing migrations run through `V025__create_shopping_runs.sql`; the next migration should be `V026__add_shopping_item_category_source.sql` unless a new migration appears first.
- Backend uses Spring Boot 3.2.1, Java 21, Flyway, Spotless, JUnit 5, MockMvc, and Testcontainers from `services/backend/build.gradle.kts`.

## Implementation File List

### Create
- `services/backend/src/main/resources/db/migration/V026__add_shopping_item_category_source.sql`
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItemCategory.java`

### Modify
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItem.java`
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunItem.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/AddShoppingItemRequest.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateShoppingItemRequest.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunItemDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingItemRepository.java`
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java`
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingRunService.java`
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingExportService.java`
- `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/ShoppingItemValidationPolicy.java`
- `services/backend/src/test/java/com/hometusk/integration/ShoppingControllerTest.java`
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunEndpointIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingRunTest.java`
- `services/backend/src/test/java/com/hometusk/shopping/service/ShoppingExportServiceTest.java`
- `docs/architecture/service-catalog.md`

## Contract Compatibility
- `category` and `source` are additive optional fields.
- Existing POST payloads without category/source remain valid.
- Existing records return `category=null` and `source=null`.
- PATCH becomes more permissive because `purchased` is no longer the only mutable field.
- Runtime must still reject empty PATCH bodies with 400.
- Export CSV header changes are acceptable because `shopping-marketplaces.openapi.yaml` is draft, but tests and docs must be updated deliberately.

## Migration Plan
- Add nullable `category` and `source` columns to `shopping_items`.
- Add nullable `category` and `source` snapshot columns to `shopping_run_items`.
- Add list-scoped indexes for filters, for example `(shopping_list_id, category)` and `(shopping_list_id, source)`.
- Use safe guards consistent with existing migrations.
- Existing rows remain valid with null values.

## Rollback Notes
- Code rollback is safe because fields are additive.
- DB rollback, if needed in a controlled environment:
  - `ALTER TABLE shopping_run_items DROP COLUMN IF EXISTS source;`
  - `ALTER TABLE shopping_run_items DROP COLUMN IF EXISTS category;`
  - `ALTER TABLE shopping_items DROP COLUMN IF EXISTS source;`
  - `ALTER TABLE shopping_items DROP COLUMN IF EXISTS category;`

## Acceptance Criteria Mapping
- AC-1: `ShoppingControllerTest` legacy POST without category/source.
- AC-2: `ShoppingControllerTest` POST with `category=groceries`, `source=Perekrestok`.
- AC-3: `ShoppingControllerTest` invalid category on POST and PATCH returns 400; data unchanged.
- AC-4: `ShoppingControllerTest` category/source filters with same labels in another household prove no leakage.
- AC-5: `ShoppingControllerTest` category/source-only PATCH preserves `purchased=false`.
- AC-6: `ShoppingControllerTest` empty PATCH returns 400.
- AC-7: `ShoppingRunTest` and `ShoppingRunEndpointIntegrationTest` prove snapshot/response propagation.
- AC-8: `ShoppingExportServiceTest` and `ShoppingExportIntegrationTest` prove text/CSV metadata output.
- AC-9: `ShoppingIntegrationTest` or equivalent command pipeline coverage proves add_shopping_item commands still create items with null metadata when upstream omits category/source; upstream snapshots remain unchanged.

## STOP-THE-LINE Conditions
- A new migration already uses `V026`.
- Implementing category/source requires changing AI Platform upstream contracts.
- Category must become household-custom taxonomy in NOW.
- PATCH cannot be made backward-compatible while preserving existing mark/unmark behavior.
- Any filter implementation bypasses existing household/list membership checks.

## Gate C Recommendation
GO for Codex APPLY after explicit Human Gate C approval.

The implementation scope is clear, contract-backed, and testable.
