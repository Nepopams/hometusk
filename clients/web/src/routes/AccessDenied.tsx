import { useNavigate } from 'react-router-dom';
import { Button, TextLink } from '../components/ui';
import './AccessDenied.css';

/**
 * 403 Access Denied page.
 * Shown when user doesn't have access to a household/resource.
 *
 * Copy is neutral and constructive, providing clear next steps.
 * Does NOT reveal household details (names/ids) for security.
 *
 * @see Pencil frames: e0Goo (1200px), ymcdH (1024px), pjCdq (390px)
 */
export default function AccessDenied() {
  const navigate = useNavigate();

  const handleSwitchHousehold = () => {
    navigate('/households');
  };

  const handleEnterInvite = () => {
    navigate('/invite');
  };

  return (
    <div className="access-denied-page">
      <div className="access-denied-page__content">
        {/* Icon */}
        <div className="access-denied-page__icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <line x1="8" y1="11" x2="16" y2="11" />
          </svg>
        </div>

        {/* Text section */}
        <div className="access-denied-page__text">
          <h1 className="access-denied-page__title">
            You don&apos;t have access to this household
          </h1>
          <p className="access-denied-page__subtitle">
            You may need to switch to a different household or use an invite code to join this one.
          </p>
        </div>

        {/* Buttons */}
        <div className="access-denied-page__buttons">
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={handleSwitchHousehold}
          >
            Switch household
          </Button>
          <Button
            variant="secondary"
            size="lg"
            fullWidth
            onClick={handleEnterInvite}
          >
            Enter invite code
          </Button>
        </div>

        {/* Support link */}
        <TextLink
          href="mailto:support@hometusk.app"
          variant="muted"
          className="access-denied-page__support"
        >
          Need help? Contact support
        </TextLink>
      </div>
    </div>
  );
}
