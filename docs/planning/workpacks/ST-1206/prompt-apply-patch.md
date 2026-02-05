# Codex APPLY Patch: ST-1206 — Rate Limit Countdown

## Context
Review выявил, что AC-5 (rate limit countdown) не реализован. Нужно добавить countdown timer в VoiceErrorMessage.

## Task
Add rate limit countdown timer to VoiceErrorMessage component.

## Files to Modify

| File | Action |
|------|--------|
| `clients/web/src/hooks/useAsrTranscription.ts` | MODIFY — parse Retry-After header, add retryAfterMs to error |
| `clients/web/src/components/commands/VoiceErrorMessage.tsx` | MODIFY — add rateLimitResetMs prop + countdown logic |
| `clients/web/src/components/commands/CommandInput.tsx` | MODIFY — pass rateLimitResetMs from asrError |

## Implementation Steps

### Step 0: Add retryAfterMs to AsrTranscriptionError

In `clients/web/src/hooks/useAsrTranscription.ts`:

1. Update interface:
```typescript
export interface AsrTranscriptionError {
  type: AsrErrorType;
  code?: string;
  message?: string;
  retryAfterMs?: number;  // ADD THIS
}
```

2. Parse Retry-After header on 429 response (in uploadAudio function, around line 80):
```typescript
if (response.status === 429) {
  const retryAfterHeader = response.headers.get('Retry-After');
  const retryAfterMs = retryAfterHeader
    ? parseInt(retryAfterHeader, 10) * 1000
    : 60000; // default 60s
  setError({
    type: 'rate_limited',
    code: errorData.code,
    message: errorData.message || 'Rate limit exceeded',
    retryAfterMs,
  });
}
```

3. Same for pollTranscription function (around line 138):
```typescript
if (response.status === 429) {
  const retryAfterHeader = response.headers.get('Retry-After');
  const retryAfterMs = retryAfterHeader
    ? parseInt(retryAfterHeader, 10) * 1000
    : 60000;
  setError({
    type: 'rate_limited',
    code: errorData.code,
    message: errorData.message,
    retryAfterMs,
  });
}
```

### Step 1: Update VoiceErrorMessage props

In `VoiceErrorMessage.tsx`, add optional prop:

```typescript
export interface VoiceErrorMessageProps {
  errorType: VoiceErrorType;
  onRetry: () => void;
  onDismiss: () => void;
  rateLimitResetMs?: number;  // ADD THIS
}
```

### Step 2: Add countdown state and effect

Inside `VoiceErrorMessage` function, add:

```typescript
import { useState, useEffect } from 'react';

// Inside component:
const [countdown, setCountdown] = useState<number | null>(null);

useEffect(() => {
  if (errorType !== 'rate_limited' || !rateLimitResetMs) {
    setCountdown(null);
    return;
  }

  const endTime = Date.now() + rateLimitResetMs;

  const tick = () => {
    const remaining = Math.max(0, Math.ceil((endTime - Date.now()) / 1000));
    setCountdown(remaining);
    if (remaining <= 0) {
      clearInterval(intervalId);
    }
  };

  tick(); // Initial
  const intervalId = setInterval(tick, 1000);

  return () => clearInterval(intervalId);
}, [errorType, rateLimitResetMs]);
```

### Step 3: Update rate_limited config

Change `showRetry` logic for rate_limited:

```typescript
// In ERROR_CONFIG:
rate_limited: {
  message: 'Too many requests.',
  showRetry: true,  // CHANGE from false to true
},
```

### Step 4: Update message rendering

Replace the static message with dynamic countdown:

```tsx
const displayMessage = (() => {
  if (errorType === 'rate_limited' && countdown !== null && countdown > 0) {
    return `Too many requests. You can try again in ${countdown}s.`;
  }
  return config.message;
})();

// In JSX:
<p className="voice-error-message__text">{displayMessage}</p>
```

### Step 5: Disable retry button during countdown

```tsx
{config.showRetry && (
  <button
    type="button"
    className="button voice-error-message__retry"
    onClick={onRetry}
    disabled={errorType === 'rate_limited' && countdown !== null && countdown > 0}
  >
    Try again
  </button>
)}
```

### Step 6: Update CommandInput to pass rateLimitResetMs

In `CommandInput.tsx`, update VoiceErrorMessage usage (around line 262-268):

```tsx
{voiceErrorType && (
  <VoiceErrorMessage
    errorType={voiceErrorType}
    onRetry={handleVoiceRetry}
    onDismiss={handleVoiceDismiss}
    rateLimitResetMs={asrError?.retryAfterMs}
  />
)}
```

The `asrError?.retryAfterMs` field was added in Step 0.

## Verification

```bash
cd /home/vad/Документы/hometusk/clients/web
npm run build
```

## Acceptance Criteria
- AC-5: Rate limit shows countdown ("You can try again in Ns")
- "Try again" button disabled until countdown reaches 0
- Countdown updates every second
- When countdown = 0, "Try again" button becomes enabled

## Constraints
- Only modify files listed above (useAsrTranscription.ts, VoiceErrorMessage.tsx, CommandInput.tsx)
- DO NOT add new dependencies
- Keep existing error types and messages unchanged (except rate_limited)
- Use standard React hooks (useState, useEffect) — already imported
