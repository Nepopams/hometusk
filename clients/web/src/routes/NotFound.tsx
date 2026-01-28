import { Link } from 'react-router-dom';
import './NotFound.css';

export default function NotFound() {
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
        <h1 className="not-found__title">Page Not Found</h1>
        <p className="not-found__message">
          The page you are looking for does not exist or has been moved.
        </p>
        <div className="not-found__actions">
          <Link to="/households" className="btn btn--primary btn--lg btn--full-width">
            <span className="btn__label">Go to Home</span>
          </Link>
          <Link to="/login" className="btn btn--ghost btn--md btn--full-width">
            <span className="btn__label">Sign In</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
