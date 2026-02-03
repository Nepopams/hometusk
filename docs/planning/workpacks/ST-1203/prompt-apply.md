# Codex APPLY Prompt: ST-1203 — ASR Upload + Polling Hook

## Mission
You are in **APPLY mode**. Create the `useAsrTranscription` hook based on PLAN findings.

---

## Context from PLAN Findings

### Auth Pattern
- Use `getAuthToken()` from `clients/web/src/lib/auth/tokenProvider`
- Or use `apiFetch` from `clients/web/src/lib/api.ts` (handles auth automatically)

### API Pattern
- Use `apiFetch` from `clients/web/src/lib/api.ts`
- Base URL from `import.meta.env.VITE_API_BASE_URL`

### Household Access
- Use `useAuth()` from `clients/web/src/hooks/useAuth.ts` to get `householdId`
- Note: Hook consumer will provide householdId, so hook doesn't need useAuth directly

### No Blockers

---

## Files to Create/Modify

### 1. CREATE: `clients/web/src/hooks/useAsrTranscription.ts`

```typescript
import { useState, useCallback, useRef } from 'react';
import { getAuthToken } from '../lib/auth/tokenProvider';

const MAX_POLL_ATTEMPTS = 30;
const DEFAULT_POLL_INTERVAL_MS = 2000;

export type AsrErrorType =
  | 'upload_failed'
  | 'transcription_failed'
  | 'timeout'
  | 'rate_limited'
  | 'network_error'
  | 'not_authenticated';

export interface AsrTranscriptionError {
  type: AsrErrorType;
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

const getApiBaseUrl = (): string => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
};

const generateCorrelationId = (): string => crypto.randomUUID();

const generateIdempotencyKey = (): string =>
  `asr-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`;

export function useAsrTranscription(): UseAsrTranscriptionResult {
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcript, setTranscript] = useState<string | null>(null);
  const [error, setError] = useState<AsrTranscriptionError | null>(null);

  const abortControllerRef = useRef<AbortController | null>(null);
  const pollCountRef = useRef(0);

  const uploadAudio = async (
    audioBlob: Blob,
    householdId: string,
    signal: AbortSignal
  ): Promise<{ id: string } | null> => {
    const token = getAuthToken();
    if (!token) {
      setError({ type: 'not_authenticated', message: 'Not authenticated' });
      return null;
    }

    const formData = new FormData();
    formData.append('file', audioBlob, 'recording.webm');
    formData.append('languageHint', 'auto');

    const baseUrl = getApiBaseUrl();

    try {
      const response = await fetch(
        `${baseUrl}/api/v1/households/${householdId}/asr/transcriptions`,
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
          setError({
            type: 'rate_limited',
            code: errorData.code,
            message: errorData.message || 'Rate limit exceeded',
          });
        } else {
          setError({
            type: 'upload_failed',
            code: errorData.code,
            message: errorData.message || 'Upload failed',
          });
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

    const baseUrl = getApiBaseUrl();
    pollCountRef.current = 0;

    while (pollCountRef.current < MAX_POLL_ATTEMPTS) {
      if (signal.aborted) return;

      pollCountRef.current++;

      try {
        const response = await fetch(
          `${baseUrl}/api/v1/households/${householdId}/asr/transcriptions/${transcriptionId}`,
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
            setError({
              type: 'rate_limited',
              code: errorData.code,
              message: errorData.message,
            });
          } else {
            setError({
              type: 'transcription_failed',
              code: errorData.code,
              message: errorData.message,
            });
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
            message: data.error?.message || 'Transcription failed',
          });
          return;
        }

        // queued or processing - wait pollAfterMs and poll again
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

  const transcribe = useCallback(
    async (audioBlob: Blob, householdId: string) => {
      setError(null);
      setTranscript(null);
      setIsTranscribing(true);
      pollCountRef.current = 0;

      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      abortControllerRef.current = new AbortController();
      const signal = abortControllerRef.current.signal;

      try {
        const result = await uploadAudio(audioBlob, householdId, signal);
        if (!result) {
          setIsTranscribing(false);
          return;
        }

        await pollTranscription(result.id, householdId, signal);
      } finally {
        setIsTranscribing(false);
      }
    },
    []
  );

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

### 2. MODIFY: `clients/web/src/hooks/index.ts`

Add exports:

```typescript
export { useAsrTranscription } from './useAsrTranscription';
export type { UseAsrTranscriptionResult, AsrTranscriptionError, AsrErrorType } from './useAsrTranscription';
```

---

## Verification

```bash
cd clients/web && npm run build
cd clients/web && npx tsc --noEmit
```

---

## Acceptance Criteria Mapping

| AC | Implementation |
|----|----------------|
| AC-1: Returns { transcribe, isTranscribing, transcript, error, reset } | Return object line ~160 |
| AC-2: Upload with headers | Idempotency-Key + X-Correlation-ID line ~55-60 |
| AC-3: Polling respects pollAfterMs | `data.pollAfterMs \|\| DEFAULT_POLL_INTERVAL_MS` line ~130 |
| AC-4: Transcript on done | `setTranscript(data.text)` line ~117 |
| AC-5: Error on failed | `setError(...)` line ~122-126 |
| AC-6: Max 30 polls | `MAX_POLL_ATTEMPTS = 30` + while check |
| AC-7: Reset clears state | `reset()` aborts + clears all state |

---

## Constraints

**ALLOWED:**
- Create/edit files listed above
- Run build/typecheck commands

**FORBIDDEN:**
- Modifying files outside listed scope
- Installing new dependencies

---

## Expected Output

Report:
1. Files created/modified
2. Build result (pass/fail)
3. TypeScript check result
