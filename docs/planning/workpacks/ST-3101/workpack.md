# Workpack: ST-3101 — Shopping Category/Source Backend + Contract Foundation

## Sources of Truth
- Scope anchor: `docs/planning/initiatives/INIT‑2026Q3‑shopping‑categories.md`
- Roadmap: `docs/planning/strategy/roadmap.md`
- Epic: `docs/planning/epics/EP-031/epic.md`
- Story: `docs/planning/epics/EP-031/stories/ST-3101-shopping-category-source-foundation.md`
- Contracts: `docs/contracts/http/commands.openapi.yaml`
- Contracts: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- Contract index: `docs/_indexes/contracts-index.md`
- Service catalog: `docs/architecture/service-catalog.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

## Status
**COMPLETED — HUMAN GATE D APPROVED** — Human Gate C approved on 2026-06-13; backend implementation, verification evidence, read-only review gate, and Human Gate D approval are recorded.

## Outcome
Shopping items can carry optional category/source metadata end-to-end in backend contracts, storage, API responses, filters, run snapshots, and exports. Existing uncategorised items and clients remain compatible.

## Acceptance Criteria
- [x] AC-1: Existing create payloads without category/source still create items and return null metadata.
- [x] AC-2: Create payloads with valid category/source persist and return those values.
- [x] AC-3: Invalid category returns 400 and does not mutate data.
- [x] AC-4: GET item filters by category/source remain household-scoped and do not leak cross-household data.
- [x] AC-5: PATCH with only category/source updates metadata without changing purchase status.
- [x] AC-6: PATCH with no mutable fields returns 400.
- [x] AC-7: Shopping run item snapshots copy category/source from original items.
- [x] AC-8: Text and CSV exports include category/source metadata.
- [x] AC-9: Command-created shopping items remain compatible and do not mutate upstream AI Platform snapshots.

## Non-goals
- Web UI controls, grouping, filters, or badges.
- Household source presets.
- Price/budget tracking.
- AI category suggestions.
- Multi-store shopping run planning.
- Changes to AI Platform upstream contracts.

## Files to change
- `services/backend/src/main/resources/db/migration/V026__add_shopping_item_category_source.sql` — nullable category/source columns and indexes.
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItem.java` — fields, getters, setters/normalisation.
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunItem.java` — snapshot fields copied from ShoppingItem.
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItemCategory.java` — enum or value object for allowed categories.
- `services/backend/src/main/java/com/hometusk/shopping/dto/AddShoppingItemRequest.java` — optional category/source input.
- `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateShoppingItemRequest.java` — partial update fields and validation shape.
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java` — category/source response fields.
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunItemDto.java` — category/source response fields.
- `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingItemRepository.java` — household/list/category/source filtered queries.
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java` — create/update/filter metadata handling.
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingRunService.java` — run creation path if snapshot propagation is service-owned.
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingExportService.java` — text/CSV output metadata.
- `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java` — query params and partial update handling.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java` — command-created shopping item metadata handling or explicit null/default preservation.
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/ShoppingItemValidationPolicy.java` — category validation for command actions if category is accepted from action parameters.
- `services/backend/src/test/java/com/hometusk/integration/ShoppingControllerTest.java` — REST integration coverage.
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingIntegrationTest.java` — command-created item regression coverage where applicable.
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunEndpointIntegrationTest.java` — run snapshot coverage.
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java` — export coverage.
- `services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingRunTest.java` — snapshot unit coverage if needed.
- `services/backend/src/test/java/com/hometusk/shopping/service/ShoppingExportServiceTest.java` — unit export coverage.
- `docs/architecture/service-catalog.md` — data catalog/endpoint notes after implementation.

## Implementation plan

### Commit 1 — Data model and domain metadata
Steps:
1. Add Flyway `V026__add_shopping_item_category_source.sql` with nullable `category` and `source` on `shopping_items` and `shopping_run_items`.
2. Add indexes for category/source filters scoped through list where useful, for example `(shopping_list_id, category)` and `(shopping_list_id, source)`.
3. Add backend category representation with allowed values: `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other`.
4. Extend `ShoppingItem` and `ShoppingRunItem` with nullable category/source fields and snapshot copying.

Files:
- `services/backend/src/main/resources/db/migration/V026__add_shopping_item_category_source.sql`
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItem.java`
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunItem.java`
- `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItemCategory.java`

Verification:
- `cd services/backend && ./gradlew test --tests "*ShoppingRunTest*"` → passes

### Commit 2 — API, service, repository, validation
Steps:
1. Extend DTOs for optional category/source.
2. Update `POST` direct add path and command-created add path to store metadata when supplied, or explicitly preserve null metadata when upstream action payloads do not support it.
3. Update list queries to support optional `purchased`, `category`, and `source` filters while preserving list-household validation.
4. Update PATCH semantics so `purchased`, `category`, and `source` are optional but at least one is required.
5. Ensure source is trimmed, blank source becomes null, and logs avoid leaking unnecessary user text.
6. Keep AI Platform upstream snapshots read-only; adapt HomeTusk command handling only.

Files:
- `services/backend/src/main/java/com/hometusk/shopping/dto/AddShoppingItemRequest.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateShoppingItemRequest.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunItemDto.java`
- `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingItemRepository.java`
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java`
- `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/ShoppingItemValidationPolicy.java`
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingIntegrationTest.java`

Verification:
- `cd services/backend && ./gradlew test --tests "*ShoppingControllerTest*"` → passes

