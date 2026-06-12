import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { loginWithPassword } from '../lib/api';
import { useAuth } from '../hooks/useAuth';
import { STORAGE_KEYS } from '../lib/constants';
import { ApiError } from '../lib/errors';
import type { AuthErrorResponse } from '../types/api';
import AuthLayout from '../components/auth/AuthLayout';
import BrandHeader from '../components/auth/BrandHeader';
import { Card, Button, TextField, PasswordField, ErrorBanner, Divider, TextLink } from '../components/ui';
import { useI18n, type TranslationKey } from '../i18n';
import './Login.css';

const ERROR_MESSAGE_KEYS: Record<string, TranslationKey> = {
  session_expired: 'auth.sessionExpiredParam',
  auth_unavailable: 'auth.authUnavailable',
  auth_failed: 'auth.authFailed',
  invalid_credentials: 'auth.invalidCredentials',
};

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [formError, setFormError] = useState('');
  const [loading, setLoading] = useState(false);
  const { clearError, refetchUser } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? t(ERROR_MESSAGE_KEYS[errorParam] ?? 'auth.genericError') : null;

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
      return t('auth.emailRequired');
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      return t('auth.validEmail');
    }
    return '';
  };

  // Validate password
  const validatePassword = (value: string): string => {
    if (!value) {
      return t('auth.passwordRequired');
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
      if (authProvider !== 'keycloak') {
        throw new Error(t('auth.unsupportedProvider'));
      }

      await loginWithPassword({ email: email.trim(), password });
      const profile = await refetchUser();

      if (!profile) {
        throw new Error(t('auth.profileAfterSignIn'));
      }

      const storedRedirect = sessionStorage.getItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);
      sessionStorage.removeItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);
      navigate(storedRedirect || '/households', { replace: true });
    } catch (err) {
      console.error('[Login] Sign in failed', err);
      setFormError(resolveLoginError(err, t));
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
            <BrandHeader tagline={t('auth.welcomeBack')} />

            {/* Error Banner (form-level errors) */}
            {(errorMessage || formError) && (
              <ErrorBanner title={t('auth.unableSignIn')}>
                {errorMessage || formError}
              </ErrorBanner>
            )}

            {/* Form Fields */}
            <div className="login-form__fields">
              <TextField
                label={t('auth.email')}
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
                label={t('auth.password')}
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
                {loading ? t('auth.signingIn') : t('auth.signIn')}
              </Button>
            </div>

            {/* Divider */}
            <Divider text={t('auth.or')} />

            {/* Create Account Link */}
            <div className="login-form__footer">
              <TextLink to="/register" centered>
                {t('auth.createAccount')}
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
        <BrandHeader tagline={t('auth.configurationError')} />
        <ErrorBanner title={t('auth.authenticationNotAvailable')}>
          {t('auth.providerRequired')}
          <br />
          {t('auth.currentValue', { value: authProvider || t('auth.notSet') })}
        </ErrorBanner>
      </Card>
    </AuthLayout>
  );
}

function resolveLoginError(err: unknown, t: (key: TranslationKey) => string): string {
  if (err instanceof ApiError) {
    const body = err.body as Partial<AuthErrorResponse> | null;
    if (body?.errorCode === 'AUTH_INVALID_CREDENTIALS' || err.status === 401) {
      return t('auth.invalidCredentials');
    }
    if (body?.errorCode === 'AUTH_PROVIDER_UNAVAILABLE' || err.status === 503) {
      return t('auth.authUnavailable');
    }
  }

  return t('auth.unableSignInTry');
}

/**
 * Dev mode login component - separate to keep main Login clean.
 * Uses JWT token pasting for local development.
 */
function DevModeLogin() {
  const { t } = useI18n();
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, clearError } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? t(ERROR_MESSAGE_KEYS[errorParam] ?? 'auth.genericError') : null;

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
      setError(err instanceof Error ? err.message : t('auth.loginFailed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-dev">
      <h1 className="login-dev__title">{t('auth.loginDevMode')}</h1>
      <p className="login-dev__description">
        {t('auth.jwtDescription')}
      </p>

      {errorMessage && (
        <ErrorBanner title={t('auth.unableSignIn')}>
          {errorMessage}
        </ErrorBanner>
      )}

      <form onSubmit={handleSubmit} className="login-dev__form">
        <div>
          <label htmlFor="jwt-token" style={{ display: 'block', marginBottom: '8px', fontWeight: 500 }}>
            {t('auth.jwtToken')}
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
          {loading ? t('auth.loggingIn') : t('auth.loginWithToken')}
        </Button>
      </form>
    </div>
  );
}
