# Codex PLAN: ST-1306 — Marketplace Link-out Buttons

## Objective
Explore ShoppingDetail item rendering to understand where to add marketplace buttons.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `cat`, `rg`, `grep`

## Questions to Answer

### Q1: Item rendering structure
- Read `clients/web/src/routes/ShoppingDetail.tsx`
- How are items rendered? (map, function, etc.)
- What is the item row structure?
- Where to add marketplace links?

### Q2: Existing hooks pattern
- `ls clients/web/src/hooks/`
- What hooks exist?
- What pattern do they follow?

### Q3: buildMarketplaceUrl location
- Verify `clients/web/src/lib/marketplaceUrl.ts` exists
- Check export: `buildMarketplaceUrl`

### Q4: API patterns for unauthenticated endpoints
- Check if there are other endpoints without auth
- How to call without token?

## Expected Output

```
## Findings

### Item rendering
- Items rendered: [how]
- Item row structure: [elements]
- Marketplace button location: [recommended]

### Hooks pattern
- Existing hooks: [list]
- Pattern: [description]

### marketplaceUrl
- File exists: [yes/no]
- Export: [function name]

### API without auth
- Pattern: [description]

### Implementation notes
[Specific recommendations]
```
