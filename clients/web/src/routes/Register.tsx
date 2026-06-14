import { useState, type FormEvent } from 'react';
import { registerWithPassword } from '../lib/api';
import { signinWithYandex } from '../lib/auth/oidc';
import { useAuth } from '../hooks/useAuth';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { STORAGE_KEYS } from '../lib/constants';
import { ApiError } from '../lib/errors';
import type { AuthErrorResponse } from '../types/api';
import AuthLayout from '../components/auth/AuthLayout';
import BrandHeader from '../components/auth/BrandHeader';
import { Card, Button, TextField, PasswordField, ErrorBanner, Divider, TextLink } from '../components/ui';
import { useI18n, type TranslationKey } from '../i18n';
import './Register.css';

const ERROR_MESSAGE_KEYS: Record<string, TranslationKey> = {
  registration_failed: 'auth.registrationFailed',
  email_exists: 'auth.emailExists',
  auth_unavailable: 'auth.authUnavailable',
};

type RegisterAction = 'password' | 'yandex' | null;

export default function Register() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [formError, setFormError] = useState('');
  const [loadingAction, setLoadingAction] = useState<RegisterAction>(null);
  const { clearError, refetchUser } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? t(ERROR_MESSAGE_KEYS[errorParam] ?? 'auth.genericError') : null;
  const loading = loadingAction !== null;

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

  // Validate password (minimum 8 characters)
  const validatePassword = (value: string): string => {
    if (!value) {
      return t('auth.passwordRequired');
    }
    if (value.length < 8) {
      return t('auth.passwordMin');
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

    setLoadingAction('password');
    try {
      if (authProvider !== 'keycloak') {
        throw new Error(t('auth.unsupportedProvider'));
      }

      await registerWithPassword({
        name: name.trim() || undefined,
        email: email.trim(),
        password,
      });
      const profile = await refetchUser();

      if (!profile) {
        throw new Error(t('auth.profileAfterRegistration'));
      }

      const storedRedirect = sessionStorage.getItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);
      sessionStorage.removeItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);
      navigate(storedRedirect || '/households', { replace: true });
    } catch (err) {
      console.error('[Register] Registration failed', err);
      setFormError(resolveRegisterError(err, t));
      setLoadingAction(null);
    }
  };

  const handleYandexSignIn = async () => {
    clearErrorParam();
    setLoadingAction('yandex');

    try {
      await signinWithYandex();
    } catch (err) {
      console.error('[Register] Yandex sign in failed', err);
      setFormError(t('auth.socialSignInFailed'));
      setLoadingAction(null);
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
            <BrandHeader tagline={t('auth.createYourAccount')} />

            {/* Error Banner (form-level errors) */}
            {(errorMessage || formError) && (
              <ErrorBanner title={t('auth.unableCreateAccount')}>
                {errorMessage || formError}
              </ErrorBanner>
            )}

            {/* Form Fields */}
            <div className="register-form__fields">
              <TextField
                label={t('auth.name')}
                labelSuffix={t('common.optional')}
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t('auth.yourName')}
                disabled={loading}
                autoComplete="name"
                autoFocus
              />
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
                hint={t('auth.passwordHint')}
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
                loading={loadingAction === 'password'}
                disabled={loading}
              >
                {loadingAction === 'password' ? t('auth.creatingAccount') : t('auth.createAccount')}
              </Button>
            </div>

            {/* Divider */}
            <Divider text={t('auth.or')} />

            <div className="register-form__actions">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                fullWidth
                loading={loadingAction === 'yandex'}
                disabled={loading}
                onClick={() => void handleYandexSignIn()}
              >
                {t('auth.continueWithYandex')}
              </Button>
            </div>

            {/* Sign In Link */}
            <div className="register-form__footer">
              <span className="register-form__footer-text">{t('auth.alreadyHaveAccount')}</span>
              <TextLink to="/login">{t('auth.signIn')}</TextLink>
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
          <BrandHeader tagline={t('auth.devMode')} />
          <ErrorBanner title={t('auth.registrationUnavailable')}>
            {t('auth.devRegistrationHint')}
          </ErrorBanner>
          <div className="register-form__footer" style={{ marginTop: 'var(--spacing-6)' }}>
            <TextLink to="/login" centered>{t('auth.goToLogin')}</TextLink>
          </div>
        </Card>
      </AuthLayout>
    );
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

function resolveRegisterError(err: unknown, t: (key: TranslationKey) => string): string {
  if (err instanceof ApiError) {
    const body = err.body as Partial<AuthErrorResponse> | null;
    if (body?.errorCode === 'AUTH_EMAIL_EXISTS' || err.status === 409) {
      return t('auth.emailExists');
    }
    if (body?.errorCode === 'AUTH_PROVIDER_UNAVAILABLE' || err.status === 503) {
      return t('auth.authUnavailable');
    }
    if (err.status === 400) {
      return t('auth.checkRegistration');
    }
  }

  return t('auth.unableCreateAccount');
}
