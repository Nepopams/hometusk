import type { ReactNode } from 'react';
import './ErrorBanner.css';

interface ErrorBannerProps {
  /** Error title */
  title?: string;
  /** Error message or description */
  children: ReactNode;
  /** Optional action button */
  action?: ReactNode;
}

/**
 * Full-width error banner for form-level errors (e.g., wrong credentials).
 *
 * @example
 * // Simple message
 * <ErrorBanner>
 *   Incorrect email or password. Please try again.
 * </ErrorBanner>
 *
 * // With title
 * <ErrorBanner title="Authentication failed">
 *   We couldn't verify your credentials.
 * </ErrorBanner>
 *
 * // With action
 * <ErrorBanner
 *   title="Account locked"
 *   action={<button>Reset password</button>}
 * >
 *   Your account has been locked due to too many failed attempts.
 * </ErrorBanner>
 */
export default function ErrorBanner({ title, children, action }: ErrorBannerProps) {
  return (
    <div className="error-banner" role="alert">
      <div className="error-banner__icon" aria-hidden="true">
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <div className="error-banner__content">
        {title && <strong className="error-banner__title">{title}</strong>}
        <p className="error-banner__message">{children}</p>
        {action && <div className="error-banner__action">{action}</div>}
      </div>
    </div>
  );
}
