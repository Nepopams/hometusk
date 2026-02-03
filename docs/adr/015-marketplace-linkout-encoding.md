# ADR-015: Marketplace Link-out Safe Encoding

**Status:** Proposed
**Date:** 2026-02-03
**Epic:** EP-013 — Shopping Marketplaces
**Blocks:** ST-1304 (Marketplace link-out templates + config)

## Context

EP-013 introduces marketplace link-outs — URLs that allow users to search for shopping items on external marketplaces (Ozon, Yandex Market). The system stores URL templates with a `{query}` placeholder, and the client substitutes the item name into the template.

### Security Requirements

1. **No XSS**: Encoded URLs must not execute scripts when rendered in browser
2. **No injection**: Special characters must not break URL structure
3. **UTF-8 support**: Cyrillic, CJK, emoji must encode correctly
4. **Length safety**: Very long names must not create malformed URLs

### Contract (from OpenAPI)

```yaml
MarketplaceTemplateDto:
  urlTemplate:
    type: string
    description: |
      URL template with {query} placeholder.
      Client must URL-encode the item name before substitution.
    example: "https://www.ozon.ru/search/?text={query}"
```

Key insight: **Client is responsible for encoding** before substitution.

### Options Considered

#### Template Storage

| Option | Pros | Cons |
|--------|------|------|
| **A) application.yml configuration** | Simple deployment, no DB migration | Requires restart to change |
| B) Database table | Dynamic updates, per-household customization | Over-engineered for v0 |

**Selected:** Option A — Configuration in application.yml.

#### Encoding Strategy

| Option | Pros | Cons |
|--------|------|------|
| **A) Standard URL encoding (RFC 3986)** | Well-defined standard, library support | Some edge cases with reserved chars |
| B) Custom encoding function | Full control | Reinventing the wheel, error-prone |

**Selected:** Option A — Use `encodeURIComponent()` on frontend. Follows RFC 3986 percent-encoding.

#### Encoding Responsibility

| Option | Pros | Cons |
|--------|------|------|
| **A) Client encodes before substitution** | Server templates remain simple | Client must implement correctly |
| B) Server returns pre-encoded URLs | Client simpler | N URLs per N items, server complexity |

**Selected:** Option A — Contract specifies "Client must URL-encode the item name before substitution."

#### Max Length Handling

| Option | Pros | Cons |
|--------|------|------|
| **A) Truncate to safe limit (200 chars pre-encoding)** | Prevents URL overflow | May cut meaningful content |
| B) No limit, let browser handle | Simple | URLs may exceed browser limits (~2048 chars) |

**Selected:** Option A — Truncate item name to 200 characters before encoding.

## Decision

### 1. URL Template Format

We will use `{query}` as the placeholder in URL templates:

```yaml
hometusk:
  marketplaces:
    templates:
      - id: ozon
        name: Ozon
        urlTemplate: "https://www.ozon.ru/search/?text={query}"
        iconUrl: /icons/ozon.svg
        enabled: true
      - id: yandex_market
        name: Yandex Market
        urlTemplate: "https://market.yandex.ru/search?text={query}"
        iconUrl: /icons/yandex-market.svg
        enabled: true
```

### 2. Encoding Rules

| Input | Encoded Output | Notes |
|-------|----------------|-------|
| Space | `%20` | Standard |
| `%` | `%25` | Must encode percent sign |
| `<`, `>` | `%3C`, `%3E` | XSS prevention |
| `"`, `'` | `%22`, `%27` | Quote encoding |
| `&`, `=` | `%26`, `%3D` | URL parameter safety |
| Cyrillic (Молоко) | `%D0%9C%D0%BE%D0%BB%D0%BE%D0%BA%D0%BE` | UTF-8 encoding |
| Emoji | Percent-encoded UTF-8 bytes | Standard |

### 3. Frontend Implementation

Utility function:

```typescript
function buildMarketplaceUrl(template: string, itemName: string): string {
  const maxLength = 200;
  const truncated = itemName.slice(0, maxLength);
  const encoded = encodeURIComponent(truncated);
  return template.replace('{query}', encoded);
}
```

### 4. Backend Configuration Service

```java
@ConfigurationProperties(prefix = "hometusk.marketplaces")
public class MarketplaceProperties {
    private List<MarketplaceTemplate> templates;
}

@Service
public class MarketplaceConfigService {
    public List<MarketplaceTemplateDto> getEnabledTemplates() {
        return properties.getTemplates().stream()
            .filter(MarketplaceTemplate::isEnabled)
            .map(this::toDto)
            .toList();
    }
}
```

### 5. Security Guardrails

1. **Template validation at startup**: Verify all templates contain `{query}` placeholder
2. **No user-provided templates**: Templates are system-configured only (v0)
3. **Frontend encoding**: Client must call `encodeURIComponent()` before substitution
4. **Max length enforcement**: Truncate to 200 chars before encoding
5. **No server-side URL construction with user input**: Templates returned as-is

### 6. Forbidden Patterns

Reject templates containing:
- `javascript:` scheme (XSS)
- `data:` scheme (data injection)
- Templates without `{query}` placeholder
- Templates with multiple `{query}` occurrences

Validation runs at application startup via `@PostConstruct`.

## Consequences

### Positive

- **XSS prevention**: URL encoding neutralizes script tags and event handlers
- **Standards-based**: RFC 3986 percent-encoding is well-understood
- **Simple deployment**: Configuration-based templates, no DB migration
- **Clear responsibility**: Client handles encoding, server provides templates

### Negative

- **Client must implement correctly**: Incorrect encoding could introduce vulnerabilities
- **Static configuration**: Changing templates requires deployment
- **Truncation may cut names**: Very long item names are shortened

### Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Client forgets to encode | Document clearly, provide utility function |
| Template misconfiguration | Startup validation, integration tests |
| Browser URL length limits | 200 char truncation keeps well under limit |

## Related

- **Contract:** `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- **Story:** `docs/planning/epics/EP-013/stories/ST-1304-marketplace-linkouts.md`
- **Security story:** `docs/planning/epics/EP-013/stories/ST-1310-url-safe-encoding.md`
