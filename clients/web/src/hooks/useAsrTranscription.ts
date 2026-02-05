import { useCallback, useRef, useState } from 'react';
import { logVoiceEvent } from '../lib/voiceTelemetry';
import { getAuthToken } from '../lib/auth/tokenProvider';

const MAX_POLL_ATTEMPTS = 30;
const DEFAULT_POLL_INTERVAL_MS = 2000;
const DEFAULT_RETRY_AFTER_MS = 60_000;

const parseRetryAfterMs = (headerValue: string | null): number => {
  if (!headerValue) return DEFAULT_RETRY_AFTER_MS;
  const seconds = Number.parseInt(headerValue, 10);
  if (Number.isNaN(seconds)) return DEFAULT_RETRY_AFTER_MS;
  return seconds * 1000;
};

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
  retryAfterMs?: number;
}

export interface UseAsrTranscriptionResult {
  transcribe: (audioBlob: Blob, householdId: string, correlationId?: string) => Promise<void>;
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

const generateIdempotencyKey = (): string => `asr-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`;

export function useAsrTranscription(): UseAsrTranscriptionResult {
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcript, setTranscript] = useState<string | null>(null);
  const [error, setError] = useState<AsrTranscriptionError | null>(null);

  const abortControllerRef = useRef<AbortController | null>(null);
  const pollCountRef = useRef(0);
  const correlationIdRef = useRef<string | null>(null);
  const startTimeRef = useRef<number>(0);

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
          const retryAfterMs = parseRetryAfterMs(response.headers.get('Retry-After'));
          logVoiceEvent({
            type: 'voice_upload_fail',
            correlationId: correlationIdRef.current || undefined,
            errorType: 'rate_limited',
          });
          setError({
            type: 'rate_limited',
            code: errorData.code,
            message: errorData.message || 'Rate limit exceeded',
            retryAfterMs,
          });
        } else {
          logVoiceEvent({
            type: 'voice_upload_fail',
            correlationId: correlationIdRef.current || undefined,
            errorType: 'upload_failed',
          });
          setError({
            type: 'upload_failed',
            code: errorData.code,
            message: errorData.message || 'Upload failed',
          });
        }
        return null;
      }

      const data = await response.json();
      logVoiceEvent({
        type: 'voice_upload_ok',
        correlationId: correlationIdRef.current || undefined,
      });
      return { id: data.id };
    } catch (err) {
      if ((err as Error).name === 'AbortError') {
        return null;
      }
      logVoiceEvent({
        type: 'voice_upload_fail',
        correlationId: correlationIdRef.current || undefined,
        errorType: 'network_error',
      });
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
            const retryAfterMs = parseRetryAfterMs(response.headers.get('Retry-After'));
            logVoiceEvent({
              type: 'voice_asr_fail',
              correlationId: correlationIdRef.current || undefined,
              errorType: 'rate_limited',
              durationMs: Date.now() - startTimeRef.current,
            });
            setError({
              type: 'rate_limited',
              code: errorData.code,
              message: errorData.message,
              retryAfterMs,
            });
          } else {
            logVoiceEvent({
              type: 'voice_asr_fail',
              correlationId: correlationIdRef.current || undefined,
              errorType: 'transcription_failed',
              durationMs: Date.now() - startTimeRef.current,
            });
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
          logVoiceEvent({
            type: 'voice_asr_ok',
            correlationId: correlationIdRef.current || undefined,
            durationMs: Date.now() - startTimeRef.current,
          });
          setTranscript(data.text || '');
          return;
        }

        if (data.status === 'failed') {
          logVoiceEvent({
            type: 'voice_asr_fail',
            correlationId: correlationIdRef.current || undefined,
            errorType: 'transcription_failed',
            durationMs: Date.now() - startTimeRef.current,
          });
          setError({
            type: 'transcription_failed',
            code: data.error?.code,
            message: data.error?.message || 'Transcription failed',
          });
          return;
        }

        const waitMs = data.pollAfterMs || DEFAULT_POLL_INTERVAL_MS;
        await new Promise((resolve) => setTimeout(resolve, waitMs));
      } catch (err) {
        if ((err as Error).name === 'AbortError') return;
        logVoiceEvent({
          type: 'voice_asr_fail',
          correlationId: correlationIdRef.current || undefined,
          errorType: 'network_error',
          durationMs: Date.now() - startTimeRef.current,
        });
        setError({ type: 'network_error', message: (err as Error).message });
        return;
      }
    }

    logVoiceEvent({
      type: 'voice_asr_fail',
      correlationId: correlationIdRef.current || undefined,
      errorType: 'timeout',
      durationMs: Date.now() - startTimeRef.current,
    });
    setError({ type: 'timeout', message: 'Transcription timed out' });
  };

  const transcribe = useCallback(
    async (audioBlob: Blob, householdId: string, correlationId?: string) => {
      correlationIdRef.current = correlationId || null;
      startTimeRef.current = Date.now();
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
    correlationIdRef.current = null;
    startTimeRef.current = 0;
  }, []);

  return {
    transcribe,
    isTranscribing,
    transcript,
    error,
    reset,
  };
}
