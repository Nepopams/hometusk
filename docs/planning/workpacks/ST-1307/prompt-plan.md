# Codex PLAN: ST-1307 — ShoppingRun Creation UI

## Objective
Explore ShoppingDetail page to understand header structure and confirm API/type patterns for adding "Start Shopping Trip" functionality.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `cat`, `rg`, `grep`, `ls`

## Questions to Answer

### Q1: ShoppingDetail header structure
- Read `clients/web/src/routes/ShoppingDetail.tsx`
- Where is the header section? (line numbers)
- What buttons exist in header-actions?
- How is button styling done (ghost-button, primary-button)?

### Q2: Modal usage pattern
- Search for Modal imports in routes: `rg "import.*Modal" clients/web/src/routes/`
- How is Modal used in other components?
- What props are commonly used?

### Q3: Navigation pattern (useNavigate)
- Is `useNavigate` already imported in ShoppingDetail?
- How do other components navigate after API calls?

### Q4: API function patterns
- Read `clients/web/src/lib/api.ts`
- Check existing POST patterns (createZone, createRoutine, etc.)
- What is the return type pattern?

### Q5: Types file structure
- Read `clients/web/src/types/api.ts`
- Where should ShoppingRun types be placed?
- Are there existing run/status patterns?

### Q6: CSS class patterns
- Read `clients/web/src/routes/ShoppingDetail.css`
- What header button styles exist?
- Is there a primary-button variant?

### Q7: Button component
- Check if Button component has variant="primary"
- `rg "variant.*primary" clients/web/src/components/ui/Button.tsx`

## Expected Output

```
## Findings

### ShoppingDetail header
- Header location: [lines X-Y]
- Existing buttons: [list]
- Button class pattern: [description]

### Modal pattern
- Usage examples: [files]
- Common props: [list]

### Navigation
- useNavigate imported: [yes/no]
- Post-API navigation pattern: [description]

### API patterns
- POST function signature: [pattern]
- Return type: [pattern]

### Types location
- ShoppingRun types placement: [recommendation]
- Existing status patterns: [if any]

### CSS
- Header button class: [name]
- Primary variant exists: [yes/no]

### Button component
- Primary variant: [supported/not]

### Implementation notes
[Any specific findings affecting implementation]
```
