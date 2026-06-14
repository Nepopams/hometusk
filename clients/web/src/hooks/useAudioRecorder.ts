import { useCallback, useEffect, useRef, useState } from 'react';
import { logVoiceEvent } from '../lib/voiceTelemetry';

const MAX_DURATION_MS = 60_000;
const DURATION_UPDATE_INTERVAL_MS = 100;

export type AudioRecorderError =
  | 'permission_denied'
  | 'not_supported'
  | 'recording_failed'
  | 'no_audio_data';

export interface UseAudioRecorderResult {
  start: () => Promise<void>;
  stop: () => void;
  isRecording: boolean;
  duration: number;
  audioBlob: Blob | null;
  error: AudioRecorderError | null;
  reset: () => void;
  correlationId: string | null;
}

function createCorrelationId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = [...bytes].map((byte) => byte.toString(16).padStart(2, '0'));
    return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex
      .slice(6, 8)
      .join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10).join('')}`;
  }

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = Math.floor(Math.random() * 16);
    const value = char === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

function logRecorderError(error: unknown) {
  if (error instanceof DOMException) {
    console.warn('[VoiceRecorder] recording failed', {
      name: error.name,
      message: error.message,
    });
    return;
  }

  console.warn('[VoiceRecorder] recording failed', error);
}

export function useAudioRecorder(): UseAudioRecorderResult {
  const [isRecording, setIsRecording] = useState(false);
  const [duration, setDuration] = useState(0);
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [error, setError] = useState<AudioRecorderError | null>(null);
  const [correlationId, setCorrelationId] = useState<string | null>(null);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const startTimeRef = useRef<number>(0);
  const timerRef = useRef<number | null>(null);
  const autoStopRef = useRef<number | null>(null);

  const cleanup = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    if (autoStopRef.current) {
      clearTimeout(autoStopRef.current);
      autoStopRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    mediaRecorderRef.current = null;
    chunksRef.current = [];
  }, []);

  const stop = useCallback(() => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state === 'recording') {
      mediaRecorderRef.current.stop();
    }
    setIsRecording(false);
  }, []);

  const start = useCallback(async () => {
    setError(null);
    setAudioBlob(null);
    setDuration(0);
    chunksRef.current = [];

    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      setError('not_supported');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      const mimeType = (() => {
        if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
          return 'audio/webm;codecs=opus';
        }
        if (MediaRecorder.isTypeSupported('audio/webm')) {
          return 'audio/webm';
        }
        if (MediaRecorder.isTypeSupported('audio/mp4')) {
          return 'audio/mp4';
        }
        if (MediaRecorder.isTypeSupported('audio/ogg')) {
          return 'audio/ogg';
        }
        return '';
      })();

      const recorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      recorder.onstop = () => {
        const recordedChunks = [...chunksRef.current];
        cleanup();
        if (recordedChunks.length === 0) {
          setError('no_audio_data');
          return;
        }
        const blob = new Blob(recordedChunks, { type: mimeType });
        setAudioBlob(blob);
      };

      recorder.onerror = (event) => {
        const recorderError = 'error' in event ? (event as Event & { error: unknown }).error : event;
        logRecorderError(recorderError);
        cleanup();
        setError('recording_failed');
        setIsRecording(false);
      };

      const newCorrelationId = createCorrelationId();
      setCorrelationId(newCorrelationId);
      logVoiceEvent({ type: 'voice_start', correlationId: newCorrelationId });

      recorder.start(1000);
      startTimeRef.current = Date.now();
      setIsRecording(true);

      timerRef.current = window.setInterval(() => {
        setDuration(Date.now() - startTimeRef.current);
      }, DURATION_UPDATE_INTERVAL_MS);

      autoStopRef.current = window.setTimeout(() => {
        stop();
      }, MAX_DURATION_MS);
    } catch (err) {
      logRecorderError(err);
      cleanup();
      if (err instanceof DOMException && err.name === 'NotAllowedError') {
        setError('permission_denied');
      } else {
        setError('recording_failed');
      }
    }
  }, [cleanup, stop]);

  const reset = useCallback(() => {
    cleanup();
    setIsRecording(false);
    setDuration(0);
    setAudioBlob(null);
    setError(null);
    if (isRecording && correlationId) {
      logVoiceEvent({ type: 'voice_cancel', correlationId });
    }
    setCorrelationId(null);
  }, [cleanup, correlationId, isRecording]);

  useEffect(() => {
    return () => {
      cleanup();
    };
  }, [cleanup]);

  return {
    start,
    stop,
    isRecording,
    duration,
    audioBlob,
    error,
    reset,
    correlationId,
  };
}
