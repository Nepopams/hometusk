import { forwardRef, type InputHTMLAttributes, useState } from 'react';
import TextField from './TextField';
import './PasswordField.css';

interface PasswordFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'size'> {
  /** Field label */
  label: string;
  /** Optional hint text */
  hint?: string;
  /** Position of hint text: 'top' (below label) or 'bottom' (below input) */
  hintPosition?: 'top' | 'bottom';
  /** Error message (field turns red when set) */
  error?: string;
  /** Hide the label visually but keep for screen readers */
  hideLabel?: boolean;
}

/**
 * Password field with show/hide toggle button.
 *
 * @example
 * // Basic usage
 * <PasswordField label="Password" />
 *
 * // With hint
 * <PasswordField label="Password" hint="At least 8 characters" />
 *
 * // With error
 * <PasswordField label="Password" error="Password is required" />
 */
const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(
  ({ label, hint, hintPosition, error, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);

    const toggleVisibility = () => {
      setShowPassword((prev) => !prev);
    };

    return (
      <TextField
        ref={ref}
        label={label}
        hint={hint}
        hintPosition={hintPosition}
        error={error}
        type={showPassword ? 'text' : 'password'}
        endAdornment={
          <button
            type="button"
            className="password-toggle"
            onClick={toggleVisibility}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            tabIndex={-1}
          >
            {showPassword ? (
              // Eye-off icon
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
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <path d="M1 1l22 22" />
                <path d="M10.58 10.59a3 3 0 1 0 4.24 4.24" />
              </svg>
            ) : (
              // Eye icon
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
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            )}
          </button>
        }
        {...props}
      />
    );
  }
);

PasswordField.displayName = 'PasswordField';

export default PasswordField;
