# Story: ST-1305 — Share/Export UI Buttons

## Status: NOT READY
**Blocker**: Blocked by ST-1303 (backend export endpoint)

## Description
Add "Share" and "Export" buttons to the shopping list detail page, allowing users to copy list to clipboard or download as CSV.

**User Value**: Quickly share list with family members or use in other apps.

## In Scope
- "Share" button with copy-to-clipboard (text format)
- "Export CSV" button triggering download
- Success/error feedback (snackbar/toast)
- Mobile-friendly tap targets
- Keyboard accessible

## Out of Scope
- Native share sheet (navigator.share) — consider for future
- Share via specific apps (WhatsApp, Telegram)
- QR code generation

## Acceptance Criteria

### AC-1: Share Button
```
Given user views shopping list with items
When clicking "Share" button
Then list text is copied to clipboard
And success snackbar shows "List copied to clipboard"
```

### AC-2: Export Button
```
Given user views shopping list
When clicking "Export CSV" button
Then CSV file downloads with filename "shopping-list-YYYY-MM-DD.csv"
And file contains all items with correct encoding
```

### AC-3: Empty List
```
Given an empty shopping list
When clicking "Share" button
Then clipboard contains "No items" or similar
And no error thrown
```

### AC-4: Error Handling
```
Given clipboard API not available (some browsers)
When clicking "Share"
Then fallback behavior (select text for manual copy) or error message
```

### AC-5: Button Placement
```
Given the ShoppingDetail page
Then Share/Export buttons are in header area
And visible without scrolling
And have appropriate icons
```

## Test Strategy

**Unit Tests**:
- Button click handlers
- Clipboard API mocking
- Error state handling

**Integration Tests**:
- Full flow with mocked API
- Download trigger verification

**Test Data**:
- List with items
- Empty list
- List with special characters

## Flags
- contract_impact: no
- adr_needed: no
- security_sensitive: no
- diagrams_needed: no

## Dependencies
- ST-1303: Export endpoint (BLOCKER)

## Points: 3
