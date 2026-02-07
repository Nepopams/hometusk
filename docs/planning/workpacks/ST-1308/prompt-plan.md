# Codex PLAN: ST-1308 — ShoppingRun Checklist UI

## Objective
Explore existing patterns for creating a new route page with optimistic UI updates, modals, and loading states.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `cat`, `rg`, `grep`, `ls`

## Questions to Answer

### Q1: Route registration pattern
- Read `clients/web/src/routes/index.tsx`
- How are routes structured under `/households/:householdId`?
- Where to place `shopping-runs/:runId`?

### Q2: Page structure patterns
- Read `clients/web/src/routes/TaskDetail.tsx` (similar detail page)
- What is the loading/error/data pattern?
- How is back navigation handled?

### Q3: ShoppingDetail patterns (sibling page)
- Read `clients/web/src/routes/ShoppingDetail.tsx`
- How are checkboxes styled and handled?
- What is the optimistic update pattern?
- How is Snackbar used for errors?

### Q4: Modal confirmation patterns
- Search for confirmation modals: `rg "confirm|Complete|Cancel" clients/web/src/routes/`
- How do other pages handle destructive actions with confirmation?

### Q5: CSS patterns
- Read `clients/web/src/routes/ShoppingDetail.css`
- What BEM naming is used?
- How are checkboxes styled?
- How are status badges styled (if any)?

### Q6: API function patterns
- Read `clients/web/src/lib/api.ts`
- Check PATCH patterns (updateShoppingItem, updateRoutine)
- Check POST patterns for actions (closeShoppingRun-like)

### Q7: Types already available
- Read `clients/web/src/types/api.ts`
- Confirm ShoppingRun, ShoppingRunItem, ShoppingRunStatus exist
- Check if any additions needed

## Expected Output

```
## Findings

### Route registration
- Location in index.tsx: [line number/pattern]
- Recommended placement: [after which route]

### Page structure (TaskDetail-like)
- Loading state: [pattern]
- Error state: [pattern]
- Data fetching: [useEffect/custom hook]
- Back navigation: [component/pattern]

### ShoppingDetail patterns
- Checkbox class: [class name]
- Optimistic update: [pattern description]
- Snackbar usage: [pattern]

### Modal confirmation
- Existing examples: [files/patterns]
- Recommended approach: [description]

### CSS patterns
- BEM prefix: [e.g., shopping-run__]
- Checkbox styles to reuse: [list]
- Status badge: [exists/create]

### API patterns
- PATCH signature: [pattern]
- POST action signature: [pattern]

### Types
- ShoppingRun: [exists/needs update]
- Additional types needed: [list]

### Implementation notes
[Any specific findings affecting implementation]
```
