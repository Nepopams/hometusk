# Checklist: ST-1104 — Guardrails

## Implementation
- [ ] Bucket4j dependency added
- [ ] V024 migration created
- [ ] AsrIdempotencyRecord entity
- [ ] AsrIdempotencyRecordRepository
- [ ] AsrValidationService
- [ ] AsrRateLimitService
- [ ] AsrIdempotencyService
- [ ] Exceptions created
- [ ] GlobalExceptionHandler updated
- [ ] Controller/Service integration
- [ ] application.yml config

## Tests
- [ ] File size validation tests
- [ ] Format validation tests
- [ ] Rate limit tests (POST/GET)
- [ ] Idempotency tests
- [ ] All ASR tests pass

## AC Verification
- [ ] AC-1: 413 for >10MB
- [ ] AC-2: 400 for invalid format
- [ ] AC-3: webm passes
- [ ] AC-4: POST 5/min
- [ ] AC-5: Isolated buckets
- [ ] AC-6: Config in yml
- [ ] AC-7: Missing file → 400
- [ ] AC-8: GET 30/min
- [ ] AC-9: Idempotency cached
- [ ] AC-10: Idempotency conflict
- [ ] AC-11: 24h TTL

## Build
- [ ] `./gradlew build` passes
- [ ] `./gradlew spotlessCheck` passes
