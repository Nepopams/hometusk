import { useState } from 'react';
import { useI18n } from '../../i18n';
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
  const { t } = useI18n();
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
        {t('routines.resume')}
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
        {t('routines.pause')}
      </Button>

      <Modal
        open={showConfirm}
        onClose={() => setShowConfirm(false)}
        title={t('routines.pauseRoutine')}
        aria-label={t('routines.pauseRoutine')}
        size="sm"
      >
        <div className="pause-confirm">
          <p className="pause-confirm__message">{t('routines.pauseMessage')}</p>
          <p className="pause-confirm__name">&quot;{routine.title}&quot;</p>
          <div className="pause-confirm__actions">
            <Button variant="ghost" size="md" onClick={() => setShowConfirm(false)}>
              {t('common.cancel')}
            </Button>
            <button type="button" className="pause-confirm__btn" onClick={handleConfirmPause}>
              {t('routines.pause')}
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
}
