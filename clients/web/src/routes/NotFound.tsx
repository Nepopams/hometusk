import { Link } from 'react-router-dom';
import { useI18n } from '../i18n';
import './NotFound.css';

export default function NotFound() {
  const { t } = useI18n();

  return (
    <div className="not-found">
      <div className="not-found__card">
        <div className="not-found__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4M12 16h.01" />
          </svg>
        </div>
        <div className="not-found__code">404</div>
        <h1 className="not-found__title">{t('error.notFoundTitle')}</h1>
        <p className="not-found__message">
          {t('error.notFoundMessage')}
        </p>
        <div className="not-found__actions">
          <Link to="/households" className="btn btn--primary btn--lg btn--full-width">
            <span className="btn__label">{t('error.goToHome')}</span>
          </Link>
          <Link to="/login" className="btn btn--ghost btn--md btn--full-width">
            <span className="btn__label">{t('error.signIn')}</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
