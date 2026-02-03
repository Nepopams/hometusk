# DoD Checklist: ST-1203 — ASR Upload + Polling Service Hook

## Sources of Truth
- Workpack: `docs/planning/workpacks/ST-1203/workpack.md`
- ASR Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- DoD: `docs/_governance/dod.md`

---

## Acceptance Criteria

- [ ] AC-1: Hook returns { transcribe, isTranscribing, transcript, error, reset }
- [ ] AC-2: Upload creates transcription job with headers (Idempotency-Key, X-Correlation-ID)
- [ ] AC-3: Polling respects pollAfterMs from response
- [ ] AC-4: Transcript returned on done status
- [ ] AC-5: Error returned on failed status
- [ ] AC-6: Max 30 polling attempts
- [ ] AC-7: Reset clears state and cancels in-flight requests

---

## Code Quality

- [ ] Code follows project conventions (React hooks, TypeScript)
- [ ] No TypeScript errors (`npx tsc --noEmit`)
- [ ] Proper error types defined
- [ ] AbortController used for cancellation

---

## Build Verification

- [ ] Web client builds successfully: `cd clients/web && npm run build`
- [ ] Hook exported from hooks/index.ts

---

## API Contract Compliance

- [ ] POST uses multipart/form-data
- [ ] Authorization header included
- [ ] Idempotency-Key header included
- [ ] X-Correlation-ID header included
- [ ] GET respects pollAfterMs
- [ ] Handles 429 rate limit responses

---

## Error Handling

- [ ] upload_failed on non-429 POST errors
- [ ] rate_limited on 429 responses
- [ ] transcription_failed on ASR failure
- [ ] timeout after 30 poll attempts
- [ ] network_error on fetch exceptions

---

## Final Verification

- [ ] All checklist items complete
- [ ] Ready for integration with CommandInput (ST-1205)
