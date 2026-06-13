# Codex APPLY Prompt: ST-3101 - Shopping Category/Source Backend + Contract Foundation

You are Codex working in APPLY mode for HomeTusk. Human Gate C is approved in `docs/planning/workpacks/ST-3101/gate-c.md`.

## Objective
Implement `ST-3101` exactly as scoped by the workpack and PLAN findings: optional category/source metadata for shopping items across storage, backend API, filters, partial updates, shopping run snapshots, exports, command-created item compatibility, and service catalog documentation.

## Sources to Read
- `AGENTS.md`
- `docs/planning/workpacks/ST-3101/workpack.md`
- `docs/planning/workpacks/ST-3101/plan-findings.md`
- `docs/planning/workpacks/ST-3101/checklist.md`
- `docs/planning/workpacks/ST-3101/gate-c.md`
- `docs/contracts/http/commands.openapi.yaml`
- `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- `docs/architecture/service-catalog.md`
- Relevant backend shopping and command pipeline files named in the workpack.

## Allowed Runtime Files
- `services/backend/src/main/resources/db/migration/V026__add_shopping_item_category_source.sql`
- `services/backend/src/main/java/com/hometusk/shopping/domain/**`
- `services/backend/src/main/java/com/hometusk/shopping/dto/**`
- `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingItemRepository.java`
- `services/backend/src/main/java/com/hometusk/shopping/service/**`
- `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/ActionExecutor.java`
- `services/backend/src/main/java/com/hometusk/commands/pipeline/guardrails/ShoppingItemValidationPolicy.java`
- Relevant backend tests listed in the workpack.
- `docs/architecture/service-catalog.md`
- ST-3101 workpack/checklist evidence after implementation.

## Forbidden Files
- `docs/integration/ai-platform/v1/upstream/**`
- Frontend UI files for ST-3201.
- Contract files unless implementation discovers approved contract drift that must be stopped and reported.
- Unrelated docs/runbooks/user changes.

## Acceptance Criteria
- AC-1: Existing create payloads without category/source still create items and return null metadata.
- AC-2: Create payloads with valid category/source persist and return those values.
- AC-3: Invalid category returns 400 and does not mutate data.
- AC-4: GET item filters by category/source remain household-scoped and do not leak cross-household data.
- AC-5: PATCH with only category/source updates metadata without changing purchase status.
- AC-6: PATCH with no mutable fields returns 400.
- AC-7: Shopping run item snapshots copy category/source from original items.
- AC-8: Text and CSV exports include category/source metadata.
- AC-9: Command-created shopping items remain compatible and do not mutate upstream AI Platform snapshots.

## Non-Negotiable Invariants
- AI is not source of truth.
- Domain invariants stay in code.
- Existing shopping item payloads without category/source remain valid.
- Household boundary enforcement remains explicit.
- Invalid categories are rejected, not silently coerced.
- Source text is trimmed; blank source becomes null.
- Do not edit AI Platform upstream snapshots.

## Required Verification
- `cd services/backend && ./gradlew test --tests "*ShoppingControllerTest*"`
- `cd services/backend && ./gradlew test --tests "*ShoppingIntegrationTest*"` if command pipeline code changes
- `cd services/backend && ./gradlew test --tests "*ShoppingRunEndpointIntegrationTest*"`
- `cd services/backend && ./gradlew test --tests "*ShoppingExportIntegrationTest*"`
- `cd services/backend && ./gradlew test --tests "*ShoppingExportServiceTest*"`
- `cd services/backend && ./gradlew spotlessCheck`
- `./scripts/test.sh` before review gate if runtime and environment allow it.

## STOP-THE-LINE Conditions
- A new migration already uses `V026`.
- Category/source requires changing AI Platform upstream contracts.
- Category must become household-custom taxonomy in NOW.
- PATCH cannot be made backward-compatible while preserving existing mark/unmark behavior.
- Any filter implementation bypasses existing household/list membership checks.
