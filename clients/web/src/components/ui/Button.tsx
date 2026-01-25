import type { ButtonHTMLAttributes, ReactNode } from 'react';
import './Button.css';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'text';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Visual style variant */
  variant?: ButtonVariant;
  /** Size of the button */
  size?: ButtonSize;
  /** Show loading spinner and disable interaction */
  loading?: boolean;
  /** Full width button */
  fullWidth?: boolean;
  /** Icon to show before children */
  iconLeft?: ReactNode;
  /** Icon to show after children */
  iconRight?: ReactNode;
  children: ReactNode;
}

/**
 * Button component with primary, secondary, ghost, and text variants.
 *
 * @example
 * // Primary (default)
 * <Button onClick={handleSubmit}>Sign in</Button>
 *
 * // Secondary
 * <Button variant="secondary">Cancel</Button>
 *
 * // Loading state
 * <Button loading>Processing...</Button>
 *
 * // With icon
 * <Button iconLeft={<IconPlus />}>Add item</Button>
 */
export default function Button({
  variant = 'primary',
  size = 'lg',
  loading = false,
  fullWidth = false,
  iconLeft,
  iconRight,
  disabled,
  className = '',
  children,
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;

  const classNames = [
    'btn',
    `btn--${variant}`,
    `btn--${size}`,
    fullWidth && 'btn--full-width',
    loading && 'btn--loading',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button className={classNames} disabled={isDisabled} {...props}>
      {loading && (
        <span className="btn__spinner" aria-hidden="true">
          <svg
            className="btn__spinner-icon"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
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
        </span>
      )}
      {!loading && iconLeft && <span className="btn__icon btn__icon--left">{iconLeft}</span>}
      <span className="btn__label">{children}</span>
      {!loading && iconRight && <span className="btn__icon btn__icon--right">{iconRight}</span>}
    </button>
  );
}
