# Workpack: ST-1206 — Voice Error Handling UX

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-012/epic.md`
- Story: `docs/planning/epics/EP-012/stories/ST-1206-error-handling-ux.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready**

---

## Goal
Replace inline voice error text with a dedicated `VoiceErrorMessage` component featuring action buttons (Try again, Type instead), user-friendly messages, and rate limit countdown.

---

## Scope

### In Scope
- New `VoiceErrorMessage.tsx` component with:
  - Error type prop (permission_denied, too_long, upload_failed, asr_failed, rate_limited, not_supported)
  - "Try again" button (restarts voice flow)
  - "Type instead" button (dismisses error, focuses text input)
  - Rate limit countdown timer
- Update `CommandInput.tsx` to use new component
- New CSS file `VoiceErrorMessage.css`
- Non-toxic, user-friendly messages (no blame)

### Out of Scope
- Backend changes (none needed)
- Telemetry events (ST-1207)
- Browser compatibility testing (ST-1208)

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `clients/web/src/components/commands/VoiceErrorMessage.tsx` | CREATE | Error message component with actions |
| `clients/web/src/components/commands/VoiceErrorMessage.css` | CREATE | Styles for error component |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY | Replace inline error with VoiceErrorMessage |
| `clients/web/src/components/commands/CommandInput.css` | MODIFY | Remove old `.command-input__voice-error` if unused |

---

## Implementation Plan

### Step 1: Define Error Types

Create type union for voice errors:

```typescript
export type VoiceErrorType =
  | 'permission_denied'
  | 'not_supported'
  | 'recording_failed'
  | 'no_audio_data'
  | 'upload_failed'
  | 'transcription_failed'
  | 'timeout'
  | 'rate_limited'
  | 'network_error'
  | 'not_authenticated';
```

### Step 2: Create VoiceErrorMessage Component

**File:** `VoiceErrorMessage.tsx`

Props:
- `errorType: VoiceErrorType`
- `onRetry: () => void` — restarts voice flow
- `onDismiss: () => void` — returns to text input
- `rateLimitResetMs?: number` — countdown for rate limit

Features:
- Icon (warning/error)
- User-friendly message (no technical jargon)
- "Try again" button (calls onRetry)
- "Type instead" button (calls onDismiss)
- Rate limit: countdown timer, disabled retry until reset

### Step 3: Message Copy (Non-toxic)

| Error Type | Message | Actions |
|------------|---------|---------|
| permission_denied | "We need microphone access for voice input. Please allow it in your browser settings." | Type instead |
| not_supported | "Voice input isn't available in this browser. Try Chrome, Firefox, or Edge." | Type instead |
| recording_failed | "Something went wrong with the recording. Want to try again?" | Try again, Type instead |
| no_audio_data | "We didn't catch any audio. Make sure your microphone is working." | Try again, Type instead |
| upload_failed | "Couldn't upload the recording. Check your connection and try again." | Try again, Type instead |
| transcription_failed | "We couldn't understand the audio. Try speaking more clearly." | Try again, Type instead |
| timeout | "The transcription took too long. Please try a shorter message." | Try again, Type instead |
| rate_limited | "Too many requests. You can try again in {countdown}." | (countdown), Type instead |
| network_error | "Network issue. Check your connection and try again." | Try again, Type instead |
| not_authenticated | "Please sign in to use voice input." | Type instead |

### Step 4: Rate Limit Countdown

For `rate_limited` error:
- Accept `rateLimitResetMs` prop (time until reset)
- Display countdown: "You can try again in 30s"
- Update every second
- Enable "Try again" when countdown reaches 0

### Step 5: CSS Styling

**File:** `VoiceErrorMessage.css`

- Error container with subtle red/orange background
- Warning icon
- Message text
- Button row with flex gap
- Primary: "Try again" (brand color)
- Secondary: "Type instead" (ghost style)
- Countdown text styled as muted

### Step 6: Integrate in CommandInput

**File:** `CommandInput.tsx`

Replace:
```tsx
{voiceErrorMessage && (
  <div className="command-input__voice-error" role="alert">
    {voiceErrorMessage}
  </div>
)}
```

With:
```tsx
{voiceError && (
  <VoiceErrorMessage
    errorType={voiceError.type}
    rateLimitResetMs={voiceError.rateLimitResetMs}
    onRetry={handleVoiceRetry}
    onDismiss={handleVoiceDismiss}
  />
)}
```

Add handlers:
- `handleVoiceRetry` — calls `resetVoiceFlow()` then starts recording
- `handleVoiceDismiss` — calls `resetVoiceFlow()` and focuses text input

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Type check
npm run build

# Lint
npm run lint

# Dev server (manual testing)
npm run dev
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Permission denied shows message + "Type instead" | Manual test |
| AC-2 | Audio too long shows message + "Try again" | Manual test |
| AC-3 | Upload failure shows both buttons | Manual test |
| AC-4 | ASR failure shows both buttons | Manual test |
| AC-5 | Rate limit shows countdown | Manual test |
| AC-6 | "Try again" restarts voice flow | Manual test |
| AC-7 | "Type instead" returns to text input | Manual test |
| AC-8 | Non-toxic messaging | Code review |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Rate limit timing mismatch | Low | Backend returns resetMs, frontend counts down |
| Countdown flicker | Low | Use setInterval cleanup properly |

---

## Rollback

- Revert frontend changes
- No backend changes to rollback

---

## References

- Current error handling: `CommandInput.tsx:98-128`
- Recording errors: `useAudioRecorder.ts`
- ASR errors: `useAsrTranscription.ts`
- Patterns: `VoiceMicButton.tsx`, `VoiceRecordingStatus.tsx`
