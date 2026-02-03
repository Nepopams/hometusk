import './VoiceMicButton.css';

export type VoiceMicButtonState = 'idle' | 'recording' | 'processing' | 'disabled';

export interface VoiceMicButtonProps {
  state: VoiceMicButtonState;
  onClick: () => void;
  disabled?: boolean;
  'aria-label'?: string;
}

export function VoiceMicButton({
  state,
  onClick,
  disabled = false,
  'aria-label': ariaLabel = 'Voice input',
}: VoiceMicButtonProps) {
  const isDisabled = disabled || state === 'disabled';

  return (
    <button
      type="button"
      className={`voice-mic-button voice-mic-button--${state}`}
      onClick={onClick}
      disabled={isDisabled}
      aria-label={ariaLabel}
      aria-pressed={state === 'recording'}
    >
      {state === 'processing' ? (
        <span className="voice-mic-button__spinner" aria-hidden="true" />
      ) : (
        <MicIcon />
      )}
    </button>
  );
}

function MicIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" aria-hidden="true">
      <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
    </svg>
  );
}
