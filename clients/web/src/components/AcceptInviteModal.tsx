import { useState, useEffect, useRef, type FormEvent, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { acceptInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';
import { Button } from './ui';
import { useI18n } from '../i18n';
import type { AcceptInviteResponse } from '../types/api';
import './AcceptInviteModal.css';

type ModalState =
  | 'input' // Code entry form
  | 'validating' // Processing invite
  | 'success' // Joined successfully
  | 'already_member' // User is already a member
  | 'invalid' // Invalid or expired code
  | 'network_error'; // Network/server error

interface ResultInfo {
  title: string;
  message: string;
}

interface AcceptInviteModalProps {
  /** Whether the modal is visible */
  open: boolean;
  /** Called when the modal should close */
  onClose: () => void;
  /** Called after successful join with the household ID */
  onSuccess?: (householdId: string) => void;
}

/**
 * Accept Invite modal with code input and result states.
 *
 * Desktop/Tablet: Centered modal (440px).
 * Mobile: Bottom sheet with stacked buttons.
 *
 * States: input, validating, success, already_member, invalid, network_error
 *
 * @see Pencil frames: NmuGH, D5bh0, w7hAD, 9wOkc, DMAll (Create Household pattern)
 * @see Join card pattern: 1aojN, mpvhZ
 */
export default function AcceptInviteModal({
  open,
  onClose,
  onSuccess,
}: AcceptInviteModalProps) {
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();
  const { t } = useI18n();

  const modalRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const selectHouseholdRef = useRef(selectHousehold);
  const refetchUserRef = useRef(refetchUser);

  const [state, setState] = useState<ModalState>('input');
  const [code, setCode] = useState('');
  const [inputError, setInputError] = useState<string | null>(null);
  const [result, setResult] = useState<ResultInfo | null>(null);
  const [household, setHousehold] = useState<{ id: string; name: string } | null>(null);

  // Keep refs updated
  useEffect(() => {
    selectHouseholdRef.current = selectHousehold;
    refetchUserRef.current = refetchUser;
  }, [selectHousehold, refetchUser]);

  // Focus input when modal opens
  useEffect(() => {
    if (open && state === 'input') {
      const timer = setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [open, state]);

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setState('input');
      setCode('');
      setInputError(null);
      setResult(null);
      setHousehold(null);
    }
  }, [open]);

  // Handle Escape key
  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape' && state !== 'validating') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, state, onClose]);

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

  // Process invite code
  const processInvite = async (inviteCode: string) => {
    setState('validating');
    setInputError(null);

    try {
      const response: AcceptInviteResponse = await acceptInvite(inviteCode.trim());

      setHousehold({ id: response.household.id, name: response.household.name });

      setState('success');
      setResult({
        title: t('invite.welcomeTeam'),
        message: t('invite.joinedHousehold'),
      });

      // Refetch user data and select household
      await refetchUserRef.current();
      selectHouseholdRef.current(response.household.id);

      // Auto-redirect after short delay
      setTimeout(() => {
        if (onSuccess) {
          onSuccess(response.household.id);
        } else {
          navigate(`/households/${response.household.id}/tasks`, { replace: true });
        }
        onClose();
      }, 2000);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 404 || err.status === 410) {
          setState('invalid');
          setResult({
            title: t('invite.noLongerValid'),
            message: t('invite.noLongerValidMsg'),
          });
        } else if (err.status === 409) {
          // Already a member (conflict)
          setState('already_member');
          setResult({
            title: t('invite.alreadyMemberTitle'),
            message: t('invite.alreadyMemberMsg'),
          });
          // Try to get household info from error response
          const body = err.body as { household?: { id: string; name: string } } | undefined;
          if (body?.household) {
            setHousehold(body.household);
          }
        } else {
          setState('network_error');
          setResult({
            title: t('invite.processErrorTitle'),
            message: t('invite.processErrorMsg'),
          });
        }
      } else {
        setState('network_error');
        setResult({
          title: t('invite.connectionProblem'),
          message: t('invite.connectionProblemMsg'),
        });
      }
    }
  };

  // Handle form submission
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    const trimmedCode = code.trim();
    if (!trimmedCode) {
      setInputError(t('invite.enterCode'));
      return;
    }

    processInvite(trimmedCode);
  };

  // Handle retry
  const handleRetry = () => {
    if (code.trim()) {
      processInvite(code.trim());
    } else {
      setState('input');
      setResult(null);
    }
  };

  // Handle "Open household" for already_member state
  const handleOpenHousehold = async () => {
    if (household) {
      await refetchUserRef.current();
      selectHouseholdRef.current(household.id);
      navigate(`/households/${household.id}/tasks`, { replace: true });
    } else {
      navigate('/households', { replace: true });
    }
    onClose();
  };

  const handleBackdropClick = () => {
    if (state !== 'validating') {
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

  // Render input state
  const renderInput = () => (
    <>
      {/* Header */}
      <div className="accept-invite-modal__header">
        <div className="accept-invite-modal__header-left">
          <div className="accept-invite-modal__icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
            </svg>
          </div>
          <h2 className="accept-invite-modal__title">{t('invite.joinHousehold')}</h2>
        </div>
        <button
          type="button"
          className="accept-invite-modal__close"
          onClick={onClose}
          aria-label={t('common.close')}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div className="accept-invite-modal__divider" />

      {/* Body */}
      <form className="accept-invite-modal__body" onSubmit={handleSubmit}>
        <p className="accept-invite-modal__microcopy">
          {t('invite.enterCodeFromMember')}
        </p>

        <div className="accept-invite-modal__field">
          <label htmlFor="invite-code" className="accept-invite-modal__label">
            {t('invite.code')}
          </label>
          <input
            ref={inputRef}
            id="invite-code"
            type="text"
            className={`accept-invite-modal__input ${inputError ? 'accept-invite-modal__input--error' : ''}`}
            placeholder={t('invite.codePlaceholder')}
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              setInputError(null);
            }}
            autoComplete="off"
            spellCheck="false"
            aria-invalid={inputError ? 'true' : undefined}
            aria-describedby={inputError ? 'invite-error' : 'invite-hint'}
          />
          {inputError ? (
            <span id="invite-error" className="accept-invite-modal__error" role="alert">
              {inputError}
            </span>
          ) : (
            <span id="invite-hint" className="accept-invite-modal__hint">
              {t('invite.codeHint')}
            </span>
          )}
        </div>
      </form>

      <div className="accept-invite-modal__divider" />

      {/* Footer */}
      <div className="accept-invite-modal__footer">
        <Button type="button" variant="ghost" size="md" onClick={onClose}>
          {t('common.cancel')}
        </Button>
        <Button type="submit" variant="secondary" size="md" onClick={handleSubmit}>
          {t('common.join')}
        </Button>
      </div>
    </>
  );

  // Render validating state
  const renderValidating = () => (
    <>
      <div className="accept-invite-modal__header">
        <div className="accept-invite-modal__header-left">
          <div className="accept-invite-modal__icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
            </svg>
          </div>
          <h2 className="accept-invite-modal__title">{t('invite.joinHousehold')}</h2>
        </div>
        <button
          type="button"
          className="accept-invite-modal__close"
          onClick={onClose}
          disabled
          aria-label={t('common.close')}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div className="accept-invite-modal__divider" />

      <div className="accept-invite-modal__validating">
        <div className="accept-invite-modal__spinner" aria-hidden="true" />
        <p className="accept-invite-modal__validating-text">{t('invite.checking')}</p>
      </div>
    </>
  );

  // Render success state
  const renderSuccess = () => (
    <>
      <div className="accept-invite-modal__header">
        <div className="accept-invite-modal__header-left">
          <div className="accept-invite-modal__icon accept-invite-modal__icon--success">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <h2 className="accept-invite-modal__title">{t('invite.joined')}</h2>
        </div>
        <button
          type="button"
          className="accept-invite-modal__close"
          onClick={onClose}
          disabled
          aria-label={t('common.close')}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div className="accept-invite-modal__divider" />

      <div className="accept-invite-modal__result">
        <div className="accept-invite-modal__result-icon accept-invite-modal__result-icon--success">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h3 className="accept-invite-modal__result-title">{result?.title}</h3>
        <p className="accept-invite-modal__result-message">
          {result?.message}{' '}
          {household && <span className="accept-invite-modal__household-name">&quot;{household.name}&quot;</span>}
        </p>
        <p className="accept-invite-modal__redirect-hint">{t('invite.redirecting')}</p>
      </div>
    </>
  );

  // Render already_member state
  const renderAlreadyMember = () => (
    <>
      <div className="accept-invite-modal__header">
        <div className="accept-invite-modal__header-left">
          <div className="accept-invite-modal__icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
          </div>
          <h2 className="accept-invite-modal__title">{t('invite.alreadyMember')}</h2>
        </div>
        <button
          type="button"
          className="accept-invite-modal__close"
          onClick={onClose}
          aria-label={t('common.close')}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div className="accept-invite-modal__divider" />

      <div className="accept-invite-modal__result">
        <div className="accept-invite-modal__result-icon accept-invite-modal__result-icon--info">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        </div>
        <h3 className="accept-invite-modal__result-title">{result?.title}</h3>
        <p className="accept-invite-modal__result-message">
          {result?.message}{' '}
          {household && <span className="accept-invite-modal__household-name">&quot;{household.name}&quot;</span>}
        </p>
        <div className="accept-invite-modal__result-footer">
          <Button variant="primary" size="lg" fullWidth onClick={handleOpenHousehold}>
            {t('invite.openHousehold')}
          </Button>
        </div>
      </div>
    </>
  );

  // Render invalid state
  const renderInvalid = () => (
    <>
      <div className="accept-invite-modal__header">
        <div className="accept-invite-modal__header-left">
          <div className="accept-invite-modal__icon accept-invite-modal__icon--error">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
          </div>
          <h2 className="accept-invite-modal__title">{t('invite.invalid')}</h2>
        </div>
        <button
          type="button"
          className="accept-invite-modal__close"
          onClick={onClose}
          aria-label={t('common.close')}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div className="accept-invite-modal__divider" />

      <div className="accept-invite-modal__result">
        <div className="accept-invite-modal__result-icon accept-invite-modal__result-icon--error">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
        </div>
        <h3 className="accept-invite-modal__result-title">{result?.title}</h3>
        <p className="accept-invite-modal__result-message">{result?.message}</p>
        <div className="accept-invite-modal__result-footer">
          <Button
            variant="secondary"
            size="lg"
            fullWidth
            onClick={() => {
              setState('input');
              setCode('');
              setResult(null);
            }}
          >
            {t('invite.tryDifferent')}
          </Button>
          <Button variant="ghost" size="md" fullWidth onClick={onClose}>
            {t('common.close')}
          </Button>
        </div>
      </div>
    </>
  );

  // Render network error state
  const renderNetworkError = () => (
    <>
      <div className="accept-invite-modal__header">
        <div className="accept-invite-modal__header-left">
          <div className="accept-invite-modal__icon accept-invite-modal__icon--error">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          </div>
          <h2 className="accept-invite-modal__title">{t('common.connectionError')}</h2>
        </div>
        <button
          type="button"
          className="accept-invite-modal__close"
          onClick={onClose}
          aria-label={t('common.close')}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div className="accept-invite-modal__divider" />

      <div className="accept-invite-modal__result">
        <div className="accept-invite-modal__result-icon accept-invite-modal__result-icon--error">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        </div>
        <h3 className="accept-invite-modal__result-title">{result?.title}</h3>
        <p className="accept-invite-modal__result-message">{result?.message}</p>
        <div className="accept-invite-modal__result-footer">
          <Button variant="primary" size="lg" fullWidth onClick={handleRetry}>
            {t('common.tryAgain')}
          </Button>
          <Button variant="ghost" size="md" fullWidth onClick={onClose}>
            {t('common.close')}
          </Button>
        </div>
      </div>
    </>
  );

  const renderContent = () => {
    switch (state) {
      case 'input':
        return renderInput();
      case 'validating':
        return renderValidating();
      case 'success':
        return renderSuccess();
      case 'already_member':
        return renderAlreadyMember();
      case 'invalid':
        return renderInvalid();
      case 'network_error':
        return renderNetworkError();
    }
  };

  return (
    <div
      className="accept-invite-modal__backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        ref={modalRef}
        className="accept-invite-modal__card"
        onClick={handlePanelClick}
        onKeyDown={handleKeyDown}
        role="dialog"
        aria-modal="true"
        aria-label={t('invite.joinHousehold')}
        tabIndex={-1}
      >
        {/* Mobile sheet handle */}
        <div className="accept-invite-modal__handle">
          <div className="accept-invite-modal__handle-bar" />
        </div>

        {renderContent()}
      </div>
    </div>
  );
}
