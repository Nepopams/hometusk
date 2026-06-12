import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui';
import { useI18n } from '../i18n';
import './Unauthorized.css';

/**
 * 401 Unauthorized page.
 * Shown when user needs to sign in to access content.
 *
 * Copy is constructive: "Please sign in to continue"
 * Provides clear next steps without blame.
 *
 * @see Pencil frames: 4dLWc (1200px), h3VbD (1024px), yKRmp (390px)
 */
export default function Unauthorized() {
  const navigate = useNavigate();
  const { t } = useI18n();

  const handleSignIn = () => {
    navigate('/login');
  };

  const handleCreateAccount = () => {
    navigate('/register');
  };

  return (
    <div className="unauthorized-page">
      <div className="unauthorized-page__content">
        {/* Icon */}
        <div className="unauthorized-page__icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            <polyline points="10 17 15 12 10 7" />
            <line x1="15" y1="12" x2="3" y2="12" />
          </svg>
        </div>

        {/* Text section */}
        <div className="unauthorized-page__text">
          <h1 className="unauthorized-page__title">{t('error.unauthorizedTitle')}</h1>
          <p className="unauthorized-page__subtitle">
            {t('error.unauthorizedSubtitle')}
          </p>
        </div>

        {/* Buttons */}
        <div className="unauthorized-page__buttons">
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={handleSignIn}
          >
            {t('auth.signIn')}
          </Button>
          <Button
            variant="secondary"
            size="lg"
            fullWidth
            onClick={handleCreateAccount}
          >
            {t('auth.createAccount')}
          </Button>
        </div>
      </div>
    </div>
  );
}
