import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { signinRedirect } from '../lib/auth/oidc';
import { useAuth } from '../hooks/useAuth';

const ERROR_MESSAGES: Record<string, string> = {
  session_expired: 'Session expired, please login again.',
  auth_unavailable: 'Authentication service unavailable. Please try again later.',
  auth_failed: 'Authentication failed. Please try again.',
};

export default function Login() {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isRedirecting, setIsRedirecting] = useState(false);
  const { login, clearError } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const authProvider = import.meta.env.VITE_AUTH_PROVIDER;
  const errorParam = searchParams.get('error');
  const errorMessage = errorParam ? ERROR_MESSAGES[errorParam] || 'An error occurred.' : null;

  const clearErrorParam = () => {
    if (errorParam) {
      setSearchParams({}, { replace: true });
    }
    clearError();
  };

  const handleSignIn = async () => {
    clearErrorParam();
    setIsRedirecting(true);
    try {
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setIsRedirecting(false);
    }
  };

  const handleRegister = async () => {
    clearErrorParam();
    setIsRedirecting(true);
    try {
      await signinRedirect();
    } catch (err) {
      console.error('[Login] OIDC redirect failed', err);
      setIsRedirecting(false);
    }
  };

  if (authProvider === 'keycloak') {
    return (
      <div className="page">
        <h1>HomeTusk</h1>
        {errorMessage && (
          <div
            className="card"
            style={{ marginBottom: '16px', background: '#fff3f3', borderColor: '#ffcdd2' }}
          >
            <p style={{ color: '#c62828', margin: 0 }}>{errorMessage}</p>
          </div>
        )}
        {isRedirecting ? (
          <p>Redirecting to login...</p>
        ) : (
          <div className="card">
            <h2>Welcome back!</h2>
            <button className="button" type="button" onClick={handleSignIn}>
              Sign in
            </button>
            <p style={{ marginTop: '16px' }}>
              Don&apos;t have an account?{' '}
              <button className="ghost-button" type="button" onClick={handleRegister}>
                Sign in to register
              </button>
            </p>
          </div>
        )}
      </div>
    );
  }

  if (authProvider !== 'dev') {
    return (
      <div className="page">
        <h1>Authentication Not Available</h1>
        <p>VITE_AUTH_PROVIDER must be &apos;dev&apos; or &apos;keycloak&apos;.</p>
        <p>Current: {authProvider || 'not set'}</p>
      </div>
    );
  }

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
    <div className="page">
      <h1>Login (Dev Mode)</h1>
      <p>Paste your JWT token to authenticate.</p>

      {errorMessage && (
        <div
          className="card"
          style={{ marginBottom: '16px', background: '#fff3f3', borderColor: '#ffcdd2' }}
        >
          <p style={{ color: '#c62828', margin: 0 }}>{errorMessage}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="card">
        <label>
          JWT Token:
          <textarea
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            rows={6}
            disabled={loading}
            required
          />
        </label>

        {error && <div className="error">{error}</div>}

        <button className="button" type="submit" disabled={loading || !token.trim()}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  );
}
