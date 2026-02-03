# Checklist: ST-1106 — Security Tests

## Tests Created
- [ ] AsrSecurityIntegrationTest.java

## Authentication (401)
- [ ] POST without JWT
- [ ] GET without JWT

## Membership (403)
- [ ] POST as non-member
- [ ] GET as non-member

## IDOR Prevention (404)
- [ ] Wrong household access
- [ ] Non-existent ID

## API Key Non-Exposure
- [ ] Not in POST response
- [ ] Not in GET response
- [ ] Not in error response

## Error Mapping
- [ ] 503 → ASR_UNAVAILABLE
- [ ] 429 → ASR_RATE_LIMITED
- [ ] Timeout → ASR_UNAVAILABLE
- [ ] 401 → INTERNAL_ERROR

## Build
- [ ] All security tests pass
- [ ] All ASR tests pass
- [ ] `./gradlew build` passes
