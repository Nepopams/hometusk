# Story: ST-3201 - Shopping Category/Source Controls + Grouping

## Status: COMPLETED - HUMAN GATE D APPROVED - UAT VERIFIED
**Epic:** EP-032 | **Priority:** P0 | **Points:** 5
**Readiness:** ST-3101 is implemented, verified, reviewed, and approved through Human Gate D. ST-3201 read-only Codex PLAN findings, Human Gate C approval, frontend APPLY, review GO, Human Gate D, automated frontend verification, and UAT verification are recorded.

## Description
Expose optional category and source metadata in the shopping list UI. Users can add metadata while creating an item, edit metadata later, see badges on list rows, and group/filter items by category or source.

**User Value:** Household members can scan a large shopping list by item type or store while keeping the quick add flow lightweight for simple lists.

## In Scope
- Extend frontend `ShoppingItem`, filters, add request, and update request types with ST-3101 category/source fields.
- Extend API client/hook support for `category` and `source` filters and metadata-only PATCH.
- Add focused metadata helper coverage for payload normalization and grouping.
- Add optional category/source controls to the add item flow.
- Add an edit affordance for existing item category/source without changing `purchased`.
- Display compact category/source badges only when metadata is present.
- Provide grouping/filter controls: no grouping, category, source.
- Preserve current unpurchased/purchased separation.
- Add i18n keys for category/source labels, grouping labels, validation/errors, and uncategorised/no-source buckets.
- Update responsive CSS for dense shopping lists.
- Add focused tests or documented manual verification depending on existing frontend test harness coverage.

## Out of Scope
- Backend implementation, migration, or contract edits.
- New category taxonomy beyond ST-3101 values.
- Household source presets.
- Price/budget summaries.
- AI category suggestions.
- Multi-store shopping run filtering.

## Acceptance Criteria

### AC-1: Add With Metadata
```
Given the backend supports category/source fields
When a user adds an item with category=groceries and source="Perekrestok"
Then the POST payload includes category/source
And the created item displays the metadata badges
```

### AC-2: Legacy Add Remains Fast
```
Given a user enters only an item name
When the item is submitted
Then the POST payload remains valid
And the row renders without empty metadata badges
```

### AC-3: Edit Metadata Without Purchase Drift
```
Given an existing unpurchased item
When the user changes only category/source
Then PATCH sends only metadata fields
And the item remains unpurchased
```

### AC-4: Group By Category
```
Given items with groceries, cleaning, and null categories
When the user selects group by category
Then items are grouped under category labels
And null-category items appear under Uncategorised
```

### AC-5: Group By Source
```
Given items with store sources and null source
When the user selects group by source
Then items are grouped under source labels
And null-source items appear under No source
```

### AC-6: Purchased Separation Preserved
```
Given both purchased and unpurchased items
When grouping or filtering is enabled
Then unpurchased items remain in the main section
And purchased items remain under the purchased section
```

### AC-7: Optimistic Rollback
```
Given an add or metadata edit request fails
When the API returns an error
Then optimistic UI changes roll back
And the user sees the existing shopping error pattern
```

### AC-8: Responsive Layout
```
Given a narrow mobile viewport
When item names, source text, and badges are long
Then controls remain usable
And text does not overlap or escape its container
```

## Test Strategy

**Unit / Helper Tests**
- Grouping helper handles category/source/null buckets.
- Update payload helper preserves partial PATCH semantics if extracted.

**Component / Integration Tests**
- Add item sends category/source fields.
- Metadata edit sends category/source without `purchased`.
- Group controls preserve purchased/unpurchased sections.

**Manual / Browser Verification**
- Desktop and mobile shopping list view.
- Long item/source text.
- Empty list, uncategorised-only list, mixed metadata list.
- Error rollback for add/edit failures.

## Flags
- contract_impact: no new contract
- data_impact: no
- adr_needed: no
- diagrams_needed: no
- security_sensitive: medium
- traceability_critical: low

## Dependencies
- ST-3101 backend APPLY and review evidence.
- Implemented OpenAPI fields:
  - `ShoppingItem.category`
  - `ShoppingItem.source`
  - `AddShoppingItemRequest.category`
  - `AddShoppingItemRequest.source`
  - `UpdateShoppingItemRequest.category`
  - `UpdateShoppingItemRequest.source`
  - GET list filters `category` and `source`
- Existing frontend route and hook:
  - `clients/web/src/routes/ShoppingDetail.tsx`
  - `clients/web/src/hooks/useShoppingItems.ts`

## Gate Notes
- Codex PLAN is complete after ST-3101 Human Gate D.
- Human Gate C is approved for ST-3201.
- Codex APPLY stayed within the approved frontend scope and did not require backend or contract changes.
- Review gate is GO.
- Human Gate D is approved for ST-3201.

## Implementation Evidence
- Frontend types, API client, shopping hook, shopping detail route, responsive CSS, i18n, and helper tests were updated for category/source metadata.
- `npm run build`, `npm run lint`, and `npm run test` passed in `clients/web` on 2026-06-13.
- Vitest result: 2 files, 27 tests.
- UAT verification accepted by human on 2026-06-13; category/source shopping flow is considered closed and verified on UAT.
- Review gate is recorded at `docs/planning/workpacks/ST-3201/review-gate.md`.
- Human Gate D is recorded at `docs/planning/workpacks/ST-3201/gate-d.md`.

## Tech Debt / Deferred Verification
- None blocking story closure.
- TD-ST-3201-001 is resolved for story verification by UAT acceptance on 2026-06-13. A reliable one-command local stack and seed path remains optional local-dev hygiene outside ST-3201 closure.
