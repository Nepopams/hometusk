import { useEffect, useState } from 'react';
import { useI18n, type TranslationKey } from '../../i18n';
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
  | 'unsupported_media'
  | 'network_error'
  | 'not_authenticated';

export interface VoiceErrorMessageProps {
  errorType: VoiceErrorType;
  onRetry: () => void;
  onDismiss: () => void;
  rateLimitResetMs?: number;
}

const ERROR_CONFIG: Record<VoiceErrorType, { messageKey: TranslationKey; showRetry: boolean }> = {
  permission_denied: {
    messageKey: 'voice.permissionDenied',
    showRetry: false,
  },
  not_supported: {
    messageKey: 'voice.notSupported',
    showRetry: false,
  },
  recording_failed: {
    messageKey: 'voice.recordingFailed',
    showRetry: true,
  },
  no_audio_data: {
    messageKey: 'voice.noAudio',
    showRetry: true,
  },
  upload_failed: {
    messageKey: 'voice.uploadFailed',
    showRetry: true,
  },
  transcription_failed: {
    messageKey: 'voice.transcriptionFailed',
    showRetry: true,
  },
  timeout: {
    messageKey: 'voice.timeout',
    showRetry: true,
  },
  rate_limited: {
    messageKey: 'voice.rateLimited',
    showRetry: true,
  },
  unsupported_media: {
    messageKey: 'voice.unsupportedMedia',
    showRetry: false,
  },
  network_error: {
    messageKey: 'voice.network',
    showRetry: true,
  },
  not_authenticated: {
    messageKey: 'voice.notAuthenticated',
    showRetry: false,
  },
};

export function VoiceErrorMessage({
  errorType,
  onRetry,
  onDismiss,
  rateLimitResetMs,
}: VoiceErrorMessageProps) {
  const { t } = useI18n();
  const config = ERROR_CONFIG[errorType] ?? {
    messageKey: 'common.somethingWentWrong',
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
      ? t('voice.rateLimitedCountdown', { count: countdown })
      : t(config.messageKey);

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
            {t('common.tryAgain')}
          </button>
        )}
        <button
          type="button"
          className="ghost-button voice-error-message__dismiss"
          onClick={onDismiss}
        >
          {t('voice.typeInstead')}
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
