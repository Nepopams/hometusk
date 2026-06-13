# DoD Checklist: ST-3101 — Shopping Category/Source Backend + Contract Foundation

## Code Quality
- [x] Code follows Java 21 / Spring Boot project conventions.
- [x] Spotless formatting checked; repo-level failure remains on pre-existing line-ending violations outside this story.
- [x] No compiler warnings introduced.
- [x] No unrelated refactors or contract drift.

## Database Migration
- [x] `V026__add_shopping_item_category_source.sql` created.
- [x] `shopping_items.category` nullable column exists.
- [x] `shopping_items.source` nullable column exists.
- [x] `shopping_run_items.category` nullable snapshot column exists.
- [x] `shopping_run_items.source` nullable snapshot column exists.
- [x] Existing records remain valid with null metadata.
- [x] Filter-supporting indexes added where appropriate.
- [x] Migration uses safe guards consistent with existing migrations.

## Domain / DTO
- [x] Shopping category allowed values implemented:
  - [x] `groceries`
  - [x] `cleaning`
  - [x] `personal_care`
  - [x] `diy`
  - [x] `electronics`
  - [x] `other`
- [x] Invalid category rejected with 400.
- [x] Source is trimmed.
- [x] Blank source becomes null.
- [x] `ShoppingItemDto` returns category/source.
- [x] `ShoppingRunItemDto` returns category/source.

## REST API
- [x] POST shopping item accepts optional category/source.
- [x] POST without category/source remains valid.
- [x] GET shopping items supports `category` filter.
- [x] GET shopping items supports `source` filter.
- [x] Existing `purchased` filter still works.
- [x] Filters compose correctly.
- [x] PATCH supports purchase status updates.
- [x] PATCH supports category/source-only updates.
- [x] PATCH empty body/no mutable field returns 400.
- [x] PATCH category/source does not change purchase state unless `purchased` is present.

## Command Pipeline
- [x] Command-created shopping items still work when upstream action payloads omit category/source.
- [x] Command-created shopping items do not silently bypass category validation if category/source action parameters are accepted.
- [x] AI Platform upstream snapshots under `docs/integration/ai-platform/v1/upstream/**` remain unchanged.
- [x] Unsupported upstream category/source shape degrades safely instead of being auto-fixed silently.

## Shopping Runs / Export
- [x] Shopping run item snapshot copies category/source.
- [x] Run detail response returns category/source.
- [x] Text export includes category/source labels when present.
- [x] CSV export includes category/source columns.
- [x] Null metadata exports as empty values.

## Security
- [x] All item queries remain list/household scoped.
- [x] Non-member access returns 403.
- [x] Filters do not bypass household boundary checks.
- [x] Source text is not logged unnecessarily.
- [x] No cross-household data leaks.

## Tests
- [x] `ShoppingControllerTest` covers create with metadata.
- [x] `ShoppingControllerTest` covers legacy create.
- [x] `ShoppingControllerTest` covers invalid category.
- [x] `ShoppingControllerTest` covers category/source filters.
- [x] `ShoppingControllerTest` covers partial PATCH.
- [x] `ShoppingControllerTest` covers empty PATCH rejection.
- [x] `ShoppingIntegrationTest` or command pipeline coverage proves command-created shopping items still work.
- [x] `ShoppingRunEndpointIntegrationTest` covers snapshot propagation.
- [x] `ShoppingExportIntegrationTest` or `ShoppingExportServiceTest` covers export metadata.
- [x] Boundary test proves filters do not leak other household data.

## Documentation
- [x] `docs/contracts/http/commands.openapi.yaml` updated.
- [x] `docs/contracts/http/shopping-marketplaces.openapi.yaml` updated.
- [x] `docs/_indexes/contracts-index.md` updated.
- [x] `docs/architecture/service-catalog.md` updated after implementation.
- [x] Workpack verification evidence recorded after APPLY.

## Build / Verification
- [x] `cd services/backend && ./gradlew test --tests "*ShoppingControllerTest*"` passes.
- [x] `cd services/backend && ./gradlew test --tests "*ShoppingIntegrationTest*"` passes if command pipeline code changes.
- [x] `cd services/backend && ./gradlew test --tests "*ShoppingRunEndpointIntegrationTest*"` passes.
- [x] `cd services/backend && ./gradlew test --tests "*ShoppingExportIntegrationTest*"` passes.
- [x] `cd services/backend && ./gradlew test --tests "*ShoppingExportServiceTest*"` passes.
- [ ] `cd services/backend && ./gradlew spotlessCheck` passes. Current result: fails on pre-existing repository line-ending violations, first reported file `KeycloakAuthService.java`.
- [x] `./scripts/test.sh` passes before review gate.

## Final
- [x] Human Gate C approved after read-only PLAN.
- [x] Codex APPLY stayed within approved files.
- [x] Review gate completed.
- [x] Human Gate D approved.
- [x] Ready for next story `ST-3201` UI slice.
