# Workpack: ST-1306 — Marketplace Link-out Buttons

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1306-marketplace-buttons-ui.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- ADR-015: `docs/adr/015-marketplace-linkout-encoding.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** (ST-1304 backend done, ST-1310 encoding utility done)

---

## Goal
Add marketplace link-out buttons to shopping items. Use `buildMarketplaceUrl()` from ST-1310 for safe URL generation.

---

## Scope

### In Scope
- Fetch marketplace templates on page load
- "Open in..." dropdown/buttons per item
- Use `buildMarketplaceUrl()` for URL generation
- Open in new tab (rel="noopener noreferrer")
- Marketplace icons (from template or fallback)
- Handle empty templates (hide buttons)

### Out of Scope
- In-app browser
- Price display
- Preferred marketplace storage

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/api.ts` | MODIFY | Add getMarketplaceTemplates API |
| `clients/web/src/hooks/useMarketplaceTemplates.ts` | CREATE | Hook to fetch templates |
| `clients/web/src/routes/ShoppingDetail.tsx` | MODIFY | Add marketplace buttons to items |
| `clients/web/src/routes/ShoppingDetail.css` | MODIFY | Button styles |

---

## Implementation Plan

### Step 1: Add API function

**File:** `clients/web/src/lib/api.ts`

```typescript
export interface MarketplaceTemplate {
  id: string;
  name: string;
  urlTemplate: string;
  iconUrl?: string;
}

export async function getMarketplaceTemplates(): Promise<MarketplaceTemplate[]> {
  const baseUrl = getApiBaseUrl();
  const response = await fetch(`${baseUrl}/api/v1/marketplace-templates`);
  if (!response.ok) {
    throw new Error('Failed to fetch marketplace templates');
  }
  return response.json();
}
```

Note: This endpoint doesn't require auth (per contract).

### Step 2: Create useMarketplaceTemplates hook

**File:** `clients/web/src/hooks/useMarketplaceTemplates.ts`

```typescript
import { useEffect, useState } from 'react';
import { getMarketplaceTemplates, MarketplaceTemplate } from '../lib/api';

export function useMarketplaceTemplates() {
  const [templates, setTemplates] = useState<MarketplaceTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let mounted = true;

    getMarketplaceTemplates()
      .then((data) => {
        if (mounted) {
          setTemplates(data);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (mounted) {
          setError(err);
          setIsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  return { templates, isLoading, error };
}
```

### Step 3: Add marketplace buttons to ShoppingDetail

**File:** `clients/web/src/routes/ShoppingDetail.tsx`

Import:
```typescript
import { useMarketplaceTemplates } from '../hooks/useMarketplaceTemplates';
import { buildMarketplaceUrl } from '../lib/marketplaceUrl';
```

In component:
```typescript
const { templates: marketplaceTemplates } = useMarketplaceTemplates();
```

In item render (inside renderItem or where items are displayed):
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
        aria-label={`Open ${item.name} in ${mp.name}`}
        title={mp.name}
      >
        {mp.iconUrl ? (
          <img src={mp.iconUrl} alt={mp.name} width="16" height="16" />
        ) : (
          <ExternalLinkIcon />
        )}
      </a>
    ))}
  </div>
)}
```

Add fallback icon:
```tsx
function ExternalLinkIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
      <polyline points="15 3 21 3 21 9" />
      <line x1="10" y1="14" x2="21" y2="3" />
    </svg>
  );
}
```

### Step 4: Add CSS styles

**File:** `clients/web/src/routes/ShoppingDetail.css`

```css
.shopping-detail__item-marketplaces {
  display: flex;
  gap: var(--spacing-1);
  margin-left: auto;
}

.shopping-detail__marketplace-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  transition: background-color 0.15s, color 0.15s;
}

.shopping-detail__marketplace-link:hover {
  background-color: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.shopping-detail__marketplace-link img {
  width: 16px;
  height: 16px;
  object-fit: contain;
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
2. Verify marketplace icons appear on each item
3. Click icon → opens marketplace search in new tab
4. Test with Cyrillic item names

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Buttons display | Visual check |
| AC-2 | Link generation | Click → new tab |
| AC-3 | Encoded names | Test Cyrillic |
| AC-4 | No templates = hidden | Mock empty response |
| AC-5 | Loading state | Brief/handled |
| AC-6 | Accessibility | aria-label check |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Icon URLs 404 | Low | Fallback icon |
| Too many marketplaces | Low | Limit to 3 or dropdown |

---

## Rollback

- Remove marketplace buttons from items
- Remove hook and API function

---

## References

- Templates endpoint: `GET /api/v1/marketplace-templates`
- URL encoding: `buildMarketplaceUrl()` from `lib/marketplaceUrl.ts`
- ADR-015: Safe encoding rules
