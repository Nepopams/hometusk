# Codex APPLY Prompt: ST-1206 — Voice Error Handling UX

## Directive
**IMPLEMENTATION.** Create and modify files as specified. Follow patterns from PLAN findings exactly.

---

## Story Context
ST-1206 adds a `VoiceErrorMessage` component with action buttons (Try again, Type instead) and user-friendly messages to replace inline error text in voice input flow.

**Note from PLAN:** No `rateLimitResetMs` exists in ASR hook. For rate_limited errors, show static message "Please wait a moment before trying again" without countdown.

---

## Files to Create

### 1. `clients/web/src/components/commands/VoiceErrorMessage.tsx`

```typescript
import './VoiceErrorMessage.css';

// Union of all voice error types from hooks
export type VoiceErrorType =
  // From useAudioRecorder
  | 'permission_denied'
  | 'not_supported'
  | 'recording_failed'
  | 'no_audio_data'
  // From useAsrTranscription
  | 'upload_failed'
  | 'transcription_failed'
  | 'timeout'
  | 'rate_limited'
  | 'network_error'
  | 'not_authenticated';

export interface VoiceErrorMessageProps {
  errorType: VoiceErrorType;
  onRetry: () => void;
  onDismiss: () => void;
}

const ERROR_CONFIG: Record<VoiceErrorType, { message: string; showRetry: boolean }> = {
  permission_denied: {
    message: 'We need microphone access for voice input. Please allow it in your browser settings.',
    showRetry: false,
  },
  not_supported: {
    message: "Voice input isn't available in this browser. Try Chrome, Firefox, or Edge.",
    showRetry: false,
  },
  recording_failed: {
    message: 'Something went wrong with the recording. Want to try again?',
    showRetry: true,
  },
  no_audio_data: {
    message: "We didn't catch any audio. Make sure your microphone is working.",
    showRetry: true,
  },
  upload_failed: {
    message: "Couldn't upload the recording. Check your connection and try again.",
    showRetry: true,
  },
  transcription_failed: {
    message: "We couldn't understand the audio. Try speaking more clearly.",
    showRetry: true,
  },
  timeout: {
    message: 'The transcription took too long. Please try a shorter message.',
    showRetry: true,
  },
  rate_limited: {
    message: 'Too many requests. Please wait a moment before trying again.',
    showRetry: false,
  },
  network_error: {
    message: 'Network issue. Check your connection and try again.',
    showRetry: true,
  },
  not_authenticated: {
    message: 'Please sign in to use voice input.',
    showRetry: false,
  },
};

export function VoiceErrorMessage({
  errorType,
  onRetry,
  onDismiss,
}: VoiceErrorMessageProps) {
  const config = ERROR_CONFIG[errorType] ?? {
    message: 'Something went wrong. Please try again.',
    showRetry: true,
  };

  return (
    <div className="voice-error-message" role="alert" aria-live="assertive">
      <div className="voice-error-message__content">
        <WarningIcon />
        <p className="voice-error-message__text">{config.message}</p>
      </div>
      <div className="voice-error-message__actions">
        {config.showRetry && (
          <button
            type="button"
            className="button voice-error-message__retry"
            onClick={onRetry}
          >
            Try again
          </button>
        )}
        <button
          type="button"
          className="ghost-button voice-error-message__dismiss"
          onClick={onDismiss}
        >
          Type instead
        </button>
      </div>
    </div>
  );
}

function WarningIcon() {
  return (
    <svg
      className="voice-error-message__icon"
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      aria-hidden="true"
    >
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
      <line x1="12" y1="9" x2="12" y2="13" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
  );
}
```

### 2. `clients/web/src/components/commands/VoiceErrorMessage.css`

