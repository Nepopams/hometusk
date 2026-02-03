# Story: ST-1308 — ShoppingRun Checklist UI

## Status: NOT READY
**Blocker**: Blocked by ST-1307 (depends on run creation flow)

## Description
Create the shopping run checklist page where users can mark items as purchased during their shopping trip, with clear progress indication and run closure.

**User Value**: Track shopping progress in real-time with satisfying checkoff experience.

## In Scope
- `/households/{householdId}/shopping-runs/{runId}` route
- Item checklist with checkboxes
- Progress indicator (X of Y purchased)
- "Complete Trip" button to close run
- "Cancel Trip" option
- Optimistic UI for checkbox updates
- Run summary after closure

## Out of Scope
- Item reordering
- Adding items mid-run
- Photos/receipts
- Multiple trip comparison

## Acceptance Criteria

### AC-1: Checklist Display
```
Given an ACTIVE shopping run with 5 items
Then all 5 items displayed as checklist
With checkboxes for each item
And progress shows "0 of 5 purchased"
```

### AC-2: Mark Purchased
```
Given unchecked item in run
When checkbox clicked
Then item visually checked (optimistic)
And API called to update
And progress updates to "1 of 5 purchased"
```

### AC-3: Unmark Purchased
```
Given checked item
When checkbox clicked again
Then item unchecked
And API called to update
And progress decrements
```

### AC-4: Complete Trip
```
Given run with 4/5 items purchased
When "Complete Trip" clicked
Then confirmation shows: "4 items purchased, 1 skipped. Complete?"
And on confirm, run closed as COMPLETED
And summary page shown
```

### AC-5: Cancel Trip
```
Given an ACTIVE run
When "Cancel Trip" clicked
Then confirmation shows
And run closed as CANCELLED
And user redirected to shopping list
```

### AC-6: Closed Run View
```
Given a COMPLETED or CANCELLED run
When user navigates to run page
Then read-only view shown
With clear status badge
And no editable checkboxes
```

### AC-7: Error Recovery
```
Given API error on checkbox update
Then checkbox reverts (optimistic rollback)
And error snackbar shown
And retry possible
```

## Test Strategy

**Unit Tests**:
- Checkbox component states
- Progress calculation
- Optimistic update logic

**Integration Tests**:
- Full check/uncheck flow
- Complete/cancel flow
- Closed run view

**Test Data**:
- ACTIVE run with various item states
- COMPLETED run
- CANCELLED run

## Flags
- contract_impact: no
- adr_needed: no
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- ST-1302: ShoppingRun endpoints (BLOCKER)
- ST-1307: Run creation UI (BLOCKER for user flow)

## Points: 8
