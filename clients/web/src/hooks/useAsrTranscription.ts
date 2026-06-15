import { useCallback, useRef, useState } from 'react';
import { logVoiceEvent } from '../lib/voiceTelemetry';
import { refreshAuthSession } from '../lib/api';
import { getAuthToken, handleAuthError } from '../lib/auth/tokenProvider';
import { normalizeAudioForAsr } from '../lib/audioWav';

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
  | 'unsupported_media'
  | 'network_error'
  | 'not_authenticated';

export interface AsrTranscriptionError {
  type: AsrErrorType;
  code?: string;
  message?: string;
  retryAfterMs?: number;
}

export interface AsrTranscriptionResult {
  transcript: string;
  traceId: string;
  latencyMs: number;
}

export interface UseAsrTranscriptionResult {
  transcribe: (audioBlob: Blob, correlationId?: string) => Promise<AsrTranscriptionResult | null>;
  isTranscribing: boolean;
  transcript: string | null;
  traceId: string | null;
  latencyMs: number | null;
  error: AsrTranscriptionError | null;
  reset: () => void;
}

const getApiBaseUrl = (rawBaseUrl = import.meta.env.VITE_API_BASE_URL || ''): string => {
  const baseUrl = rawBaseUrl || '';
  return baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
};

const generateCorrelationId = (): string => crypto.randomUUID();

export const getVoiceTranscriptionUrl = (rawBaseUrl?: string): string =>
  `${getApiBaseUrl(rawBaseUrl)}/voice/transcriptions`;

export const getAudioFileName = (contentType: string): string => {
  const normalizedType = contentType.split(';')[0]?.trim().toLowerCase();
  switch (normalizedType) {
    case 'audio/mp4':
    case 'audio/m4a':
      return 'recording.m4a';
    case 'audio/ogg':
      return 'recording.ogg';
    case 'audio/wav':
    case 'audio/x-wav':
      return 'recording.wav';
    case 'audio/mpeg':
    case 'audio/mp3':
      return 'recording.mp3';
    case 'audio/webm':
    default:
      return 'recording.webm';
  }
};

export const mapAsrErrorType = (status: number, code?: string): AsrErrorType => {
  if (status === 401) return 'not_authenticated';
  if (status === 415 || code === 'unsupported_media') return 'unsupported_media';
  if (status === 429 || code === 'local_rate_limit') return 'rate_limited';
  if (status === 504 || code === 'timeout') return 'timeout';
  if (status === 502 || code === 'upstream_unavailable' || code === 'bad_upstream_response') {
    return 'transcription_failed';
  }
  return 'upload_failed';
};

function createTranscriptionRequestInit(
  audioBlob: Blob,
  correlationId: string,
  signal: AbortSignal,
  includeBearerToken: boolean
): RequestInit {
  const token = includeBearerToken ? getAuthToken() : null;
  const headers: HeadersInit = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    'X-Correlation-ID': correlationId,
  };
  const formData = new FormData();
  formData.append('file', audioBlob, getAudioFileName(audioBlob.type));

  return {
    method: 'POST',
    headers,
    body: formData,
    credentials: 'include',
    signal,
  };
}

export function useAsrTranscription(): UseAsrTranscriptionResult {
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcript, setTranscript] = useState<string | null>(null);
  const [traceId, setTraceId] = useState<string | null>(null);
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const [error, setError] = useState<AsrTranscriptionError | null>(null);

  const abortControllerRef = useRef<AbortController | null>(null);
  const correlationIdRef = useRef<string | null>(null);
  const startTimeRef = useRef<number>(0);

  const transcribe = useCallback(
    async (audioBlob: Blob, correlationId?: string): Promise<AsrTranscriptionResult | null> => {
      correlationIdRef.current = correlationId || generateCorrelationId();
      startTimeRef.current = Date.now();
      setError(null);
      setTranscript(null);
      setTraceId(null);
      setLatencyMs(null);
      setIsTranscribing(true);

      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      abortControllerRef.current = new AbortController();

      try {
        const uploadBlob = await normalizeAudioForAsr(audioBlob).catch((err) => {
          console.warn('[VoiceASR] audio normalization failed', err);
          return audioBlob;
        });
        const url = getVoiceTranscriptionUrl();
        let response = await fetch(
          url,
          createTranscriptionRequestInit(
            uploadBlob,
            correlationIdRef.current,
            abortControllerRef.current.signal,
            true
          )
        );

        if (response.status === 401) {
          const refreshed = await refreshAuthSession().catch(() => false);
          if (refreshed) {
            response = await fetch(
              url,
              createTranscriptionRequestInit(
                uploadBlob,
                correlationIdRef.current,
                abortControllerRef.current.signal,
                false
              )
            );
          }
        }

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
          const code = typeof data.code === 'string' ? data.code : undefined;
          const errorType = mapAsrErrorType(response.status, code);
          if (errorType === 'not_authenticated') {
            handleAuthError('session_expired');
          }
          const retryAfterMs =
            errorType === 'rate_limited' ? parseRetryAfterMs(response.headers.get('Retry-After')) : undefined;

          logVoiceEvent({
            type: 'voice_asr_fail',
            correlationId: correlationIdRef.current || undefined,
            errorType,
            durationMs: Date.now() - startTimeRef.current,
          });
          setError({
            type: errorType,
            code,
            message: typeof data.message === 'string' ? data.message : 'Transcription failed',
            retryAfterMs,
          });
          return null;
        }

        const result: AsrTranscriptionResult = {
          transcript: typeof data.transcript === 'string' ? data.transcript : '',
          traceId: typeof data.traceId === 'string' ? data.traceId : correlationIdRef.current,
          latencyMs: typeof data.latencyMs === 'number' ? data.latencyMs : Date.now() - startTimeRef.current,
        };

        logVoiceEvent({
          type: 'voice_asr_ok',
          correlationId: correlationIdRef.current || undefined,
          durationMs: Date.now() - startTimeRef.current,
        });
        setTranscript(result.transcript);
        setTraceId(result.traceId);
        setLatencyMs(result.latencyMs);
        return result;
      } catch (err) {
        if ((err as Error).name === 'AbortError') {
          return null;
        }
        logVoiceEvent({
          type: 'voice_asr_fail',
          correlationId: correlationIdRef.current || undefined,
          errorType: 'network_error',
          durationMs: Date.now() - startTimeRef.current,
        });
        setError({ type: 'network_error', message: (err as Error).message });
        return null;
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
    setTraceId(null);
    setLatencyMs(null);
    setError(null);
    correlationIdRef.current = null;
    startTimeRef.current = 0;
  }, []);

  return {
    transcribe,
    isTranscribing,
    transcript,
    traceId,
    latencyMs,
    error,
    reset,
  };
}
