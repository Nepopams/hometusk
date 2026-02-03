# Story: ST-1303 — Export Shopping List (Text/CSV)

## Status: READY
**Sprint:** S16 | **Points:** 3
**Artifacts:** Contract approved 2026-02-03

## Description
Add export functionality for shopping lists, allowing users to download or copy their list in plain text or CSV format.

**User Value**: Quickly share shopping list via messaging apps or use in other tools.

## In Scope
- `GET /api/v1/households/{householdId}/shopping-lists/{listId}/export?format=text|csv`
- Text format: Simple newline-separated list with quantities
- CSV format: Headers (name, quantity, unit, purchased) with proper escaping
- Content-Type headers: text/plain for text, text/csv for CSV
- Content-Disposition header for CSV download filename

## Out of Scope
- PDF export
- Rich formatting (markdown)
- Export history/versioning
- Bulk export (all lists at once)

## Acceptance Criteria

### AC-1: Text Export
```
Given a shopping list with items: "Milk (2 pcs)", "Bread", "Eggs (12 pcs)"
When GET /export?format=text
Then response is:
  Milk - 2 pcs
  Bread
  Eggs - 12 pcs
And Content-Type: text/plain; charset=utf-8
```

### AC-2: CSV Export
```
Given a shopping list with items including special chars ("Cheese, cheddar")
When GET /export?format=csv
Then response has proper CSV escaping:
  name,quantity,unit,purchased
  "Cheese, cheddar",1,,false
And Content-Type: text/csv
And Content-Disposition: attachment; filename="shopping-list-{date}.csv"
```

### AC-3: Empty List
```
Given an empty shopping list
When GET /export?format=text
Then response is empty string or "No items"
And status 200 OK
```

### AC-4: Household Boundary
```
Given a list in household A
When exported from household B
Then 403 Forbidden or 404 Not Found
```

### AC-5: Invalid Format
```
Given a shopping list
When GET /export?format=xml
Then 400 Bad Request with error message
```

## Test Strategy

**Unit Tests**:
- Text formatting logic
- CSV escaping (quotes, commas, newlines)
- Empty list handling

**Integration Tests**:
- Full export flow
- Content-Type verification
- Household boundary
- Unicode/special character handling

**Test Data**:
- List with various item types
- Items with special characters
- Empty list

## Flags
- contract_impact: yes (new endpoint)
- adr_needed: no
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- Contract: Export endpoint OpenAPI spec (BLOCKER)

## Points: 3
