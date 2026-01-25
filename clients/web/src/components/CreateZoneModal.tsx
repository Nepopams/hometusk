import { useState, useEffect, useRef, type FormEvent, type KeyboardEvent } from 'react';
import { createZone } from '../lib/api';
import { ApiError } from '../lib/errors';
import { Button } from './ui';
import './CreateZoneModal.css';

const MAX_NAME_LENGTH = 50;

interface CreateZoneModalProps {
  /** Household ID to create zone in */
  householdId: string;
  /** Whether the modal is visible */
  open: boolean;
  /** Called when the modal should close */
  onClose: () => void;
  /** Called after successful creation with the new zone */
  onSuccess?: (zone: { id: string; name: string }) => void;
}

/**
 * Create Zone modal with form validation and loading states.
 *
 * Desktop/Tablet: Centered modal (440px).
 * Mobile: Bottom sheet with stacked buttons.
 *
 * Pattern follows CreateHouseholdModal (no dedicated Pencil frames for zones).
 * @see Pencil frames: NmuGH (pattern), D5bh0 (error), w7hAD (loading), DMAll (mobile)
 */
export default function CreateZoneModal({
  householdId,
  open,
  onClose,
  onSuccess,
}: CreateZoneModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const trimmedName = name.trim();

  // Focus input when modal opens
  useEffect(() => {
    if (open) {
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
      return 'Please enter a zone name';
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
      const zone = await createZone(householdId, trimmedName);

      if (onSuccess) {
        onSuccess(zone);
      }

      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        const apiMessage =
          typeof err.body === 'object' && err.body !== null && 'message' in err.body
            ? (err.body as { message?: string }).message
            : undefined;
        setError(apiMessage || 'Unable to create zone. Please try again.');
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
      className="create-zone-modal__backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        ref={modalRef}
        className="create-zone-modal__card"
        onClick={handlePanelClick}
        onKeyDown={handleKeyDown}
        role="dialog"
        aria-modal="true"
        aria-label="Create zone"
        tabIndex={-1}
      >
        {/* Header */}
        <div className="create-zone-modal__header">
          <div className="create-zone-modal__header-left">
            <div className="create-zone-modal__icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="14" y="14" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
              </svg>
            </div>
            <h2 className="create-zone-modal__title">Create zone</h2>
          </div>
          <button
            type="button"
            className="create-zone-modal__close"
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
        <div className="create-zone-modal__divider" />

        {/* Body */}
        <form className="create-zone-modal__body" onSubmit={handleSubmit}>
          <p className="create-zone-modal__microcopy">
            Zones help organize tasks by area of your home.
          </p>

          <div className="create-zone-modal__field">
            <label htmlFor="zone-name" className="create-zone-modal__label">
              Zone name
            </label>
            <input
              ref={inputRef}
              id="zone-name"
              type="text"
              className={`create-zone-modal__input ${error ? 'create-zone-modal__input--error' : ''}`}
              placeholder="e.g. Kitchen, Living Room, Garage"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={MAX_NAME_LENGTH + 10}
              disabled={isSubmitting}
              aria-invalid={error ? 'true' : undefined}
              aria-describedby={error ? 'create-zone-error' : 'create-zone-hint'}
            />
            {error && (
              <span id="create-zone-error" className="create-zone-modal__error" role="alert">
                {error}
              </span>
            )}
            <span id="create-zone-hint" className="create-zone-modal__hint">
              Max {MAX_NAME_LENGTH} characters
            </span>
          </div>
        </form>

        {/* Divider */}
        <div className="create-zone-modal__divider" />

        {/* Footer */}
        <div className="create-zone-modal__footer">
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
            Create zone
          </Button>
        </div>
      </div>
    </div>
  );
}
