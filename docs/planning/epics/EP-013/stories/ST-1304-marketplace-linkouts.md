# Story: ST-1304 — Marketplace Link-out Templates + Config

## Status: NOT READY
**Blocker**: Needs ADR-015 (link-out encoding design)

## Description
Implement marketplace link-out URL generation using configurable templates. URLs allow users to search for shopping items on external marketplaces (Ozon, Yandex Market, etc.).

**User Value**: One-click jump from HomeTusk item to marketplace search.

## In Scope
- `GET /api/v1/marketplace-templates` — Return available marketplace configs
- MarketplaceTemplate configuration structure (id, name, urlTemplate, iconUrl, enabled)
- URL generation with safe encoding: `https://ozon.ru/search?text={encoded_name}`
- Configuration via application.yml (initial) with future DB migration path
- Minimum 2 marketplaces: Ozon, Yandex Market
- XSS-safe URL encoding

## Out of Scope
- Per-household marketplace config
- Marketplace API integrations (OAuth, cart)
- Affiliate links/tracking
- Price comparison

## Acceptance Criteria

### AC-1: Template Config
```
Given application.yml has marketplace templates
When GET /marketplace-templates
Then response includes:
  - id: ozon, name: Ozon, urlTemplate: https://ozon.ru/search?text={query}
  - id: yandex_market, name: Yandex Market, urlTemplate: https://market.yandex.ru/search?text={query}
```

### AC-2: URL Generation
```
Given item name "Молоко 3.2%"
When URL generated for Ozon template
Then URL is: https://ozon.ru/search?text=%D0%9C%D0%BE%D0%BB%D0%BE%D0%BA%D0%BE%203.2%25
And special characters properly URL-encoded
```

### AC-3: XSS Prevention
```
Given item name "<script>alert('xss')</script>"
When URL generated
Then no raw HTML in URL
And script tags are encoded as %3Cscript%3E
```

### AC-4: Disabled Templates
```
Given a template with enabled=false
When GET /marketplace-templates
Then disabled template not returned (or marked disabled)
```

### AC-5: Missing Placeholder
```
Given a malformed template without {query}
When URL generation attempted
Then graceful fallback (use name as-is or error)
```

## Test Strategy

**Unit Tests**:
- URL encoding logic
- XSS vector handling
- Template parsing
- Special character edge cases (Cyrillic, emoji, quotes)

**Integration Tests**:
- Config loading from application.yml
- API response structure
- Encoding verification

**Test Data**:
- Items with Cyrillic names
- Items with special chars (<, >, &, ", ')
- Items with emoji
- Very long item names

## Flags
- contract_impact: no (static config, not new entity)
- adr_needed: lite (ADR-015 for encoding decisions)
- security_sensitive: no (but encoding is security-relevant)
- diagrams_needed: no

## Dependencies
- ADR-015: Marketplace link-out safe encoding (BLOCKER)

## Points: 5
