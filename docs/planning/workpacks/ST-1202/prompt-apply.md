# Codex APPLY Prompt: ST-1202 — Audio Recording Hook

## Mission
You are in **APPLY mode**. Create the `useAudioRecorder` hook based on PLAN findings.

---

## Context from PLAN Findings

### Confirmed Structure
- **Hooks folder**: `clients/web/src/hooks/` (EXISTS)
- **Barrel export**: `clients/web/src/hooks/index.ts` (EXISTS)
- **Pattern**: `export function useX()` (named export, not default)
- **React import**: `import { useState, useEffect, ... } from 'react';`
- **Type export**: `export type` inline

### No Blockers
No existing audio/media recording hooks.

---

## Files to Create/Modify

### 1. CREATE: `clients/web/src/hooks/useAudioRecorder.ts`

```typescript
import { useState, useRef, useCallback, useEffect } from 'react';

const MAX_DURATION_MS = 60_000; // 60 seconds
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
  duration: number; // milliseconds
  audioBlob: Blob | null;
  error: AudioRecorderError | null;
  reset: () => void;
}

export function useAudioRecorder(): UseAudioRecorderResult {
  const [isRecording, setIsRecording] = useState(false);
  const [duration, setDuration] = useState(0);
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [error, setError] = useState<AudioRecorderError | null>(null);

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
  }, [cleanup]);

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
  };
}
```

### 2. MODIFY: `clients/web/src/hooks/index.ts`

Add exports:

```typescript
export { useAudioRecorder } from './useAudioRecorder';
export type { UseAudioRecorderResult, AudioRecorderError } from './useAudioRecorder';
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
| AC-1: Returns { start, stop, isRecording, duration, audioBlob, error } | Return object with all fields + reset |
| AC-2: Start requests microphone | `getUserMedia({ audio: true })` |
| AC-3: Produces WebM blob | `new Blob(chunks, { type: mimeType })` |
| AC-4: Duration updates | setInterval every 100ms |
| AC-5: Auto-stop at 60s | `setTimeout(stop, MAX_DURATION_MS)` |
| AC-6: Error on permission denied | catch NotAllowedError → 'permission_denied' |
| AC-7: Stop cleans up | cleanup() stops tracks, clears timers |

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
