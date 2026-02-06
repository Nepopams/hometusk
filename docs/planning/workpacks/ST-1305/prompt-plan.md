# Codex PLAN: ST-1305 — Share/Export UI Buttons

## Objective
Explore ShoppingDetail page to understand structure for adding Share/Export buttons.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `cat`, `rg`, `grep`

## Questions to Answer

### Q1: ShoppingDetail structure
- Read `clients/web/src/routes/ShoppingDetail.tsx`
- Where is the header/title area?
- What is the component structure?
- Are there existing action buttons?

### Q2: API patterns
- Read `clients/web/src/lib/api.ts`
- How are API calls structured?
- Is there existing export function?

### Q3: Toast/feedback patterns
- Search for existing toast/snackbar usage
- `rg "toast|snackbar|notification" clients/web/src/`

### Q4: Icon patterns
- Are there existing icon components or inline SVGs?
- `rg "function.*Icon|<svg" clients/web/src/components/`

### Q5: CSS patterns
- Read `clients/web/src/routes/ShoppingDetail.css`
- What class naming convention is used?
- Are there existing action button styles?

## Expected Output

```
## Findings

### ShoppingDetail structure
- Header location: [line numbers]
- Existing actions: [yes/no, what]
- Component pattern: [description]

### API patterns
- Base URL: [how accessed]
- Auth: [how handled]
- Export function: [exists/needs creation]

### Toast/feedback
- Existing pattern: [yes/no, where]
- Component used: [name or inline]

### Icons
- Pattern: [inline SVG / component]
- Existing icons: [list]

### CSS
- Naming: [convention]
- Existing action styles: [yes/no]

### Implementation notes
[Any specific notes for implementation]
```
