# Codex APPLY: ST-1310 — URL Safe Encoding Guardrails

## Context
Implement `buildMarketplaceUrl()` utility with proper URL encoding per ADR-015. No test framework exists — add vitest.

## Files to Create/Modify

| File | Action |
|------|--------|
| `clients/web/package.json` | MODIFY — add vitest devDependency + test script |
| `clients/web/vite.config.ts` | MODIFY — add vitest config (if needed) |
| `clients/web/src/lib/marketplaceUrl.ts` | CREATE |
| `clients/web/src/lib/marketplaceUrl.test.ts` | CREATE |

---

## Step 1: Add vitest to package.json

**File:** `clients/web/package.json`

Add to devDependencies:
```json
"vitest": "^1.0.0"
```

Add to scripts:
```json
"test": "vitest run",
"test:watch": "vitest"
```

Run:
```bash
npm install
```

---

## Step 2: Configure vitest (if needed)

**File:** `clients/web/vite.config.ts`

If vitest config needed, add:
```typescript
/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'node',
  },
});
```

Note: vitest often works with vite config out of the box.

---

## Step 3: Create marketplaceUrl.ts

**File:** `clients/web/src/lib/marketplaceUrl.ts`

```typescript
const MAX_QUERY_LENGTH = 200;
const QUERY_PLACEHOLDER = '{query}';

/**
 * Build a marketplace search URL by substituting the item name
 * into the URL template with proper encoding.
 *
 * Per ADR-015: Client is responsible for URL encoding before substitution.
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

  // Handle null/empty/whitespace-only
  if (!itemName || itemName.trim() === '') {
    return template.replace(QUERY_PLACEHOLDER, '');
  }

  // Truncate to max length before encoding
  const truncated = itemName.slice(0, MAX_QUERY_LENGTH);

  // URL encode per RFC 3986 (handles Cyrillic, XSS vectors, special chars)
  const encoded = encodeURIComponent(truncated);

  return template.replace(QUERY_PLACEHOLDER, encoded);
}

/**
 * Constants exported for testing
 */
export const MARKETPLACE_URL_CONFIG = {
  maxQueryLength: MAX_QUERY_LENGTH,
  placeholder: QUERY_PLACEHOLDER,
} as const;
```

---

## Step 4: Create unit tests

