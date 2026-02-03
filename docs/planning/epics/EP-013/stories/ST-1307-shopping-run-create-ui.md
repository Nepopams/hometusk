# Story: ST-1307 — ShoppingRun Creation UI

## Status: NOT READY
**Blocker**: Blocked by ST-1302 (backend endpoints)

## Description
Add UI to create a new shopping run from the shopping list page. Users can start a "shopping trip" that snapshots the current list for tracking.

**User Value**: Start organized shopping trip with clear checklist.

## In Scope
- "Start Shopping Trip" button on ShoppingLists page or ShoppingDetail
- Confirmation modal with list preview
- API call to create run
- Redirect to run checklist page after creation
- Error handling for API failures
- Loading state during creation

## Out of Scope
- Run from multiple lists
- Scheduled runs
- Location-based auto-start
- Run templates

## Acceptance Criteria

### AC-1: Start Button
```
Given user views shopping list with unpurchased items
Then "Start Shopping Trip" button is visible
And button is disabled if list is empty
```

### AC-2: Confirmation Modal
```
Given user clicks "Start Shopping Trip"
Then modal shows:
  - List name
  - Number of items to shop
  - "Start" and "Cancel" buttons
```

### AC-3: Run Creation
```
Given user confirms in modal
When "Start" clicked
Then loading spinner shows
And API creates shopping run
And user is redirected to /shopping-runs/{runId}
```

### AC-4: Error Handling
```
Given API returns error during creation
Then error message shown in modal
And modal stays open for retry
```

### AC-5: Empty List Prevention
```
Given shopping list has 0 unpurchased items
When "Start Shopping Trip" clicked
Then helpful message: "No items to shop. Add items first."
```

## Test Strategy

**Unit Tests**:
- Button state logic (disabled when empty)
- Modal component
- API call mocking

**Integration Tests**:
- Full creation flow
- Redirect verification
- Error scenarios

**Test Data**:
- List with items
- Empty list
- List with all items purchased

## Flags
- contract_impact: no
- adr_needed: no
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- ST-1302: ShoppingRun endpoints (BLOCKER)

## Points: 5
