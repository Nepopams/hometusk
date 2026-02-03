# Workpack: ST-1203 — ASR Upload + Polling Service Hook

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/epics/EP-012/epic.md`
- Story spec: `docs/planning/epics/EP-012/stories/ST-1203-asr-upload-polling.md`
- ASR Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status: Ready

## Outcome
A React hook `useAsrTranscription` that uploads audio blobs to ASR proxy and polls for transcription results.

## Acceptance Criteria (from story)
- AC-1: Hook returns { transcribe, isTranscribing, transcript, error, reset }
- AC-2: Upload creates transcription job with headers
- AC-3: Polling respects pollAfterMs
- AC-4: Transcript returned on done status
- AC-5: Error returned on failed status
- AC-6: Max 30 polling attempts
- AC-7: Reset clears state

---

## API Contract Summary

### POST /api/v1/households/{householdId}/asr/transcriptions
- **Request**: multipart/form-data with `file` (Blob), `languageHint` (optional)
- **Headers**: `Authorization: Bearer <JWT>`, `Idempotency-Key`, `X-Correlation-ID`
- **Response 202**: `{ id, status: "queued", createdAt }`

### GET /api/v1/households/{householdId}/asr/transcriptions/{id}
- **Response 200**: `{ id, status, text, pollAfterMs, error, ... }`
- **Statuses**: `queued` → `processing` → `done` | `failed`
- **pollAfterMs**: milliseconds to wait before next poll (2000ms typical)

---

## Files to Change

### New Files
| Path | Purpose |
|------|---------|
| `clients/web/src/hooks/useAsrTranscription.ts` | ASR upload + polling hook |

### Modified Files
| Path | Change |
|------|--------|
| `clients/web/src/hooks/index.ts` | Export useAsrTranscription |

---

## Implementation Plan

### Commit 1: useAsrTranscription hook

```typescript
// clients/web/src/hooks/useAsrTranscription.ts
import { useState, useCallback, useRef } from 'react';

const MAX_POLL_ATTEMPTS = 30;
const DEFAULT_POLL_INTERVAL_MS = 2000;

export type AsrError =
  | 'upload_failed'
  | 'transcription_failed'
  | 'timeout'
  | 'rate_limited'
  | 'network_error';

export interface AsrTranscriptionError {
  type: AsrError;
  code?: string;
  message?: string;
}

export interface UseAsrTranscriptionResult {
  transcribe: (audioBlob: Blob, householdId: string) => Promise<void>;
  isTranscribing: boolean;
  transcript: string | null;
  error: AsrTranscriptionError | null;
  reset: () => void;
}

export function useAsrTranscription(): UseAsrTranscriptionResult {
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcript, setTranscript] = useState<string | null>(null);
  const [error, setError] = useState<AsrTranscriptionError | null>(null);

  const abortControllerRef = useRef<AbortController | null>(null);
  const pollCountRef = useRef(0);

  const getAuthToken = (): string | null => {
    // Get JWT from localStorage or auth context
    return localStorage.getItem('auth_token');
  };

  const generateCorrelationId = (): string => {
    return crypto.randomUUID();
  };

  const generateIdempotencyKey = (): string => {
    return `asr-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`;
  };

  const uploadAudio = async (
    audioBlob: Blob,
    householdId: string,
    signal: AbortSignal
  ): Promise<{ id: string } | null> => {
    const token = getAuthToken();
    if (!token) {
      setError({ type: 'upload_failed', message: 'Not authenticated' });
      return null;
    }

    const formData = new FormData();
    formData.append('file', audioBlob, 'recording.webm');
    formData.append('languageHint', 'auto');

    try {
      const response = await fetch(
        `/api/v1/households/${householdId}/asr/transcriptions`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Idempotency-Key': generateIdempotencyKey(),
            'X-Correlation-ID': generateCorrelationId(),
          },
          body: formData,
          signal,
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        if (response.status === 429) {
          setError({ type: 'rate_limited', code: errorData.code, message: errorData.message });
        } else {
          setError({ type: 'upload_failed', code: errorData.code, message: errorData.message });
        }
        return null;
      }

      const data = await response.json();
      return { id: data.id };
    } catch (err) {
      if ((err as Error).name === 'AbortError') {
        return null;
      }
      setError({ type: 'network_error', message: (err as Error).message });
      return null;
    }
  };

  const pollTranscription = async (
    transcriptionId: string,
    householdId: string,
    signal: AbortSignal
  ): Promise<void> => {
    const token = getAuthToken();
    if (!token) return;

    pollCountRef.current = 0;

    while (pollCountRef.current < MAX_POLL_ATTEMPTS) {
      if (signal.aborted) return;

      pollCountRef.current++;

      try {
        const response = await fetch(
          `/api/v1/households/${householdId}/asr/transcriptions/${transcriptionId}`,
          {
            method: 'GET',
            headers: {
              Authorization: `Bearer ${token}`,
              'X-Correlation-ID': generateCorrelationId(),
            },
            signal,
          }
        );

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({}));
          if (response.status === 429) {
            setError({ type: 'rate_limited', code: errorData.code, message: errorData.message });
          } else {
            setError({ type: 'transcription_failed', code: errorData.code, message: errorData.message });
          }
          return;
        }

        const data = await response.json();

        if (data.status === 'done') {
          setTranscript(data.text || '');
          return;
        }

        if (data.status === 'failed') {
          setError({
            type: 'transcription_failed',
            code: data.error?.code,
            message: data.error?.message,
          });
          return;
        }

        // queued or processing - wait and poll again
        const waitMs = data.pollAfterMs || DEFAULT_POLL_INTERVAL_MS;
        await new Promise((resolve) => setTimeout(resolve, waitMs));
      } catch (err) {
        if ((err as Error).name === 'AbortError') return;
        setError({ type: 'network_error', message: (err as Error).message });
        return;
      }
    }

    // Exceeded max attempts
    setError({ type: 'timeout', message: 'Transcription timed out' });
  };

  const transcribe = useCallback(async (audioBlob: Blob, householdId: string) => {
    // Reset state
    setError(null);
    setTranscript(null);
    setIsTranscribing(true);
    pollCountRef.current = 0;

    // Cancel any existing operation
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;

    try {
      // Upload audio
      const result = await uploadAudio(audioBlob, householdId, signal);
      if (!result) {
        setIsTranscribing(false);
        return;
      }

      // Poll for result
      await pollTranscription(result.id, householdId, signal);
    } finally {
      setIsTranscribing(false);
    }
  }, []);

  const reset = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsTranscribing(false);
    setTranscript(null);
    setError(null);
    pollCountRef.current = 0;
  }, []);

  return {
    transcribe,
    isTranscribing,
    transcript,
    error,
    reset,
  };
}
```

---

## Verification Commands

```bash
cd clients/web && npm run build
cd clients/web && npx tsc --noEmit
```

---

## DoD Checklist Reference
See `checklist.md` for full DoD verification.

---

## Risks

| Risk | Mitigation |
|------|------------|
| Network failures during polling | AbortController + error handling |
| Rate limiting | Respect pollAfterMs, max 30 attempts |
| Memory leak on unmount | AbortController cancellation |

---

## Rollback
Hook is additive. Rollback = delete file, remove export.

---

## Dependencies
- ST-1202 (provides audioBlob)

## Blocked By
- None

## Blocks
- ST-1205 (CommandInput integration)