**File:** `clients/web/src/lib/marketplaceUrl.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import { buildMarketplaceUrl, MARKETPLACE_URL_CONFIG } from './marketplaceUrl';

const TEMPLATE = 'https://example.com/search?q={query}';

describe('buildMarketplaceUrl', () => {
  describe('standard encoding', () => {
    it('encodes spaces as %20', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Milk 3.2%');
      expect(url).toBe('https://example.com/search?q=Milk%203.2%25');
    });

    it('handles simple ASCII unchanged', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'bread');
      expect(url).toBe('https://example.com/search?q=bread');
    });

    it('encodes ampersand and equals', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'salt & pepper = good');
      expect(url).toContain('%26'); // &
      expect(url).toContain('%3D'); // =
    });
  });

  describe('Cyrillic/UTF-8 encoding (AC-2)', () => {
    it('encodes Cyrillic text correctly', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Молоко');
      expect(url).toBe('https://example.com/search?q=%D0%9C%D0%BE%D0%BB%D0%BE%D0%BA%D0%BE');
    });

    it('round-trips correctly', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Молоко');
      const query = url.split('q=')[1];
      expect(decodeURIComponent(query)).toBe('Молоко');
    });

    it('handles mixed Cyrillic and ASCII', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'Молоко 2.5%');
      const query = url.split('q=')[1];
      expect(decodeURIComponent(query)).toBe('Молоко 2.5%');
    });
  });

  describe('XSS prevention (AC-3, AC-4)', () => {
    it('encodes script tags', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '<script>alert(1)</script>');
      expect(url).not.toContain('<');
      expect(url).not.toContain('>');
      expect(url).toContain('%3Cscript%3E');
      expect(url).toContain('%3C%2Fscript%3E');
    });

    it('encodes event handler attempts', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'x onmouseover=alert(1)');
      expect(url).not.toContain(' onmouseover');
      expect(url).toContain('x%20onmouseover');
    });

    it('encodes img onerror vector', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '<img src=x onerror=alert(1)>');
      expect(url).not.toContain('<img');
      expect(url).toContain('%3Cimg');
    });

    it('encodes quotes (double and single)', () => {
      const url = buildMarketplaceUrl(TEMPLATE, 'test"value\'other');
      expect(url).toContain('%22'); // "
      expect(url).toContain('%27'); // '
    });
  });

  describe('SQL injection prevention (AC-5)', () => {
    it('encodes SQL injection attempt', () => {
      const url = buildMarketplaceUrl(TEMPLATE, "'; DROP TABLE users;--");
      expect(url).not.toContain("'");
      expect(url).not.toContain(';');
      expect(url).toContain('%27'); // '
      expect(url).toContain('%3B'); // ;
    });

    it('encodes OR 1=1 injection', () => {
      const url = buildMarketplaceUrl(TEMPLATE, "' OR '1'='1");
      expect(url).toContain('%27');
      expect(url).toContain('%3D');
    });
  });

  describe('long input handling (AC-6)', () => {
    it('truncates input over 200 chars', () => {
      const longName = 'a'.repeat(500);
      const url = buildMarketplaceUrl(TEMPLATE, longName);
      const query = url.split('q=')[1];
      expect(decodeURIComponent(query).length).toBe(MARKETPLACE_URL_CONFIG.maxQueryLength);
    });

    it('keeps input under 200 chars intact', () => {
      const shortName = 'a'.repeat(100);
      const url = buildMarketplaceUrl(TEMPLATE, shortName);
      const query = url.split('q=')[1];
      expect(decodeURIComponent(query).length).toBe(100);
    });

    it('truncates before encoding (not after)', () => {
      // Cyrillic chars encode to multiple bytes - ensure truncation happens before
      const cyrillicLong = 'М'.repeat(250);
      const url = buildMarketplaceUrl(TEMPLATE, cyrillicLong);
      const query = url.split('q=')[1];
      const decoded = decodeURIComponent(query);
      expect(decoded.length).toBe(MARKETPLACE_URL_CONFIG.maxQueryLength);
    });
  });

  describe('null/empty handling (AC-7)', () => {
    it('handles empty string', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '');
      expect(url).toBe('https://example.com/search?q=');
    });

    it('handles whitespace-only string', () => {
      const url = buildMarketplaceUrl(TEMPLATE, '   ');
      expect(url).toBe('https://example.com/search?q=');
    });
  });

  describe('template validation', () => {
    it('throws if template missing placeholder', () => {
      expect(() => buildMarketplaceUrl('https://example.com', 'test')).toThrow(
        'Template must contain {query} placeholder'
      );
    });

    it('works with placeholder in different position', () => {
      const url = buildMarketplaceUrl('https://site.com/{query}/results', 'milk');
      expect(url).toBe('https://site.com/milk/results');
    });
  });
});
```

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web

# Install vitest
npm install

# Run tests
npm test

# Build (type check)
npm run build
```

Expected: All tests pass, build succeeds.

---

## Acceptance Criteria Checklist

- [ ] AC-1: Standard URL encoding (spaces → %20, % → %25)
- [ ] AC-2: Cyrillic encoding (UTF-8 percent-encoding)
- [ ] AC-3: XSS - script tags encoded
- [ ] AC-4: XSS - event handlers encoded
- [ ] AC-5: SQL injection chars encoded
- [ ] AC-6: Long input truncated to 200 chars
- [ ] AC-7: Null/empty handled gracefully

---

## Constraints
- Use `encodeURIComponent()` — standard, well-tested
- Truncate BEFORE encoding (not after)
- Follow existing lib/ module patterns (plain TS, no classes)
