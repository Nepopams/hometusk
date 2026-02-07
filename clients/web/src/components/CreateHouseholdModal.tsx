import { useState, useEffect, useRef, type FormEvent, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createHousehold } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';
import { Button } from './ui';
import './CreateHouseholdModal.css';

const MAX_NAME_LENGTH = 50;

interface CreateHouseholdModalProps {
  /** Whether the modal is visible */
  open: boolean;
  /** Called when the modal should close */
  onClose: () => void;
  /** Called after successful creation with the new household ID */
  onSuccess?: (householdId: string) => void;
}

/**
 * Create Household modal with form validation and loading states.
 *
 * Desktop/Tablet: Centered modal (440px).
 * Mobile: Bottom sheet with stacked buttons.
 *
 * @see Pencil frames: NmuGH, D5bh0, w7hAD, 9wOkc, DMAll
 */
export default function CreateHouseholdModal({
  open,
  onClose,
  onSuccess,
}: CreateHouseholdModalProps) {
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();

  const modalRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const trimmedName = name.trim();

  // Focus input when modal opens
  useEffect(() => {
    if (open) {
      // Small delay to ensure animation completes
      const timer = setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [open]);

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setName('');
      setError(null);
      setIsSubmitting(false);
    }
  }, [open]);

  // Handle Escape key
  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape' && !isSubmitting) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, isSubmitting, onClose]);

  // Prevent body scroll when modal is open
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  if (!open) return null;

  const getValidationError = (): string | null => {
    if (trimmedName.length === 0) {
      return 'Please enter a household name';
    }
    if (trimmedName.length > MAX_NAME_LENGTH) {
      return `Name must be ${MAX_NAME_LENGTH} characters or less`;
    }
    return null;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    const validationError = getValidationError();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);

    try {
      const household = await createHousehold(trimmedName);
      await refetchUser();
      selectHousehold(household.id);

      if (onSuccess) {
        onSuccess(household.id);
      } else {
        navigate(`/households/${household.id}/tasks`, { replace: true });
      }

      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        const apiMessage =
          typeof err.body === 'object' && err.body !== null && 'message' in err.body
            ? (err.body as { message?: string }).message
            : undefined;
        setError(apiMessage || 'Unable to create household. Please try again.');
      } else {
        setError('Something went wrong. Please try again.');
      }
      setIsSubmitting(false);
    }
  };

  const handleBackdropClick = () => {
    if (!isSubmitting) {
      onClose();
    }
  };

  const handlePanelClick = (e: React.MouseEvent) => {
    e.stopPropagation();
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    // Trap focus within modal
    if (e.key === 'Tab') {
      const focusableElements = modalRef.current?.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      if (!focusableElements?.length) return;

      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

      if (e.shiftKey && document.activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      } else if (!e.shiftKey && document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    }
  };

  return (
    <div
      className="create-household-modal__backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        ref={modalRef}
        className="create-household-modal__card"
        onClick={handlePanelClick}
        onKeyDown={handleKeyDown}
        role="dialog"
        aria-modal="true"
        aria-label="Create household"
        tabIndex={-1}
      >
        {/* Header */}
        <div className="create-household-modal__header">
          <div className="create-household-modal__header-left">
            <div className="create-household-modal__icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </div>
            <h2 className="create-household-modal__title">Create household</h2>
          </div>
          <button
            type="button"
            className="create-household-modal__close"
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="Close"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Divider */}
        <div className="create-household-modal__divider" />

        {/* Body */}
        <form className="create-household-modal__body" onSubmit={handleSubmit}>
          <p className="create-household-modal__microcopy">
            Give your household a name you&apos;ll recognize.
          </p>

          <div className="create-household-modal__field">
            <label htmlFor="household-name" className="create-household-modal__label">
              Household name
            </label>
            <input
              ref={inputRef}
              id="household-name"
              type="text"
              className={`create-household-modal__input ${error ? 'create-household-modal__input--error' : ''}`}
              placeholder="e.g. The Smiths, Beach House"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={MAX_NAME_LENGTH + 10}
              disabled={isSubmitting}
              aria-invalid={error ? 'true' : undefined}
              aria-describedby={error ? 'create-household-error' : 'create-household-hint'}
            />
            {error && (
              <span id="create-household-error" className="create-household-modal__error" role="alert">
                {error}
              </span>
            )}
            <span id="create-household-hint" className="create-household-modal__hint">
              Max {MAX_NAME_LENGTH} characters
            </span>
          </div>
        </form>

        {/* Divider */}
        <div className="create-household-modal__divider" />

        {/* Footer */}
        <div className="create-household-modal__footer">
          <Button
            type="button"
            variant="ghost"
            size="md"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            size="md"
            loading={isSubmitting}
            disabled={isSubmitting}
            onClick={handleSubmit}
          >
            Create household
          </Button>
        </div>
      </div>
    </div>
  );
}
