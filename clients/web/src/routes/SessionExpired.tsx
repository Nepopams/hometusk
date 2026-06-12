import { useNavigate } from 'react-router-dom';
import { Button, TextLink } from '../components/ui';
import { useI18n } from '../i18n';
import './SessionExpired.css';

/**
 * Session Expired full-page.
 * Used on mobile viewports and as standalone fallback.
 *
 * @see Pencil frames: kvKxQ (390px mobile), lw1ZF (1200px full-page)
 */
export default function SessionExpired() {
  const navigate = useNavigate();
  const { t } = useI18n();

  const handleSignIn = () => {
    navigate('/login');
  };

  return (
    <div className="session-expired-page">
      <div className="session-expired-page__content">
        {/* Icon */}
        <div className="session-expired-page__icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>

        {/* Title */}
        <h1 className="session-expired-page__title">{t('session.expired')}</h1>

        {/* Body */}
        <p className="session-expired-page__body">
          {t('session.expiredBody')}
        </p>

        {/* Preserved hint */}
        <div className="session-expired-page__hint">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
          <span>{t('session.inputPreserved')}</span>
        </div>

        {/* Buttons */}
        <div className="session-expired-page__buttons">
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={handleSignIn}
          >
            {t('session.signInAgain')}
          </Button>
          <TextLink to="/" centered>
            {t('common.backHome')}
          </TextLink>
        </div>
      </div>
    </div>
  );
}
