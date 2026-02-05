import { useEffect, useState } from 'react';
import './VoiceErrorMessage.css';

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

export interface VoiceErrorMessageProps {
  errorType: VoiceErrorType;
  onRetry: () => void;
  onDismiss: () => void;
  rateLimitResetMs?: number;
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
    message: 'Too many requests.',
    showRetry: true,
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
  rateLimitResetMs,
}: VoiceErrorMessageProps) {
  const config = ERROR_CONFIG[errorType] ?? {
    message: 'Something went wrong. Please try again.',
    showRetry: true,
  };

  const [countdown, setCountdown] = useState<number | null>(null);

  useEffect(() => {
    if (errorType !== 'rate_limited' || !rateLimitResetMs) {
      setCountdown(null);
      return;
    }

    const endTime = Date.now() + rateLimitResetMs;
    let intervalId: number | null = null;

    const tick = () => {
      const remaining = Math.max(0, Math.ceil((endTime - Date.now()) / 1000));
      setCountdown(remaining);
      if (remaining <= 0 && intervalId !== null) {
        clearInterval(intervalId);
      }
    };

    tick();
    intervalId = window.setInterval(tick, 1000);

    return () => {
      if (intervalId !== null) {
        clearInterval(intervalId);
      }
    };
  }, [errorType, rateLimitResetMs]);

  const displayMessage =
    errorType === 'rate_limited' && countdown !== null && countdown > 0
      ? `Too many requests. You can try again in ${countdown}s.`
      : config.message;

  return (
    <div className="voice-error-message" role="alert" aria-live="assertive">
      <div className="voice-error-message__content">
        <WarningIcon />
        <p className="voice-error-message__text">{displayMessage}</p>
      </div>
      <div className="voice-error-message__actions">
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
