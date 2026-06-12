import './Spinner.css';
import { useI18n } from '../../i18n';

type SpinnerSize = 'sm' | 'md' | 'lg';

interface SpinnerProps {
  /** Size of the spinner */
  size?: SpinnerSize;
  /** Accessible label */
  label?: string;
  /** Whether to show the label text */
  showLabel?: boolean;
}

/**
 * Loading spinner with accessible label.
 *
 * @example
 * // Default
 * <Spinner />
 *
 * // With visible label
 * <Spinner label="Loading tasks..." showLabel />
 *
 * // Small size
 * <Spinner size="sm" />
 */
export default function Spinner({
  size = 'md',
  label,
  showLabel = false,
}: SpinnerProps) {
  const { t } = useI18n();
  const resolvedLabel = label ?? t('common.loading');

  return (
    <div className={`spinner spinner--${size}`} role="status">
      <svg
        className="spinner__icon"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray="31.4 31.4"
        />
      </svg>
      <span className={showLabel ? 'spinner__label' : 'sr-only'}>{resolvedLabel}</span>
    </div>
  );
}
