# Sprint S15 — Scope Detail

## Sources of Truth
- Sprint Plan: `docs/planning/pi/2026Q1-PI01/sprints/S15/sprint.md`
- Initiative: `docs/planning/initiatives/INIT-2026Q2-voice-input-web.md`
- Epic: `docs/planning/epics/EP-012/epic.md`
- ASR Proxy Contract: `docs/contracts/http/asr-proxy.openapi.yaml`
- Previous Sprint Scope: `docs/planning/pi/2026Q1-PI01/sprints/S14/scope.md`

---

## Committed Scope

### ST-1201: VoiceMicButton Component
**Points:** 2
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-012/stories/ST-1201-voice-mic-button.md`
**Workpack:** `docs/planning/workpacks/ST-1201/workpack.md` (to be created)

**What's included:**

**Component:**
- `clients/web/src/components/commands/VoiceMicButton.tsx`
- Props: state (idle | recording | processing | disabled), onClick, disabled

**Visual States:**
- **idle:** Mic icon, neutral color, clickable
- **recording:** Mic icon with pulse animation, accent color
- **processing:** Spinner icon, muted color
- **disabled:** Greyed out mic, not clickable

**Accessibility:**
- aria-label for each state ("Start recording", "Stop recording", "Processing...")
- Keyboard focusable (Tab)
- Activatable via Enter/Space

**What's NOT included:**
- Recording logic (ST-1202)
- ASR logic (ST-1203)
- Integration with CommandInput (ST-1205)

**Flags:**
- contract_impact: no
- security_sensitive: no

**DoR:** PASS
**Dependencies:** None

---

### ST-1202: Audio Recording with MediaRecorder
**Points:** 3
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-012/stories/ST-1202-audio-recording.md`
**Workpack:** `docs/planning/workpacks/ST-1202/workpack.md` (to be created)

**What's included:**

**Hook:**
- `clients/web/src/hooks/useAudioRecorder.ts`
- Interface: `{ start, stop, isRecording, duration, audioBlob, error, reset }`

**Recording Behavior:**
- `start()`: Requests microphone permission, starts recording
- `stop()`: Stops recording, produces WebM/Opus blob
- `duration`: Updates every second during recording (for timer UI)
- `audioBlob`: Available after stop()
- `error`: Permission denied, MediaRecorder not supported
- `reset()`: Clears blob and error state

**Constraints:**
- Audio format: WebM/Opus (MediaRecorder default)
- Max duration: 60 seconds (auto-stop)
- Single stream at a time

**Resource Cleanup:**
- stop() releases MediaStream tracks
- Unmount cleans up resources

**What's NOT included:**
- UI components (ST-1201, ST-1204)
- ASR upload (ST-1203)
- Format conversion (WebM is ASR-compatible)

**Flags:**
- security_sensitive: yes (microphone access)

**DoR:** PASS
**Dependencies:** None

---

### ST-1203: ASR Upload + Polling Service Hook
**Points:** 5
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-012/stories/ST-1203-asr-upload-polling.md`
**Workpack:** `docs/planning/workpacks/ST-1203/workpack.md` (to be created)

**What's included:**

**Hook:**
- `clients/web/src/hooks/useAsrTranscription.ts`
- Interface: `{ transcribe, isTranscribing, transcript, error, reset }`

**Upload Flow:**
```typescript
transcribe(audioBlob: Blob): Promise<void>
// 1. Generate Idempotency-Key (uuid)
// 2. POST /api/v1/households/{id}/asr/transcriptions
//    - Headers: Idempotency-Key, X-Correlation-ID, Content-Type: multipart/form-data
//    - Body: file=audioBlob
// 3. Receive 202 { id, status, pollAfterMs }
// 4. Start polling
```

**Polling Flow:**
```typescript
// Loop until status=done or status=failed or maxAttempts
// GET /api/v1/households/{id}/asr/transcriptions/{transcriptionId}
// Wait pollAfterMs between attempts
// Max 30 attempts (~60s with 2s intervals)
// On done: set transcript
// On failed: set error
```

**State:**
- `isTranscribing`: true during upload + polling
- `transcript`: string result on success
- `error`: string on failure (generic user-friendly message)
- `reset()`: clears state for retry

**Headers:**
```typescript
{
  'Idempotency-Key': uuid(),
  'X-Correlation-ID': correlationId,
  'Authorization': `Bearer ${token}`
}
```

**What's NOT included:**
- Recording logic (ST-1202)
- UI components (ST-1204)
- Detailed error categorization (ST-1206)

**Flags:**
- contract_impact: no (uses existing contract)

**DoR:** PASS
**Dependencies:** None (uses existing ASR proxy contract)

---

### ST-1204: VoiceRecordingStates UI
**Points:** 2
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-012/stories/ST-1204-recording-states-ui.md`
**Workpack:** `docs/planning/workpacks/ST-1204/workpack.md` (to be created)

