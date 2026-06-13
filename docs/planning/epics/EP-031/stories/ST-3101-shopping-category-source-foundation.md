# Story: ST-3101 — Shopping Category/Source Backend + Contract Foundation

## Status: COMPLETED - HUMAN GATE D APPROVED
**Epic:** EP-031 | **Priority:** P0 | **Points:** 8
**Artifacts:** Contract delta, implementation evidence, review gate, and Human Gate D approval documented 2026-06-13.

## Description
Add optional `category` and `source` metadata to shopping items and propagate it through API responses, filters, shopping runs, and exports. Existing lists and clients must continue to work when these fields are omitted.

**User Value:** Shopping lists become structured enough to group and filter by category or store in the next UI slice, while existing flat lists remain usable.

## In Scope
- Add nullable `category` and `source` columns to `shopping_items`.
- Add nullable `category` and `source` snapshot columns to `shopping_run_items`.
- Add category enum/normalisation in backend with the NOW taxonomy:
  `groceries`, `cleaning`, `personal_care`, `diy`, `electronics`, `other`.
- Extend `ShoppingItem`, `ShoppingRunItem`, DTOs, REST requests, service methods, repositories, and filters.
- Keep `POST /shopping-lists/{listId}/items` backward-compatible with optional fields.
- Extend `PATCH /shopping-items/{itemId}` into a safe partial update: `purchased`, `category`, and `source` optional, but at least one mutable field required.
- Add `category` and `source` query filters to `GET /shopping-lists/{listId}/items`.
- Propagate category/source into shopping run item snapshots.
- Include category/source in text/CSV export output.
- Update service catalog after implementation.
- Add backend integration/unit tests for validation, filters, partial update, run snapshots, exports, and household boundary.

## Out of Scope
- Web UI controls, grouping, or badges.
- Household-level source presets.
- Price/budget fields.
- AI category suggestions.
- Multi-store runs or delegation.
- Changing shopping run lifecycle semantics.

## Acceptance Criteria

### AC-1: Backward-Compatible Create
```
Given an existing client sends name/quantity/unit only
When POST /api/v1/households/{householdId}/shopping-lists/{listId}/items is called
Then the item is created
And the response includes category=null and source=null
```

### AC-2: Create With Category/Source
```
Given a household member submits a shopping item with category=groceries and source="Perekrestok"
When the item is created
Then category and source are persisted
And the response returns the same values
```

### AC-3: Category Validation
```
Given a request with category="unknown_bucket"
When POST or PATCH shopping item is called
Then the API returns 400
And no shopping item metadata is changed
```

### AC-4: Filters Are Household-Scoped
```
Given shopping items in household A and household B
When household A lists items with category/source filters
Then only matching items from household A are returned
And items from household B are never returned
```

### AC-5: Partial Update Preserves Purchase State
```
Given an unpurchased item
When PATCH /shopping-items/{itemId} contains only category/source
Then category/source are updated
And purchased remains false
```

### AC-6: Empty Update Rejected
```
Given a PATCH request with no mutable fields
When the request is submitted
Then the API returns 400 with a validation error
```

### AC-7: Run Snapshot Propagation
```
Given a shopping item has category/source
When a shopping run is created from the list
Then the run item snapshot includes category/source
```

### AC-8: Export Propagation
```
Given shopping items with category/source
When the list is exported as text or CSV
Then the export includes category/source metadata
And null metadata is rendered as empty values
```

### AC-9: Command-Created Items Stay Compatible
```
Given the AI Platform upstream add_shopping_item action does not define category/source
When a command creates a shopping item through the command pipeline
Then the item is still created successfully
And category/source are stored as null unless HomeTusk explicitly accepts and validates those action parameters
And upstream snapshots under docs/integration/ai-platform/v1/upstream/** remain unchanged
```

## Test Strategy

**Unit Tests**
- Category normalisation/validation.
- Shopping item metadata setter behaviour.
- Shopping run item snapshot copies category/source.
- Export text/CSV includes category/source without malformed CSV.

**Integration Tests**
- `ShoppingControllerTest`: create, invalid category, filtered list, partial patch, empty patch.
- `ShoppingRunEndpointIntegrationTest`: run item snapshots include category/source.
- `ShoppingExportIntegrationTest`: text/CSV exports include metadata.
- Negative household boundary check for filtered listing.
- `ShoppingIntegrationTest` or equivalent command pipeline coverage: command-created item compatibility.

**Test Data**
- Household A with multiple items across category/source.
- Household B with same category/source labels to prove isolation.
- Legacy item with null category/source.

## Flags
- contract_impact: yes
- data_impact: yes
- adr_needed: no
- diagrams_needed: no
- security_sensitive: yes
- traceability_critical: medium

## Dependencies
- Contract delta: `docs/contracts/http/commands.openapi.yaml`
- Contract delta: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- Existing migrations through `V025__create_shopping_runs.sql`
- Existing shopping controller/service/repository patterns

## Gate Notes
- Human Gate C approved on 2026-06-13.
- Human Gate D approved on 2026-06-13.
- ST-3201 may proceed to Codex PLAN; ST-3201 APPLY still requires its own Human Gate C.
