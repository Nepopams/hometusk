import { useState, type FormEvent } from 'react';
import { signinRedirect } from '../lib/auth/oidc';
import { useAuth } from '../hooks/useAuth';
import { useSearchParams } from 'react-router-dom';
import AuthLayout from '../components/auth/AuthLayout';
import BrandHeader from '../components/auth/BrandHeader';
import { Card, Button, TextField, PasswordField, ErrorBanner, Divider, TextLink } from '../components/ui';
import './Register.css';

const ERROR_MESSAGES: Record<string, string> = {
  registration_failed: 'Registration failed. Please try again.',
  email_exists: 'An account with this email already exists.',
  auth_unavailable: 'Authentication service is temporarily unavailable. Please try again later.',
};

export default function Register() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [formError, setFormError] = useState('');
  const [loading, setLoading] = useState(false);
  const { clearError } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? ERROR_MESSAGES[errorParam] || 'An error occurred. Please try again.' : null;

  const clearErrorParam = () => {
    if (errorParam) {
      setSearchParams({}, { replace: true });
    }
    clearError();
    setFormError('');
  };

  // Validate email format
  const validateEmail = (value: string): string => {
    if (!value.trim()) {
      return 'Email is required';
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      return 'Please enter a valid email address';
    }
    return '';
  };

  // Validate password (minimum 8 characters)
  const validatePassword = (value: string): string => {
    if (!value) {
      return 'Password is required';
    }
    if (value.length < 8) {
      return 'Password must be at least 8 characters';
    }
    return '';
  };

  const handleCreateAccount = async (e: FormEvent) => {
    e.preventDefault();
    clearErrorParam();

    // Validate fields
    const emailErr = validateEmail(email);
    const passwordErr = validatePassword(password);

    setEmailError(emailErr);
    setPasswordError(passwordErr);

    if (emailErr || passwordErr) {
      return;
    }

    setLoading(true);
    try {
      // Redirect to Keycloak for actual registration
      // Name/email/password are visual - Keycloak handles the real registration
      await signinRedirect();
    } catch (err) {
      console.error('[Register] OIDC redirect failed', err);
      setFormError('Unable to connect to authentication service. Please try again.');
      setLoading(false);
    }
  };

  // Keycloak mode - styled registration form
  if (authProvider === 'keycloak') {
    return (
      <AuthLayout>
        <Card padding="lg">
          <form
            className={`register-form ${loading ? 'register-form--loading' : ''}`}
            onSubmit={handleCreateAccount}
            noValidate
          >
            {/* Brand Header */}
            <BrandHeader tagline="Create your account" />

            {/* Error Banner (form-level errors) */}
            {(errorMessage || formError) && (
              <ErrorBanner title="Unable to create account">
                {errorMessage || formError}
              </ErrorBanner>
            )}

            {/* Form Fields */}
            <div className="register-form__fields">
              <TextField
                label="Name"
                labelSuffix="(optional)"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
                disabled={loading}
                autoComplete="name"
                autoFocus
              />
              <TextField
                label="Email"
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (emailError) setEmailError('');
                }}
                onBlur={() => setEmailError(validateEmail(email))}
                placeholder="you@example.com"
                error={emailError}
                disabled={loading}
                autoComplete="email"
              />
              <PasswordField
                label="Password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (passwordError) setPasswordError('');
                }}
                onBlur={() => setPasswordError(validatePassword(password))}
                placeholder="••••••••"
                hint="At least 8 characters"
                hintPosition="bottom"
                error={passwordError}
                disabled={loading}
                autoComplete="new-password"
              />
            </div>

            {/* Actions */}
            <div className="register-form__actions">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                fullWidth
                loading={loading}
                disabled={loading}
              >
                {loading ? 'Creating account...' : 'Create account'}
              </Button>
            </div>

            {/* Divider */}
            <Divider text="or" />

            {/* Sign In Link */}
            <div className="register-form__footer">
              <span className="register-form__footer-text">Already have an account? </span>
              <TextLink to="/login">Sign in</TextLink>
            </div>
          </form>
        </Card>
      </AuthLayout>
    );
  }

  // Dev mode - redirect to login (dev mode doesn't support registration)
  if (authProvider === 'dev') {
    return (
      <AuthLayout>
        <Card padding="lg">
          <BrandHeader tagline="Dev Mode" />
          <ErrorBanner title="Registration Not Available">
            In dev mode, use the login page to authenticate with a JWT token.
          </ErrorBanner>
          <div className="register-form__footer" style={{ marginTop: 'var(--spacing-6)' }}>
            <TextLink to="/login" centered>Go to Login</TextLink>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  // No auth provider configured
  return (
    <AuthLayout>
      <Card padding="lg">
        <BrandHeader tagline="Configuration Error" />
        <ErrorBanner title="Authentication Not Available">
          VITE_AUTH_PROVIDER must be &apos;dev&apos; or &apos;keycloak&apos;.
          <br />
          Current value: {authProvider || 'not set'}
        </ErrorBanner>
      </Card>
    </AuthLayout>
  );
}
