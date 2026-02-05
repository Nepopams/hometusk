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

      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : MediaRecorder.isTypeSupported('audio/webm')
          ? 'audio/webm'
          : 'audio/ogg';

      const recorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      recorder.onstop = () => {
        cleanup();
        if (chunksRef.current.length === 0) {
          setError('no_audio_data');
          return;
        }
        const blob = new Blob(chunksRef.current, { type: mimeType });
        setAudioBlob(blob);
      };

      recorder.onerror = () => {
        cleanup();
        setError('recording_failed');
        setIsRecording(false);
      };

      const newCorrelationId = crypto.randomUUID();
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
