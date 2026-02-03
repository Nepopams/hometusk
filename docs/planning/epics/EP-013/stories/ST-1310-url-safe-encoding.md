# Story: ST-1310 — URL Safe Encoding Guardrails

## Status: NOT READY
**Blocker**: Blocked by ST-1304 (marketplace templates must be designed first)

## Description
Implement and verify URL encoding guardrails for marketplace link-outs to prevent XSS, injection, and encoding issues. This is a security-focused story ensuring safe URL generation.

**User Value**: Safe, reliable links to marketplaces without security risks.

## In Scope
- URLEncoder utility with proper UTF-8 encoding
- XSS prevention (HTML entity encoding where needed)
- Input validation (max length, forbidden characters)
- Unit test suite covering attack vectors
- Security review checklist
- Documentation of encoding rules

## Out of Scope
- CSP headers (separate infrastructure story)
- Rate limiting on link generation
- Link click tracking/analytics

## Acceptance Criteria

### AC-1: Standard URL Encoding
```
Given item name "Milk 3.2%"
When URL encoded
Then result is "Milk%203.2%25"
And URL is valid per RFC 3986
```

### AC-2: Cyrillic Encoding
```
Given item name "Молоко"
When URL encoded (UTF-8)
Then result is "%D0%9C%D0%BE%D0%BB%D0%BE%D0%BA%D0%BE"
And decodes correctly on target site
```

### AC-3: XSS Prevention — Script Tags
```
Given item name "<script>alert(1)</script>"
When URL encoded
Then "<" becomes "%3C", ">" becomes "%3E"
And no executable script in final URL
```

### AC-4: XSS Prevention — Event Handlers
```
Given item name "x onmouseover=alert(1)"
When URL encoded
Then spaces encoded, no raw event handler
```

### AC-5: SQL Injection Prevention
```
Given item name "'; DROP TABLE users;--"
When URL encoded
Then special chars encoded
And URL is safe for browser navigation
```

### AC-6: Long Input Handling
```
Given item name with 1000 characters
When URL encoded
Then truncated to safe max length (e.g., 200 chars)
And URL remains valid
```

### AC-7: Null/Empty Handling
```
Given null or empty item name
When URL generation attempted
Then graceful handling (empty query or error)
And no NullPointerException
```

## Test Strategy

**Unit Tests**:
- Encoding correctness (ASCII, Cyrillic, CJK)
- XSS vector coverage (OWASP top vectors)
- Edge cases (empty, null, very long, special chars)
- Round-trip encoding/decoding

**Security Tests**:
- Manual review of generated URLs
- Browser testing with encoded URLs
- Penetration testing checklist

**Test Data**:
- OWASP XSS filter evasion cheat sheet inputs
- Real Cyrillic product names
- Emoji, mathematical symbols
- Boundary length inputs

## Flags
- contract_impact: no
- adr_needed: no (covered by ADR-015)
- security_sensitive: yes
- diagrams_needed: no

## Dependencies
- ST-1304: Marketplace templates design (BLOCKER — defines where encoding is used)

## Points: 3