### Commit 3 — Run/export propagation and docs
Steps:
1. Update shopping run creation/snapshot paths if needed so category/source appear in `ShoppingRunItemDto`.
2. Update text and CSV exports to include category/source metadata with empty values for nulls.
3. Update service catalog to mention shopping item metadata and filters.
4. Add/adjust tests for run snapshots and exports.

Files:
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingRunService.java`
- `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingExportService.java`
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunEndpointIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java`
- `services/backend/src/test/java/com/hometusk/shopping/service/ShoppingExportServiceTest.java`
- `docs/architecture/service-catalog.md`

Verification:
- `cd services/backend && ./gradlew test --tests "*ShoppingRunEndpointIntegrationTest*"` → passes
- `cd services/backend && ./gradlew test --tests "*ShoppingExportIntegrationTest*"` → passes
- `cd services/backend && ./gradlew test --tests "*ShoppingExportServiceTest*"` → passes

## Contract impact
- OpenAPI updates already drafted:
  - `docs/contracts/http/commands.openapi.yaml`
  - `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- Contract index updated:
  - `docs/_indexes/contracts-index.md`
- Compatibility:
  - Non-breaking additive fields.
  - Existing request payloads remain valid.
  - Existing items/runs may return `category=null` and `source=null`.
  - `PATCH /shopping-items/{itemId}` becomes more permissive, but implementation must reject empty update bodies.

## Docs updates
- [x] `docs/architecture/service-catalog.md` updated after implementation.
- [x] Contract index updated.
- [x] ADR not required.
- [x] Diagrams not required.

## Tests
- [x] Unit: category validation/normalisation, run item snapshot, export formatting.
- [x] Integration: create with metadata, legacy create, command-created item path, filters, invalid category, partial update, empty update.
- [x] Negative/boundary: non-member access still 403; filters do not bypass household/list checks.

## Verification commands
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*ShoppingRunTest*" --tests "*ShoppingExportServiceTest*" --rerun-tasks --no-daemon` — passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*ShoppingControllerTest*" --rerun-tasks --no-daemon` — passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*ShoppingRunEndpointIntegrationTest*" --rerun-tasks --no-daemon` — passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "*ShoppingExportIntegrationTest*" --rerun-tasks --no-daemon` — passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests "com.hometusk.integration.shopping.ShoppingIntegrationTest*" --rerun-tasks --no-daemon` — passed.
- `./scripts/test.sh` via Git Bash — passed.
- `git diff --check` — passed.
- `cd services/backend && java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain spotlessCheck --no-daemon` — failed on pre-existing line-ending violations, first reported file `src/main/java/com/hometusk/auth/keycloak/KeycloakAuthService.java`, plus 35 other files. `spotlessApply` intentionally not run to avoid unrelated churn.

## DoD checklist
- [x] Tests pass.
- [x] No cross-household leaks verified by negative tests.
- [x] Contract docs match implementation.
- [x] Service catalog updated.
- [x] Migration tested.
- [x] Workpack contains implementation evidence/commands after APPLY.

## Implementation Evidence
- Gate C was approved by the human on 2026-06-13.
- Added nullable `category` and `source` metadata to shopping items and shopping run item snapshots through Flyway V026.
- Added backend validation/normalisation for category and source, including command guardrails.
- Extended REST create/list/filter/PATCH responses and request handling.
- Extended run snapshot DTOs and text/CSV export output.
- Preserved compatibility for legacy requests and AI Platform upstream action payloads that omit category/source.
- Full backend test script passed after fixing `ShoppingIntegrationTest` assertions to scope item counts to the current test shopping list instead of global table state.

## Risks
- Partial PATCH could accidentally unmark purchased if null is treated as false → explicitly branch only when `purchased != null`; add regression test.
- Command-created shopping items could diverge from direct REST items → cover `ActionExecutor`/shopping command flow or explicitly assert null metadata preservation until upstream supports category/source.
- Category taxonomy may not match user habits → keep `other` and defer custom categories to NEXT.
- Source is user text and could be displayed in UI later → trim and return as plain text only; UI must escape naturally.
- Export CSV header changes may surprise consumers → contract is draft for shopping export; document category/source columns and test formatting.
- Filter query performance can degrade on large lists → add scoped indexes and keep filters list-scoped.

## Rollback
- Revert code commits and service catalog update.
- Migration rollback, if required in a controlled environment:
  - `ALTER TABLE shopping_run_items DROP COLUMN IF EXISTS source;`
  - `ALTER TABLE shopping_run_items DROP COLUMN IF EXISTS category;`
  - `ALTER TABLE shopping_items DROP COLUMN IF EXISTS source;`
  - `ALTER TABLE shopping_items DROP COLUMN IF EXISTS category;`
- Because new columns are nullable and additive, leaving them in place during code rollback is acceptable if no incompatible code depends on them.

## Prompt Pack
- PLAN: `docs/planning/workpacks/ST-3101/prompt-plan.md`
- PLAN findings: `docs/planning/workpacks/ST-3101/plan-findings.md`
- Gate C handoff: `docs/planning/workpacks/ST-3101/gate-c.md`
- APPLY: `docs/planning/workpacks/ST-3101/prompt-apply.md`
- Review gate report: `docs/planning/workpacks/ST-3101/review-gate.md`
- Gate D approval: `docs/planning/workpacks/ST-3101/gate-d.md`
- No `prompt-review.md`; review gates stay read-only.
