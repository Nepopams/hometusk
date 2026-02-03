# Story: ST-1203 — ASR Upload + Polling Service Hook

## Status: Ready

## Description
Create `useAsrTranscription` hook that uploads audio to ASR proxy and polls for result.

## In Scope
- `clients/web/src/hooks/useAsrTranscription.ts`
- Upload via POST /api/v1/households/{id}/asr/transcriptions
- Poll via GET .../transcriptions/{id}
- Use pollAfterMs from response
- Include Idempotency-Key and X-Correlation-ID headers

## Out of Scope
- Recording logic (ST-1202)
- UI components (ST-1204)

## Acceptance Criteria
- AC-1: Hook returns { transcribe, isTranscribing, transcript, error, reset }
- AC-2: Upload creates transcription job with headers
- AC-3: Polling respects pollAfterMs
- AC-4: Transcript returned on done status
- AC-5: Error returned on failed status
- AC-6: Max 30 polling attempts
- AC-7: Reset clears state

## Test Strategy
- Unit tests with mocked fetch
- Integration tests with MSW

## Points: 5

## Flags
- contract_impact: no (uses existing contract)
