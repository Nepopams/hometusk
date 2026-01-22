import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signinCallback } from '../lib/auth/oidc';

const AUTH_TOKEN_KEY = 'hometusk_auth_token';

type CallbackStatus = 'processing' | 'success' | 'error';

export default function Callback() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<CallbackStatus>('processing');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function handleCallback() {
      try {
        const user = await signinCallback();

        if (!isMounted) return;

        if (user.access_token) {
          localStorage.setItem(AUTH_TOKEN_KEY, user.access_token);
          setStatus('success');
          navigate('/households', { replace: true });
        } else {
          throw new Error('No access token received');
        }
      } catch (err) {
        if (!isMounted) return;

        const message = err instanceof Error ? err.message : 'Unknown error';
        console.error('[Callback] OIDC callback failed:', message);
        setError(message);
        setStatus('error');
      }
    }

    handleCallback();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  if (status === 'processing') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Processing login...</p>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Login failed: {error}</p>
        <p>
          <a href="/login">Return to login</a>
        </p>
      </div>
    );
  }

  return null;
}