**What's included:**

**Component:**
- `clients/web/src/components/commands/VoiceRecordingStatus.tsx`
- Props: state, duration, onCancel

**States:**
- **recording:** Timer (mm:ss) + Cancel button
- **uploading:** "Uploading..." + Spinner
- **transcribing:** "Transcribing..." + Spinner

**Timer Format:**
```typescript
formatDuration(seconds: number): string
// 0 -> "00:00"
// 75 -> "01:15"
// 3599 -> "59:59"
```

**Cancel Button:**
- Visible only in recording state
- Calls onCancel() handler
- Text: "Cancel" or X icon

**Accessibility:**
- aria-live="polite" for state announcements
- Screen reader friendly ("Recording: 1 minute 15 seconds")

**What's NOT included:**
- Recording logic (ST-1202)
- ASR logic (ST-1203)
- Error states (ST-1206)

**Flags:**
- contract_impact: no

**DoR:** PASS
**Dependencies:** None

---

### ST-1205: Integration with CommandInput
**Points:** 5
**Priority:** P1
**Status:** Ready
**Story:** `docs/planning/epics/EP-012/stories/ST-1205-command-input-integration.md`
**Workpack:** `docs/planning/workpacks/ST-1205/workpack.md` (to be created)

**What's included:**

**Modified Component:**
- `clients/web/src/components/commands/CommandInput.tsx`

**Integration Points:**
1. **Mic button placement:** Next to text input (right side)
2. **State machine:** idle -> recording -> uploading -> transcribing -> ready
3. **Hook wiring:** useAudioRecorder + useAsrTranscription
4. **Transcript handling:** Populate title input field (editable)

**User Flow:**
```
1. User sees mic button in idle state
2. Click mic -> request permission -> start recording
3. Click again -> stop recording -> start upload
4. Show uploading/transcribing states
5. On transcript -> populate input field
6. User can edit text
7. User submits (normal command flow)
```

**State Management:**
```typescript
type VoiceState =
  | 'idle'
  | 'requesting-permission'
  | 'recording'
  | 'uploading'
  | 'transcribing'
  | 'ready'
  | 'error';

// Transitions:
// idle -> recording (on mic click, permission granted)
// recording -> uploading (on stop)
// uploading -> transcribing (on upload complete)
// transcribing -> ready (on transcript received)
// ready -> idle (on submit or clear)
// any -> error (on failure)
// error -> idle (on retry/dismiss)
```

**Mic Button Behavior:**
- Disabled during: uploading, transcribing, command executing
- Click in idle: start recording
- Click in recording: stop recording

**Cancel Behavior:**
- Cancel during recording: discard audio, return to idle
- Cancel during uploading/transcribing: abort request, return to idle

**What's NOT included:**
- Detailed error handling (ST-1206)
- Telemetry events (ST-1207)
- Cross-browser polish (ST-1208)

**Flags:**
- diagrams_needed: lite (state diagram)

**DoR:** PASS
**Dependencies:** ST-1201, ST-1202, ST-1203, ST-1204

---

## Out of Scope (Explicit)

### Not in S15 Scope (deferred to S16)
- Error handling UX (permission denied dialog, ASR failure recovery)
- Client telemetry events (voice_start, voice_cancel, etc.)
- Cross-browser testing and fixes
- Accessibility hardening (ARIA improvements)

### Never in EP-012 Scope
- Wake word / hands-free activation
- Real-time transcription (streaming ASR)
- Voice notes as standalone entity
- Offline transcription
- Multiple language support

---

## Acceptance Criteria Summary

**Sprint succeeds if:**

**ST-1201 (VoiceMicButton):**
1. Renders idle state with mic icon
2. Renders recording state with pulse animation
3. Renders processing state with spinner
4. Disabled state greyed out, not clickable
5. onClick fires on click
6. Keyboard accessible (Tab, Enter/Space)

