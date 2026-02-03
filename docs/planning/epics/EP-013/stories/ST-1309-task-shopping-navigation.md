# Story: ST-1309 — Task-Shopping Navigation Surfaces

## Status: READY
**Sprint:** S16 | **Points:** 3

## Description
Add navigation links between tasks and their linked shopping items. Users should be able to see related shopping items from task detail and navigate to the linked task from shopping items.

**User Value**: Clear visibility of task-shopping relationships, completing the "action loop".

## In Scope
- TaskDetail page: "Related Shopping Items" section (if linked items exist)
- ShoppingDetail page: Show linked task name on each item (clickable link)
- Empty states when no links exist
- Consistent styling with existing detail pages

## Out of Scope
- Creating links from UI (existing via AI/commands)
- Bulk link management
- Link history/audit

## Acceptance Criteria

### AC-1: Task Detail — Shopping Section
```
Given a task with 3 linked shopping items
When viewing TaskDetail page
Then "Shopping Items" section shows 3 items
With item names, purchased status
And link to shopping list
```

### AC-2: Task Detail — No Shopping Items
```
Given a task with no linked shopping items
When viewing TaskDetail page
Then "Shopping Items" section is hidden
Or shows "No shopping items linked"
```

### AC-3: Shopping Item — Task Link
```
Given a shopping item linked to task "Clean bathroom"
When viewing item in ShoppingDetail
Then item shows "For task: Clean bathroom"
And task name is clickable link to TaskDetail
```

### AC-4: Shopping Item — No Task Link
```
Given a shopping item not linked to any task
When viewing item in ShoppingDetail
Then no task reference shown (clean UI)
```

### AC-5: Navigation Works
```
Given user on TaskDetail with linked items
When clicking shopping item
Then navigates to ShoppingDetail with item visible
And back navigation works
```

## Test Strategy

**Unit Tests**:
- Conditional section rendering
- Link component behavior

**Integration Tests**:
- Full navigation flow (task -> shopping -> task)
- Data fetching for linked items

**Test Data**:
- Task with linked items
- Task without linked items
- Item with linked task
- Item without linked task

## Flags
- contract_impact: no (using existing linkedTaskId field)
- adr_needed: no
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- None (uses existing data structures)

## Points: 3
