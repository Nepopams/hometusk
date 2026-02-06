# Workpack: ST-1305 — Share/Export UI Buttons

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1305-share-export-ui.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml` (export endpoint)
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** (ST-1303 backend export done)

---

## Goal
Add "Share" and "Export CSV" buttons to ShoppingDetail page for copying list to clipboard or downloading as CSV.

---

## Scope

### In Scope
- "Share" button — copy list text to clipboard
- "Export CSV" button — download CSV file
- Success/error feedback (toast/snackbar)
- Icons for buttons
- Keyboard accessible

### Out of Scope
- navigator.share API (native share sheet)
- App-specific sharing (WhatsApp, Telegram)
- QR code

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY | Add Share/Export buttons |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY | Button styles |
| `clients/web/src/lib/api.ts` | MODIFY | Add exportShoppingList API call |

---

## Implementation Plan

### Step 1: Add export API function

**File:** `clients/web/src/lib/api.ts`

```typescript
export async function exportShoppingList(
  householdId: string,
  listId: string,
  format: 'text' | 'csv' = 'text'
): Promise<string> {
  const token = getAuthToken();
  const response = await fetch(
    `${API_BASE}/households/${householdId}/shopping-lists/${listId}/export?format=${format}`,
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

### Step 2: Add Share/Export buttons to ShoppingDetail

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

Add buttons in header area (after list title):

```tsx
const [copyStatus, setCopyStatus] = useState<'idle' | 'success' | 'error'>('idle');

const handleShare = async () => {
  try {
    const text = await exportShoppingList(householdId, listId, 'text');
    await navigator.clipboard.writeText(text);
    setCopyStatus('success');
    setTimeout(() => setCopyStatus('idle'), 2000);
  } catch (err) {
    setCopyStatus('error');
    setTimeout(() => setCopyStatus('idle'), 2000);
  }
};

const handleExportCsv = async () => {
  try {
    const csv = await exportShoppingList(householdId, listId, 'csv');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `shopping-list-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    // Show error toast
  }
};

// In JSX, header area:
<div className="shopping-detail__actions">
  <button
    type="button"
    className="ghost-button shopping-detail__action-btn"
    onClick={handleShare}
    aria-label="Copy list to clipboard"
  >
    <ShareIcon />
    Share
  </button>
  <button
    type="button"
    className="ghost-button shopping-detail__action-btn"
    onClick={handleExportCsv}
    aria-label="Export as CSV"
  >
    <DownloadIcon />
    Export
  </button>
</div>

{copyStatus === 'success' && (
  <div className="shopping-detail__toast" role="status">
    List copied to clipboard
  </div>
)}
```

### Step 3: Add icons

Inline SVG icons or use existing icon pattern:

**ShareIcon:**
```tsx
function ShareIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
      <polyline points="16 6 12 2 8 6" />
      <line x1="12" y1="2" x2="12" y2="15" />
    </svg>
  );
}
```

**DownloadIcon:**
```tsx
function DownloadIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <polyline points="7 10 12 15 17 10" />
      <line x1="12" y1="15" x2="12" y2="3" />
    </svg>
  );
}
```

### Step 4: Add CSS styles

**File:** `clients/web/src/routes/ShoppingDetail.css`

```css
.shopping-detail__actions {
  display: flex;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-3);
}

.shopping-detail__action-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1);
  font-size: var(--font-size-sm);
}

.shopping-detail__action-btn svg {
  flex-shrink: 0;
}

.shopping-detail__toast {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-success);
  color: white;
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  z-index: 100;
  animation: fadeInOut 2s ease-in-out;
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0; }
  10%, 90% { opacity: 1; }
}
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

npm run build
npm run lint
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Share copies to clipboard | Manual test |
| AC-2 | Export downloads CSV | Manual test |
| AC-3 | Empty list handled | Manual test |
| AC-4 | Error handling | Test without clipboard |
| AC-5 | Button placement in header | Visual check |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Clipboard API not available | Low | Try/catch, show fallback |
| Large list performance | Low | Backend handles, frontend just downloads |

---

## Rollback

- Remove buttons and handlers
- Remove API function

---

## References

- Export endpoint: `GET /households/{id}/shopping-lists/{listId}/export?format=text|csv`
- Pattern: existing ShoppingDetail.tsx structure
