# Checklist: ST-1102 — AsrClient HTTP Adapter

## DoR Verification
- [x] Story has clear title and description
- [x] Acceptance criteria defined
- [x] External contract available
- [x] Error codes reference available
- [x] Flags identified: security_sensitive=yes

---

## Files Created
- [ ] `AsrClient.java` — interface
- [ ] `AsrClientImpl.java` — implementation
- [ ] `AsrProperties.java` — configuration
- [ ] `AsrResilienceConfig.java` — retry config
- [ ] `AsrJobCreated.java` — DTO
- [ ] `AsrJobResult.java` — DTO
- [ ] `AsrSegment.java` — DTO
- [ ] `AsrTranscriptionError.java` — DTO
- [ ] `AsrException.java` — base exception
- [ ] `AsrInvalidFormatException.java`
- [ ] `AsrAudioTooLongException.java`
- [ ] `AsrFileTooLargeException.java`
- [ ] `AsrRateLimitedException.java`
- [ ] `AsrUnavailableException.java`
- [ ] `AsrNotFoundException.java`
- [ ] `AsrTimeoutException.java`
- [ ] `AsrClientImplTest.java` — tests

## Files Modified
- [ ] `application.yml` — asr.* config added

---

## Test Coverage
- [ ] Happy path: createTranscription returns AsrJobCreated
- [ ] Happy path: getTranscription returns AsrJobResult
- [ ] Error: INVALID_FORMAT -> AsrInvalidFormatException
- [ ] Error: AUDIO_TOO_LONG -> AsrAudioTooLongException
- [ ] Error: FILE_TOO_LARGE -> AsrFileTooLargeException
- [ ] Error: RATE_LIMIT_EXCEEDED -> AsrRateLimitedException
- [ ] Error: SERVICE_UNAVAILABLE -> AsrUnavailableException
- [ ] Error: NOT_FOUND -> AsrNotFoundException
- [ ] Error: timeout -> AsrTimeoutException
- [ ] Header: correlationId propagated as X-Request-Id
- [ ] Header: idempotencyKey propagated
- [ ] Retry: 503 triggers retry

---

## Security (security_sensitive=yes)
- [ ] API key never logged
- [ ] API key not in exception messages
- [ ] API key from environment variable

---

## Build Verification
- [ ] `./gradlew build` passes
- [ ] `./gradlew test --tests "com.hometusk.asr.client.*"` passes
- [ ] `./gradlew spotlessCheck` passes

---

## Final Sign-off
- [ ] All tests pass
- [ ] DoD criteria met
- [ ] Ready for ST-1103 (Controller)
