import './VoiceRecordingStatus.css';

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

const getStateLabel = (state: VoiceRecordingState): string => {
  switch (state) {
    case 'recording':
      return 'Recording...';
    case 'uploading':
      return 'Uploading...';
    case 'transcribing':
      return 'Transcribing...';
  }
};

export function VoiceRecordingStatus({
  state,
  durationMs,
  onCancel,
}: VoiceRecordingStatusProps) {
  const stateLabel = getStateLabel(state);

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
              aria-label={`Recording time: ${formatDuration(durationMs)}`}
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
        aria-label="Cancel voice input"
      >
        Cancel
      </button>
      <span className="sr-only" aria-live="assertive">
        {stateLabel}
      </span>
    </div>
  );
}
