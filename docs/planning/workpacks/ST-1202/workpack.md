# Workpack: ST-1202 — Audio Recording with MediaRecorder

## Sources of Truth
- Product goal: `docs/planning/strategy/product-goal.md`
- Scope anchor: `docs/planning/epics/EP-012/epic.md`
- Story spec: `docs/planning/epics/EP-012/stories/ST-1202-audio-recording.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status: Ready

## Outcome
A React hook `useAudioRecorder` that handles browser audio recording via MediaRecorder API, producing WebM/Opus blobs compatible with the ASR proxy.

## Acceptance Criteria (from story)
- AC-1: Hook returns { start, stop, isRecording, duration, audioBlob, error }
- AC-2: Start requests microphone permission
- AC-3: Recording produces WebM blob
- AC-4: Duration updates during recording
- AC-5: Auto-stop at 60 seconds
- AC-6: Error on permission denied
- AC-7: Stop cleans up resources

---

## Files to Change

### New Files
| Path | Purpose |
|------|---------|
| `clients/web/src/hooks/useAudioRecorder.ts` | React hook for audio recording |

### Modified Files
| Path | Change |
|------|--------|
| `clients/web/src/hooks/index.ts` | Export useAudioRecorder (if exists) |

---

## Implementation Plan

### Commit 1: useAudioRecorder hook

**Step 1: Create useAudioRecorder.ts**

```typescript
// clients/web/src/hooks/useAudioRecorder.ts
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
    // Stop timer
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    // Stop auto-stop timeout
    if (autoStopRef.current) {
      clearTimeout(autoStopRef.current);
      autoStopRef.current = null;
    }
    // Stop media stream
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
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
    // Reset state
    setError(null);
    setAudioBlob(null);
    setDuration(0);
    chunksRef.current = [];

    // Check support
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      setError('not_supported');
      return;
    }

    try {
      // Request microphone permission
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      // Determine MIME type (prefer WebM/Opus)
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

      // Start recording
      recorder.start(1000); // Collect data every second
      startTimeRef.current = Date.now();
      setIsRecording(true);

      // Duration timer
      timerRef.current = window.setInterval(() => {
        setDuration(Date.now() - startTimeRef.current);
      }, DURATION_UPDATE_INTERVAL_MS);

      // Auto-stop at max duration
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

  // Cleanup on unmount
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

**Step 2: Export from hooks index (if exists)**

If `clients/web/src/hooks/index.ts` exists, add:
```typescript
export { useAudioRecorder } from './useAudioRecorder';
export type { UseAudioRecorderResult, AudioRecorderError } from './useAudioRecorder';
```

---

## Verification Commands

```bash
# Build check
cd clients/web && npm run build

# Type check
cd clients/web && npx tsc --noEmit

# Lint
cd clients/web && npm run lint 2>/dev/null || echo "No lint script"
```

---

## Tests

### Manual Testing (no test infra)
1. Open app in Chrome
2. Click mic button (after ST-1205 integration)
3. Verify:
   - Permission dialog appears
   - Recording indicator shows
   - Duration updates
   - Auto-stops at 60s
   - Blob created on stop

### Test Scenarios
| Scenario | Expected |
|----------|----------|
| Permission granted | Recording starts, blob produced |
| Permission denied | error = 'permission_denied' |
| User stops early | Blob produced with partial audio |
| 60s reached | Auto-stop, blob produced |
| MediaRecorder not supported | error = 'not_supported' |

---

## DoD Checklist Reference
See `checklist.md` for full DoD verification.

---

## Risks

| Risk | Mitigation |
|------|------------|
| Safari MIME type differences | Fallback chain: webm/opus → webm → ogg |
| Memory leak on unmount | useEffect cleanup + cleanup callback |
| Timer drift | Use Date.now() diff, not increment |

---

## Rollback
Hook is additive. Rollback = delete file, remove export.

---

## Dependencies
- None

## Blocked By
- None

## Blocks
- ST-1203 (ASR upload hook)
- ST-1205 (CommandInput integration)