**ST-1202 (useAudioRecorder):**
7. Hook returns start/stop/isRecording/duration/audioBlob/error
8. Start requests microphone permission
9. Recording produces WebM blob
10. Duration updates during recording
11. Auto-stop at 60 seconds
12. Error on permission denied
13. Stop cleans up resources

**ST-1203 (useAsrTranscription):**
14. Hook returns transcribe/isTranscribing/transcript/error/reset
15. Upload creates transcription with required headers
16. Polling respects pollAfterMs
17. Transcript returned on done status
18. Error returned on failed status
19. Max 30 polling attempts
20. Reset clears state

**ST-1204 (VoiceRecordingStatus):**
21. Recording state shows timer and cancel button
22. Timer formats correctly (75s -> 01:15)
23. Uploading state shows spinner
24. Transcribing state shows spinner
25. Cancel button calls handler
26. Screen reader announces state changes

**ST-1205 (Integration):**
27. Mic button visible in CommandInput
28. Click mic starts recording
29. Click again stops recording
30. Transcript populates input field (editable)
31. Submit command works normally
32. Cancel discards audio and returns to idle
33. Mic disabled during command execution

**Sprint fails if:**
- Recording does not work in Chrome
- Transcript does not populate input field
- Submit fails after voice input
- Unit tests fail

---

## Readiness Notes

**All committed stories:**
- Have clear ACs with testable conditions
- Have defined test strategies
- All dependencies completed (EP-011)
- Web patterns established

**Key patterns to follow:**
- Component: existing design system patterns
- Hooks: standard React hook patterns (useState, useCallback)
- Testing: Vitest + React Testing Library + MSW
- State management: local component state (no global store needed)

**Human gates:**
- Gate B: approve committed scope (this document)
- Gate C: approve PLAN before APPLY (each story)
- Gate D: approve merge (each story)

---

## Story Dependency Graph

```
      ST-1201          ST-1202          ST-1203          ST-1204
     (button)        (recorder)       (asr hook)       (status UI)
         |               |                |                |
         +-------+-------+-------+--------+----------------+
                 |
                 v
             ST-1205
           (integration)
                 |
                 v
          Milestone M1
        (Core flow works)
```

**Parallel work possible:** ST-1201, ST-1202, ST-1203, ST-1204 have no inter-dependencies.

**Recommended execution order:**
1. ST-1201 + ST-1204 (UI components, simpler)
2. ST-1202 + ST-1203 (hooks, more complex)
3. ST-1205 (integration, needs all above)

---

## Technical Notes

### New Web Files (S15)

```
clients/web/src/
├── components/
│   └── commands/
│       ├── VoiceMicButton.tsx        # ST-1201
│       ├── VoiceMicButton.test.tsx   # ST-1201
│       ├── VoiceRecordingStatus.tsx  # ST-1204
│       ├── VoiceRecordingStatus.test.tsx # ST-1204
│       └── CommandInput.tsx          # ST-1205 (modify existing)
├── hooks/
│   ├── useAudioRecorder.ts           # ST-1202
│   ├── useAudioRecorder.test.ts      # ST-1202
│   ├── useAsrTranscription.ts        # ST-1203
│   └── useAsrTranscription.test.ts   # ST-1203
└── ...
```

### Key Interfaces

```typescript
// ST-1201
interface VoiceMicButtonProps {
  state: 'idle' | 'recording' | 'processing' | 'disabled';
  onClick: () => void;
  disabled?: boolean;
}

// ST-1202
interface UseAudioRecorderReturn {
  start: () => Promise<void>;
  stop: () => void;
  isRecording: boolean;
  duration: number; // seconds
  audioBlob: Blob | null;
  error: Error | null;
  reset: () => void;
}

// ST-1203
interface UseAsrTranscriptionReturn {
  transcribe: (audioBlob: Blob) => Promise<void>;
  isTranscribing: boolean;
  transcript: string | null;
  error: string | null;
  reset: () => void;
}

// ST-1204
interface VoiceRecordingStatusProps {
  state: 'recording' | 'uploading' | 'transcribing';
  duration?: number;
  onCancel: () => void;
}
```

### ASR Proxy Endpoints (from contract)

```yaml
# POST /api/v1/households/{householdId}/asr/transcriptions
# Request: multipart/form-data with file
# Response: 202 { id, status, pollAfterMs }

# GET /api/v1/households/{householdId}/asr/transcriptions/{transcriptionId}
# Response: { id, status, transcript?, error?, pollAfterMs? }
```
