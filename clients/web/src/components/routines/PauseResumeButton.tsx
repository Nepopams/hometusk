import { useState } from 'react';
import type { Routine } from '../../types/api';
import { Button } from '../ui';
import Modal from '../ui/Modal';
import './PauseResumeButton.css';

interface Props {
  routine: Routine;
  isPausing: boolean;
  isResuming: boolean;
  onPause: () => void;
  onResume: () => void;
}

export default function PauseResumeButton({
  routine,
  isPausing,
  isResuming,
  onPause,
  onResume,
}: Props) {
  const [showConfirm, setShowConfirm] = useState(false);

  if (routine.status === 'DELETED') {
    return null;
  }

  const handlePauseClick = () => {
    setShowConfirm(true);
  };

  const handleConfirmPause = () => {
    setShowConfirm(false);
    onPause();
  };

  if (routine.status === 'PAUSED') {
    return (
      <Button
        variant="secondary"
        size="sm"
        onClick={onResume}
        loading={isResuming}
        disabled={isPausing}
      >
        Resume
      </Button>
    );
  }

  return (
    <>
      <Button
        variant="secondary"
        size="sm"
        onClick={handlePauseClick}
        loading={isPausing}
        disabled={isResuming}
      >
        Pause
      </Button>

      <Modal
        open={showConfirm}
        onClose={() => setShowConfirm(false)}
        title="Pause routine"
        aria-label="Pause routine"
        size="sm"
      >
        <div className="pause-confirm">
          <p className="pause-confirm__message">
            Pause this routine? No new tasks will be created while paused.
          </p>
          <p className="pause-confirm__name">&quot;{routine.title}&quot;</p>
          <div className="pause-confirm__actions">
            <Button variant="ghost" size="md" onClick={() => setShowConfirm(false)}>
              Cancel
            </Button>
            <button
              type="button"
              className="pause-confirm__btn"
              onClick={handleConfirmPause}
            >
              Pause
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
}
