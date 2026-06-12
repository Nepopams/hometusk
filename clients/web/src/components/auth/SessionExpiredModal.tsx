import { useNavigate } from 'react-router-dom';
import { Modal, Button } from '../ui';
import { useIsMobile } from '../../hooks/useMediaQuery';
import { useI18n } from '../../i18n';
import './SessionExpiredModal.css';

interface SessionExpiredModalProps {
  /** Whether the modal is visible */
  open: boolean;
  /** Called when the modal should close */
  onClose: () => void;
  /** Whether sign-in action is loading */
  loading?: boolean;
  /** Error message to display (re-auth failed state) */
  error?: string | null;
  /** Called when user clicks "Sign in again" */
  onSignIn?: () => void;
  /** Whether to show the "input preserved" hint */
  showPreservedHint?: boolean;
}

/**
 * Session expired modal for desktop/tablet viewports.
 * On mobile (< 480px), renders as full-page via route instead.
 *
 * States:
 * - Default: session expired, option to sign in or cancel
 * - Error: re-auth failed, shows error banner
 * - Loading: sign-in in progress
 *
 * @see Pencil frames: 8CHBm (1200px), 5IASX (1024px), G7gtU (re-auth failed)
 */
export default function SessionExpiredModal({
  open,
  onClose,
  loading = false,
  error = null,
  onSignIn,
  showPreservedHint = true,
}: SessionExpiredModalProps) {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const { t } = useI18n();

  // On mobile, redirect to full-page instead of showing modal
  if (isMobile && open) {
    navigate('/session-expired');
    return null;
  }

  const handleSignIn = () => {
    if (onSignIn) {
      onSignIn();
    } else {
      navigate('/login');
    }
  };

  const isError = !!error;

  return (
    <Modal
      open={open}
      onClose={onClose}
      closeOnBackdrop={!loading}
      closeOnEscape={!loading}
      showCloseButton={false}
      aria-label={isError ? t('session.unableSignIn') : t('session.expired')}
    >
      <div className="session-expired-modal">
        {/* Header with icon */}
        <div className="session-expired-modal__header">
          <div className={`session-expired-modal__icon ${isError ? 'session-expired-modal__icon--error' : ''}`}>
            {isError ? (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            )}
          </div>
          <h2 className="session-expired-modal__title">
            {isError ? t('session.unableSignIn') : t('session.expired')}
          </h2>
        </div>

        {/* Body text */}
        <p className="session-expired-modal__body">
          {isError
            ? t('session.verifyCredentials')
            : t('session.expiredBody')}
        </p>

        {/* Error banner (when re-auth failed) */}
        {isError && (
          <div className="session-expired-modal__error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
            <span>{error}</span>
          </div>
        )}

        {/* Preserved hint (success state) */}
        {!isError && showPreservedHint && (
          <div className="session-expired-modal__hint">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <span>{t('session.unsavedInputPreserved')}</span>
          </div>
        )}

        {/* Help text (error state only) */}
        {isError && (
          <p className="session-expired-modal__help">
            {t('session.forgotPasswordHelp')}
          </p>
        )}

        {/* Button row */}
        <div className="session-expired-modal__buttons">
          <Button
            variant="secondary"
            size="md"
            onClick={onClose}
            disabled={loading}
          >
            {t('common.cancel')}
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={handleSignIn}
            loading={loading}
            disabled={loading}
          >
            {isError ? t('common.tryAgain') : t('session.signInAgain')}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
