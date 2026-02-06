# Workpack: ST-1310 — URL Safe Encoding Guardrails

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1310-url-safe-encoding.md`
- ADR-015: `docs/adr/015-marketplace-linkout-encoding.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** (ST-1304 completed, blocker resolved)

---

## Goal
Implement the `buildMarketplaceUrl` utility function per ADR-015 with proper URL encoding, truncation, and unit tests covering XSS/injection vectors.

---

## Scope

### In Scope
- Create `buildMarketplaceUrl()` utility function
- URL encoding via `encodeURIComponent()`
- Truncation to 200 characters before encoding
- Unit test suite covering:
  - Standard ASCII encoding
  - Cyrillic/UTF-8 encoding
  - XSS vectors (script tags, event handlers)
  - SQL injection strings
  - Long input handling
  - Null/empty handling

### Out of Scope
- Backend changes (encoding happens on frontend per ADR-015)
- CSP headers
- Rate limiting

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/lib/marketplaceUrl.ts` | CREATE | URL encoding utility |
| `clients/web/src/lib/marketplaceUrl.test.ts` | CREATE | Unit tests |

---

## Implementation Plan

### Step 1: Create marketplaceUrl utility

**File:** `clients/web/src/lib/marketplaceUrl.ts`

```typescript
const MAX_QUERY_LENGTH = 200;
const QUERY_PLACEHOLDER = '{query}';

/**
 * Build a marketplace search URL by substituting the item name
 * into the URL template with proper encoding.
 *
 * @param template - URL template with {query} placeholder
 * @param itemName - Item name to search for
 * @returns Fully constructed URL with encoded query
 * @throws Error if template doesn't contain {query} placeholder
 */
export function buildMarketplaceUrl(template: string, itemName: string): string {
  if (!template.includes(QUERY_PLACEHOLDER)) {
    throw new Error('Template must contain {query} placeholder');
  }

  // Handle null/empty
  if (!itemName || itemName.trim() === '') {
    return template.replace(QUERY_PLACEHOLDER, '');
  }

  // Truncate to max length
  const truncated = itemName.slice(0, MAX_QUERY_LENGTH);

  // URL encode (handles Cyrillic, special chars, XSS vectors)
  const encoded = encodeURIComponent(truncated);

  return template.replace(QUERY_PLACEHOLDER, encoded);
}

/**
 * Constants exported for testing
 */
export const MARKETPLACE_URL_CONFIG = {
  maxQueryLength: MAX_QUERY_LENGTH,
  placeholder: QUERY_PLACEHOLDER,
};
```

### Step 2: Create unit tests

**File:** `clients/web/src/lib/marketplaceUrl.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import { buildMarketplaceUrl, MARKETPLACE_URL_CONFIG } from './marketplaceUrl';

const TEMPLATE = 'https://example.com/search?q={query}';

describe('buildMarketplaceUrl', () => {
  describe('standard encoding', () => {
    it('encodes spaces', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Milk 3.2%');
      expect(url).toBe('https://example.com/search?q=Milk%203.2%25');
    });

    it('handles simple ASCII', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'bread');
      expect(url).toBe('https://example.com/search?q=bread');
    });
  });

  describe('Cyrillic/UTF-8 encoding', () => {
    it('encodes Cyrillic text', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Молоко');
      expect(url).toBe('https://example.com/search?q=%D0%9C%D0%BE%D0%BB%D0%BE%D0%BA%D0%BE');
    });

    it('decodes correctly', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Молоко');
      const decoded = decodeURIComponent(url.split('q=')[1]);
      expect(decoded).toBe('Молоко');
    });
  });

  describe('XSS prevention', () => {
    it('encodes script tags', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '<script>alert(1)</script>');
      expect(url).not.toContain('<');
      expect(url).not.toContain('>');
      expect(url).toContain('%3Cscript%3Ealert(1)%3C%2Fscript%3E');
    });

    it('encodes event handlers', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'x onmouseover=alert(1)');
      expect(url).not.toContain(' onmouseover');
      expect(url).toContain('%20onmouseover');
    });

    it('encodes quotes', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'test"value\'other');
      expect(url).toContain('%22'); // double quote
      expect(url).toContain('%27'); // single quote
    });
  });

  describe('SQL injection prevention', () => {
    it('encodes SQL injection attempt', () => {
      const url = buildMarketplaceUrl(TEMPLATE, "'; DROP TABLE users;--");
      expect(url).not.toContain("'");
      expect(url).not.toContain(';');
      expect(url).toContain('%27');
      expect(url).toContain('%3B');
    });
  });

  describe('length handling', () => {
    it('truncates long input', () => {
      const longName = 'a'.repeat(500);
      const url = buildMarketplaceUrl(TEMPLATE, longName);
      const encoded = url.split('q=')[1];
      const decoded = decodeURIComponent(encoded);
      expect(decoded.length).toBe(MARKETPLACE_URL_CONFIG.maxQueryLength);
    });

    it('keeps short input intact', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'short');
      expect(url).toBe('https://example.com/search?q=short');
    });
  });

  describe('null/empty handling', () => {
    it('handles empty string', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '');
      expect(url).toBe('https://example.com/search?q=');
    });

    it('handles whitespace only', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '   ');
      expect(url).toBe('https://example.com/search?q=');
    });
  });

  describe('template validation', () => {
    it('throws if template has no placeholder', () => {
      expect(() => buildMarketplaceUrl('https://example.com', 'test')).toThrow(
        'Template must contain {query} placeholder'
      );
    });
  });
});
```

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Type check
npm run build

# Run tests
npm test -- marketplaceUrl

# All tests
npm test
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Standard URL encoding | Unit test: `encodes spaces` |
| AC-2 | Cyrillic encoding | Unit test: `encodes Cyrillic text` |
| AC-3 | XSS - script tags | Unit test: `encodes script tags` |
| AC-4 | XSS - event handlers | Unit test: `encodes event handlers` |
| AC-5 | SQL injection | Unit test: `encodes SQL injection` |
| AC-6 | Long input | Unit test: `truncates long input` |
| AC-7 | Null/empty | Unit test: `handles empty string` |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| encodeURIComponent edge cases | Low | Standard function, well-tested |
| Test coverage gaps | Medium | Follow OWASP vectors |

---

## Rollback

- Delete `marketplaceUrl.ts` and test file
- No other changes needed

---

## References

- ADR-015: `docs/adr/015-marketplace-linkout-encoding.md`
- RFC 3986: URI percent-encoding
- OWASP XSS Prevention Cheat Sheet
