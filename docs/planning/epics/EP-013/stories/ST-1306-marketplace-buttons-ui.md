# Story: ST-1306 — Marketplace Link-out Buttons

## Status: NOT READY
**Blocker**: Blocked by ST-1304 (marketplace templates backend)

## Description
Add marketplace link-out buttons to each shopping item, allowing users to quickly search for the item on Ozon, Yandex Market, etc.

**User Value**: One-click jump from shopping list to marketplace search.

## In Scope
- "Open in..." button/dropdown per shopping item
- Fetch marketplace templates on page load
- Generate safe URL with item name
- Open in new tab (target="_blank" with rel="noopener")
- Icons for each marketplace
- Mobile-friendly layout (icon buttons or menu)

## Out of Scope
- In-app browser/webview
- Price display from marketplace
- "Buy" actions within HomeTusk
- Remembering preferred marketplace

## Acceptance Criteria

### AC-1: Button Display
```
Given marketplace templates are loaded
And item "Milk" is displayed
Then "Open in Ozon" and "Open in Yandex Market" options are available
As dropdown or icon buttons
```

### AC-2: Link Generation
```
Given item name "Bread"
When clicking "Open in Ozon"
Then new tab opens with URL: https://ozon.ru/search?text=Bread
```

### AC-3: Encoded Names
```
Given item name "Сыр Российский"
When clicking marketplace button
Then URL contains properly encoded Cyrillic
And page loads correctly on marketplace
```

### AC-4: No Templates Available
```
Given marketplace templates API returns empty array
Then marketplace buttons are hidden (not broken UI)
```

### AC-5: Loading State
```
Given templates are loading
Then buttons show loading state or are disabled
And no layout shift when loaded
```

### AC-6: Accessibility
```
Given marketplace buttons exist
Then buttons have appropriate aria-labels
And keyboard navigable
```

## Test Strategy

**Unit Tests**:
- URL generation with encoding
- Button component states
- Dropdown behavior

**Integration Tests**:
- Template fetch + display
- Link clicks (mocked new tab)

**Test Data**:
- Various item names (ASCII, Cyrillic, special chars)
- Multiple vs. single template scenarios

## Flags
- contract_impact: no
- adr_needed: no
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- ST-1304: Marketplace templates backend (BLOCKER)

## Points: 3
