# Codex APPLY: ST-1306 — Marketplace Link-out Buttons

## Context
Add marketplace link-out buttons to shopping items. Place inside `.shopping-detail__item-info` after meta/task link.

## Files to Create/Modify

| File | Action |
|------|--------|
| `clients/web/src/lib/api.ts` | MODIFY — add getMarketplaceTemplates |
| `clients/web/src/hooks/useMarketplaceTemplates.ts` | CREATE |
| `clients/web/src/hooks/index.ts` | MODIFY — export new hook |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY — add marketplace links |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY — add styles |

---

## Step 1: Add API function

**File:** `clients/web/src/lib/api.ts`

Add type and function:

```typescript
export interface MarketplaceTemplate {
  id: string;
  name: string;
  urlTemplate: string;
  iconUrl?: string;
}

export async function getMarketplaceTemplates(): Promise<MarketplaceTemplate[]> {
  const baseUrl = getApiBaseUrl();
  // Public endpoint - no auth required
  const response = await fetch(`${baseUrl}/api/v1/marketplace-templates`);
  if (!response.ok) {
    return []; // Graceful fallback
  }
  return response.json();
}
```

---

## Step 2: Create useMarketplaceTemplates hook

**File:** `clients/web/src/hooks/useMarketplaceTemplates.ts`

```typescript
import { useEffect, useState } from 'react';
import { getMarketplaceTemplates, MarketplaceTemplate } from '../lib/api';

export function useMarketplaceTemplates() {
  const [templates, setTemplates] = useState<MarketplaceTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    getMarketplaceTemplates()
      .then((data) => {
        if (mounted) {
          setTemplates(data);
        }
      })
      .catch(() => {
        // Silently fail - marketplace buttons just won't show
      })
      .finally(() => {
        if (mounted) {
          setIsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  return { templates, isLoading };
}
```

**File:** `clients/web/src/hooks/index.ts`

Add export:
```typescript
export { useMarketplaceTemplates } from './useMarketplaceTemplates';
```

---

## Step 3: Add marketplace links to ShoppingDetail

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

### 3a: Add imports

```typescript
import { useMarketplaceTemplates } from '../hooks/useMarketplaceTemplates';
import { buildMarketplaceUrl } from '../lib/marketplaceUrl';
```

### 3b: Use hook in component

```typescript
const { templates: marketplaceTemplates } = useMarketplaceTemplates();
```

### 3c: Modify renderItem function

Inside `renderItem`, after the task link section (inside `.shopping-detail__item-info`), add:

```tsx
{marketplaceTemplates.length > 0 && (
  <div className="shopping-detail__item-marketplaces">
    {marketplaceTemplates.map((mp) => (
      <a
        key={mp.id}
        href={buildMarketplaceUrl(mp.urlTemplate, item.name)}
        target="_blank"
        rel="noopener noreferrer"
        className="shopping-detail__marketplace-link"
        aria-label={`Search ${item.name} on ${mp.name}`}
        title={mp.name}
        onClick={(e) => e.stopPropagation()}
      >
        {mp.iconUrl ? (
          <img src={mp.iconUrl} alt="" width="14" height="14" />
        ) : (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        )}
      </a>
    ))}
  </div>
)}
```

Note: `onClick={(e) => e.stopPropagation()}` prevents item row click handlers from interfering.

---

## Step 4: Add CSS styles

**File:** `clients/web/src/routes/ShoppingDetail.css`

```css
.shopping-detail__item-marketplaces {
  display: inline-flex;
  gap: 4px;
  margin-left: var(--spacing-2);
}

.shopping-detail__marketplace-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--radius-sm);
  color: var(--color-text-tertiary);
  transition: background-color 0.15s, color 0.15s;
}

.shopping-detail__marketplace-link:hover {
  background-color: var(--color-bg-hover);
  color: var(--color-primary);
}

.shopping-detail__marketplace-link img {
  width: 14px;
  height: 14px;
  object-fit: contain;
}

/* Hide for purchased items (optional - cleaner UI) */
.shopping-detail__item--purchased .shopping-detail__item-marketplaces {
  display: none;
}
```

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web

npm run build
npm run lint
```

Manual test:
1. Open shopping list with items
2. Verify marketplace icons appear on unpurchased items
3. Click icon → opens marketplace in new tab with item name
4. Test with Cyrillic item name (e.g., "Молоко")
5. Verify no icons on purchased items (if styled to hide)

---

## Constraints
- Use `buildMarketplaceUrl()` for safe URL encoding
- Place links inside item-info (not separate column)
- Graceful fallback if templates API fails (just hide buttons)
- `stopPropagation` to prevent row click interference
