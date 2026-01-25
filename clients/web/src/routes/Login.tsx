import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { signinRedirect } from '../lib/auth/oidc';
import { useAuth } from '../hooks/useAuth';
import AuthLayout from '../components/auth/AuthLayout';
import BrandHeader from '../components/auth/BrandHeader';
import { Card, Button, TextField, PasswordField, ErrorBanner, Divider, TextLink } from '../components/ui';
import './Login.css';

const ERROR_MESSAGES: Record<string, string> = {
  session_expired: 'Your session has expired. Please sign in again.',
  auth_unavailable: 'Authentication service is temporarily unavailable. Please try again later.',
  auth_failed: 'Authentication failed. Please check your credentials and try again.',
  invalid_credentials: 'The email or password you entered doesn\'t match our records. Please check and try again.',
};

export default function Login() {
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

  // Validate password
  const validatePassword = (value: string): string => {
    if (!value) {
      return 'Password is required';
    }
    return '';
  };

  const handleSignIn = async (e: FormEvent) => {
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
      // For Keycloak mode, we redirect to the OIDC provider
      // Email/password are visual only - actual auth happens on Keycloak
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setFormError('Unable to connect to authentication service. Please try again.');
      setLoading(false);
    }
  };

  const handleCreateAccount = async () => {
    clearErrorParam();
    setLoading(true);
    try {
      // Redirect to Keycloak registration
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setFormError('Unable to connect to authentication service. Please try again.');
      setLoading(false);
    }
  };

  // Keycloak mode - styled login form
  if (authProvider === 'keycloak') {
    return (
      <AuthLayout>
        <Card padding="lg">
          <form
            className={`login-form ${loading ? 'login-form--loading' : ''}`}
            onSubmit={handleSignIn}
            noValidate
          >
            {/* Brand Header */}
            <BrandHeader tagline="Welcome back" />

            {/* Error Banner (form-level errors) */}
            {(errorMessage || formError) && (
              <ErrorBanner title="Unable to sign in">
                {errorMessage || formError}
              </ErrorBanner>
            )}

            {/* Form Fields */}
            <div className="login-form__fields">
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
                autoFocus
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
                error={passwordError}
                disabled={loading}
                autoComplete="current-password"
              />
            </div>

            {/* Actions */}
            <div className="login-form__actions">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                fullWidth
                loading={loading}
                disabled={loading}
              >
                {loading ? 'Signing in...' : 'Sign in'}
              </Button>
              <TextLink to="/forgot-password" centered>
                Forgot password?
              </TextLink>
            </div>

            {/* Divider */}
            <Divider text="or" />

            {/* Create Account Link */}
            <div className="login-form__footer">
              <TextLink onClick={handleCreateAccount} centered>
                Create account
              </TextLink>
            </div>
          </form>
        </Card>
      </AuthLayout>
    );
  }

  // Dev mode - JWT token input (unchanged functionality, styled with tokens)
  if (authProvider === 'dev') {
    return <DevModeLogin />;
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

/**
 * Dev mode login component - separate to keep main Login clean.
 * Uses JWT token pasting for local development.
 */
function DevModeLogin() {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, clearError } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? ERROR_MESSAGES[errorParam] || 'An error occurred.' : null;

  const clearErrorParam = () => {
    if (errorParam) {
      setSearchParams({}, { replace: true });
    }
    clearError();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    clearErrorParam();
    setError('');
    setLoading(true);

    try {
      await login(token.trim());
      navigate('/households');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-dev">
      <h1 className="login-dev__title">Login (Dev Mode)</h1>
      <p className="login-dev__description">
        Paste your JWT token to authenticate in development mode.
      </p>

      {errorMessage && (
        <ErrorBanner title="Authentication Error">
          {errorMessage}
        </ErrorBanner>
      )}

      <form onSubmit={handleSubmit} className="login-dev__form">
        <div>
          <label htmlFor="jwt-token" style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>
            JWT Token
          </label>
          <textarea
            id="jwt-token"
            className="login-dev__textarea"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            disabled={loading}
            required
          />
        </div>

        {error && <p className="login-dev__error">{error}</p>}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          loading={loading}
          disabled={loading || !token.trim()}
        >
          {loading ? 'Logging in...' : 'Login with Token'}
        </Button>
      </form>
    </div>
  );
}
