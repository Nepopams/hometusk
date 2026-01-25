import { forwardRef, type InputHTMLAttributes, type ReactNode, useId } from 'react';
import './TextField.css';

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  /** Field label */
  label: string;
  /** Optional hint text below label */
  hint?: string;
  /** Error message (field turns red when set) */
  error?: string;
  /** Icon or element to show at the end of the input */
  endAdornment?: ReactNode;
  /** Hide the label visually but keep for screen readers */
  hideLabel?: boolean;
}

/**
 * TextField component with label, error state, and optional hint.
 *
 * @example
 * // Basic usage
 * <TextField label="Email" type="email" placeholder="you@example.com" />
 *
 * // With error
 * <TextField label="Email" error="Please enter a valid email" />
 *
 * // With hint
 * <TextField label="Password" type="password" hint="At least 8 characters" />
 */
const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  ({ label, hint, error, endAdornment, hideLabel, className = '', id, ...props }, ref) => {
    const generatedId = useId();
    const inputId = id || generatedId;
    const errorId = error ? `${inputId}-error` : undefined;
    const hintId = hint && !error ? `${inputId}-hint` : undefined;

    const describedBy = [errorId, hintId].filter(Boolean).join(' ') || undefined;

    return (
      <div className={`text-field ${error ? 'text-field--error' : ''} ${className}`}>
        <label
          htmlFor={inputId}
          className={`text-field__label ${hideLabel ? 'sr-only' : ''}`}
        >
          {label}
          {props.required && <span className="text-field__required"> *</span>}
        </label>

        {hint && !error && (
          <span id={hintId} className="text-field__hint">
            {hint}
          </span>
        )}

        <div className="text-field__input-wrapper">
          <input
            ref={ref}
            id={inputId}
            className="text-field__input"
            aria-invalid={error ? 'true' : undefined}
            aria-describedby={describedBy}
            {...props}
          />
          {endAdornment && <div className="text-field__end-adornment">{endAdornment}</div>}
        </div>

        {error && (
          <span id={errorId} className="text-field__error" role="alert">
            {error}
          </span>
        )}
      </div>
    );
  }
);

TextField.displayName = 'TextField';

export default TextField;
