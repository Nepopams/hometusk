import './VoiceRecordingStatus.css';
import { useI18n, type TranslationKey } from '../../i18n';

export type VoiceRecordingState = 'recording' | 'uploading' | 'transcribing';

export interface VoiceRecordingStatusProps {
  state: VoiceRecordingState;
  durationMs: number;
  onCancel: () => void;
}

const formatDuration = (ms: number): string => {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
};

const getStateLabelKey = (state: VoiceRecordingState): TranslationKey => {
  switch (state) {
    case 'recording':
      return 'voice.recording';
    case 'uploading':
      return 'voice.uploading';
    case 'transcribing':
      return 'voice.transcribing';
  }
};

export function VoiceRecordingStatus({
  state,
  durationMs,
  onCancel,
}: VoiceRecordingStatusProps) {
  const { t } = useI18n();
  const stateLabel = t(getStateLabelKey(state));

  return (
    <div className="voice-recording-status" role="status" aria-live="polite">
      <div className="voice-recording-status__content">
        {state === 'recording' ? (
          <>
            <span
              className="voice-recording-status__indicator voice-recording-status__indicator--recording"
              aria-hidden="true"
            />
            <span
              className="voice-recording-status__timer"
              aria-label={t('voice.recordingTime', { time: formatDuration(durationMs) })}
            >
              {formatDuration(durationMs)}
            </span>
          </>
        ) : (
          <>
            <span className="voice-recording-status__spinner" aria-hidden="true" />
            <span className="voice-recording-status__label">{stateLabel}</span>
          </>
        )}
      </div>
      <button
        type="button"
        className="voice-recording-status__cancel"
        onClick={onCancel}
        aria-label={t('voice.cancelInput')}
      >
        {t('voice.cancel')}
      </button>
      <span className="sr-only" aria-live="assertive">
        {stateLabel}
      </span>
    </div>
  );
}
