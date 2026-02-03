# Workpack: ST-1103 — ASR Proxy Endpoints (Controller)

## Sources of Truth
- Story: `docs/planning/epics/EP-011/stories/ST-1103-asr-proxy-endpoints.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- Sprint: `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md`
- HomeTusk Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- Product Goal: `docs/planning/strategy/product-goal.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Goal
Implement REST controller for ASR proxy with JWT auth, household membership enforcement, and IDOR prevention via AsrTranscriptionRef.

## User Value
> Могу отправить аудио через HomeTusk API и получить текст, не зная про asr-service.

---

## Scope

### In Scope
- `AsrController` with POST/GET endpoints
- JWT authentication (existing Spring Security)
- Household membership check (existing MembershipService)
- `AsrTranscriptionRef` entity + repository (IDOR prevention)
- Correlation ID handling (generate if missing)
- Exception to HTTP response mapping
- `pollAfterMs` in GET response for queued/processing
- Integration tests with WireMock

### Out of Scope
- Input validation beyond basic (ST-1104)
- Rate limiting (ST-1104)
- Metrics (ST-1105)
- Security edge cases (ST-1106)
- AsrIdempotencyRecord (ST-1104)

---

## Anchors (Non-negotiables)

| Anchor | Constraint |
|--------|------------|
| Contract | `docs/contracts/http/asr-proxy.openapi.yaml` |
| Decision G | AsrTranscriptionRef for IDOR prevention |
| Decision H | pollAfterMs in GET response (2000ms default) |
| Security | Return 404 (not 403) for wrong household |

---

## Files to Change

| File | Action | Purpose |
|------|--------|---------|
| `services/backend/src/main/resources/db/migration/V023__create_asr_transcription_refs.sql` | CREATE | Migration |
| `services/backend/src/main/java/com/hometusk/asr/domain/AsrTranscriptionRef.java` | CREATE | Entity |
| `services/backend/src/main/java/com/hometusk/asr/repository/AsrTranscriptionRefRepository.java` | CREATE | Repository |
| `services/backend/src/main/java/com/hometusk/asr/dto/CreateTranscriptionResponse.java` | CREATE | Response DTO |
| `services/backend/src/main/java/com/hometusk/asr/dto/TranscriptionResultResponse.java` | CREATE | Response DTO |
| `services/backend/src/main/java/com/hometusk/asr/service/AsrService.java` | CREATE | Service layer |
| `services/backend/src/main/java/com/hometusk/asr/controller/AsrController.java` | CREATE | Controller |
| `services/backend/src/main/java/com/hometusk/shared/exception/GlobalExceptionHandler.java` | MODIFY | Add ASR handlers |
| `services/backend/src/test/java/com/hometusk/asr/controller/AsrControllerIntegrationTest.java` | CREATE | Tests |

---

## Implementation Plan

### Commit 1: Create DB migration
```sql
CREATE TABLE asr_transcription_refs (
    transcription_id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_asr_transcription_refs_household ON asr_transcription_refs(household_id);
CREATE INDEX idx_asr_transcription_refs_expires ON asr_transcription_refs(expires_at);
```

### Commit 2: Create AsrTranscriptionRef entity + repository
- Entity with transcriptionId (PK), householdId, createdByUserId, createdAt, expiresAt
- Repository with `findByTranscriptionId(UUID)`

### Commit 3: Create response DTOs
- `CreateTranscriptionResponse`: id, status, createdAt
- `TranscriptionResultResponse`: id, status, text, segments, model, durationMs, lang, createdAt, finishedAt, pollAfterMs, error

### Commit 4: Create AsrService
- `createTranscription()`: call AsrClient, persist AsrTranscriptionRef, return response
- `getTranscription()`: lookup ref, verify household, call AsrClient, add pollAfterMs

### Commit 5: Create AsrController
```java
@RestController
@RequestMapping("/api/v1/households/{householdId}/asr")
public class AsrController {
    @PostMapping("/transcriptions")
    public ResponseEntity<CreateTranscriptionResponse> createTranscription(...);

    @GetMapping("/transcriptions/{transcriptionId}")
    public ResponseEntity<TranscriptionResultResponse> getTranscription(...);
}
```

### Commit 6: Extend GlobalExceptionHandler
Add handlers for AsrException subtypes → mapped HTTP responses

### Commit 7: Create integration tests
- `createTranscription_asMember_returns202`
- `createTranscription_notMember_returns403`
- `createTranscription_noAuth_returns401`
- `getTranscription_asMember_returns200`
- `getTranscription_wrongHousehold_returns404`
- `getTranscription_queued_includesPollAfterMs`
- `correlationId_generated_ifMissing`
- `correlationId_preserved_ifPresent`

---

## Verification Commands

```bash
cd services/backend && ./gradlew build
cd services/backend && ./gradlew test --tests "*Asr*"
cd services/backend && ./gradlew spotlessCheck
```

---

## Acceptance Criteria Mapping

| AC | Description | Verification |
|----|-------------|--------------|
| AC-1 | POST returns 202 | Integration test |
| AC-2 | GET returns 200 with pollAfterMs | Integration test |
| AC-3 | Auth required (401) | Integration test |
| AC-4 | Membership required (403) | Integration test |
| AC-5 | Errors mapped | Integration test |
| AC-6 | CorrelationId generated | Integration test |
| AC-7 | CorrelationId preserved | Integration test |
| AC-8 | Wrong household → 404 | Integration test |
| AC-9 | AsrTranscriptionRef persisted | Integration test |
| AC-10 | GET validates via ref | Integration test |

---

## DoD Checklist

- [ ] Migration V023 created
- [ ] AsrTranscriptionRef entity works
- [ ] AsrService orchestrates correctly
- [ ] AsrController endpoints work
- [ ] JWT auth enforced
- [ ] Membership enforced
- [ ] IDOR prevention works (404 for wrong household)
- [ ] pollAfterMs returned for queued/processing
- [ ] All integration tests pass
- [ ] Spotless formatting applied

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| WireMock setup complexity | MEDIUM | Use existing test patterns |
| Multipart handling | MEDIUM | Test with real audio files |

---

## Rollback

1. Revert commits
2. Migration can remain (drop table if needed)
