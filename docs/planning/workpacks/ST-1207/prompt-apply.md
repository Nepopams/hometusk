# Codex APPLY: ST-1207 — Client Telemetry Events

## Context
Add client-side telemetry for voice input flow. Events logged to console + localStorage.

## Files to Create/Modify

| File | Action |
|------|--------|
| `clients/web/src/lib/voiceTelemetry.ts` | CREATE |
| `clients/web/src/hooks/useAudioRecorder.ts` | MODIFY |
| `clients/web/src/hooks/useAsrTranscription.ts` | MODIFY |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY |
| `clients/web/src/components/commands/CreateTaskForm.tsx` | MODIFY |

---

## Step 1: Create voiceTelemetry.ts

**File:** `clients/web/src/lib/voiceTelemetry.ts`

Follow `commandHistory.ts` pattern:

```typescript
const STORAGE_KEY = 'hometusk:voiceTelemetry';
const MAX_EVENTS = 100;

export type VoiceEventType =
  | 'voice_start'
  | 'voice_cancel'
  | 'voice_upload_ok'
  | 'voice_upload_fail'
  | 'voice_asr_ok'
  | 'voice_asr_fail'
  | 'voice_transcript_edited'
  | 'voice_command_submitted';

export interface VoiceEvent {
  type: VoiceEventType;
  timestamp: number;
  correlationId?: string;
  durationMs?: number;
  errorType?: string;
}

export function logVoiceEvent(
  event: Omit<VoiceEvent, 'timestamp'>
): void {
  const fullEvent: VoiceEvent = {
    ...event,
    timestamp: Date.now(),
  };

  // Console log for debugging
  console.log('[VoiceTelemetry]', fullEvent.type, fullEvent);

  // Persist to localStorage
  try {
    const existing = getVoiceEvents();
    const updated = [...existing, fullEvent].slice(-MAX_EVENTS);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  } catch {
    // Ignore storage errors
  }
}

export function getVoiceEvents(): VoiceEvent[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as VoiceEvent[];
  } catch {
    return [];
  }
}

export function clearVoiceEvents(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore
  }
}
```

---

## Step 2: Modify useAudioRecorder.ts

**File:** `clients/web/src/hooks/useAudioRecorder.ts`

Import telemetry:
```typescript
import { logVoiceEvent } from '../lib/voiceTelemetry';
```

Add correlationId state + return it:
```typescript
const [correlationId, setCorrelationId] = useState<string | null>(null);
```

In `start` function (around line 59), after successful MediaRecorder setup:
```typescript
const newCorrelationId = crypto.randomUUID();
setCorrelationId(newCorrelationId);
logVoiceEvent({ type: 'voice_start', correlationId: newCorrelationId });
```

In `reset` function (around line 126), if recording was active:
```typescript
if (correlationId) {
  logVoiceEvent({ type: 'voice_cancel', correlationId });
}
setCorrelationId(null);
```

Update return to include `correlationId`:
```typescript
return {
  start,
  stop,
  duration,
  audioBlob,
  error,
  reset,
  correlationId,  // ADD
};
```

---

## Step 3: Modify useAsrTranscription.ts

**File:** `clients/web/src/hooks/useAsrTranscription.ts`

Import telemetry:
```typescript
import { logVoiceEvent } from '../lib/voiceTelemetry';
```

Add correlationId tracking:
```typescript
const correlationIdRef = useRef<string | null>(null);
const startTimeRef = useRef<number>(0);
```

Modify `transcribe` function signature to accept correlationId:
```typescript
const transcribe = useCallback(async (
  audioBlob: Blob,
  householdId: string,
  correlationId?: string
) => {
  correlationIdRef.current = correlationId || null;
  startTimeRef.current = Date.now();
  // ... existing code
}, []);
```

In `uploadAudio` (around line 55), on success:
```typescript
logVoiceEvent({
  type: 'voice_upload_ok',
  correlationId: correlationIdRef.current || undefined,
});
```

In `uploadAudio`, on error (429 or other):
```typescript
logVoiceEvent({
  type: 'voice_upload_fail',
  correlationId: correlationIdRef.current || undefined,
  errorType: response.status === 429 ? 'rate_limited' : 'upload_failed',
});
```

In `pollTranscription` (around line 170), on success:
```typescript
logVoiceEvent({
  type: 'voice_asr_ok',
  correlationId: correlationIdRef.current || undefined,
  durationMs: Date.now() - startTimeRef.current,
});
```

In `pollTranscription`, on failure:
```typescript
logVoiceEvent({
  type: 'voice_asr_fail',
  correlationId: correlationIdRef.current || undefined,
  errorType: data.status === 'failed' ? 'transcription_failed' : 'timeout',
  durationMs: Date.now() - startTimeRef.current,
});
```

