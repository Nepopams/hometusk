import { useI18n } from '../../i18n';
import type { Routine } from '../../types/api';
import { Button } from '../ui';
import Modal from '../ui/Modal';
import './DeleteRoutineModal.css';

interface DeleteRoutineModalProps {
  open: boolean;
  routine: Routine | null;
  isDeleting: boolean;
  error: string | null;
  onClose: () => void;
  onConfirm: () => void;
}

export default function DeleteRoutineModal({
  open,
  routine,
  isDeleting,
  error,
  onClose,
  onConfirm,
}: DeleteRoutineModalProps) {
  const { t } = useI18n();

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t('routines.deleteRoutine')}
      aria-label={t('routines.deleteRoutine')}
      closeOnBackdrop={!isDeleting}
      closeOnEscape={!isDeleting}
      size="sm"
    >
      <div className="delete-routine">
        <p className="delete-routine__message">{t('routines.deleteMessage')}</p>
        {routine && <p className="delete-routine__name">&quot;{routine.title}&quot;</p>}
        {error && (
          <div className="delete-routine__error" role="alert">
            <span className="delete-routine__error-icon">!</span>
            <span>{error}</span>
          </div>
        )}
        <div className="delete-routine__actions">
          <Button type="button" variant="ghost" size="md" onClick={onClose} disabled={isDeleting}>
            {t('common.cancel')}
          </Button>
          <Button type="button" variant="primary" size="md" loading={isDeleting} onClick={onConfirm}>
            {t('common.delete')}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
