# Codex APPLY: ST-1305 — Share/Export UI Buttons

## Context
Add Share and Export CSV buttons to ShoppingDetail header. Snackbar component exists for feedback.

## Files to Modify

| File | Action |
|------|--------|
| `clients/web/src/lib/api.ts` | MODIFY — add exportShoppingList function |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY — add buttons + handlers |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY — add action button styles |

---

## Step 1: Add export API function

**File:** `clients/web/src/lib/api.ts`

Add near other shopping functions:

```typescript
export async function exportShoppingList(
  householdId: string,
  listId: string,
  format: 'text' | 'csv' = 'text'
): Promise<string> {
  const baseUrl = getApiBaseUrl();
  const token = getAuthToken();

  const response = await fetch(
    `${baseUrl}/api/v1/households/${householdId}/shopping-lists/${listId}/export?format=${format}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error('Export failed');
  }

  return response.text();
}
```

Note: Use existing `getApiBaseUrl()` and `getAuthToken()` patterns from the file.

---

## Step 2: Add buttons to ShoppingDetail header

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

### 2a: Add imports

```typescript
import { useState } from 'react';
import { exportShoppingList } from '../lib/api';
import { Snackbar } from '../components/ui/Snackbar';
```

### 2b: Add state for snackbar

Inside component, add:

```typescript
const [snackbar, setSnackbar] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
```

### 2c: Add handlers

```typescript
const handleShare = async () => {
  if (!householdId || !listId) return;
  try {
    const text = await exportShoppingList(householdId, listId, 'text');
    await navigator.clipboard.writeText(text);
    setSnackbar({ message: 'List copied to clipboard', type: 'success' });
  } catch {
    setSnackbar({ message: 'Failed to copy list', type: 'error' });
  }
};

const handleExportCsv = async () => {
  if (!householdId || !listId) return;
  try {
    const csv = await exportShoppingList(householdId, listId, 'csv');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `shopping-list-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch {
    setSnackbar({ message: 'Failed to export list', type: 'error' });
  }
};
```

### 2d: Modify header (around line 326)

Find the header div and add action buttons:

```tsx
<div className="shopping-detail__header">
  <h1 className="shopping-detail__title">{list?.name}</h1>
  <div className="shopping-detail__header-actions">
    <button
      type="button"
      className="ghost-button shopping-detail__header-btn"
      onClick={handleShare}
      aria-label="Copy list to clipboard"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
        <polyline points="16 6 12 2 8 6" />
        <line x1="12" y1="2" x2="12" y2="15" />
      </svg>
      Share
    </button>
    <button
      type="button"
      className="ghost-button shopping-detail__header-btn"
      onClick={handleExportCsv}
      aria-label="Export as CSV"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <polyline points="7 10 12 15 17 10" />
        <line x1="12" y1="15" x2="12" y2="3" />
      </svg>
      Export
    </button>
  </div>
</div>
```

### 2e: Add Snackbar at end of component (before closing div)

```tsx
{snackbar && (
  <Snackbar
    message={snackbar.message}
    type={snackbar.type}
    onClose={() => setSnackbar(null)}
  />
)}
```

Check Snackbar component props — may need to adjust based on actual interface.

---

## Step 3: Add CSS styles

**File:** `clients/web/src/routes/ShoppingDetail.css`

```css
.shopping-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-2);
}

.shopping-detail__header-actions {
  display: flex;
  gap: var(--spacing-2);
}

.shopping-detail__header-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-sm);
  padding: var(--spacing-1) var(--spacing-2);
}

.shopping-detail__header-btn svg {
  flex-shrink: 0;
}
```

Note: Check if `.shopping-detail__header` already has styles — merge rather than duplicate.

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web

npm run build
npm run lint
```

Manual test:
1. Open shopping list with items
2. Click Share → check clipboard content
3. Click Export → check downloaded CSV file
4. Verify snackbar shows

---

## Constraints
- Use existing Snackbar component for feedback
- Follow BEM naming (shopping-detail__*)
- Inline SVG icons (consistent with existing patterns)
- Handle empty list gracefully (backend returns appropriate text)