Update UseAsrTranscriptionResult interface — transcribe now takes optional correlationId:
```typescript
transcribe: (audioBlob: Blob, householdId: string, correlationId?: string) => Promise<void>;
```

---

## Step 4: Modify CreateTaskForm.tsx

**File:** `clients/web/src/components/commands/CreateTaskForm.tsx`

Add callback prop for title change:
```typescript
interface CreateTaskFormProps {
  householdId: string;
  onSubmit: (payload: CreateTaskPayload) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
  initialTitle?: string;
  onTitleChange?: (title: string, wasEdited: boolean) => void;  // ADD
}
```

Track if edited (around line 30-33 where initialTitle effect exists):
```typescript
const [wasEdited, setWasEdited] = useState(false);
const initialTitleRef = useRef(initialTitle);

useEffect(() => {
  if (initialTitle && initialTitle !== initialTitleRef.current) {
    setTitle(initialTitle);
    initialTitleRef.current = initialTitle;
    setWasEdited(false);
  }
}, [initialTitle]);
```

On title input change:
```typescript
const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  const newTitle = e.target.value;
  setTitle(newTitle);
  if (initialTitleRef.current && newTitle !== initialTitleRef.current) {
    setWasEdited(true);
  }
  onTitleChange?.(newTitle, wasEdited);
};
```

Pass `wasEdited` on submit:
```typescript
// Before calling onSubmit, call onTitleChange one more time
onTitleChange?.(title, wasEdited);
```

---

## Step 5: Modify CommandInput.tsx

**File:** `clients/web/src/components/commands/CommandInput.tsx`

Import telemetry:
```typescript
import { logVoiceEvent } from '../../lib/voiceTelemetry';
```

Get correlationId from useAudioRecorder:
```typescript
const {
  start: startRecording,
  stop: stopRecording,
  duration: recordingDuration,
  audioBlob,
  error: recordingError,
  reset: resetRecording,
  correlationId: voiceCorrelationId,  // ADD
} = useAudioRecorder();
```

Pass correlationId to transcribe (in useEffect around line 61-79):
```typescript
await transcribe(audioBlob, householdId, voiceCorrelationId || undefined);
```

Track if voice was used + transcript edited:
```typescript
const [voiceWasUsed, setVoiceWasUsed] = useState(false);
const [transcriptWasEdited, setTranscriptWasEdited] = useState(false);
```

In handleMicClick, set voiceWasUsed:
```typescript
setVoiceWasUsed(true);
```

In resetVoiceFlow:
```typescript
setVoiceWasUsed(false);
setTranscriptWasEdited(false);
```

Add handler for title change from CreateTaskForm:
```typescript
const handleTitleChange = (title: string, wasEdited: boolean) => {
  if (voiceWasUsed && wasEdited && !transcriptWasEdited) {
    setTranscriptWasEdited(true);
    logVoiceEvent({
      type: 'voice_transcript_edited',
      correlationId: voiceCorrelationId || undefined,
    });
  }
};
```

In handleCreateTask, log submit if voice was used:
```typescript
const handleCreateTask = async (payload: CreateTaskPayload) => {
  if (voiceWasUsed) {
    logVoiceEvent({
      type: 'voice_command_submitted',
      correlationId: voiceCorrelationId || undefined,
    });
  }
  // ... existing code
};
```

Pass onTitleChange to CreateTaskForm:
```typescript
<CreateTaskForm
  key={`create-${formKey}`}
  householdId={householdId}
  onSubmit={handleCreateTask}
  onCancel={handleCancel}
  isLoading={isLoading}
  initialTitle={voiceTranscript || undefined}
  onTitleChange={handleTitleChange}  // ADD
/>
```

---

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web
npm run build
npm run lint
```

Manual test:
1. Open DevTools Console
2. Record voice → see `[VoiceTelemetry] voice_start`
3. Cancel → see `voice_cancel`
4. Complete flow → see upload/asr events
5. Edit transcript → see `voice_transcript_edited`
6. Submit → see `voice_command_submitted`
7. Check localStorage: `localStorage.getItem('hometusk:voiceTelemetry')`

---

## Acceptance Criteria Checklist

- [ ] AC-1: voice_start logged when recording starts
- [ ] AC-2: voice_cancel logged when cancelled
- [ ] AC-3: upload events logged with correlationId
- [ ] AC-4: ASR events logged with duration
- [ ] AC-5: voice_transcript_edited logged if modified
- [ ] AC-6: voice_command_submitted logged on submit
- [ ] AC-7: Events stored in localStorage (max 100)
- [ ] AC-8: No PII in events (review: no transcript text, no user IDs)

---

## Constraints
- No PII: do NOT log transcript text, user IDs, or household IDs
- Max 100 events in localStorage (FIFO)
- Follow existing code style (commandHistory.ts pattern)