```css
/**
 * Voice Error Message component styles
 * Uses project tokens from styles/tokens.css
 */

.voice-error-message {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--color-error-soft);
  border: 1px solid var(--color-error);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-3);
}

.voice-error-message__content {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-2);
}

.voice-error-message__icon {
  flex-shrink: 0;
  color: var(--color-error);
  margin-top: 2px;
}

.voice-error-message__text {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-error-text);
  line-height: 1.4;
}

.voice-error-message__actions {
  display: flex;
  gap: var(--spacing-2);
  margin-left: 28px; /* align with text (icon width + gap) */
}

.voice-error-message__retry,
.voice-error-message__dismiss {
  font-size: var(--font-size-sm);
  padding: var(--spacing-1) var(--spacing-3);
}

/* Responsive: stack buttons on narrow screens */
@media (max-width: 480px) {
  .voice-error-message__actions {
    flex-direction: column;
    margin-left: 0;
  }

  .voice-error-message__retry,
  .voice-error-message__dismiss {
    width: 100%;
  }
}
```

---

## Files to Modify

### 3. `clients/web/src/components/commands/CommandInput.tsx`

**Add import** (after other voice imports ~line 9-10):
```typescript
import { VoiceErrorMessage, VoiceErrorType } from './VoiceErrorMessage';
```

**Replace voiceErrorMessage computation** (lines ~98-128).

Remove this entire block:
```typescript
const voiceErrorMessage = (() => {
  if (recordingError) {
    switch (recordingError) {
      // ... all cases
    }
  }
  if (asrError) {
    return (
      asrError.message ||
      // ... all mappings
    );
  }
  return null;
})();
```

Replace with:
```typescript
// Derive error type for VoiceErrorMessage component
const voiceErrorType: VoiceErrorType | null = (() => {
  if (recordingError) {
    return recordingError as VoiceErrorType;
  }
  if (asrError) {
    return asrError.type as VoiceErrorType;
  }
  return null;
})();
```

**Add handlers** (after `handleVoiceCancel`, around line 213):
```typescript
const handleVoiceRetry = () => {
  resetVoiceFlow();
  // Small delay to ensure state is cleared before restarting
  requestAnimationFrame(() => {
    handleMicClick();
  });
};

const handleVoiceDismiss = () => {
  resetVoiceFlow();
  // Focus the text input
  requestAnimationFrame(() => {
    const input = containerRef.current?.querySelector<HTMLInputElement>(
      'input[type="text"], textarea'
    );
    input?.focus();
  });
};
```

**Replace error display** (lines ~266-270).

Remove:
```tsx
{voiceErrorMessage && (
  <div className="command-input__voice-error" role="alert">
    {voiceErrorMessage}
  </div>
)}
```

Replace with:
```tsx
{voiceErrorType && (
  <VoiceErrorMessage
    errorType={voiceErrorType}
    onRetry={handleVoiceRetry}
    onDismiss={handleVoiceDismiss}
  />
)}
```

### 4. `clients/web/src/components/commands/CommandInput.css`

**Remove** the `.command-input__voice-error` class if it exists (no longer used).

If it doesn't exist as a separate class, no changes needed.

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/clients/web

# Type check and build
npm run build

# Lint
npm run lint

# Dev server for manual testing
npm run dev
```

---

## Acceptance Criteria Checklist

| AC | Test |
|----|------|
| AC-1 | Deny microphone permission → shows message + only "Type instead" |
| AC-2 | Record very long audio (if supported) → shows retry message |
| AC-3 | Disconnect network during upload → shows both buttons |
| AC-4 | ASR returns error → shows both buttons |
| AC-5 | Trigger rate limit → shows message (no countdown, static) |
| AC-6 | Click "Try again" → starts recording again |
| AC-7 | Click "Type instead" → error dismisses, text input focused |
| AC-8 | Review messages → no blame language |

---

## Constraints

- **DO NOT** modify hooks (useAudioRecorder, useAsrTranscription)
- **DO NOT** add countdown (rateLimitResetMs doesn't exist)
- **USE** existing button classes: `button`, `ghost-button`
- **USE** tokens from `styles/tokens.css`
- Keep error types as string union (no enum)

---

## Stop Condition

After implementation:
1. Run `npm run build` — must pass
2. Run `npm run lint` — report any errors
3. Report files changed and any issues
