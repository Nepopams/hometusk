import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useI18n } from '../i18n';
import { createAuthSession } from '../lib/api';
import { signinCallback } from '../lib/auth/oidc';
import { STORAGE_KEYS } from '../lib/constants';

type CallbackStatus = 'processing' | 'success' | 'error';

interface LocationState {
  from?: { pathname: string; search?: string };
}

export default function Callback() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();
  const [status, setStatus] = useState<CallbackStatus>('processing');

  useEffect(() => {
    let isMounted = true;

    async function handleCallback() {
      try {
        const user = await signinCallback();

        if (!isMounted) return;

        if (user.access_token) {
          setStatus('success');
          try {
            await createAuthSession(user.access_token);
          } catch (err) {
            console.warn('[Callback] Failed to create auth session:', err);
          }

          const state = location.state as LocationState | null;
          const stateRedirect = state?.from
            ? state.from.pathname + (state.from.search || '')
            : null;
          const storedRedirect = sessionStorage.getItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);

          sessionStorage.removeItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);

          const redirectTo = stateRedirect || storedRedirect || '/households';
          navigate(redirectTo, { replace: true });
        } else {
          throw new Error('No access token received');
        }
      } catch (err) {
        if (!isMounted) return;

        const message = err instanceof Error ? err.message : 'Unknown error';
        console.error('[Callback] OIDC callback failed:', message);
        setStatus('error');
        sessionStorage.removeItem(STORAGE_KEYS.POST_LOGIN_REDIRECT);
        navigate('/login?error=auth_failed', { replace: true });
      }
    }

    handleCallback();

    return () => {
      isMounted = false;
    };
  }, [navigate, location.state]);

  if (status === 'processing') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>{t('auth.processingLogin')}</p>
      </div>
    );
  }

  return null;
}
