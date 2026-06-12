import { useEffect, type ReactNode } from 'react';
import { useI18n } from '../../i18n';
import './Snackbar.css';

export type SnackbarVariant = 'default' | 'success' | 'error' | 'warning' | 'info';

interface SnackbarProps {
  /** Whether the snackbar is visible */
  open: boolean;
  /** Called when the snackbar should close */
  onClose: () => void;
  /** Message content */
  children: ReactNode;
  /** Visual variant */
  variant?: SnackbarVariant;
  /** Auto-dismiss duration in ms (0 = no auto-dismiss) */
  duration?: number;
  /** Action button (e.g., "Undo") */
  action?: ReactNode;
  /** Position on screen */
  position?: 'bottom' | 'top';
}

/**
 * Snackbar/toast notification component.
 *
 * @example
 * <Snackbar
 *   open={showToast}
 *   onClose={() => setShowToast(false)}
 *   variant="success"
 *   duration={4000}
 * >
 *   Changes saved successfully
 * </Snackbar>
 *
 * @example
 * // With action
 * <Snackbar
 *   open={showUndo}
 *   onClose={() => setShowUndo(false)}
 *   action={<button onClick={handleUndo}>Undo</button>}
 * >
 *   Item deleted
 * </Snackbar>
 */
export default function Snackbar({
  open,
  onClose,
  children,
  variant = 'default',
  duration = 4000,
  action,
  position = 'bottom',
}: SnackbarProps) {
  const { t } = useI18n();

  // Auto-dismiss
  useEffect(() => {
    if (!open || duration === 0) return;

    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [open, duration, onClose]);

  if (!open) return null;

  const classNames = [
    'snackbar',
    `snackbar--${variant}`,
    `snackbar--${position}`,
  ].join(' ');

  return (
    <div
      className={classNames}
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {variant !== 'default' && (
        <span className="snackbar__icon" aria-hidden="true">
          {getIcon(variant)}
        </span>
      )}
      <span className="snackbar__message">{children}</span>
      {action && <span className="snackbar__action">{action}</span>}
      <button
        type="button"
        className="snackbar__close"
        onClick={onClose}
        aria-label={t('common.dismiss')}
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
}

function getIcon(variant: SnackbarVariant): ReactNode {
  switch (variant) {
    case 'success':
      return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      );
    case 'error':
      return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="15" y1="9" x2="9" y2="15" />
          <line x1="9" y1="9" x2="15" y2="15" />
        </svg>
      );
    case 'warning':
      return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      );
    case 'info':
      return (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
      );
    default:
      return null;
  }
}
