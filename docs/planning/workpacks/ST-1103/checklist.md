# Checklist: ST-1103 — ASR Proxy Endpoints (Controller)

## DoR Verification
- [x] Story has clear title and description
- [x] Acceptance criteria defined (AC-1 to AC-10)
- [x] Contract available: `docs/contracts/http/asr-proxy.openapi.yaml`
- [x] Dependencies: ST-1102 (AsrClient)
- [x] Flags: contract_impact=yes, security_sensitive=yes

---

## Files Created
- [ ] `V023__create_asr_transcription_refs.sql` — migration
- [ ] `AsrTranscriptionRef.java` — entity
- [ ] `AsrTranscriptionRefRepository.java` — repository
- [ ] `CreateTranscriptionResponse.java` — DTO
- [ ] `TranscriptionResultResponse.java` — DTO
- [ ] `AsrService.java` — service
- [ ] `AsrController.java` — controller
- [ ] `AsrControllerIntegrationTest.java` — tests

## Files Modified
- [ ] `GlobalExceptionHandler.java` — ASR exception handlers

---

## Acceptance Criteria Verification

| AC | Description | Status |
|----|-------------|--------|
| AC-1 | POST returns 202 with id, status, createdAt | [ ] |
| AC-2 | GET returns 200 with pollAfterMs for queued/processing | [ ] |
| AC-3 | No JWT → 401 Unauthorized | [ ] |
| AC-4 | Not member → 403 Forbidden | [ ] |
| AC-5 | AsrException → mapped error response | [ ] |
| AC-6 | Missing correlationId → generated | [ ] |
| AC-7 | Present correlationId → preserved | [ ] |
| AC-8 | Wrong household → 404 (not 403) | [ ] |
| AC-9 | AsrTranscriptionRef persisted on POST | [ ] |
| AC-10 | GET validates via AsrTranscriptionRef | [ ] |

---

## Security (security_sensitive=yes)
- [ ] JWT auth enforced on both endpoints
- [ ] Household membership checked
- [ ] IDOR prevention via AsrTranscriptionRef
- [ ] Wrong household returns 404 (not 403)
- [ ] No cross-household data leaks

---

## Integration Tests
- [ ] `createTranscription_asMember_returns202`
- [ ] `createTranscription_notMember_returns403`
- [ ] `createTranscription_noAuth_returns401`
- [ ] `getTranscription_asMember_returns200`
- [ ] `getTranscription_wrongHousehold_returns404`
- [ ] `getTranscription_queued_includesPollAfterMs`
- [ ] `correlationId_generated_ifMissing`
- [ ] `correlationId_preserved_ifPresent`
- [ ] `asrError_returnsMappedError`

---

## Build Verification
- [ ] `./gradlew build` passes
- [ ] `./gradlew test --tests "*Asr*"` passes
- [ ] `./gradlew spotlessCheck` passes

---

## Final Sign-off
- [ ] All ACs verified
- [ ] All tests pass
- [ ] DoD criteria met
- [ ] E2E ASR proxy works
