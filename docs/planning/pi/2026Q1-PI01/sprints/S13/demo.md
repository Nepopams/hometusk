# Sprint S13 — Demo Plan

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S13/sprint.md`
- Epic: `docs/planning/epics/EP-011/epic.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`

---

## Demo Goal

Demonstrate end-to-end ASR proxy functionality: submit audio via HomeTusk API and receive transcription result.

---

## Demo Scenarios

### Scenario 1: Contract Validation (ST-1101)
**Purpose:** Show validated OpenAPI contract

**Steps:**
1. Open `docs/contracts/http/asr-proxy.openapi.yaml` in Swagger Editor or Redoc
2. Show endpoints: POST and GET
3. Show error responses (400, 401, 403, 404, 409, 413, 429, 500, 503)
4. Run spectral lint: `npx @stoplight/spectral-cli lint docs/contracts/http/asr-proxy.openapi.yaml`
5. Show contracts-index.md entry

**Expected Result:**
- Contract renders correctly
- Spectral lint passes with no errors
- Index updated

---

### Scenario 2: Create Transcription (ST-1102, ST-1103)
**Purpose:** Demonstrate audio upload and job creation

**Preconditions:**
- User authenticated (JWT token)
- User is member of household
- WireMock stub for asr-service (or real service if available)

**Steps:**
1. Prepare sample audio file (30 sec, OGG format)
2. Execute:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/households/{householdId}/asr/transcriptions" \
     -H "Authorization: Bearer ${JWT_TOKEN}" \
     -H "X-Correlation-ID: demo-123" \
     -F "file=@sample.ogg" \
     -F "languageHint=ru"
   ```
3. Show response:
   ```json
   {
     "id": "550e8400-e29b-41d4-a716-446655440000",
     "status": "queued",
     "createdAt": "2026-02-02T10:30:00Z"
   }
   ```
4. Show X-Correlation-ID in response header

**Expected Result:**
- 202 Accepted
- transcriptionId returned
- Correlation ID preserved

---

### Scenario 3: Poll Transcription Result (ST-1103)
**Purpose:** Demonstrate polling for transcription status/result

**Steps:**
1. Using transcriptionId from Scenario 2
2. Execute:
   ```bash
   curl -X GET "http://localhost:8080/api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}" \
     -H "Authorization: Bearer ${JWT_TOKEN}"
   ```
3. Show response (in-progress):
   ```json
   {
     "id": "550e8400-e29b-41d4-a716-446655440000",
     "status": "processing",
     "pollAfterMs": 2000,
     ...
   }
   ```
4. Wait and poll again
5. Show response (done):
   ```json
   {
     "id": "550e8400-e29b-41d4-a716-446655440000",
     "status": "done",
     "text": "Привет, убери пожалуйста на кухне",
     "lang": "ru",
     "durationMs": 3200,
     ...
   }
   ```

**Expected Result:**
- Status transitions: queued -> processing -> done
- Text appears when done
- pollAfterMs guides polling

---

### Scenario 4: Authentication Enforcement
**Purpose:** Demonstrate 401 without token

**Steps:**
1. Execute without Authorization header:
   ```bash
   curl -X GET "http://localhost:8080/api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}"
   ```
2. Show response:
   ```json
   {
     "code": "UNAUTHORIZED",
     "message": "Authorization header is required"
   }
   ```

**Expected Result:**
- 401 Unauthorized

---

### Scenario 5: Membership Enforcement
**Purpose:** Demonstrate 403 for non-member

**Steps:**
1. Use JWT for user NOT member of household
2. Execute:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/households/{otherHouseholdId}/asr/transcriptions" \
     -H "Authorization: Bearer ${OTHER_USER_TOKEN}" \
     -F "file=@sample.ogg"
   ```
3. Show response:
   ```json
   {
     "code": "FORBIDDEN",
     "message": "You are not a member of this household"
   }
   ```

**Expected Result:**
- 403 Forbidden

---

### Scenario 6: IDOR Prevention (ST-1103)
**Purpose:** Demonstrate household boundary enforcement

**Steps:**
1. Create transcription in household A
2. User from household B tries to GET:
   ```bash
   curl -X GET "http://localhost:8080/api/v1/households/{householdB}/asr/transcriptions/{transcriptionIdFromA}" \
     -H "Authorization: Bearer ${USER_B_TOKEN}"
   ```
3. Show response:
   ```json
   {
     "code": "ASR_NOT_FOUND",
     "message": "Transcription not found"
   }
   ```

**Expected Result:**
- 404 Not Found (not 403, to avoid leaking existence)

---

### Scenario 7: Error Handling (ST-1102)
**Purpose:** Demonstrate error mapping from asr-service

**Steps:**
1. Configure WireMock to return 400 INVALID_FORMAT
2. Upload unsupported file:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/households/{householdId}/asr/transcriptions" \
     -H "Authorization: Bearer ${JWT_TOKEN}" \
     -F "file=@sample.txt"
   ```
3. Show response:
   ```json
   {
     "code": "ASR_INVALID_FORMAT",
     "message": "Unsupported audio format. Supported: OGG, MP3, WAV, M4A, WebM",
     "correlationId": "..."
   }
   ```

**Expected Result:**
- 400 Bad Request
- Error code mapped from asr-service

---

## Demo Checklist

- [ ] Contract validated and renders in Swagger/Redoc
- [ ] POST creates transcription job (202)
- [ ] GET returns status and result (200)
- [ ] pollAfterMs included for in-progress status
- [ ] Authentication required (401)
- [ ] Membership required (403)
- [ ] IDOR prevention works (404 for wrong household)
- [ ] Error mapping works (400, 503)
- [ ] Correlation ID flows through
- [ ] Tests pass: `./gradlew test --tests "*Asr*"`

---

## Demo Environment

- Backend running locally (or test environment)
- WireMock stubs for asr-service
- Sample audio files (OGG, 30 sec)
- JWT tokens for test users (member and non-member)
- Two test households

---

## Demo Script (5 min)

1. **Contract** (1 min): Show OpenAPI, spectral lint
2. **Happy Path** (2 min): POST audio, GET result
3. **Security** (1 min): 401, 403, IDOR demo
4. **Error Handling** (1 min): Show mapped error

---

## Post-Demo Questions to Address

1. What happens if asr-service is down? (503 with Retry-After)
2. How do we prevent abuse? (Rate limiting in S14)
3. How do we clean up old AsrTranscriptionRef records? (TTL + scheduled job, S14+)
4. Where is the audio stored? (Not stored - pass-through proxy)
