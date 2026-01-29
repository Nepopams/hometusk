import { Button } from '../ui';
import Modal from '../ui/Modal';
import type { Routine } from '../../types/api';
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
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Delete routine"
      aria-label="Delete routine"
      closeOnBackdrop={!isDeleting}
      closeOnEscape={!isDeleting}
      size="sm"
    >
      <div className="delete-routine">
        <p className="delete-routine__message">
          Delete routine? Pending tasks will remain.
        </p>
        {routine && (
          <p className="delete-routine__name">&quot;{routine.title}&quot;</p>
        )}
        {error && (
          <div className="delete-routine__error" role="alert">
            <span className="delete-routine__error-icon">⚠</span>
            <span>{error}</span>
          </div>
        )}
        <div className="delete-routine__actions">
          <Button type="button" variant="ghost" size="md" onClick={onClose} disabled={isDeleting}>
            Cancel
          </Button>
          <Button type="button" variant="primary" size="md" loading={isDeleting} onClick={onConfirm}>
            Delete
          </Button>
        </div>
      </div>
    </Modal>
  );
}
